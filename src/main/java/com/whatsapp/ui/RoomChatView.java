package com.whatsapp.ui;

import com.whatsapp.model.Room;
import com.whatsapp.model.Usuario;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.network.observer.NetworkEventObserver;
import com.whatsapp.service.ChatService;
import com.whatsapp.service.NetworkFacade;
import com.whatsapp.service.RoomService;
import com.whatsapp.service.UserAliasRegistry;
import com.whatsapp.service.VideoStreamService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Vista de chat para rooms/grupos.
 * Permite al servidor participar en rooms enviando mensajes, archivos, video y audio a todos los miembros.
 * Patrón: Observer (para eventos de red)
 */
public class RoomChatView extends BorderPane implements NetworkEventObserver {
    private final Usuario currentUser;
    private final Room room;
    private final NetworkFacade networkFacade;
    private final RoomService roomService;
    private final ListView<String> messagesList;
    private final ListView<String> membersList;
    private ImageView remoteVideoView;
    private Label videoStatusLabel;
    private TextField messageField;
    private Label statusLabel;
    private ToggleButton muteMicButton;
    private ToggleButton muteSpeakerButton;
    private final UserAliasRegistry aliasRegistry;
    private final Map<String, String> memberConnectionIds = new HashMap<>();
    private boolean isServerMode;

    public RoomChatView(Usuario currentUser, Room room, NetworkFacade networkFacade, boolean isServerMode) {
        this.currentUser = currentUser;
        this.room = room;
        this.networkFacade = networkFacade;
        this.roomService = RoomService.getInstance();
        this.messagesList = new ListView<>();
        this.membersList = new ListView<>();
        this.aliasRegistry = UserAliasRegistry.getInstance();
        this.isServerMode = isServerMode;

        // Cargar miembros del room
        loadRoomMembers();

        EventAggregator.getInstance().subscribe(this);
        setupUI();
    }

    private void loadRoomMembers() {
        Optional<Room> roomOpt = roomService.getRoom(room.getId());
        if (roomOpt.isPresent()) {
            Room currentRoom = roomOpt.get();
            for (String memberId : currentRoom.getMembers()) {
                String displayName = aliasRegistry.getAliasOrDefault(memberId);
                memberConnectionIds.put(memberId, displayName);
            }
            refreshMembersList();
        }
    }

    private void setupUI() {
        // Panel superior - Información del room
        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(10));
        topBox.setStyle("-fx-background-color: #128C7E;");

        Label roomLabel = new Label("Room: " + room.getName() + " (" + memberConnectionIds.size() + " miembros)");
        roomLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        statusLabel = new Label("Conectado");
        statusLabel.setStyle("-fx-text-fill: white;");

        topBox.getChildren().addAll(roomLabel, statusLabel);
        setTop(topBox);

        // Panel central - Mensajes, miembros y video
        HBox centerBox = new HBox(10);
        centerBox.setPadding(new Insets(10));

        // Lista de mensajes
        messagesList.setPrefWidth(400);
        messagesList.setPrefHeight(350);
        messagesList.setStyle("-fx-font-size: 12px;");

        // Lista de miembros
        VBox membersBox = new VBox(5);
        membersBox.setPrefWidth(200);
        Label membersLabel = new Label("Miembros:");
        membersLabel.setFont(javafx.scene.text.Font.font(12));
        membersList.setPrefHeight(200);
        membersBox.getChildren().addAll(membersLabel, membersList);

        // Video
        remoteVideoView = new ImageView();
        remoteVideoView.setFitWidth(260);
        remoteVideoView.setPreserveRatio(true);
        remoteVideoView.setStyle("-fx-border-color: #ccc; -fx-background-color: #000;");

        videoStatusLabel = new Label("Video: sin señal");
        videoStatusLabel.setStyle("-fx-text-fill: #555;");

        VBox videoBox = new VBox(6, new Label("Video remoto"), remoteVideoView, videoStatusLabel);
        videoBox.setPadding(new Insets(5));
        videoBox.setPrefWidth(280);

        centerBox.getChildren().addAll(messagesList, membersBox, videoBox);
        setCenter(centerBox);

        // Panel inferior - Controles
        VBox bottomBox = new VBox(10);
        bottomBox.setPadding(new Insets(10));

        HBox messageBox = new HBox(10);
        messageField = new TextField();
        messageField.setPromptText("Escribe un mensaje para todos...");
        messageField.setPrefWidth(400);
        messageField.setOnAction(e -> sendMessageToAll());

        Button sendButton = new Button("Enviar a Todos");
        sendButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white;");
        sendButton.setOnAction(e -> sendMessageToAll());
        messageBox.getChildren().addAll(messageField, sendButton);

        HBox actionBox = new HBox(10);
        Button fileButton = new Button("Enviar Archivo a Todos");
        fileButton.setStyle("-fx-background-color: #128C7E; -fx-text-fill: white;");
        fileButton.setOnAction(e -> sendFileToAll());

