package com.whatsapp.ui;

import com.whatsapp.model.Usuario;
import com.whatsapp.service.ControlService;
import com.whatsapp.service.NetworkFacade;
import com.whatsapp.service.UserAliasRegistry;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;

/**
 * Vista sencilla de chat de room para clientes.
 * Envía mensajes de room al servidor usando CONTROL_ROOM_MESSAGE y muestra mensajes recibidos.
 */
public class ClientRoomChatView extends BorderPane implements com.whatsapp.network.observer.NetworkEventObserver {
    private final Long roomId;
    private final String roomName;
    private final String serverConnectionId;
    private final Usuario currentUser;
    private final NetworkFacade networkFacade;
    private final com.whatsapp.service.ControlService controlService;
    private final com.whatsapp.network.observer.EventAggregator eventAggregator;
    private final UserAliasRegistry aliasRegistry;
    private final ListView<String> messagesList;
    private final ListView<String> membersList;
    private final VBox videoBox;
    private final TextField messageField;
    private final Set<String> members;
    private final Map<String, String> pendingFileDownloads = new HashMap<>(); // fileName -> filePath
    private final Runnable onDispose;

    public ClientRoomChatView(Long roomId, String roomName, Set<String> members, String serverConnectionId, 
                              Usuario currentUser, NetworkFacade networkFacade, Runnable onDispose) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.serverConnectionId = serverConnectionId;
        this.currentUser = currentUser;
        this.networkFacade = networkFacade;
        this.controlService = new ControlService();
        this.eventAggregator = com.whatsapp.network.observer.EventAggregator.getInstance();
        this.aliasRegistry = UserAliasRegistry.getInstance();
        this.messagesList = new ListView<>();
        this.membersList = new ListView<>();
        this.videoBox = new VBox();
        this.messageField = new TextField();
        this.members = members == null ? new HashSet<>() : new HashSet<>(members);
        this.onDispose = onDispose == null ? () -> {} : onDispose;

