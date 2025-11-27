package com.whatsapp.ui;

import com.whatsapp.model.Usuario;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.network.observer.NetworkEventObserver;
import com.whatsapp.service.ControlService;
import com.whatsapp.service.NetworkFacade;
import com.whatsapp.service.UserAliasRegistry;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.*;

public class ClientView extends BorderPane implements NetworkEventObserver {
    private final NetworkFacade networkFacade;
    private final Usuario currentUser;
    private final ControlService controlService;
    private final UserAliasRegistry aliasRegistry;
    private final ListView<ControlService.UserDescriptor> usersList;
    private final String connectedHost;
    private final int connectedPort;
    private final Label statusLabel;
    private boolean isApproved = false;
    private Button openChatButton;

    public ClientView(Usuario currentUser, NetworkFacade networkFacade, String serverHost, int serverPort) {
        this.currentUser = currentUser;
        this.networkFacade = networkFacade;
        this.controlService = new ControlService();
        this.aliasRegistry = UserAliasRegistry.getInstance();
        this.usersList = new ListView<>();
        this.connectedHost = serverHost;
        this.connectedPort = serverPort;
        this.statusLabel = new Label();

        EventAggregator.getInstance().subscribe(this);
        setupUI();
        updateStatus("Estado: Pendiente de aprobación - Esperando verificación del servidor");
        requestUserListRefresh();
    }

    private void setupUI() {
        // Panel superior - Información del cliente
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(20));
        topPanel.setStyle("-fx-background-color: #128C7E;");

        Text title = new Text("Modo Cliente - " + currentUser.getUsername());
        title.setFont(Font.font(20));
        title.setStyle("-fx-fill: white;");

        Button disconnectButton = new Button("Desconectar");
        disconnectButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        disconnectButton.setOnAction(e -> disconnectFromServer());

        HBox actionBox = new HBox(10, new Label("Sesión activa en: " + connectedHost + ":" + connectedPort), disconnectButton);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topPanel.getChildren().addAll(title, actionBox);
        setTop(topPanel);

        // Panel central - Lista de usuarios
        VBox centerBox = new VBox(10);
        centerBox.setPadding(new Insets(20));

        Label usersLabel = new Label("Usuarios en la Sala:");
        usersLabel.setFont(Font.font(14));

        usersList.setPrefHeight(300);
        usersList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ControlService.UserDescriptor item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });

        openChatButton = new Button("Abrir Sala Grupal");
        openChatButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; -fx-font-size: 14px;");
        openChatButton.setDisable(true);
        openChatButton.setOnAction(e -> openGroupChat());
        openChatButton.setPrefWidth(200);

        centerBox.getChildren().addAll(usersLabel, usersList, openChatButton);
        setCenter(centerBox);

        // Panel inferior - Estado
        VBox bottomPanel = new VBox(5);
        bottomPanel.setPadding(new Insets(10));
        bottomPanel.setStyle("-fx-background-color: #f0f0f0;");

        statusLabel.setId("statusLabel");
        bottomPanel.getChildren().add(statusLabel);
        setBottom(bottomPanel);
    }

    private void disconnectFromServer() {
        networkFacade.disconnectClients();

        Platform.runLater(() -> {
            updateStatus("Estado: Desconectado");
            usersList.getItems().clear();
            serverUserMap.clear();
        });
    }

    private void openGroupChat() {
        if (!isApproved) {
            showAlert("Error", "Debes ser aprobado por el servidor para usar el chat.", Alert.AlertType.WARNING);
            return;
        }
        
        String serverConnectionId = networkFacade.getPrimaryConnectionId();
        if (serverConnectionId == null) {
            showAlert("Error", "No hay conexión activa con el servidor.", Alert.AlertType.ERROR);
            return;
        }

        // Para sala grupal, usar "BROADCAST" como connectionId
        ChatView chatView = new ChatView(currentUser, "BROADCAST", serverConnectionId, networkFacade);
        javafx.stage.Stage chatStage = new javafx.stage.Stage();
        chatStage.setTitle("Sala Grupal");
        chatStage.setScene(new javafx.scene.Scene(chatView, 600, 500));
        chatStage.show();
    }

    private void updateStatus(String status) {
        statusLabel.setText(status);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void requestUserListRefresh() {
        try {
            String serverConnectionId = networkFacade.getPrimaryConnectionId();
            if (serverConnectionId != null) {
                controlService.sendAliasUpdate(serverConnectionId, currentUser.getUsername());
            }
        } catch (IOException e) {
            showAlert("Error", "No se pudo sincronizar usuarios: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @Override
    public void onNetworkEvent(NetworkEvent event) {
        Platform.runLater(() -> {
            if (event.getType() == NetworkEvent.EventType.CONNECTED && "SERVER_STATUS".equals(event.getSource())) {
                // Evento de estado de verificación
                String status = (String) event.getData();
                if ("APPROVED".equals(status)) {
                    isApproved = true;
                    updateStatus("Estado: Aprobado - Puedes usar todas las funciones");
                    openChatButton.setDisable(false);
                } else if ("REJECTED".equals(status)) {
                    isApproved = false;
                    updateStatus("Estado: Rechazado - Esperando aprobación del servidor");
                    openChatButton.setDisable(true);
                } else if ("KICKED".equals(status)) {
                    isApproved = false;
                    updateStatus("Estado: Expulsado del servidor");
                    openChatButton.setDisable(true);
                    showAlert("Expulsado", "Has sido expulsado del servidor por el administrador.", Alert.AlertType.WARNING);
                }
                return;
            }
            
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