        Button videoButton = new Button("Iniciar Video/Audio");
        videoButton.setStyle("-fx-background-color: #128C7E; -fx-text-fill: white;");
        videoButton.setOnAction(e -> startVideoAudioToAll());

        Button stopVideoButton = new Button("Detener Video/Audio");
        stopVideoButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        stopVideoButton.setOnAction(e -> stopVideoAudio());

        muteMicButton = new ToggleButton("Silenciar micrófono");
        muteMicButton.setOnAction(e -> toggleMicrophone());

        muteSpeakerButton = new ToggleButton("Silenciar altavoz");
        muteSpeakerButton.setOnAction(e -> toggleSpeaker());

        actionBox.getChildren().addAll(
            fileButton,
            videoButton,
            stopVideoButton,
            muteMicButton,
            muteSpeakerButton
        );
        bottomBox.getChildren().addAll(messageBox, actionBox);
        setBottom(bottomBox);

        refreshMembersList();
    }

    private void sendMessageToAll() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        try {
            // Enviar mensaje a todos los miembros del room
            for (String memberConnectionId : getDeliverableMembers()) {
                if (isServerMode) {
                    // Si es servidor, enviar directamente
                    String encodedMessage = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
                    String payload = "FROM:" + currentUser.getUsername() + "|" + encodedMessage;
                    networkFacade.sendMessage(memberConnectionId, payload, currentUser.getId(), memberConnectionId);
                } else {
                    // Si es cliente, usar el formato TO: para que el servidor lo reenvíe
                    String serverConnectionId = networkFacade.getPrimaryConnectionId();
                    if (serverConnectionId != null) {
                        String encodedMessage = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
                        String payload = "TO:" + memberConnectionId + "|" + encodedMessage;
                        networkFacade.sendMessage(serverConnectionId, payload, currentUser.getId(), memberConnectionId);
                    }
                }
            }

            Platform.runLater(() -> {
                addMessage("Yo: " + message);
                messageField.clear();
            });
        } catch (Exception e) {
            showAlert("Error", "No se pudo enviar el mensaje: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void sendFileToAll() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo para enviar a todos");
        File file = fileChooser.showOpenDialog((Stage) getScene().getWindow());

        if (file != null) {
            try {
                // Enviar archivo a todos los miembros del room
                for (String memberConnectionId : getDeliverableMembers()) {
                    if (isServerMode) {
                        // Si es servidor, enviar directamente
                        networkFacade.sendFile(memberConnectionId, memberConnectionId, file.getAbsolutePath(), currentUser.getId());
                    } else {
                        // Si es cliente, usar el servidor como intermediario
                        String serverConnectionId = networkFacade.getPrimaryConnectionId();
                        if (serverConnectionId != null) {
                            networkFacade.sendFile(serverConnectionId, memberConnectionId, file.getAbsolutePath(), currentUser.getId());
                        }
                    }
                }
                addMessage("Archivo enviado a todos los miembros: " + file.getName());
            } catch (Exception e) {
                showAlert("Error", "No se pudo enviar el archivo: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void startVideoAudioToAll() {
        try {
            // Iniciar video y audio para todos los miembros del room
            for (String memberConnectionId : getDeliverableMembers()) {
                if (isServerMode) {
                    // Si es servidor, iniciar directamente
                    networkFacade.startVideoCall(memberConnectionId, memberConnectionId);
                } else {
                    // Si es cliente, usar el servidor como intermediario
                    String serverConnectionId = networkFacade.getPrimaryConnectionId();
                    if (serverConnectionId != null) {
                        networkFacade.startVideoCall(serverConnectionId, memberConnectionId);
                    }
                }
            }
            addMessage("Video y audio iniciados para todos los miembros");
        } catch (Exception e) {
            showAlert("Error", "No se pudo iniciar video/audio: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void stopVideoAudio() {
        try {
            networkFacade.stopVideoCall();
            addMessage("Video y audio detenidos");
        } catch (Exception e) {
            showAlert("Error", "No se pudo detener video/audio: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Retorna los connectionIds a los que sí debemos enviar (omite IDs pseudo de servidor y desconectados).
     */
    private List<String> getDeliverableMembers() {
        List<String> recipients = new ArrayList<>();
        Set<String> connected = networkFacade.getConnectedClients();
        for (String memberId : memberConnectionIds.keySet()) {
            if (memberId == null) {
                continue;
            }
            // Omitir el alias virtual del servidor
            if (memberId.startsWith("SERVER_")) {
                continue;
            }
            // En modo servidor, solo enviar a conexiones activas
            if (isServerMode && (connected == null || !connected.contains(memberId))) {
                continue;
            }
            recipients.add(memberId);
        }
        return recipients;
    }

    private void addMessage(String message) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        );
        messagesList.getItems().add("[" + timestamp + "] " + message);
        messagesList.scrollTo(messagesList.getItems().size() - 1);
    }

    private void refreshMembersList() {
        membersList.getItems().clear();
        for (String displayName : memberConnectionIds.values()) {
            membersList.getItems().add(displayName);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void toggleMicrophone() {
        boolean muted = muteMicButton.isSelected();
        muteMicButton.setText(muted ? "Activar micrófono" : "Silenciar micrófono");
        try {
            networkFacade.setMicrophoneMuted(muted);
        } catch (Exception e) {
            showAlert("Error", "No se pudo cambiar el estado del micrófono: " + e.getMessage(), Alert.AlertType.ERROR);
            muteMicButton.setSelected(!muted);
            muteMicButton.setText(!muted ? "Activar micrófono" : "Silenciar micrófono");
        }
    }

    private void toggleSpeaker() {
        boolean muted = muteSpeakerButton.isSelected();
        muteSpeakerButton.setText(muted ? "Activar altavoz" : "Silenciar altavoz");
        try {
            networkFacade.setSpeakerMuted(muted);
        } catch (Exception e) {
            showAlert("Error", "No se pudo cambiar el estado del altavoz: " + e.getMessage(), Alert.AlertType.ERROR);
            muteSpeakerButton.setSelected(!muted);
            muteSpeakerButton.setText(!muted ? "Activar altavoz" : "Silenciar altavoz");
        }
    }

    @Override
    public void onNetworkEvent(NetworkEvent event) {
        Platform.runLater(() -> {
            switch (event.getType()) {
                case MESSAGE_RECEIVED -> {
                    if (event.getData() instanceof ChatService.ChatMessage msg) {
                        // Verificar si el mensaje viene de un miembro del room
                        if (memberConnectionIds.containsKey(msg.getSource())) {
                            String displayName = memberConnectionIds.get(msg.getSource());
                            addMessage(displayName + ": " + msg.getMessage());
                        }
                    }
                }
                case FILE_PROGRESS -> {
                    if (event.getData() instanceof com.whatsapp.service.FileTransferService.FileProgress progress
                        && progress.getProgress() >= 100.0) {
                        if (memberConnectionIds.containsKey(event.getSource())) {
                            String displayName = memberConnectionIds.get(event.getSource());
                            if (progress.isIncoming()) {
                                handleIncomingFile(progress, displayName);
                            } else {
                                addMessage("Yo: Archivo enviado - " + progress.getFileName());
                            }
                        }
                    }
                }
                case VIDEO_FRAME -> {
                    if (event.getData() instanceof VideoStreamService.VideoFramePayload framePayload) {
                        String peerId = framePayload.getPeerId();
                        if (memberConnectionIds.containsKey(peerId)) {
                            showVideoFrame(framePayload.getData());
                        }
                    }
                }
                case ROOM_MEMBER_ADDED -> {
                    if (event.getData() instanceof RoomService.RoomMemberEvent memberEvent
                        && memberEvent.getRoomId().equals(room.getId())) {
                        loadRoomMembers();
                        addMessage("Nuevo miembro unido al room");
                    }
                }
                case ROOM_MEMBER_REMOVED -> {
                    if (event.getData() instanceof RoomService.RoomMemberEvent memberEvent
                        && memberEvent.getRoomId().equals(room.getId())) {
                        memberConnectionIds.remove(memberEvent.getConnectionId());
                        refreshMembersList();
                        addMessage("Miembro salió del room");
                    }
                }
                default -> { }
            }
        });
    }

    private void showVideoFrame(byte[] data) {
        try {
            Image image = new Image(new ByteArrayInputStream(data));
            remoteVideoView.setImage(image);
            videoStatusLabel.setText("Video: recibiendo");
            videoStatusLabel.setStyle("-fx-text-fill: #25D366;");
        } catch (Exception e) {
            videoStatusLabel.setText("Video: error al decodificar");
            videoStatusLabel.setStyle("-fx-text-fill: #dc3545;");
        }
    }

    private void handleIncomingFile(com.whatsapp.service.FileTransferService.FileProgress progress, String senderName) {
        String localPath = progress.getLocalPath();
        if (localPath == null) {
            addMessage(senderName + ": Archivo recibido - " + progress.getFileName());
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Selecciona la carpeta para guardar el archivo");
        File selectedDir = dirChooser.showDialog(getScene().getWindow());

        if (selectedDir == null) {
            addMessage(senderName + ": Archivo recibido - " + progress.getFileName()
                + " (aún en " + localPath + ")");
            return;
        }

        File destination = new File(selectedDir, progress.getFileName());

        try {
            Path destPath = destination.toPath();
            Path parent = destPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(Paths.get(localPath), destPath, StandardCopyOption.REPLACE_EXISTING);
            addMessage(senderName + ": Archivo guardado en " + destPath);
            Files.deleteIfExists(Paths.get(localPath));
        } catch (IOException e) {
            showAlert("Error", "No se pudo guardar el archivo: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}

