package com.whatsapp.ui;

import com.whatsapp.service.ControlService;
import com.whatsapp.service.UserAliasRegistry;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Vista sencilla de chat de room para clientes.
 * Envía mensajes de room al servidor usando CONTROL_ROOM_MESSAGE y muestra mensajes recibidos.
 */
public class ClientRoomChatView extends BorderPane implements com.whatsapp.network.observer.NetworkEventObserver {
    private final Long roomId;
    private final String roomName;
    private final String serverConnectionId;
    private final com.whatsapp.service.ControlService controlService;
    private final com.whatsapp.network.observer.EventAggregator eventAggregator;
    private final UserAliasRegistry aliasRegistry;
    private final ListView<String> messagesList;
    private final ListView<String> membersList;
    private final TextField messageField;
    private final Set<String> members;

    public ClientRoomChatView(Long roomId, String roomName, Set<String> members, String serverConnectionId) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.serverConnectionId = serverConnectionId;
        this.controlService = new ControlService();
        this.eventAggregator = com.whatsapp.network.observer.EventAggregator.getInstance();
        this.aliasRegistry = UserAliasRegistry.getInstance();
        this.messagesList = new ListView<>();
        this.membersList = new ListView<>();
        this.messageField = new TextField();
        this.members = members == null ? new HashSet<>() : new HashSet<>(members);

        eventAggregator.subscribe(this);
        setupUI();
        refreshMembers();
    }

    private void setupUI() {
        setPadding(new Insets(10));

        Label title = new Label("Room: " + roomName);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        messagesList.setPrefHeight(320);
        membersList.setPrefWidth(200);
        VBox membersBox = new VBox(5, new Label("Miembros"), membersList);
        membersBox.setPadding(new Insets(0, 0, 0, 10));

        HBox center = new HBox(10, messagesList, membersBox);
        setTop(title);
        setCenter(center);

        messageField.setPromptText("Escribe un mensaje para todos...");
        Button sendBtn = new Button("Enviar");
        sendBtn.setOnAction(e -> sendMessage());
        messageField.setOnAction(e -> sendMessage());
        HBox bottom = new HBox(8, messageField, sendBtn);
        bottom.setPadding(new Insets(10, 0, 0, 0));
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

    @Override
    public void onNetworkEvent(com.whatsapp.network.observer.NetworkEvent event) {
        if (event.getType() != com.whatsapp.network.observer.NetworkEvent.EventType.ROOM_MESSAGE) {
            return;
        }
        if (!(event.getData() instanceof ControlService.RoomChatMessage roomMsg)) {
            return;
        }
        if (!roomId.equals(roomMsg.getRoomId())) {
            return;
        }
        Platform.runLater(() -> {
            members.add(roomMsg.getSenderConnectionId());
            refreshMembers();
            String senderName = aliasRegistry.getAliasOrDefault(roomMsg.getSenderConnectionId());
            appendMessage(senderName + ": " + roomMsg.getMessage());
        });
    }

    public void onClose() {
        eventAggregator.unsubscribe(this);
    }

    public void openInNewStage() {
        Stage stage = new Stage();
        stage.setTitle("Room: " + roomName);
        stage.setScene(new javafx.scene.Scene(this, 650, 400));
        stage.setOnCloseRequest(e -> onClose());
        stage.show();
    }
}
