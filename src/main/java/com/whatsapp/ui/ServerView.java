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
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import javafx.stage.FileChooser;

public class ServerView extends BorderPane implements NetworkEventObserver {
    private final NetworkFacade networkFacade;
    private final Usuario currentUser;
    private final ListView<String> activityList;
    private final ListView<String> connectedUsersList;
    private final TextField portField;
    private Button startServerButton;
    private Button stopServerButton;
    private final UserAliasRegistry aliasRegistry;
    private final Map<String, String> connectedUserMap = new LinkedHashMap<>();
    private final ControlService controlService;
    private Button blockClientToClientButton;
    private Button unblockClientToClientButton;
    private TextField broadcastMessageField;
    private Button sendBroadcastMessageButton;
    private Button sendBroadcastFileButton;
    private Button startBroadcastVideoButton;
    private Button stopBroadcastVideoButton;
    private CheckBox selectAllClientsCheckBox;
    private final Map<String, CheckBox> clientCheckBoxes = new HashMap<>();

    public ServerView(Usuario currentUser) {
        this.currentUser = currentUser;
        this.networkFacade = new NetworkFacade();
        this.activityList = new ListView<>();
        this.connectedUsersList = new ListView<>();
        this.portField = new TextField("8080");
        this.aliasRegistry = UserAliasRegistry.getInstance();
        this.controlService = new ControlService();
        
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

        // Panel central - Usuarios conectados, controles y actividades
        HBox centerBox = new HBox(10);
        centerBox.setPadding(new Insets(10));

        // Panel izquierdo - Usuarios conectados con checkboxes
        VBox usersBox = new VBox(5);
        usersBox.setPrefWidth(300);
        
        // Pestañas para Pendientes y Aprobados
        TabPane userTabs = new TabPane();
        
        // Tab de clientes pendientes
        Tab pendingTab = new Tab("Pendientes");
        VBox pendingBox = new VBox(5);
        ListView<String> pendingList = new ListView<>();
        pendingList.setPrefHeight(200);
        pendingList.setId("pendingClientsList");
        Button approveButton = new Button("Aprobar Seleccionado");
        approveButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        approveButton.setOnAction(e -> approveSelectedClient(pendingList));
        Button rejectButton = new Button("Rechazar Seleccionado");
        rejectButton.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black;");
        rejectButton.setOnAction(e -> rejectSelectedClient(pendingList));
        pendingBox.getChildren().addAll(new Label("Clientes Pendientes:"), pendingList, approveButton, rejectButton);
        pendingTab.setContent(pendingBox);
        pendingTab.setClosable(false);
        
        // Tab de clientes aprobados
        Tab approvedTab = new Tab("Aprobados");
        VBox approvedBox = new VBox(5);
        Label usersLabel = new Label("Usuarios Aprobados:");
        usersLabel.setFont(Font.font(14));
        
        selectAllClientsCheckBox = new CheckBox("Seleccionar todos");
        selectAllClientsCheckBox.setOnAction(e -> {
            boolean selected = selectAllClientsCheckBox.isSelected();
            for (CheckBox cb : clientCheckBoxes.values()) {
                cb.setSelected(selected);
            }
        });
        
        ScrollPane usersScrollPane = new ScrollPane();
        VBox usersCheckBoxContainer = new VBox(5);
        usersCheckBoxContainer.setId("usersCheckBoxContainer");
        usersScrollPane.setContent(usersCheckBoxContainer);
        usersScrollPane.setPrefHeight(200);
        usersScrollPane.setFitToWidth(true);
        
        Button kickButton = new Button("Expulsar Seleccionado");
        kickButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        kickButton.setOnAction(e -> kickSelectedClient());
        
        approvedBox.getChildren().addAll(usersLabel, selectAllClientsCheckBox, usersScrollPane, kickButton);
        approvedTab.setContent(approvedBox);
        approvedTab.setClosable(false);
        
        userTabs.getTabs().addAll(pendingTab, approvedTab);
        
        connectedUsersList.setPrefHeight(50);
        connectedUsersList.setVisible(false); // Ocultamos la lista antigua
        
        usersBox.getChildren().addAll(userTabs, connectedUsersList);

        // Panel central - Controles del servidor
        VBox controlsBox = new VBox(10);
        controlsBox.setPrefWidth(250);
        controlsBox.setPadding(new Insets(10));
        controlsBox.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ccc; -fx-border-radius: 5;");
        
        Label controlsLabel = new Label("Controles del Servidor");
        controlsLabel.setFont(Font.font(14));
        controlsLabel.setStyle("-fx-font-weight: bold;");
        
        // Control de comunicación cliente-cliente
        Label blockLabel = new Label("Comunicación Cliente-Cliente:");
        blockClientToClientButton = new Button("Bloquear");
        blockClientToClientButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        blockClientToClientButton.setOnAction(e -> blockClientToClient());
        blockClientToClientButton.setMaxWidth(Double.MAX_VALUE);
        
        unblockClientToClientButton = new Button("Desbloquear");
        unblockClientToClientButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        unblockClientToClientButton.setOnAction(e -> unblockClientToClient());
        unblockClientToClientButton.setMaxWidth(Double.MAX_VALUE);
        
        VBox blockBox = new VBox(5, blockLabel, blockClientToClientButton, unblockClientToClientButton);
        
        Separator sep1 = new Separator();
        
        // Broadcast de mensajes
        Label broadcastLabel = new Label("Enviar Mensaje:");
        broadcastMessageField = new TextField();
        broadcastMessageField.setPromptText("Escribe un mensaje...");
        sendBroadcastMessageButton = new Button("Enviar a Seleccionados");
        sendBroadcastMessageButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        sendBroadcastMessageButton.setOnAction(e -> sendBroadcastMessage());
        sendBroadcastMessageButton.setMaxWidth(Double.MAX_VALUE);
        
        VBox broadcastBox = new VBox(5, broadcastLabel, broadcastMessageField, sendBroadcastMessageButton);
        
        Separator sep2 = new Separator();
        
        // Broadcast de archivos
        sendBroadcastFileButton = new Button("Enviar Archivo a Seleccionados");
        sendBroadcastFileButton.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");
        sendBroadcastFileButton.setOnAction(e -> sendBroadcastFile());
        sendBroadcastFileButton.setMaxWidth(Double.MAX_VALUE);
        
        Separator sep3 = new Separator();
        
        // Broadcast de video
        startBroadcastVideoButton = new Button("Iniciar Videollamada a Seleccionados");
        startBroadcastVideoButton.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white;");
        startBroadcastVideoButton.setOnAction(e -> startBroadcastVideo());
        startBroadcastVideoButton.setMaxWidth(Double.MAX_VALUE);
        
        stopBroadcastVideoButton = new Button("Detener Videollamada");
        stopBroadcastVideoButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        stopBroadcastVideoButton.setOnAction(e -> stopBroadcastVideo());
        stopBroadcastVideoButton.setMaxWidth(Double.MAX_VALUE);
        stopBroadcastVideoButton.setDisable(true);
        
        VBox videoBox = new VBox(5, startBroadcastVideoButton, stopBroadcastVideoButton);
        
        controlsBox.getChildren().addAll(controlsLabel, blockBox, sep1, broadcastBox, sep2, 
            sendBroadcastFileButton, sep3, videoBox);

        // Panel derecho - Actividades
        VBox activityBox = new VBox(5);
        Label activityLabel = new Label("Actividades:");
        activityLabel.setFont(Font.font(14));
        activityList.setPrefHeight(400);
        activityBox.getChildren().addAll(activityLabel, activityList);

        centerBox.getChildren().addAll(usersBox, controlsBox, activityBox);
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
        
        Platform.runLater(() -> {
            startServerButton.setDisable(false);
            stopServerButton.setDisable(true);
            portField.setDisable(false);
            addActivity("Servidor detenido");
            updateStatus("Estado: Desconectado");
            connectedUsersList.getItems().clear();
            connectedUserMap.clear();
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
                    if ("SERVER_UI".equals(event.getSource())) {
                        handleAliasSnapshot(event.getData().toString());
                        break;
                    }
                    String clientId = event.getData().toString();
                    if (!"Servidor iniciado".equals(clientId)) {
                        String displayName = aliasRegistry.getAliasOrDefault(clientId);
                        if (!displayName.equals(clientId)) {
                            connectedUserMap.put(clientId, displayName);
                            refreshConnectedUsersList();
                            // Los nuevos clientes se agregan como pendientes automáticamente
                            addActivity("Usuario conectado (pendiente): " + displayName);
                        }
                    }
                    break;
                case DISCONNECTED:
                    String disconnectedId = event.getData().toString();
                    connectedUserMap.remove(disconnectedId);
                    aliasRegistry.removeAlias(disconnectedId);
                    refreshConnectedUsersList();
                    addActivity("Usuario desconectado: " + resolveDisplayName(disconnectedId));
                    break;
                case MESSAGE_RECEIVED:
                    if (event.getData() instanceof com.whatsapp.service.ChatService.ChatMessage) {
                        com.whatsapp.service.ChatService.ChatMessage msg = 
                            (com.whatsapp.service.ChatService.ChatMessage) event.getData();
                        addActivity(resolveDisplayName(msg.getSource()) + ": Envió mensaje");
                    }
                    break;
                case FILE_PROGRESS:
                    if (event.getData() instanceof com.whatsapp.service.FileTransferService.FileProgress) {
                        com.whatsapp.service.FileTransferService.FileProgress progress = 
                            (com.whatsapp.service.FileTransferService.FileProgress) event.getData();
                        if (progress.getProgress() == 100.0) {
                            addActivity(resolveDisplayName(event.getSource()) + ": Archivo transferido completado");
                        }
                    }
                    break;
                case VIDEO_FRAME:
                    addActivity(resolveDisplayName(event.getSource()) + ": Videollamada activa");
                    break;
                default:
                    break;
            }
        });
    }

    private void handleAliasSnapshot(String data) {
        if (!data.startsWith("[") || !data.endsWith("]")) {
            return;
        }
        connectedUserMap.clear();
        for (ControlService.UserDescriptor descriptor : ControlService.parseUserListJson(data)) {
            connectedUserMap.put(descriptor.getConnectionId(), descriptor.getDisplayName());
            aliasRegistry.registerAlias(descriptor.getConnectionId(), descriptor.getDisplayName());
        }
        refreshConnectedUsersList();
    }

    private void refreshConnectedUsersList() {
        connectedUsersList.getItems().setAll(connectedUserMap.values());
        
        // Actualizar checkboxes de clientes aprobados
        Platform.runLater(() -> {
            VBox container = (VBox) lookup("#usersCheckBoxContainer");
            if (container == null) return;
            
            container.getChildren().clear();
            clientCheckBoxes.clear();
            
            Set<String> approvedClients = ControlService.getApprovedClients();
            for (Map.Entry<String, String> entry : connectedUserMap.entrySet()) {
                String connectionId = entry.getKey();
                String displayName = entry.getValue();
                
                // Solo mostrar clientes aprobados
                if (approvedClients.contains(connectionId)) {
                    CheckBox checkBox = new CheckBox(displayName);
                    checkBox.setUserData(connectionId);
                    clientCheckBoxes.put(connectionId, checkBox);
                    container.getChildren().add(checkBox);
                }
            }
            
            // Actualizar lista de pendientes
            ListView<String> pendingList = (ListView<String>) lookup("#pendingClientsList");
            if (pendingList != null) {
                Set<String> pendingClients = controlService.getPendingClients();
                pendingList.getItems().clear();
                for (String connectionId : pendingClients) {
                    String displayName = aliasRegistry.getAliasOrDefault(connectionId);
                    pendingList.getItems().add(displayName + " (" + connectionId + ")");
                }
            }
        });
    }

    private String resolveDisplayName(String connectionId) {
        return aliasRegistry.getAliasOrDefault(connectionId);
    }

    private void blockClientToClient() {
        try {
            networkFacade.blockClientToClientCommunication();
            addActivity("Comunicación cliente-cliente BLOQUEADA");
            blockClientToClientButton.setDisable(true);
            unblockClientToClientButton.setDisable(false);
        } catch (IOException e) {
            showAlert("Error", "No se pudo bloquear la comunicación: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void unblockClientToClient() {
        try {
            networkFacade.unblockClientToClientCommunication();
            addActivity("Comunicación cliente-cliente DESBLOQUEADA");
            blockClientToClientButton.setDisable(false);
            unblockClientToClientButton.setDisable(true);
        } catch (IOException e) {
            showAlert("Error", "No se pudo desbloquear la comunicación: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void sendBroadcastMessage() {
        String message = broadcastMessageField.getText().trim();
        if (message.isEmpty()) {
            showAlert("Error", "Escribe un mensaje antes de enviar", Alert.AlertType.WARNING);
            return;
        }

        Set<String> selectedClients = getSelectedClients();
        if (selectedClients.isEmpty()) {
            showAlert("Error", "Selecciona al menos un cliente", Alert.AlertType.WARNING);
            return;
        }

        try {
            if (selectedClients.size() == connectedUserMap.size()) {
                // Broadcast a todos
                networkFacade.broadcastMessage(message, currentUser.getId());
                addActivity("Mensaje broadcast enviado a todos los clientes: " + message);
            } else {
                // Enviar solo a seleccionados
                networkFacade.sendMessageToClients(selectedClients, message, currentUser.getId());
                addActivity("Mensaje enviado a " + selectedClients.size() + " clientes seleccionados: " + message);
            }
            broadcastMessageField.clear();
        } catch (IOException e) {
            showAlert("Error", "No se pudo enviar el mensaje: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void sendBroadcastFile() {
        Set<String> selectedClients = getSelectedClients();
        if (selectedClients.isEmpty()) {
            showAlert("Error", "Selecciona al menos un cliente", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo para enviar");
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile == null) {
            return;
        }

        try {
            String serverConnectionId = networkFacade.getPrimaryConnectionId();
            if (serverConnectionId == null) {
                showAlert("Error", "No hay conexión disponible", Alert.AlertType.ERROR);
                return;
            }

            if (selectedClients.size() == connectedUserMap.size()) {
                // Broadcast a todos
                networkFacade.broadcastFile(selectedFile.getAbsolutePath(), currentUser.getId());
                addActivity("Archivo broadcast enviado a todos los clientes: " + selectedFile.getName());
            } else {
                // Enviar solo a seleccionados
                networkFacade.sendFileToClients(selectedClients, selectedFile.getAbsolutePath(), currentUser.getId());
                addActivity("Archivo enviado a " + selectedClients.size() + " clientes seleccionados: " + selectedFile.getName());
            }
        } catch (IOException e) {
            showAlert("Error", "No se pudo enviar el archivo: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void startBroadcastVideo() {
        Set<String> selectedClients = getSelectedClients();
        if (selectedClients.isEmpty()) {
            showAlert("Error", "Selecciona al menos un cliente", Alert.AlertType.WARNING);
            return;
        }

        try {
            String serverConnectionId = networkFacade.getPrimaryConnectionId();
            if (serverConnectionId == null) {
                showAlert("Error", "No hay conexión disponible", Alert.AlertType.ERROR);
                return;
            }

            if (selectedClients.size() == connectedUserMap.size()) {
                // Broadcast a todos
                networkFacade.startBroadcastVideoCall(serverConnectionId);
                addActivity("Videollamada broadcast iniciada a todos los clientes");
            } else {
                // Enviar solo a seleccionados
                networkFacade.startVideoCallToClients(serverConnectionId, selectedClients);
                addActivity("Videollamada iniciada a " + selectedClients.size() + " clientes seleccionados");
            }
            
            startBroadcastVideoButton.setDisable(true);
            stopBroadcastVideoButton.setDisable(false);
        } catch (Exception e) {
            showAlert("Error", "No se pudo iniciar la videollamada: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void stopBroadcastVideo() {
        try {
            networkFacade.stopVideoCall();
            addActivity("Videollamada detenida");
            startBroadcastVideoButton.setDisable(false);
            stopBroadcastVideoButton.setDisable(true);
        } catch (Exception e) {
            showAlert("Error", "No se pudo detener la videollamada: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private Set<String> getSelectedClients() {
        Set<String> selected = new HashSet<>();
        for (CheckBox checkBox : clientCheckBoxes.values()) {
            if (checkBox.isSelected()) {
                selected.add((String) checkBox.getUserData());
            }
        }
        return selected;
    }

    private void approveSelectedClient(ListView<String> pendingList) {
        String selected = pendingList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Selecciona un cliente pendiente", Alert.AlertType.WARNING);
            return;
        }
        
        // Extraer connectionId del formato "DisplayName (connectionId)"
        int startIdx = selected.lastIndexOf("(");
        int endIdx = selected.lastIndexOf(")");
        if (startIdx == -1 || endIdx == -1) {
            showAlert("Error", "Formato de cliente inválido", Alert.AlertType.ERROR);
            return;
        }
        String connectionId = selected.substring(startIdx + 1, endIdx);
        
        try {
            controlService.approveClient(connectionId);
            addActivity("Cliente aprobado: " + aliasRegistry.getAliasOrDefault(connectionId));
            refreshConnectedUsersList();
        } catch (IOException e) {
            showAlert("Error", "No se pudo aprobar el cliente: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void rejectSelectedClient(ListView<String> pendingList) {
        String selected = pendingList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Selecciona un cliente pendiente", Alert.AlertType.WARNING);
            return;
        }
        
        int startIdx = selected.lastIndexOf("(");
        int endIdx = selected.lastIndexOf(")");
        if (startIdx == -1 || endIdx == -1) {
            showAlert("Error", "Formato de cliente inválido", Alert.AlertType.ERROR);
            return;
        }
        String connectionId = selected.substring(startIdx + 1, endIdx);
        
        try {
            controlService.rejectClient(connectionId);
            addActivity("Cliente rechazado: " + aliasRegistry.getAliasOrDefault(connectionId));
            refreshConnectedUsersList();
        } catch (IOException e) {
            showAlert("Error", "No se pudo rechazar el cliente: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void kickSelectedClient() {
        Set<String> selected = getSelectedClients();
        if (selected.isEmpty()) {
            showAlert("Error", "Selecciona al menos un cliente aprobado", Alert.AlertType.WARNING);
            return;
        }
        
        for (String connectionId : selected) {
            try {
                String displayName = aliasRegistry.getAliasOrDefault(connectionId);
                controlService.kickClient(connectionId);
                addActivity("Cliente expulsado: " + displayName);
            } catch (IOException e) {
                showAlert("Error", "No se pudo expulsar el cliente: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
        refreshConnectedUsersList();
    }
}

