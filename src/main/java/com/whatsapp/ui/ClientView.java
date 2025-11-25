package com.whatsapp.ui;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.whatsapp.model.Usuario;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.network.observer.NetworkEventObserver;
import com.whatsapp.service.ControlService;
import com.whatsapp.service.NetworkFacade;
import com.whatsapp.service.UserAliasRegistry;

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
    private final ControlService controlService;
    private final UserAliasRegistry aliasRegistry;
    private final ListView<ControlService.UserDescriptor> usersList;
    private final TextField serverHostField;
    private final TextField serverPortField;
    private Button connectButton;
    private Button disconnectButton;

    public ClientView(Usuario currentUser) {
        this.currentUser = currentUser;
        this.networkFacade = new NetworkFacade();
        this.controlService = new ControlService();
        this.aliasRegistry = UserAliasRegistry.getInstance();
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
        usersList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ControlService.UserDescriptor item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        usersList.setOnMouseClicked(e -> {
            ControlService.UserDescriptor selected = usersList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openChatWindow(selected.getConnectionId(), selected.getDisplayName());
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
            announceAliasToServer();
            
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
        networkFacade.disconnectClients();
        
        Platform.runLater(() -> {
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
            serverHostField.setDisable(false);
            serverPortField.setDisable(false);
            updateStatus("Estado: Desconectado");
            usersList.getItems().clear();
            serverUserMap.clear();
        });
    }

    private void openChatWindow(String connectionId, String displayName) {
        String serverConnectionId = networkFacade.getPrimaryConnectionId();
        if (serverConnectionId == null) {
            showAlert("Error", "No hay conexión activa con el servidor.", Alert.AlertType.ERROR);
            return;
        }

        ChatView chatView = new ChatView(currentUser, connectionId, serverConnectionId, networkFacade);
        javafx.stage.Stage chatStage = new javafx.stage.Stage();
        chatStage.setTitle("Chat con " + displayName);
        chatStage.setScene(new javafx.scene.Scene(chatView, 600, 500));
        chatStage.show();
    }

    private void announceAliasToServer() {
        try {
            String serverConnectionId = networkFacade.getPrimaryConnectionId();
            if (serverConnectionId != null) {
                controlService.sendAliasUpdate(serverConnectionId, currentUser.getUsername());
            }
        } catch (IOException e) {
            showAlert("Error", "No se pudo anunciar el usuario al servidor: " + e.getMessage(), Alert.AlertType.ERROR);
        }
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
            if (!(event.getData() instanceof String)) {
                return;
            }

            String payload = (String) event.getData();
            switch (event.getType()) {
                case CONNECTED:
                    if ("SERVER".equals(event.getSource())) {
                        handleServerConnectedPayload(payload);
                    }
                    break;
                case DISCONNECTED:
                    serverUserMap.remove(payload);
                    aliasRegistry.removeAlias(payload);
                    refreshUsersList();
                    break;
                default:
                    break;
            }
        });
    }

    private void handleServerConnectedPayload(String data) {
        if (data.startsWith("[") && data.endsWith("]")) {
            try {
                var users = ControlService.parseUserListJson(data);
                serverUserMap.clear();
                for (ControlService.UserDescriptor descriptor : users) {
                    serverUserMap.put(descriptor.getConnectionId(), descriptor);
                    aliasRegistry.registerAlias(descriptor.getConnectionId(), descriptor.getDisplayName());
                }
                refreshUsersList();
                return;
            } catch (Exception ignored) {
                // caer a manejo como usuario individual
            }
        }

        ControlService.UserDescriptor descriptor = ControlService.parseUserDescriptor(data);
        if (descriptor != null) {
            if (!descriptor.getConnectionId().equals(descriptor.getDisplayName())) {
                serverUserMap.put(descriptor.getConnectionId(), descriptor);
                aliasRegistry.registerAlias(descriptor.getConnectionId(), descriptor.getDisplayName());
                refreshUsersList();
            }
        }
    }

    private void refreshUsersList() {
        usersList.getItems().setAll(serverUserMap.values());
    }

    private final Map<String, ControlService.UserDescriptor> serverUserMap = new LinkedHashMap<>();
}

