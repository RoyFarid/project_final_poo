package com.whatsapp.ui;

import com.whatsapp.model.Room;
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
    private ListView<Room> roomsList;
    private final String connectedHost;
    private final int connectedPort;
    private final Label statusLabel;
    private final Map<Long, Room> availableRooms = new LinkedHashMap<>();

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
        updateStatus("Conectado a " + connectedHost + ":" + connectedPort);
        requestUserListRefresh();
        requestRoomList();
    }

    private void setupUI() {
        // Panel superior - InformaciÃ³n del cliente
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(20));
        topPanel.setStyle("-fx-background-color: #128C7E;");

        Text title = new Text("Modo Cliente - " + currentUser.getUsername());
        title.setFont(Font.font(20));
        title.setStyle("-fx-fill: white;");

        Button disconnectButton = new Button("Desconectar");
        disconnectButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        disconnectButton.setOnAction(e -> disconnectFromServer());

        HBox actionBox = new HBox(10, new Label("SesiÃ³n activa en: " + connectedHost + ":" + connectedPort), disconnectButton);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topPanel.getChildren().addAll(title, actionBox);
        setTop(topPanel);

        // Panel central - Lista de usuarios y rooms
        HBox centerBox = new HBox(10);
        centerBox.setPadding(new Insets(20));

        // Panel de usuarios
        VBox usersBox = new VBox(5);
        usersBox.setPrefWidth(300);
        Label usersLabel = new Label("Usuarios Conectados (Haz click para chatear):");
        usersLabel.setFont(Font.font(14));

        usersList.setPrefHeight(300);
        usersList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ControlService.UserDescriptor item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String alias = aliasRegistry.getAliasOrDefault(item.getConnectionId());
                    setText(alias);
                }
            }
        });
        usersList.setOnMouseClicked(e -> {
            ControlService.UserDescriptor selected = usersList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openChatWindow(selected.getConnectionId(), aliasRegistry.getAliasOrDefault(selected.getConnectionId()));
            }
        });
        usersBox.getChildren().addAll(usersLabel, usersList);

        // Panel de rooms
        VBox roomsBox = new VBox(5);
        roomsBox.setPrefWidth(300);
        Label roomsLabel = new Label("Rooms Disponibles:");
        roomsLabel.setFont(Font.font(14));

        roomsList = new ListView<>();
        roomsList.setPrefHeight(200);
        roomsList.setCellFactory(list -> new javafx.scene.control.ListCell<Room>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText(null);
                } else {
                    setText(room.getName() + " (" + room.getMembers().size() + " miembros)");
                }
            }
        });
        roomsList.setOnMouseClicked(e -> {
            Room selected = roomsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                joinRoom(selected);
            }
        });

        // Botones de rooms
        HBox roomButtons = new HBox(5);
        Button createRoomButton = new Button("Crear Room");
        createRoomButton.setOnAction(e -> showCreateRoomDialog());
        Button refreshRoomsButton = new Button("Actualizar");
        refreshRoomsButton.setOnAction(e -> requestRoomList());
        roomButtons.getChildren().addAll(createRoomButton, refreshRoomsButton);

        roomsBox.getChildren().addAll(roomsLabel, roomsList, roomButtons);

        centerBox.getChildren().addAll(usersBox, roomsBox);
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
            availableRooms.clear();
            roomsList.getItems().clear();
        });
    }

    private void openChatWindow(String connectionId, String displayName) {
        String serverConnectionId = networkFacade.getPrimaryConnectionId();
        if (serverConnectionId == null) {
            showAlert("Error", "No hay conexiÃ³n activa con el servidor.", Alert.AlertType.ERROR);
            return;
        }

        ChatView chatView = new ChatView(currentUser, connectionId, serverConnectionId, networkFacade);
        javafx.stage.Stage chatStage = new javafx.stage.Stage();
        chatStage.setTitle("Chat con " + displayName);
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

    private void showCreateRoomDialog() {
        Dialog<RoomCreationData> dialog = new Dialog<>();
        dialog.setTitle("Crear Room");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        TextField roomNameField = new TextField();
        roomNameField.setPromptText("Nombre del room");
        roomNameField.setId("roomName");

        // Lista de usuarios para seleccionar miembros
        ListView<ControlService.UserDescriptor> memberSelectionList = new ListView<>();
        memberSelectionList.getItems().addAll(serverUserMap.values());
        memberSelectionList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        memberSelectionList.setPrefHeight(200);
        memberSelectionList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ControlService.UserDescriptor item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(aliasRegistry.getAliasOrDefault(item.getConnectionId()));
                }
            }
        });

        // Checkbox para incluir al servidor/admin
        CheckBox includeServerCheckbox = new CheckBox("Incluir al Servidor/Admin en el room");
        includeServerCheckbox.setSelected(false);

        TextArea reasonField = new TextArea();
        reasonField.setPromptText("Mensaje para el servidor (motivo de la solicitud)");
        reasonField.setWrapText(true);
        reasonField.setPrefRowCount(3);

        vbox.getChildren().addAll(
            new Label("Nombre del room:"),
            roomNameField,
            new Label("Selecciona miembros (Ctrl+Click para multiples):"),
            memberSelectionList,
            includeServerCheckbox,
            new Label("Mensaje para el administrador:"),
            reasonField
        );

        dialog.getDialogPane().setContent(vbox);
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String roomName = roomNameField.getText().trim();
                if (roomName.isEmpty()) {
                    showAlert("Error", "El nombre del room no puede estar vacÃ­o", Alert.AlertType.ERROR);
                    return null;
                }

                Set<String> selectedMembers = new HashSet<>();
                for (ControlService.UserDescriptor desc : memberSelectionList.getSelectionModel().getSelectedItems()) {
                    selectedMembers.add(desc.getConnectionId());
                }

                // Agregar servidor si está seleccionado
                boolean includeServer = includeServerCheckbox.isSelected();

                return new RoomCreationData(roomName, selectedMembers, reasonField.getText().trim(), includeServer);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(data -> {
            createRoom(data.roomName, data.members, data.requestMessage, data.includeServer);
        });
    }

    private void createRoom(String roomName, Set<String> memberConnectionIds, String requestMessage, boolean includeServer) {
        try {
            String serverConnectionId = networkFacade.getPrimaryConnectionId();
            if (serverConnectionId == null) {
                showAlert("Error", "No hay conexión activa con el servidor.", Alert.AlertType.ERROR);
                return;
            }

            // Construir payload: roomName|creatorUsername|member1,member2,member3|mensaje|includeServer
            String membersStr = String.join(",", memberConnectionIds);
            String payload = encodeBase64(roomName) + "|" + 
                           encodeBase64(currentUser.getUsername()) + "|" + 
                           encodeBase64(membersStr) + "|" +
                           encodeBase64(requestMessage == null ? "" : requestMessage) + "|" +
                           (includeServer ? "true" : "false");

            controlService.sendControlMessage(serverConnectionId, ControlService.CONTROL_ROOM_CREATE_REQUEST, payload);
            updateStatus("Solicitud de room '" + roomName + "' enviada. Esperando aprobación...");
        } catch (IOException e) {
            showAlert("Error", "No se pudo crear el room: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void joinRoom(Room room) {
        try {
            String serverConnectionId = networkFacade.getPrimaryConnectionId();
            if (serverConnectionId == null) {
                showAlert("Error", "No hay conexiÃ³n activa con el servidor.", Alert.AlertType.ERROR);
                return;
            }

            String payload = encodeBase64(String.valueOf(room.getId()));
            controlService.sendControlMessage(serverConnectionId, ControlService.CONTROL_ROOM_JOIN_REQUEST, payload);
            updateStatus("Solicitando unirse al room '" + room.getName() + "'...");
        } catch (IOException e) {
            showAlert("Error", "No se pudo unir al room: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void requestRoomList() {
        try {
            String serverConnectionId = networkFacade.getPrimaryConnectionId();
            if (serverConnectionId != null) {
                controlService.sendControlMessage(serverConnectionId, ControlService.CONTROL_ROOM_LIST, "");
            }
        } catch (IOException e) {
            showAlert("Error", "No se pudo obtener la lista de rooms: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String encodeBase64(String value) {
        return java.util.Base64.getEncoder().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static class RoomCreationData {
        final String roomName;
        final Set<String> members;
        final String requestMessage;
        final boolean includeServer;

        RoomCreationData(String roomName, Set<String> members, String requestMessage, boolean includeServer) {
            this.roomName = roomName;
            this.members = members;
            this.requestMessage = requestMessage;
            this.includeServer = includeServer;
        }
    }

    @Override
    public void onNetworkEvent(NetworkEvent event) {
        Platform.runLater(() -> {
            switch (event.getType()) {
                case CONNECTED:
                    if ("SERVER".equals(event.getSource()) && event.getData() instanceof String) {
                        handleServerConnectedPayload((String) event.getData());
                    }
                    break;
                case DISCONNECTED:
                    if (event.getData() instanceof String) {
                        String payload = (String) event.getData();
                        serverUserMap.remove(payload);
                        aliasRegistry.removeAlias(payload);
                        refreshUsersList();
                    }
                    break;
                case ROOM_CREATED:
                    if (event.getData() instanceof Room) {
                        Room room = (Room) event.getData();
                        availableRooms.put(room.getId(), room);
                        refreshRoomsList();
                        requestRoomList();
                    } else if (event.getData() instanceof String) {
                        // Parsear lista de rooms desde JSON
                        // Por ahora, solo actualizamos la lista
                        requestRoomList();
                    }
                    break;
                case ROOM_LIST:
                    if (event.getData() instanceof java.util.List<?> list) {
                        availableRooms.clear();
                        for (Object obj : list) {
                            if (obj instanceof ControlService.RoomSummary summary) {
                                Room room = new Room();
                                room.setId(summary.getId());
                                room.setName(summary.getName());
                                room.setCreatorUsername(summary.getCreatorUsername());
                                availableRooms.put(room.getId(), room);
                            }
                        }
                        refreshRoomsList();
                        updateStatus("Rooms sincronizados con el servidor");
                    }
                    break;
                case ROOM_APPROVED:
                    if (event.getData() instanceof Room) {
                        Room room = (Room) event.getData();
                        availableRooms.put(room.getId(), room);
                        refreshRoomsList();
                        showAlert("Room Aprobado", "El room '" + room.getName() + "' ha sido aprobado y estÃ¡ activo.", Alert.AlertType.INFORMATION);
                        requestRoomList();
                    }
                    break;
                case ROOM_REJECTED:
                    if (event.getData() instanceof Room) {
                        Room room = (Room) event.getData();
                        availableRooms.remove(room.getId());
                        refreshRoomsList();
                        showAlert("Room Rechazado", "El room '" + room.getName() + "' ha sido rechazado por el servidor.", Alert.AlertType.WARNING);
                        requestRoomList();
                    }
                    break;
                case ROOM_MEMBER_ADDED:
                    if (event.getData() instanceof ControlService.RoomJoinResponse joinResponse) {
                        if (joinResponse.isSuccess()) {
                            updateStatus("Unido al room " + joinResponse.getRoomId());
                            requestRoomList();
                        } else {
                            showAlert("Room", joinResponse.getMessage(), Alert.AlertType.WARNING);
                        }
                    }
                    break;
                default:
                    break;
            }
        });
    }

    private void refreshRoomsList() {
        roomsList.getItems().setAll(availableRooms.values());
    }
}


