package com.whatsapp.ui;

import com.whatsapp.command.*;
import com.whatsapp.model.Usuario;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.network.observer.NetworkEventObserver;
import com.whatsapp.service.ChatService;
import com.whatsapp.service.NetworkFacade;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ChatView extends BorderPane implements NetworkEventObserver {
    private final Usuario currentUser;
    private final String connectionId;
    private final String serverConnectionId;
    private final NetworkFacade networkFacade;
    private final CommandInvoker commandInvoker;
    private final ListView<String> messagesList;
    private TextField messageField;
    private Label statusLabel;

    public ChatView(Usuario currentUser, String connectionId, String serverConnectionId, NetworkFacade networkFacade) {
        this.currentUser = currentUser;
        this.connectionId = connectionId;
        this.serverConnectionId = serverConnectionId;
        this.networkFacade = networkFacade;
        this.commandInvoker = new CommandInvoker();
        this.messagesList = new ListView<>();
        
        EventAggregator.getInstance().subscribe(this);
        setupUI();
    }

    private void setupUI() {
        // Panel superior - Información del chat
        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(10));
        topBox.setStyle("-fx-background-color: #128C7E;");
        
        Label chatLabel = new Label("Chat con: " + connectionId);
        chatLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        
        statusLabel = new Label("Conectado");
        statusLabel.setStyle("-fx-text-fill: white;");
        
        topBox.getChildren().addAll(chatLabel, statusLabel);
        setTop(topBox);

        // Panel central - Mensajes
        messagesList.setPrefHeight(350);
        messagesList.setStyle("-fx-font-size: 12px;");
        setCenter(messagesList);

        // Panel inferior - Controles
        VBox bottomBox = new VBox(10);
        bottomBox.setPadding(new Insets(10));

        // Campo de mensaje y botón enviar
        HBox messageBox = new HBox(10);
        messageField = new TextField();
        messageField.setPromptText("Escribe un mensaje...");
        messageField.setPrefWidth(400);
        messageField.setOnAction(e -> sendMessage());

        Button sendButton = new Button("Enviar");
        sendButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white;");
        sendButton.setOnAction(e -> sendMessage());

        messageBox.getChildren().addAll(messageField, sendButton);

        // Botones de acciones
        HBox actionBox = new HBox(10);
        Button fileButton = new Button("Enviar Archivo");
        fileButton.setStyle("-fx-background-color: #128C7E; -fx-text-fill: white;");
        fileButton.setOnAction(e -> sendFile());

        Button videoButton = new Button("Videollamada");
        videoButton.setStyle("-fx-background-color: #128C7E; -fx-text-fill: white;");
        videoButton.setOnAction(e -> startVideoCall());

        actionBox.getChildren().addAll(fileButton, videoButton);
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
                addMessage("Yo: Archivo enviado - " + file.getName());
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
            if (event.getType() == NetworkEvent.EventType.MESSAGE_RECEIVED) {
                if (event.getData() instanceof ChatService.ChatMessage) {
                    ChatService.ChatMessage msg = (ChatService.ChatMessage) event.getData();
                    if (msg.getSource().equals(connectionId)) {
                        addMessage(connectionId + ": " + msg.getMessage());
                    }
                }
            } else if (event.getType() == NetworkEvent.EventType.FILE_PROGRESS) {
                if (event.getData() instanceof com.whatsapp.service.FileTransferService.FileProgress) {
                    com.whatsapp.service.FileTransferService.FileProgress progress = 
                        (com.whatsapp.service.FileTransferService.FileProgress) event.getData();
                    if (progress.getProgress() == 100.0) {
                        addMessage(connectionId + ": Archivo recibido");
                    }
                }
            } else if (event.getType() == NetworkEvent.EventType.DISCONNECTED) {
                if (event.getData().toString().equals(connectionId)) {
                    statusLabel.setText("Desconectado");
                    statusLabel.setStyle("-fx-text-fill: red;");
                }
            }
        });
    }
}

