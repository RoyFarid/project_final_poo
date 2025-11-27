package com.whatsapp.ui;

import com.whatsapp.model.Room;
import com.whatsapp.model.Usuario;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.network.observer.NetworkEventObserver;
import com.whatsapp.service.ChatService;
import com.whatsapp.service.ControlService;
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
    private final Map<String, String> pendingFileDownloads = new HashMap<>(); // fileName -> filePath
    private final Map<String, String> fileNameToOriginal = new HashMap<>(); // fileName con timestamp -> nombre original
    private boolean isServerMode;
    private final ControlService controlService;

    public RoomChatView(Usuario currentUser, Room room, NetworkFacade networkFacade, boolean isServerMode) {
        this.currentUser = currentUser;
        this.room = room;
        this.networkFacade = networkFacade;
        this.roomService = RoomService.getInstance();
        this.messagesList = new ListView<>();
        this.membersList = new ListView<>();
        this.aliasRegistry = UserAliasRegistry.getInstance();
        this.isServerMode = isServerMode;
        this.controlService = new ControlService();

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
        messagesList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) { // Doble click para descargar
                String selected = messagesList.getSelectionModel().getSelectedItem();
                if (selected != null && selected.contains("📎")) {
                    handleFileDownload(selected);
                }
            }
        });

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
            String encodedMessage = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
            String roomIdEncoded = Base64.getEncoder().encodeToString(String.valueOf(room.getId()).getBytes(StandardCharsets.UTF_8));
            String senderId = isServerMode ? "SERVER_" + currentUser.getUsername() : currentUser.getUsername();
            aliasRegistry.registerAlias(senderId, currentUser.getUsername());
            String payload = roomIdEncoded + "|" + Base64.getEncoder().encodeToString(senderId.getBytes(StandardCharsets.UTF_8)) + "|" + encodedMessage;
            if (isServerMode) {
                for (String memberConnectionId : getDeliverableMembers()) {
                    controlService.sendControlMessage(memberConnectionId, ControlService.CONTROL_ROOM_MESSAGE, payload);
                }
            } else {
                String serverConnectionId = networkFacade.getPrimaryConnectionId();
                if (serverConnectionId != null) {
                    controlService.sendControlMessage(serverConnectionId, ControlService.CONTROL_ROOM_MESSAGE, payload);
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
                long fileSize = file.length();
                String fileName = file.getName();
                
                // Enviar notificación de archivo como mensaje de sala
                String roomIdEncoded = Base64.getEncoder().encodeToString(String.valueOf(room.getId()).getBytes(StandardCharsets.UTF_8));
                String senderId = isServerMode ? "SERVER_" + currentUser.getUsername() : currentUser.getUsername();
                aliasRegistry.registerAlias(senderId, currentUser.getUsername());
                String fileNameEncoded = Base64.getEncoder().encodeToString(fileName.getBytes(StandardCharsets.UTF_8));
                String fileSizeEncoded = Base64.getEncoder().encodeToString(String.valueOf(fileSize).getBytes(StandardCharsets.UTF_8));
                String senderIdEncoded = Base64.getEncoder().encodeToString(senderId.getBytes(StandardCharsets.UTF_8));
                
                String payload = roomIdEncoded + "|" + senderIdEncoded + "|" + fileNameEncoded + "|" + fileSizeEncoded + "|";
                
                if (isServerMode) {
                    // Notificar a todos los miembros
                    for (String memberConnectionId : getDeliverableMembers()) {
                        controlService.sendControlMessage(memberConnectionId, ControlService.CONTROL_ROOM_FILE, payload);
                    }
                    // También enviar el archivo físico
                    for (String memberConnectionId : getDeliverableMembers()) {
                        networkFacade.sendFile(memberConnectionId, memberConnectionId, file.getAbsolutePath(), currentUser.getId());
                    }
                } else {
                    // Si es cliente, usar el servidor como intermediario
                    String serverConnectionId = networkFacade.getPrimaryConnectionId();
                    if (serverConnectionId != null) {
                        controlService.sendControlMessage(serverConnectionId, ControlService.CONTROL_ROOM_FILE, payload);
                        // También enviar el archivo físico
                        for (String memberConnectionId : getDeliverableMembers()) {
                            networkFacade.sendFile(serverConnectionId, memberConnectionId, file.getAbsolutePath(), currentUser.getId());
                        }
                    }
                }
                
                // Mostrar en el chat local
                Platform.runLater(() -> {
                    addFileMessage(currentUser.getUsername(), fileName, fileSize, null, true);
                });
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

    private void addFileMessage(String senderName, String fileName, long fileSize, String filePath, boolean isSent) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        );
        String sizeStr = formatFileSize(fileSize);
        String prefix = isSent ? "Yo" : senderName;
        String message;
        if (filePath != null && !isSent) {
            message = String.format("[%s] %s: 📎 %s (%s) [Disponible para descargar]", timestamp, prefix, fileName, sizeStr);
            // Guardar información del archivo para descarga
            pendingFileDownloads.put(fileName, filePath);
        } else {
            message = String.format("[%s] %s: 📎 %s (%s)", timestamp, prefix, fileName, sizeStr);
        }
        messagesList.getItems().add(message);
        messagesList.scrollTo(messagesList.getItems().size() - 1);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private void refreshFileMessage(String fileName, String filePath) {
        // Buscar el mensaje que contiene este archivo y actualizarlo si es necesario
        for (int i = messagesList.getItems().size() - 1; i >= 0; i--) {
            String item = messagesList.getItems().get(i);
            // Buscar mensaje de archivo que contenga este nombre (parcial o completo)
            if (item.contains("📎") && (item.contains(fileName) || fileName.contains(extractFileNameFromMessage(item)))) {
                if (!item.contains("[Disponible para descargar]")) {
                    String updated = item + " [Disponible para descargar]";
                    messagesList.getItems().set(i, updated);
                }
                break;
            }
        }
    }
    
    private String extractFileNameFromMessage(String message) {
        // Extraer el nombre del archivo del mensaje
        // Formato: "[HH:mm] Sender: 📎 fileName (size)"
        if (!message.contains("📎")) {
            return "";
        }
        int fileIconIndex = message.indexOf("📎");
        int startIndex = fileIconIndex + 2; // Después del emoji y espacio
        int endIndex = message.indexOf(" (", startIndex);
        if (endIndex == -1) {
            endIndex = message.indexOf(" [", startIndex);
        }
        if (endIndex == -1) {
            endIndex = message.length();
        }
        return message.substring(startIndex, endIndex).trim();
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
                        String senderId = event.getSource();
                        if (memberConnectionIds.containsKey(senderId) || senderId.equals(isServerMode ? "SERVER_" + currentUser.getUsername() : currentUser.getUsername())) {
                            if (progress.isIncoming()) {
                                // Guardar la ruta del archivo recibido para poder descargarlo
                                if (progress.getLocalPath() != null) {
                                    String fileName = progress.getFileName(); // Nombre con timestamp
                                    String baseFileName = fileName;
                                    // Extraer nombre original (sin timestamp)
                                    if (fileName.matches("^\\d+_.+")) {
                                        baseFileName = fileName.substring(fileName.indexOf('_') + 1);
                                        fileNameToOriginal.put(fileName, baseFileName);
                                    }
                                    
                                    // Guardar con ambos nombres
                                    pendingFileDownloads.put(fileName, progress.getLocalPath());
                                    pendingFileDownloads.put(baseFileName, progress.getLocalPath());
                                    
                                    // Actualizar el mensaje si ya existe uno para este archivo
                                    refreshFileMessage(baseFileName, progress.getLocalPath());
                                    refreshFileMessage(fileName, progress.getLocalPath());
                                }
                            } else {
                                // Archivo enviado - ya se muestra en sendFileToAll
                            }
                        }
                    }
                }
                case ROOM_FILE -> {
                    if (event.getData() instanceof ControlService.RoomFileMessage roomFileMsg
                        && roomFileMsg.getRoomId().equals(room.getId())) {
                        String senderId = roomFileMsg.getSenderConnectionId();
                        memberConnectionIds.put(senderId, aliasRegistry.getAliasOrDefault(senderId));
                        refreshMembersList();
                        String senderName = aliasRegistry.getAliasOrDefault(senderId);
                        boolean isMine = senderId.equals(isServerMode ? "SERVER_" + currentUser.getUsername() : currentUser.getUsername());
                        String originalFileName = roomFileMsg.getFileName();
                        addFileMessage(senderName, originalFileName, roomFileMsg.getFileSize(), 
                                     roomFileMsg.getFilePath(), isMine);
                        // Si el archivo ya está disponible, asociarlo
                        if (roomFileMsg.getFilePath() != null) {
                            pendingFileDownloads.put(originalFileName, roomFileMsg.getFilePath());
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
                case ROOM_MESSAGE -> {
                    if (event.getData() instanceof ControlService.RoomChatMessage roomMsg
                        && roomMsg.getRoomId().equals(room.getId())) {
                        String senderId = roomMsg.getSenderConnectionId();
                        memberConnectionIds.put(senderId, aliasRegistry.getAliasOrDefault(senderId));
                        refreshMembersList();
                        addMessage(aliasRegistry.getAliasOrDefault(senderId) + ": " + roomMsg.getMessage());
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


    private void handleFileDownload(String messageText) {
        // Extraer el nombre del archivo del mensaje
        // Formato: "[HH:mm] Sender: 📎 fileName (size) [Disponible para descargar]"
        if (!messageText.contains("📎")) {
            return;
        }
        
        try {
            String fileName = extractFileNameFromMessage(messageText);
            if (fileName.isEmpty()) {
                showAlert("Error", "No se pudo identificar el nombre del archivo.", Alert.AlertType.WARNING);
                return;
            }
            
            // Buscar el archivo en los pendientes (intentar con nombre completo y base)
            String filePath = pendingFileDownloads.get(fileName);
            if (filePath == null) {
                // Intentar con nombre base (sin timestamp si tiene)
                String baseFileName = fileName;
                if (fileName.matches("^\\d+_.+")) {
                    baseFileName = fileName.substring(fileName.indexOf('_') + 1);
                    filePath = pendingFileDownloads.get(baseFileName);
                }
            }
            
            // Buscar en todos los valores del mapa por nombre parcial
            if (filePath == null) {
                for (Map.Entry<String, String> entry : pendingFileDownloads.entrySet()) {
                    if (entry.getKey().contains(fileName) || fileName.contains(entry.getKey())) {
                        filePath = entry.getValue();
                        break;
                    }
                }
            }
            
            if (filePath == null || !new File(filePath).exists()) {
                showAlert("Error", "El archivo no está disponible para descargar. Espere a que se complete la transferencia.", Alert.AlertType.WARNING);
                return;
            }
            
            // Pedir al usuario dónde guardar el archivo
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Selecciona la carpeta para guardar: " + fileName);
            File selectedDir = dirChooser.showDialog(getScene().getWindow());
            
            if (selectedDir == null) {
                return; // Usuario canceló
            }
            
            File sourceFile = new File(filePath);
            File destination = new File(selectedDir, fileName);
            
            // Copiar el archivo
            Path destPath = destination.toPath();
            Path parent = destPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(sourceFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
            
            showAlert("Éxito", "Archivo guardado en: " + destPath, Alert.AlertType.INFORMATION);
            addMessage("✓ Archivo descargado: " + fileName);
            
        } catch (Exception e) {
            showAlert("Error", "No se pudo descargar el archivo: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}

