package com.whatsapp.ui;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.whatsapp.model.Room;
import com.whatsapp.model.Usuario;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.network.observer.NetworkEventObserver;
import com.whatsapp.service.ControlService;
import com.whatsapp.service.NetworkFacade;
import com.whatsapp.service.RoomService;
import com.whatsapp.service.UserAliasRegistry;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

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
    private final RoomService roomService;
    private final ListView<Room> pendingRoomsList;
    private final ListView<Room> activeRoomsList;
    private final Map<String, String> selectedUserConnectionId = new HashMap<>(); // Para controles de admin
    private final Map<Long, RoomChatView> openRoomChats = new HashMap<>(); // Rooms abiertos en chat
    private String serverConnectionId; // Connection ID del servidor (para cuando actúa como cliente)
    private final TextField roomNameField = new TextField();
    private final CheckBox includeAdminCheck = new CheckBox("Incluir admin en el room");

    public ServerView(Usuario currentUser) {
        this.currentUser = currentUser;
        this.networkFacade = new NetworkFacade();
        this.activityList = new ListView<>();
        this.connectedUsersList = new ListView<>();
        this.portField = new TextField("8080");
        this.aliasRegistry = UserAliasRegistry.getInstance();
        this.controlService = new ControlService();
        this.roomService = RoomService.getInstance();
        this.pendingRoomsList = new ListView<>();
        this.activeRoomsList = new ListView<>();
        
        // Configurar RoomService con el username del servidor
        roomService.setServerUsername(currentUser.getUsername());
        
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

        // Panel central - Usuarios conectados, rooms y actividades
        HBox centerBox = new HBox(10);
        centerBox.setPadding(new Insets(10));

        // Panel de usuarios con controles de admin
        VBox usersBox = new VBox(5);
        usersBox.setPrefWidth(250);
        Label usersLabel = new Label("Usuarios Conectados:");
        usersLabel.setFont(Font.font(14));
        connectedUsersList.setPrefHeight(200);
        connectedUsersList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Botones de control de admin
        VBox adminControls = new VBox(5);
        HBox adminRow1 = new HBox(5);
        HBox adminRow2 = new HBox(5);
        HBox adminRow3 = new HBox(5);
        
        Button muteButton = new Button("Silenciar");
        muteButton.setPrefWidth(120);
        muteButton.setWrapText(true);
        muteButton.setOnAction(e -> handleMuteUser());
        
        Button unmuteButton = new Button("Activar Audio");
        unmuteButton.setPrefWidth(120);
        unmuteButton.setWrapText(true);
        unmuteButton.setOnAction(e -> handleUnmuteUser());
        
        Button disableCameraButton = new Button("Desactivar Cámara");
        disableCameraButton.setPrefWidth(120);
        disableCameraButton.setWrapText(true);
        disableCameraButton.setOnAction(e -> handleDisableCamera());
        
        Button enableCameraButton = new Button("Activar Cámara");
        enableCameraButton.setPrefWidth(120);
        enableCameraButton.setWrapText(true);
        enableCameraButton.setOnAction(e -> handleEnableCamera());
        
        Button blockMessagesButton = new Button("Bloquear Mensajes");
        blockMessagesButton.setPrefWidth(120);
        blockMessagesButton.setWrapText(true);
        blockMessagesButton.setOnAction(e -> handleBlockMessages());
        
        Button unblockMessagesButton = new Button("Permitir Mensajes");
        unblockMessagesButton.setPrefWidth(120);
        unblockMessagesButton.setWrapText(true);
        unblockMessagesButton.setOnAction(e -> handleUnblockMessages());
        
        adminRow1.getChildren().addAll(muteButton, unmuteButton);
        adminRow2.getChildren().addAll(disableCameraButton, enableCameraButton);
        adminRow3.getChildren().addAll(blockMessagesButton, unblockMessagesButton);
        adminControls.getChildren().addAll(adminRow1, adminRow2, adminRow3);
        
        usersBox.getChildren().addAll(usersLabel, connectedUsersList, new Label("Controles Admin:"), adminControls);

        // Panel de rooms
        VBox roomsBox = new VBox(5);
        roomsBox.setPrefWidth(250);
        Label createRoomLabel = new Label("Crear Room (admin):");
        createRoomLabel.setFont(Font.font(12));
        roomNameField.setPromptText("Nombre del room");
        includeAdminCheck.setSelected(true);
        Button createRoomButton = new Button("Crear Room");
        createRoomButton.setOnAction(e -> handleCreateRoom());
        VBox createRoomBox = new VBox(5, createRoomLabel, roomNameField, includeAdminCheck, createRoomButton);
        createRoomBox.setPadding(new Insets(5, 0, 5, 0));

        Label pendingRoomsLabel = new Label("Rooms Pendientes:");
        pendingRoomsLabel.setFont(Font.font(12));
        pendingRoomsList.setPrefHeight(150);
        pendingRoomsList.setCellFactory(list -> new ListCell<Room>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText(null);
                } else {
                    String creatorName = room.getCreatorUsername() != null
                        ? room.getCreatorUsername()
                        : aliasRegistry.getAliasOrDefault(room.getCreatorConnectionId());
                    String reason = room.getRequestMessage();
                    StringBuilder summary = new StringBuilder();
                    summary.append(room.getName()).append(" (por ").append(creatorName).append(")");
                    if (room.isIncludeServer()) {
                        summary.append(" [Incluir Admin]");
                    }
                    if (reason != null && !reason.isBlank()) {
                        summary.append(" - Motivo: ").append(reason);
                    }
                    setText(summary.toString());
                }
            }
        });
        
        HBox roomControls = new HBox(5);
        Button approveRoomButton = new Button("Aprobar");
        approveRoomButton.setOnAction(e -> handleApproveRoom());
        Button rejectRoomButton = new Button("Rechazar");
        rejectRoomButton.setOnAction(e -> handleRejectRoom());
        Button refreshPendingButton = new Button("Actualizar");
        refreshPendingButton.setOnAction(e -> refreshRoomsList());
        roomControls.getChildren().addAll(approveRoomButton, rejectRoomButton, refreshPendingButton);
        
        Label activeRoomsLabel = new Label("Rooms Activos:");
        activeRoomsLabel.setFont(Font.font(12));
        activeRoomsList.setPrefHeight(150);
        activeRoomsList.setCellFactory(list -> new ListCell<Room>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText(null);
                } else {
                    setText(room.getName());
                }
            }
        });
        
        VBox activeRoomControls = new VBox(5);
        HBox activeRoomRow1 = new HBox(5);
        HBox activeRoomRow2 = new HBox(5);
        
        Button joinRoomButton = new Button("Unirse al Room");
        joinRoomButton.setPrefWidth(120);
        joinRoomButton.setWrapText(true);
        joinRoomButton.setOnAction(e -> handleJoinRoom());
        
        Button openRoomChatButton = new Button("Abrir Chat del Room");
        openRoomChatButton.setPrefWidth(120);
        openRoomChatButton.setWrapText(true);
        openRoomChatButton.setOnAction(e -> handleOpenRoomChat());
        
        Button leaveRoomButton = new Button("Salir del Room");
        leaveRoomButton.setPrefWidth(120);
        leaveRoomButton.setWrapText(true);
        leaveRoomButton.setOnAction(e -> handleLeaveRoom());
        
        Button closeRoomButton = new Button("Cerrar Room");
        closeRoomButton.setPrefWidth(120);
        closeRoomButton.setWrapText(true);
        closeRoomButton.setOnAction(e -> handleCloseRoom());
        
        activeRoomRow1.getChildren().addAll(joinRoomButton, openRoomChatButton);
        activeRoomRow2.getChildren().addAll(leaveRoomButton, closeRoomButton);
        activeRoomControls.getChildren().addAll(activeRoomRow1, activeRoomRow2);
        
        roomsBox.getChildren().addAll(createRoomBox, pendingRoomsLabel, pendingRoomsList, roomControls,
            activeRoomsLabel, activeRoomsList, activeRoomControls);

        VBox activityBox = new VBox(5);
        Label activityLabel = new Label("Actividades:");
        activityLabel.setFont(Font.font(14));
        activityList.setPrefHeight(400);
        activityBox.getChildren().addAll(activityLabel, activityList);

        centerBox.getChildren().addAll(usersBox, roomsBox, activityBox);
        setCenter(centerBox);
        
        // Configurar selección de usuarios
        connectedUsersList.setOnMouseClicked(e -> {
            String selected = connectedUsersList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Encontrar el connectionId correspondiente
                for (Map.Entry<String, String> entry : connectedUserMap.entrySet()) {
                    if (entry.getValue().equals(selected)) {
                        selectedUserConnectionId.put("selected", entry.getKey());
                        break;
                    }
                }
            }
        });
        
        // Refrescar lista de rooms periódicamente
        refreshRoomsList();

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
            
            // Obtener el connection ID del servidor (usando el primer cliente conectado como referencia)
            // En modo servidor, el servidor puede usar su propio connection ID
            serverConnectionId = "SERVER_" + currentUser.getUsername();
            
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
                        // Agregar usuario a la lista inmediatamente, aunque aún no tenga alias
                        // El alias se actualizará cuando llegue el mensaje CONTROL_USER_ALIAS
                        String displayName = aliasRegistry.getAliasOrDefault(clientId);
                        // Si no tiene alias aún, usar el connectionId temporalmente
                        if (displayName.equals(clientId)) {
                            // Usuario recién conectado, agregarlo con su connectionId
                            connectedUserMap.put(clientId, clientId);
                            refreshConnectedUsersList();
                            addActivity("Usuario conectado: " + clientId);
                        } else {
                            // Usuario con alias, agregarlo con su nombre
                            connectedUserMap.put(clientId, displayName);
                            refreshConnectedUsersList();
                            addActivity("Usuario conectado: " + displayName);
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
                case ROOM_CREATED:
                    System.out.println("[ServerView] Evento ROOM_CREATED recibido, data type: " + 
                        (event.getData() != null ? event.getData().getClass().getName() : "null"));
                    if (event.getData() instanceof Room) {
                        Room room = (Room) event.getData();
                        System.out.println("[ServerView] Room recibido: " + room.getName() + ", Estado: " + room.getEstado());
                        String creatorName = room.getCreatorUsername() != null
                            ? room.getCreatorUsername()
                            : aliasRegistry.getAliasOrDefault(room.getCreatorConnectionId());
                        String reason = room.getRequestMessage();
                        StringBuilder activity = new StringBuilder();
                        activity.append("Nueva solicitud de room: ").append(room.getName())
                                .append(" (por ").append(creatorName).append(")");
                        if (room.isIncludeServer()) {
                            activity.append(" [Quiere incluir al Admin]");
                        }
                        if (reason != null && !reason.isBlank()) {
                            activity.append(" - Motivo: ").append(reason);
                        }
                        addActivity(activity.toString());
                        pendingRoomsList.getItems().add(room);
                        refreshRoomsList();
                    } else {
                        System.out.println("[ServerView] Data no es instancia de Room: " + event.getData());
                    }
                    break;
                case ROOM_APPROVED:
                case ROOM_REJECTED:
                case ROOM_CLOSED:
                    refreshRoomsList();
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
        // No limpiar el mapa, solo actualizar los alias existentes y agregar nuevos
        Set<ControlService.UserDescriptor> descriptors = ControlService.parseUserListJson(data);
        for (ControlService.UserDescriptor descriptor : descriptors) {
            String connectionId = descriptor.getConnectionId();
            String displayName = descriptor.getDisplayName();
            
            // Si el usuario ya está en la lista, actualizar su nombre
            if (connectedUserMap.containsKey(connectionId)) {
                String oldName = connectedUserMap.get(connectionId);
                connectedUserMap.put(connectionId, displayName);
                // Si el nombre cambió (de connectionId a displayName), actualizar actividad
                if (oldName.equals(connectionId) && !displayName.equals(connectionId)) {
                    addActivity("Usuario identificado: " + displayName);
                }
            } else {
                // Usuario nuevo, agregarlo
                connectedUserMap.put(connectionId, displayName);
                addActivity("Usuario conectado: " + displayName);
            }
            aliasRegistry.registerAlias(connectionId, displayName);
        }
        refreshConnectedUsersList();
    }

    private void refreshConnectedUsersList() {
        connectedUsersList.getItems().setAll(connectedUserMap.values());
    }

    private String resolveDisplayName(String connectionId) {
        return aliasRegistry.getAliasOrDefault(connectionId);
    }

    private void handleMuteUser() {
        String connectionId = selectedUserConnectionId.get("selected");
        if (connectionId != null) {
            try {
                controlService.sendAdminControl(connectionId, ControlService.CONTROL_ADMIN_MUTE);
                addActivity("Usuario " + resolveDisplayName(connectionId) + " silenciado");
            } catch (IOException e) {
                showAlert("Error", "No se pudo silenciar al usuario: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Error", "Seleccione un usuario primero", Alert.AlertType.WARNING);
        }
    }

    private void handleUnmuteUser() {
        String connectionId = selectedUserConnectionId.get("selected");
        if (connectionId != null) {
            try {
                controlService.sendAdminControl(connectionId, ControlService.CONTROL_ADMIN_UNMUTE);
                addActivity("Audio activado para " + resolveDisplayName(connectionId));
            } catch (IOException e) {
                showAlert("Error", "No se pudo activar el audio: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Error", "Seleccione un usuario primero", Alert.AlertType.WARNING);
        }
    }

    private void handleDisableCamera() {
        String connectionId = selectedUserConnectionId.get("selected");
        if (connectionId != null) {
            try {
                controlService.sendAdminControl(connectionId, ControlService.CONTROL_ADMIN_DISABLE_CAMERA);
                addActivity("Cámara desactivada para " + resolveDisplayName(connectionId));
            } catch (IOException e) {
                showAlert("Error", "No se pudo desactivar la cámara: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Error", "Seleccione un usuario primero", Alert.AlertType.WARNING);
        }
    }

    private void handleEnableCamera() {
        String connectionId = selectedUserConnectionId.get("selected");
        if (connectionId != null) {
            try {
                controlService.sendAdminControl(connectionId, ControlService.CONTROL_ADMIN_ENABLE_CAMERA);
                addActivity("Cámara activada para " + resolveDisplayName(connectionId));
            } catch (IOException e) {
                showAlert("Error", "No se pudo activar la cámara: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Error", "Seleccione un usuario primero", Alert.AlertType.WARNING);
        }
    }

    private void handleBlockMessages() {
        String connectionId = selectedUserConnectionId.get("selected");
        if (connectionId != null) {
            try {
                controlService.sendAdminControl(connectionId, ControlService.CONTROL_ADMIN_BLOCK_MESSAGES);
                addActivity("Mensajes bloqueados para " + resolveDisplayName(connectionId));
            } catch (IOException e) {
                showAlert("Error", "No se pudieron bloquear los mensajes: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Error", "Seleccione un usuario primero", Alert.AlertType.WARNING);
        }
    }

    private void handleUnblockMessages() {
        String connectionId = selectedUserConnectionId.get("selected");
        if (connectionId != null) {
            try {
                controlService.sendAdminControl(connectionId, ControlService.CONTROL_ADMIN_UNBLOCK_MESSAGES);
                addActivity("Mensajes permitidos para " + resolveDisplayName(connectionId));
            } catch (IOException e) {
                showAlert("Error", "No se pudieron permitir los mensajes: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Error", "Seleccione un usuario primero", Alert.AlertType.WARNING);
        }
    }

    private void handleCreateRoom() {
        String roomName = roomNameField.getText() != null ? roomNameField.getText().trim() : "";
        if (roomName.isEmpty()) {
            showAlert("Error", "Ingrese un nombre para el room", Alert.AlertType.WARNING);
            return;
        }

        Set<String> memberIds = new HashSet<>();
        var selectedDisplays = connectedUsersList.getSelectionModel().getSelectedItems();
        for (String display : selectedDisplays) {
            for (Map.Entry<String, String> entry : connectedUserMap.entrySet()) {
                if (display.equals(entry.getValue())) {
                    memberIds.add(entry.getKey());
                    break;
                }
            }
        }

        boolean includeAdmin = includeAdminCheck.isSelected();
        if (memberIds.isEmpty() && !includeAdmin) {
            showAlert("Error", "Seleccione al menos un miembro o incluya al admin.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Room room = controlService.createRoomAsServer(roomName, memberIds, includeAdmin);
            addActivity("Room creado por admin: " + room.getName());
            roomNameField.clear();
            refreshRoomsList();
        } catch (Exception e) {
            showAlert("Error", "No se pudo crear el room: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleApproveRoom() {
        Room selected = pendingRoomsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                controlService.approveRoom(selected.getId());
                addActivity("Room '" + selected.getName() + "' aprobado");
                refreshRoomsList();
            } catch (IOException e) {
                showAlert("Error", "No se pudo aprobar el room: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Error", "Seleccione un room pendiente primero", Alert.AlertType.WARNING);
        }
    }

    private void handleRejectRoom() {
        Room selected = pendingRoomsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                controlService.rejectRoom(selected.getId());
                addActivity("Room '" + selected.getName() + "' rechazado");
                refreshRoomsList();
            } catch (IOException e) {
                showAlert("Error", "No se pudo rechazar el room: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Error", "Seleccione un room pendiente primero", Alert.AlertType.WARNING);
        }
    }

    private void handleCloseRoom() {
        Room selected = activeRoomsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                controlService.closeRoom(selected.getId());
                addActivity("Room '" + selected.getName() + "' cerrado");
                refreshRoomsList();
            } catch (IOException e) {
                showAlert("Error", "No se pudo cerrar el room: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Error", "Seleccione un room activo primero", Alert.AlertType.WARNING);
        }
    }

    private void refreshRoomsList() {
        Platform.runLater(() -> {
            System.out.println("[ServerView] Refrescando lista de rooms...");
            List<Room> pending = roomService.getPendingRooms();
            System.out.println("[ServerView] Rooms pendientes encontrados: " + pending.size());
            for (Room r : pending) {
                System.out.println("[ServerView]   - " + r.getName() + " (ID: " + r.getId() + ", Estado: " + r.getEstado() + ")");
            }
            if (pending.isEmpty() && !pendingRoomsList.getItems().isEmpty()) {
                // Mantener los que llegaron por eventos si la BD todavía no refleja
                pendingRoomsList.getItems().setAll(pendingRoomsList.getItems());
            } else {
                pendingRoomsList.getItems().setAll(pending);
            }
            
            List<Room> active = roomService.getActiveRooms();
            System.out.println("[ServerView] Rooms activos encontrados: " + active.size());
            activeRoomsList.getItems().setAll(active);
        });
    }

    private void handleJoinRoom() {
        Room selected = activeRoomsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // Agregar el servidor como miembro del room
                // Como servidor, usamos un connection ID especial
                String serverMemberId = "SERVER_" + currentUser.getUsername();
                if (roomService.addMemberToRoom(selected.getId(), serverMemberId)) {
                    addActivity("Servidor unido al room: " + selected.getName());
                    refreshRoomsList();
                } else {
                    showAlert("Error", "No se pudo unir al room", Alert.AlertType.ERROR);
                }
            } catch (Exception e) {
                showAlert("Error", "Error al unirse al room: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Error", "Seleccione un room activo primero", Alert.AlertType.WARNING);
        }
    }

    private void handleOpenRoomChat() {
        Room selected = activeRoomsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Verificar si el servidor es miembro del room
            Optional<Room> roomOpt = roomService.getRoom(selected.getId());
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();
                String serverMemberId = "SERVER_" + currentUser.getUsername();
                
                // Si no es miembro, unirse primero
                if (!room.hasMember(serverMemberId)) {
                    roomService.addMemberToRoom(room.getId(), serverMemberId);
                }

                // Abrir o mostrar la ventana de chat del room
                RoomChatView roomChatView = openRoomChats.get(room.getId());
                if (roomChatView == null) {
                    roomChatView = new RoomChatView(currentUser, room, networkFacade, true);
                    openRoomChats.put(room.getId(), roomChatView);
                }

                javafx.stage.Stage chatStage = new javafx.stage.Stage();
                chatStage.setTitle("Room: " + room.getName() + " - " + currentUser.getUsername());
                chatStage.setScene(new javafx.scene.Scene(roomChatView, 900, 700));
                chatStage.setOnCloseRequest(e -> {
                    // No eliminar de openRoomChats para poder reabrir
                });
                chatStage.show();
                addActivity("Chat del room '" + room.getName() + "' abierto");
            }
        } else {
            showAlert("Error", "Seleccione un room activo primero", Alert.AlertType.WARNING);
        }
    }

    private void handleLeaveRoom() {
        Room selected = activeRoomsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                String serverMemberId = "SERVER_" + currentUser.getUsername();
                if (roomService.removeMemberFromRoom(selected.getId(), serverMemberId)) {
                    addActivity("Servidor salió del room: " + selected.getName());
                    // Cerrar la ventana de chat si está abierta
                    RoomChatView chatView = openRoomChats.remove(selected.getId());
                    if (chatView != null) {
                        javafx.stage.Stage stage = (javafx.stage.Stage) chatView.getScene().getWindow();
                        if (stage != null) {
                            stage.close();
                        }
                    }
                    refreshRoomsList();
                } else {
                    showAlert("Error", "No se pudo salir del room", Alert.AlertType.ERROR);
                }
            } catch (Exception e) {
                showAlert("Error", "Error al salir del room: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Error", "Seleccione un room activo primero", Alert.AlertType.WARNING);
        }
    }
}