        eventAggregator.subscribe(this);
        setupUI();
        refreshMembers();
    }

    private void setupUI() {
        setPadding(new Insets(0));

        // Top bar estilo server
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(12));
        topBar.setStyle("-fx-background-color: #128C7E;");
        Label title = new Label("Room: " + roomName);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        topBar.getChildren().add(title);

        messagesList.setPrefHeight(320);
        messagesList.setStyle("-fx-font-size: 12px;");
        messagesList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) { // Doble click para descargar
                String selected = messagesList.getSelectionModel().getSelectedItem();
                if (selected != null && selected.contains("📎")) {
                    handleFileDownload(selected);
                }
            }
        });
        membersList.setPrefWidth(200);
        Label membersLabel = new Label("Miembros");
        membersLabel.setStyle("-fx-font-weight: bold;");
        VBox membersBox = new VBox(5, membersLabel, membersList);
        membersBox.setPadding(new Insets(0, 0, 0, 10));

        // Placeholder de video (pendiente de implementar)
        videoBox.setPrefSize(220, 180);
        videoBox.setStyle("-fx-border-color: #cccccc; -fx-background-color: #f5f5f5;");
        Label videoLabel = new Label("Video remoto (próximamente)");
        videoLabel.setStyle("-fx-text-fill: #666;");
        videoBox.setPadding(new Insets(8));
        videoBox.getChildren().setAll(videoLabel);
        VBox rightBox = new VBox(10, membersBox, videoBox);

        HBox center = new HBox(10, messagesList, rightBox);
        center.setPadding(new Insets(10));
        setTop(topBar);
        setCenter(center);

        messageField.setPromptText("Escribe un mensaje para todos...");
        Button sendBtn = new Button("Enviar");
        sendBtn.setOnAction(e -> sendMessage());
        messageField.setOnAction(e -> sendMessage());
        
        Button fileBtn = new Button("Enviar Archivo");
        fileBtn.setStyle("-fx-background-color: #128C7E; -fx-text-fill: white;");
        fileBtn.setOnAction(e -> sendFileToAll());
        
        HBox bottom = new HBox(8, messageField, sendBtn, fileBtn);
        bottom.setPadding(new Insets(10));
        setBottom(bottom);
    }

    private void refreshMembers() {
        membersList.getItems().setAll(members.stream()
            .map(id -> aliasRegistry.getAliasOrDefault(id))
            .toList());
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        try {
            String payload = encodeBase64(String.valueOf(roomId)) + "|" + encodeBase64(text);
            controlService.sendControlMessage(serverConnectionId, ControlService.CONTROL_ROOM_MESSAGE, payload);
            appendMessage("Yo: " + text);
            messageField.clear();
        } catch (Exception e) {
            showAlert("Error", "No se pudo enviar el mensaje: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void appendMessage(String msg) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        messagesList.getItems().add("[" + timestamp + "] " + msg);
        messagesList.scrollTo(messagesList.getItems().size() - 1);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String encodeBase64(String value) {
        return java.util.Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private void sendFileToAll() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo para enviar a todos");
        File file = fileChooser.showOpenDialog(getScene() != null ? (Stage) getScene().getWindow() : null);

        if (file != null) {
            try {
                long fileSize = file.length();
                String fileName = file.getName();
                
                // Enviar notificación de archivo como mensaje de sala
                String roomIdEncoded = encodeBase64(String.valueOf(roomId));
                String senderId = currentUser.getUsername();
                aliasRegistry.registerAlias(senderId, currentUser.getUsername());
                String fileNameEncoded = encodeBase64(fileName);
                String fileSizeEncoded = encodeBase64(String.valueOf(fileSize));
                String senderIdEncoded = encodeBase64(senderId);
                
                String payload = roomIdEncoded + "|" + senderIdEncoded + "|" + fileNameEncoded + "|" + fileSizeEncoded + "|";
                
                // Enviar notificación al servidor (el servidor la reenviará a todos los miembros)
                controlService.sendControlMessage(serverConnectionId, ControlService.CONTROL_ROOM_FILE, payload);
                
                // Enviar el archivo físico a cada miembro a través del servidor
                for (String memberId : members) {
                    if (memberId == null || memberId.equals(currentUser.getUsername()) || memberId.startsWith("SERVER_")) {
                        continue;
                    }
                    try {
                        networkFacade.sendFile(serverConnectionId, memberId, file.getAbsolutePath(), currentUser.getId());
                    } catch (Exception e) {
                        // Continuar con otros miembros si uno falla
                        System.err.println("Error enviando archivo a " + memberId + ": " + e.getMessage());
                    }
                }
                
                Platform.runLater(() -> {
                    addFileMessage(currentUser.getUsername(), fileName, fileSize, null, true);
                });
            } catch (Exception e) {
                showAlert("Error", "No se pudo enviar el archivo: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void addFileMessage(String senderName, String fileName, long fileSize, String filePath, boolean isSent) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
        );
        String sizeStr = formatFileSize(fileSize);
        String prefix = isSent ? "Yo" : senderName;
        String message;
        if (filePath != null && !isSent) {
            message = String.format("[%s] %s: 📎 %s (%s) [Disponible para descargar]", timestamp, prefix, fileName, sizeStr);
            pendingFileDownloads.put(fileName, filePath);
        } else {
            message = String.format("[%s] %s: 📎 %s (%s)", timestamp, prefix, fileName, sizeStr);
        }
        appendMessage(message);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private void handleFileDownload(String messageText) {
        if (!messageText.contains("📎")) {
            return;
        }
        
        try {
            int fileIconIndex = messageText.indexOf("📎");
            int startIndex = fileIconIndex + 2;
            int endIndex = messageText.indexOf(" (", startIndex);
            if (endIndex == -1) {
                endIndex = messageText.indexOf(" [", startIndex);
            }
            if (endIndex == -1) {
                endIndex = messageText.length();
            }
            String fileName = messageText.substring(startIndex, endIndex).trim();
            
            String filePath = pendingFileDownloads.get(fileName);
            if (filePath == null || !new File(filePath).exists()) {
                showAlert("Error", "El archivo no está disponible para descargar.", Alert.AlertType.WARNING);
                return;
            }
            
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Selecciona la carpeta para guardar: " + fileName);
            File selectedDir = dirChooser.showDialog(getScene() != null ? (Stage) getScene().getWindow() : null);
            
            if (selectedDir == null) {
                return;
            }
            
            File sourceFile = new File(filePath);
            File destination = new File(selectedDir, fileName);
            
            Path destPath = destination.toPath();
            Path parent = destPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(sourceFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
            
            showAlert("Éxito", "Archivo guardado en: " + destPath, Alert.AlertType.INFORMATION);
            appendMessage("✓ Archivo descargado: " + fileName);
            
        } catch (Exception e) {
            showAlert("Error", "No se pudo descargar el archivo: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @Override
    public void onNetworkEvent(com.whatsapp.network.observer.NetworkEvent event) {
        Platform.runLater(() -> {
            switch (event.getType()) {
                case ROOM_MESSAGE -> {
                    if (!(event.getData() instanceof ControlService.RoomChatMessage roomMsg)) {
                        return;
                    }
                    if (!roomId.equals(roomMsg.getRoomId())) {
                        return;
                    }
                    members.add(roomMsg.getSenderConnectionId());
                    refreshMembers();
                    String senderName = aliasRegistry.getAliasOrDefault(roomMsg.getSenderConnectionId());
                    appendMessage(senderName + ": " + roomMsg.getMessage());
                }
                case ROOM_FILE -> {
                    if (!(event.getData() instanceof ControlService.RoomFileMessage roomFileMsg)) {
                        return;
                    }
                    if (!roomId.equals(roomFileMsg.getRoomId())) {
                        return;
                    }
                    String senderId = roomFileMsg.getSenderConnectionId();
                    members.add(senderId);
                    refreshMembers();
                    String senderName = aliasRegistry.getAliasOrDefault(senderId);
                    boolean isMine = senderId.equals(currentUser.getUsername());
                    addFileMessage(senderName, roomFileMsg.getFileName(), roomFileMsg.getFileSize(), 
                                 roomFileMsg.getFilePath(), isMine);
                }
                case FILE_PROGRESS -> {
                    if (event.getData() instanceof com.whatsapp.service.FileTransferService.FileProgress progress
                        && progress.getProgress() >= 100.0 && progress.isIncoming()) {
                        if (progress.getLocalPath() != null) {
                            pendingFileDownloads.put(progress.getFileName(), progress.getLocalPath());
                            refreshFileMessage(progress.getFileName(), progress.getLocalPath());
                        }
                    }
                }
                default -> {
                    // Otros tipos de eventos no manejados
                }
            }
        });
    }

    private void refreshFileMessage(String fileName, String filePath) {
        for (int i = 0; i < messagesList.getItems().size(); i++) {
            String item = messagesList.getItems().get(i);
            if (item.contains(fileName) && !item.contains("[Disponible para descargar]")) {
                String updated = item + " [Disponible para descargar]";
                messagesList.getItems().set(i, updated);
                break;
            }
        }
    }

    public void onClose() {
        eventAggregator.unsubscribe(this);
        onDispose.run();
    }

    public void openInNewStage() {
        Stage stage = new Stage();
        stage.setTitle("Room: " + roomName);
        stage.setScene(new javafx.scene.Scene(this, 650, 400));
        stage.setOnCloseRequest(e -> onClose());
        stage.show();
    }
}
