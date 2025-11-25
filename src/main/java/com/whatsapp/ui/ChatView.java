package com.whatsapp.ui;

import com.whatsapp.command.CommandInvoker;
import com.whatsapp.command.SendFileCommand;
import com.whatsapp.command.SendMessageCommand;
import com.whatsapp.command.StartVideoCallCommand;
import com.whatsapp.model.Usuario;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.network.observer.NetworkEventObserver;
import com.whatsapp.service.ChatService;
import com.whatsapp.service.NetworkFacade;
import com.whatsapp.service.UserAliasRegistry;
import com.whatsapp.service.VideoStreamService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

public class ChatView extends BorderPane implements NetworkEventObserver {
    private final Usuario currentUser;
    private final String connectionId;
    private final String serverConnectionId;
    private final NetworkFacade networkFacade;
    private final CommandInvoker commandInvoker;
    private final ListView<String> messagesList;
    private ImageView remoteVideoView;
    private Label videoStatusLabel;
    private TextField messageField;
    private Label statusLabel;
    private ToggleButton muteMicButton;
    private ToggleButton muteSpeakerButton;
    private final UserAliasRegistry aliasRegistry;

    public ChatView(Usuario currentUser, String connectionId, String serverConnectionId, NetworkFacade networkFacade) {
        this.currentUser = currentUser;
        this.connectionId = connectionId;
        this.serverConnectionId = serverConnectionId;
        this.networkFacade = networkFacade;
        this.commandInvoker = new CommandInvoker();
        this.messagesList = new ListView<>();
        this.aliasRegistry = UserAliasRegistry.getInstance();

        EventAggregator.getInstance().subscribe(this);
        setupUI();
    }

    private void setupUI() {
        // Panel superior - Información del chat
        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(10));
        topBox.setStyle("-fx-background-color: #128C7E;");

        Label chatLabel = new Label("Chat con: " + getPeerDisplayName());
        chatLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        statusLabel = new Label("Conectado");
        statusLabel.setStyle("-fx-text-fill: white;");

        topBox.getChildren().addAll(chatLabel, statusLabel);
        setTop(topBox);

        // Panel central - Mensajes + video
        messagesList.setPrefHeight(350);
        messagesList.setStyle("-fx-font-size: 12px;");

        remoteVideoView = new ImageView();
        remoteVideoView.setFitWidth(260);
        remoteVideoView.setPreserveRatio(true);
        remoteVideoView.setStyle("-fx-border-color: #ccc; -fx-background-color: #000;");

        videoStatusLabel = new Label("Video: sin señal");
        videoStatusLabel.setStyle("-fx-text-fill: #555;");

        VBox videoBox = new VBox(6, new Label("Video remoto"), remoteVideoView, videoStatusLabel);
        videoBox.setPadding(new Insets(5));
        videoBox.setPrefWidth(280);

        HBox centerBox = new HBox(10, messagesList, videoBox);
        centerBox.setPadding(new Insets(0, 10, 0, 10));
        setCenter(centerBox);

        // Panel inferior - Controles
        VBox bottomBox = new VBox(10);
        bottomBox.setPadding(new Insets(10));

        HBox messageBox = new HBox(10);
        messageField = new TextField();
        messageField.setPromptText("Escribe un mensaje...");
        messageField.setPrefWidth(400);
        messageField.setOnAction(e -> sendMessage());

        Button sendButton = new Button("Enviar");
        sendButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white;");
        sendButton.setOnAction(e -> sendMessage());
        messageBox.getChildren().addAll(messageField, sendButton);

        HBox actionBox = new HBox(10);
        Button fileButton = new Button("Enviar Archivo");
        fileButton.setStyle("-fx-background-color: #128C7E; -fx-text-fill: white;");
        fileButton.setOnAction(e -> sendFile());

        Button videoButton = new Button("Videollamada");
        videoButton.setStyle("-fx-background-color: #128C7E; -fx-text-fill: white;");
        videoButton.setOnAction(e -> startVideoCall());

