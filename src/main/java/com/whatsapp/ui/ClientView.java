package com.whatsapp.ui;

import java.io.IOException;
import java.util.Set;

import com.whatsapp.model.Usuario;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.network.observer.NetworkEventObserver;
import com.whatsapp.service.NetworkFacade;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class ClientView extends BorderPane implements NetworkEventObserver {
    private final NetworkFacade networkFacade;
    private final Usuario currentUser;
    private final ListView<String> usersList;
    private final TextField serverHostField;
    private final TextField serverPortField;
    private Button connectButton;
    private Button disconnectButton;
    private String selectedConnectionId;

    public ClientView(Usuario currentUser) {
        this.currentUser = currentUser;
        this.networkFacade = new NetworkFacade();
        this.usersList = new ListView<>();
        this.serverHostField = new TextField("localhost");
        this.serverPortField = new TextField("8080");
        
        EventAggregator.getInstance().subscribe(this);
        setupUI();
    }

    private void setupUI() {
        // Panel superior - Conexión al servidor
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(20));
        topPanel.setStyle("-fx-background-color: #128C7E;");

        Text title = new Text("Modo Cliente - " + currentUser.getUsername());
        title.setFont(Font.font(20));
        title.setStyle("-fx-fill: white;");

        HBox connectionBox = new HBox(10);
        connectionBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label hostLabel = new Label("Servidor:");
        hostLabel.setStyle("-fx-text-fill: white;");
        serverHostField.setPrefWidth(150);
        serverHostField.setPromptText("IP del servidor (ej: 25.x.x.x para Hamachi)");
        Tooltip hostTooltip = new Tooltip("Si usas Hamachi, ingresa la IP de Hamachi del servidor.\nSi estás en la misma red local, usa la IP local o 'localhost'.");
        serverHostField.setTooltip(hostTooltip);
        
        Label portLabel = new Label("Puerto:");
        portLabel.setStyle("-fx-text-fill: white;");
        serverPortField.setPrefWidth(100);
        
        connectButton = new Button("Conectar");
        connectButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white;");
        connectButton.setOnAction(e -> connectToServer());
        
        disconnectButton = new Button("Desconectar");
        disconnectButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        disconnectButton.setDisable(true);
        disconnectButton.setOnAction(e -> disconnectFromServer());

        connectionBox.getChildren().addAll(hostLabel, serverHostField, portLabel, serverPortField, 
                                          connectButton, disconnectButton);
        topPanel.getChildren().addAll(title, connectionBox);
        setTop(topPanel);

        // Panel central - Lista de usuarios conectados
        VBox centerBox = new VBox(10);
        centerBox.setPadding(new Insets(20));

        Label usersLabel = new Label("Usuarios Conectados (Haz click para chatear):");
        usersLabel.setFont(Font.font(14));
        
        usersList.setPrefHeight(400);
        usersList.setOnMouseClicked(e -> {
            String selected = usersList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedConnectionId = selected;
                openChatWindow(selected);
            }
        });

        centerBox.getChildren().addAll(usersLabel, usersList);
        setCenter(centerBox);

        // Panel inferior - Estado
        VBox bottomPanel = new VBox(5);
        bottomPanel.setPadding(new Insets(10));
        bottomPanel.setStyle("-fx-background-color: #f0f0f0;");
        
        Label statusLabel = new Label("Estado: Desconectado");
        statusLabel.setId("statusLabel");
        bottomPanel.getChildren().add(statusLabel);
        setBottom(bottomPanel);
    }

    private void connectToServer() {
        try {
            String host = serverHostField.getText();
            int port = Integer.parseInt(serverPortField.getText());
            
            networkFacade.connectToServer(host, port);
            
            Platform.runLater(() -> {
                connectButton.setDisable(true);
                disconnectButton.setDisable(false);
                serverHostField.setDisable(true);
                serverPortField.setDisable(true);
                updateStatus("Estado: Conectado a " + host + ":" + port);
            });
        } catch (NumberFormatException e) {
            showAlert("Error", "Puerto inválido", Alert.AlertType.ERROR);
        } catch (IOException e) {
            showAlert("Error", "No se pudo conectar al servidor: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void disconnectFromServer() {
        networkFacade.disconnect();
        
        Platform.runLater(() -> {
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
            serverHostField.setDisable(false);
            serverPortField.setDisable(false);
            updateStatus("Estado: Desconectado");
            usersList.getItems().clear();
            serverUserList.clear();
            selectedConnectionId = null;
        });
    }

    private void openChatWindow(String connectionId) {
        ChatView chatView = new ChatView(currentUser, connectionId, networkFacade);
        javafx.stage.Stage chatStage = new javafx.stage.Stage();
        chatStage.setTitle("Chat con " + connectionId);
        chatStage.setScene(new javafx.scene.Scene(chatView, 600, 500));
        chatStage.show();
    }

    private void updateStatus(String status) {
        Label statusLabel = (Label) lookup("#statusLabel");
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
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
                case CONNECTED:
                    // Si el evento viene del servidor con datos JSON, es la lista de usuarios
                    if (event.getSource().equals("SERVER") && event.getData() instanceof String) {
                        String data = (String) event.getData();
                        System.out.println("Cliente recibió evento CONNECTED del servidor: " + data);
                        // Verificar si es JSON (lista de usuarios)
                        if (data.startsWith("[") && data.endsWith("]")) {
                            try {
                                Set<String> users = com.whatsapp.service.ControlService.parseUserListJson(data);
                                System.out.println("Parseó lista de usuarios: " + users);
                                serverUserList.clear();
                                serverUserList.addAll(users);
                                usersList.getItems().clear();
                                usersList.getItems().addAll(serverUserList);
                                System.out.println("Lista actualizada en UI: " + usersList.getItems());
                            } catch (Exception e) {
                                System.err.println("Error parseando JSON de usuarios: " + e.getMessage());
                                e.printStackTrace();
                                // Si no es JSON, es un ID de usuario individual
                                String userId = data;
                                if (!serverUserList.contains(userId)) {
                                    serverUserList.add(userId);
                                    usersList.getItems().clear();
                                    usersList.getItems().addAll(serverUserList);
                                }
                            }
                        } else {
                            // Es un ID de usuario individual
                            String userId = data;
                            System.out.println("Agregando usuario individual: " + userId);
                            if (!serverUserList.contains(userId)) {
                                serverUserList.add(userId);
                                usersList.getItems().clear();
                                usersList.getItems().addAll(serverUserList);
                            }
                        }
                    } else {
                        // Actualizar lista de usuarios conectados (método antiguo)
                        updateConnectedUsers();
                    }
                    break;
                case DISCONNECTED:
                    // Si el evento viene del servidor, es una desconexión
                    if (event.getSource().equals("SERVER") && event.getData() instanceof String) {
                        String userId = (String) event.getData();
                        serverUserList.remove(userId);
                        usersList.getItems().clear();
                        usersList.getItems().addAll(serverUserList);
                    } else {
                        // Actualizar lista de usuarios conectados (método antiguo)
                        updateConnectedUsers();
                    }
                    break;
            }
        });
    }

    private void updateConnectedUsers() {
        // Obtener usuarios del servidor (si está disponible)
        Set<String> connectedUsers = networkFacade.getConnectedClients();
        usersList.getItems().clear();
        
        // Si hay usuarios, agregarlos
        if (connectedUsers != null && !connectedUsers.isEmpty()) {
            usersList.getItems().addAll(connectedUsers);
        }
    }
    
    // Almacenar la lista de usuarios recibida del servidor
    private final java.util.Set<String> serverUserList = new java.util.HashSet<>();
}

