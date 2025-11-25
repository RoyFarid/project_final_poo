package com.whatsapp.ui;

import com.whatsapp.model.Usuario;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.network.observer.NetworkEventObserver;
import com.whatsapp.service.NetworkFacade;
import com.whatsapp.service.LogService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ServerView extends BorderPane implements NetworkEventObserver {
    private final NetworkFacade networkFacade;
    private final Usuario currentUser;
    private final LogService logService;
    private final ListView<String> activityList;
    private final ListView<String> connectedUsersList;
    private final TextField portField;
    private Button startServerButton;
    private Button stopServerButton;
    private boolean serverStarted = false;

    public ServerView(Usuario currentUser) {
        this.currentUser = currentUser;
        this.networkFacade = new NetworkFacade();
        this.logService = LogService.getInstance();
        this.activityList = new ListView<>();
        this.connectedUsersList = new ListView<>();
        this.portField = new TextField("8080");
        
        EventAggregator.getInstance().subscribe(this);
        setupUI();
    }

    private void setupUI() {
        // Panel superior - Configuración del servidor
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(20));
        topPanel.setStyle("-fx-background-color: #128C7E;");

        Text title = new Text("Modo Servidor - " + currentUser.getUsername());
        title.setFont(Font.font(20));
        title.setStyle("-fx-fill: white;");

        HBox configBox = new HBox(10);
        configBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label portLabel = new Label("Puerto:");
        portLabel.setStyle("-fx-text-fill: white;");
        portField.setPrefWidth(100);
        
        startServerButton = new Button("Iniciar Servidor");
        startServerButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white;");
        startServerButton.setOnAction(e -> startServer());
        
        stopServerButton = new Button("Detener Servidor");
        stopServerButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        stopServerButton.setDisable(true);
        stopServerButton.setOnAction(e -> stopServer());

        configBox.getChildren().addAll(portLabel, portField, startServerButton, stopServerButton);
        topPanel.getChildren().addAll(title, configBox);
        setTop(topPanel);

        // Panel central - Usuarios conectados y actividades
        HBox centerBox = new HBox(10);
        centerBox.setPadding(new Insets(10));

        VBox usersBox = new VBox(5);
        usersBox.setPrefWidth(300);
        Label usersLabel = new Label("Usuarios Conectados:");
        usersLabel.setFont(Font.font(14));
        connectedUsersList.setPrefHeight(400);
        usersBox.getChildren().addAll(usersLabel, connectedUsersList);

        VBox activityBox = new VBox(5);
        Label activityLabel = new Label("Actividades:");
        activityLabel.setFont(Font.font(14));
        activityList.setPrefHeight(400);
        activityBox.getChildren().addAll(activityLabel, activityList);

        centerBox.getChildren().addAll(usersBox, activityBox);
        setCenter(centerBox);

        // Panel inferior - Información del servidor
        VBox bottomPanel = new VBox(5);
        bottomPanel.setPadding(new Insets(10));
        bottomPanel.setStyle("-fx-background-color: #f0f0f0;");
        
        Label statusLabel = new Label("Estado: Desconectado");
        statusLabel.setId("statusLabel");
        
        Label ipInfoLabel = new Label("IPs disponibles: " + getAvailableIPs());
        ipInfoLabel.setId("ipInfoLabel");
        ipInfoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        ipInfoLabel.setWrapText(true);
        
        bottomPanel.getChildren().addAll(statusLabel, ipInfoLabel);
        setBottom(bottomPanel);
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText());
            networkFacade.startServer(port);
            serverStarted = true;
            
            Platform.runLater(() -> {
                startServerButton.setDisable(true);
                stopServerButton.setDisable(false);
                portField.setDisable(true);
                addActivity("Servidor iniciado en puerto " + port);
                updateStatus("Estado: Activo - Puerto " + port);
                // Actualizar información de IPs
                Label ipInfoLabel = (Label) lookup("#ipInfoLabel");
                if (ipInfoLabel != null) {
                    ipInfoLabel.setText("IPs disponibles: " + getAvailableIPs() + " | Comparte la IP de Hamachi con los clientes");
                }
            });
        } catch (NumberFormatException e) {
            showAlert("Error", "Puerto inválido", Alert.AlertType.ERROR);
        } catch (IOException e) {
            showAlert("Error", "No se pudo iniciar el servidor: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void stopServer() {
        networkFacade.disconnect();
        serverStarted = false;
        
        Platform.runLater(() -> {
            startServerButton.setDisable(false);
            stopServerButton.setDisable(true);
            portField.setDisable(false);
            addActivity("Servidor detenido");
            updateStatus("Estado: Desconectado");
            connectedUsersList.getItems().clear();
        });
    }

    private void addActivity(String activity) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
        );
        activityList.getItems().add(0, "[" + timestamp + "] " + activity);
        
        // Limitar a 100 actividades
        if (activityList.getItems().size() > 100) {
            activityList.getItems().remove(activityList.getItems().size() - 1);
        }
    }

    private void updateStatus(String status) {
        Label statusLabel = (Label) lookup("#statusLabel");
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }
    
    private String getAvailableIPs() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') < 0) {
                            String ip = address.getHostAddress();
                            // Detectar IPs de Hamachi (generalmente empiezan con 25. o 5.)
                            if (ip.startsWith("25.") || ip.startsWith("5.")) {
                                ips.add(ip + " (Hamachi)");
                            } else {
                                ips.add(ip);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignorar errores
        }
        return ips.isEmpty() ? "No detectadas" : String.join(", ", ips);
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
                    String clientId = event.getData().toString();
                    if (!connectedUsersList.getItems().contains(clientId)) {
                        connectedUsersList.getItems().add(clientId);
                        addActivity("Usuario conectado: " + clientId);
                    }
                    break;
                case DISCONNECTED:
                    String disconnectedId = event.getData().toString();
                    connectedUsersList.getItems().remove(disconnectedId);
                    addActivity("Usuario desconectado: " + disconnectedId);
                    break;
                case MESSAGE_RECEIVED:
                    if (event.getData() instanceof com.whatsapp.service.ChatService.ChatMessage) {
                        com.whatsapp.service.ChatService.ChatMessage msg = 
                            (com.whatsapp.service.ChatService.ChatMessage) event.getData();
                        addActivity(msg.getSource() + ": Envió mensaje");
                    }
                    break;
                case FILE_PROGRESS:
                    if (event.getData() instanceof com.whatsapp.service.FileTransferService.FileProgress) {
                        com.whatsapp.service.FileTransferService.FileProgress progress = 
                            (com.whatsapp.service.FileTransferService.FileProgress) event.getData();
                        if (progress.getProgress() == 100.0) {
                            addActivity(event.getSource() + ": Archivo transferido completado");
                        }
                    }
                    break;
                case VIDEO_FRAME:
                    addActivity(event.getSource() + ": Videollamada activa");
                    break;
            }
        });
    }
}