        Button stopVideoButton = new Button("Terminar Videollamada");
        stopVideoButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        stopVideoButton.setOnAction(e -> stopVideoCall());

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
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        try {
            if (serverConnectionId == null) {
                showAlert("Error", "No hay conexión activa con el servidor.", Alert.AlertType.ERROR);
                return;
            }

            String encodedMessage = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
            String payload = "TO:" + connectionId + "|" + encodedMessage;

            SendMessageCommand command = new SendMessageCommand(
                networkFacade,
                serverConnectionId,
                payload,
                currentUser.getId(),
                connectionId
            );
            commandInvoker.executeCommand(command);

            Platform.runLater(() -> {
                addMessage("Yo: " + message);
                messageField.clear();
            });
        } catch (Exception e) {
            showAlert("Error", "No se pudo enviar el mensaje: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void sendFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo para enviar");
        File file = fileChooser.showOpenDialog((Stage) getScene().getWindow());

        if (file != null) {
            try {
                SendFileCommand command = new SendFileCommand(
                    networkFacade,
                    serverConnectionId,
                    connectionId,
                    file.getAbsolutePath(),
                    currentUser.getId()
                );
                commandInvoker.executeCommand(command);
            } catch (Exception e) {
                showAlert("Error", "No se pudo enviar el archivo: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void startVideoCall() {
        try {
            if (serverConnectionId == null) {
                showAlert("Error", "No hay conexión activa con el servidor.", Alert.AlertType.ERROR);
                return;
            }

            StartVideoCallCommand command = new StartVideoCallCommand(
                networkFacade,
                serverConnectionId,
                connectionId
            );
            commandInvoker.executeCommand(command);

            addMessage("Videollamada iniciada");
        } catch (Exception e) {
            showAlert("Error", "No se pudo iniciar la videollamada: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void addMessage(String message) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        );
        messagesList.getItems().add("[" + timestamp + "] " + message);
        messagesList.scrollTo(messagesList.getItems().size() - 1);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void onNetworkEvent(NetworkEvent event) {
        Platform.runLater(() -> {
            switch (event.getType()) {
                case MESSAGE_RECEIVED -> {
                    if (event.getData() instanceof ChatService.ChatMessage msg
                        && msg.getSource().equals(connectionId)) {
                        addMessage(getPeerDisplayName() + ": " + msg.getMessage());
                    }
                }
                case FILE_PROGRESS -> {
                    if (!connectionId.equals(event.getSource())) {
                        return;
                    }
                    if (event.getData() instanceof com.whatsapp.service.FileTransferService.FileProgress progress
                        && progress.getProgress() >= 100.0) {
                        if (progress.isIncoming()) {
                            handleIncomingFile(progress);
                        } else {
                            addMessage("Yo: Archivo enviado - " + progress.getFileName());
                        }
                    }
                }
                case VIDEO_FRAME -> {
                    if (event.getData() instanceof VideoStreamService.VideoFramePayload framePayload
                        && framePayload.getPeerId().equals(connectionId)) {
                        showVideoFrame(framePayload.getData());
                    }
                }
                case DISCONNECTED -> {
                    if (event.getData().toString().equals(connectionId)) {
                        statusLabel.setText("Desconectado");
                        statusLabel.setStyle("-fx-text-fill: red;");
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

    private void stopVideoCall() {
        try {
            networkFacade.stopVideoCall();
            videoStatusLabel.setText("Video: detenido");
            videoStatusLabel.setStyle("-fx-text-fill: #dc3545;");
            addMessage("Videollamada finalizada");
        } catch (Exception e) {
            showAlert("Error", "No se pudo detener la videollamada: " + e.getMessage(), Alert.AlertType.ERROR);
        }
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

    private void handleIncomingFile(com.whatsapp.service.FileTransferService.FileProgress progress) {
        String localPath = progress.getLocalPath();
        if (localPath == null) {
            addMessage(getPeerDisplayName() + ": Archivo recibido - " + progress.getFileName());
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Selecciona la carpeta para guardar el archivo");
        File selectedDir = dirChooser.showDialog(getScene().getWindow());

        if (selectedDir == null) {
            addMessage(getPeerDisplayName() + ": Archivo recibido - " + progress.getFileName()
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
            addMessage(getPeerDisplayName() + ": Archivo guardado en " + destPath);
            Files.deleteIfExists(Paths.get(localPath));
        } catch (IOException e) {
            showAlert("Error", "No se pudo guardar el archivo: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String getPeerDisplayName() {
        return aliasRegistry.getAliasOrDefault(connectionId);
    }
}
