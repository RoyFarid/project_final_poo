package com.whatsapp.service;

import com.whatsapp.model.Room;
import com.whatsapp.model.Usuario;
import com.whatsapp.network.ConnectionManager;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.protocol.MessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio para manejar mensajes de control, como la lista de usuarios conectados
 */
public class ControlService {
    private static final Logger logger = LoggerFactory.getLogger(ControlService.class);
    private final ConnectionManager connectionManager;
    private final EventAggregator eventAggregator;
    private final LogService logService;
    private final AtomicInteger correlIdGenerator;
    private String traceId;
    private final UserAliasRegistry aliasRegistry;
    private final RoomService roomService;
    
    // Tipos de mensajes de control
    public static final byte CONTROL_USER_LIST = 1;
    public static final byte CONTROL_USER_CONNECTED = 2;
    public static final byte CONTROL_USER_DISCONNECTED = 3;
    public static final byte CONTROL_USER_ALIAS = 4;
    public static final byte CONTROL_AUTH_REQUEST = 5;
    public static final byte CONTROL_AUTH_RESPONSE = 6;
    public static final byte CONTROL_REGISTER_REQUEST = 7;
    public static final byte CONTROL_REGISTER_RESPONSE = 8;
    // Room control messages
    public static final byte CONTROL_ROOM_CREATE_REQUEST = 9;
    public static final byte CONTROL_ROOM_CREATE_RESPONSE = 10;
    public static final byte CONTROL_ROOM_APPROVE = 11;
    public static final byte CONTROL_ROOM_REJECT = 12;
    public static final byte CONTROL_ROOM_JOIN_REQUEST = 13;
    public static final byte CONTROL_ROOM_JOIN_RESPONSE = 14;
    public static final byte CONTROL_ROOM_LEAVE = 15;
    public static final byte CONTROL_ROOM_CLOSE = 16;
    public static final byte CONTROL_ROOM_LIST = 17;
    // Admin control messages
    public static final byte CONTROL_ADMIN_MUTE = 18;
    public static final byte CONTROL_ADMIN_UNMUTE = 19;
    public static final byte CONTROL_ADMIN_DISABLE_CAMERA = 20;
    public static final byte CONTROL_ADMIN_ENABLE_CAMERA = 21;
    public static final byte CONTROL_ADMIN_BLOCK_MESSAGES = 22;
    public static final byte CONTROL_ADMIN_UNBLOCK_MESSAGES = 23;

    public ControlService() {
        this.connectionManager = ConnectionManager.getInstance();
        this.eventAggregator = EventAggregator.getInstance();
        this.logService = LogService.getInstance();
        this.correlIdGenerator = new AtomicInteger(0);
        this.traceId = logService.generateTraceId();
        this.aliasRegistry = UserAliasRegistry.getInstance();
        this.roomService = RoomService.getInstance();
    }

    /**
     * Envía la lista de usuarios conectados a un cliente específico
     */
    public void sendUserList(String connectionId) throws IOException {
        Set<String> connectedUsers = new java.util.HashSet<>(connectionManager.getConnectedClients());
        logger.info("Enviando lista de usuarios a " + connectionId + ". Usuarios: " + connectedUsers);
        String userListJson = buildUserListJson(connectedUsers, connectionId);
        logger.info("JSON generado: " + userListJson);
        sendControlMessage(connectionId, CONTROL_USER_LIST, userListJson);
        logger.info("Mensaje de control enviado exitosamente");
    }

    public void broadcastUserList() {
        for (String clientId : connectionManager.getConnectedClients()) {
            try {
                sendUserList(clientId);
            } catch (IOException e) {
                logger.error("No se pudo enviar lista de usuarios a " + clientId, e);
            }
        }
        publishUserListSnapshotForServer();
    }

    private void publishUserListSnapshotForServer() {
        Set<String> connectedUsers = new java.util.HashSet<>(connectionManager.getConnectedClients());
        String snapshot = buildUserListJson(connectedUsers, null);
        eventAggregator.publish(new NetworkEvent(
            NetworkEvent.EventType.CONNECTED,
            snapshot,
            "SERVER_UI"
        ));
    }

    /**
     * Notifica a todos los clientes que un usuario se conectó
     */
    public void notifyUserConnected(String userId) throws IOException {
        sendControlMessageToAll(CONTROL_USER_CONNECTED, formatUserEntry(userId));
    }

    /**
     * Notifica a todos los clientes que un usuario se desconectó
     */
    public void notifyUserDisconnected(String userId) throws IOException {
        aliasRegistry.removeAlias(userId);
        sendControlMessageToAll(CONTROL_USER_DISCONNECTED, userId);
        broadcastUserList();
    }

    /**
     * Envía un mensaje de control a un cliente específico
     */
    public void sendControlMessage(String connectionId, byte controlType, String data) throws IOException {
        try {
            int correlId = correlIdGenerator.incrementAndGet();
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            int checksum = calculateChecksum(dataBytes);
            
            // Crear header con tipo CONTROL
            MessageHeader header = new MessageHeader(
                MessageHeader.MessageType.CONTROL,
                dataBytes.length + 1, // +1 para el tipo de control
                correlId,
                checksum
            );

            // Serializar mensaje completo: header + tipoControl + datos
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(header.toBytes());
            baos.write(controlType); // Tipo de control
            baos.write(dataBytes);
            byte[] fullMessage = baos.toByteArray();

            connectionManager.send(connectionId, fullMessage);
            logger.info("Mensaje de control enviado a " + connectionId + " (tipo: " + controlType + ", datos: " + data + ")");
            logService.logInfo("Mensaje de control enviado a " + connectionId, "ControlService", traceId, null);
        } catch (Exception e) {
            logger.error("Error enviando mensaje de control", e);
            throw new IOException("Error enviando mensaje de control", e);
        }
    }

    public void sendAliasUpdate(String connectionId, String alias) throws IOException {
        if (connectionId == null || alias == null || alias.isBlank()) {
            return;
        }
        sendControlMessage(connectionId, CONTROL_USER_ALIAS, alias);
    }

    public void sendAuthRequest(String serverConnectionId, String username, String password) throws IOException {
        String payload = encodeCredential(username) + "|" + encodeCredential(password);
        sendControlMessage(serverConnectionId, CONTROL_AUTH_REQUEST, payload);
    }

    public void sendRegisterRequest(String serverConnectionId, String username, String password, String email) throws IOException {
        String payload = encodeCredential(username) + "|" + encodeCredential(password) + "|" + encodeCredential(email);
        sendControlMessage(serverConnectionId, CONTROL_REGISTER_REQUEST, payload);
    }

    /**
     * Envía un mensaje de control a todos los clientes
     */
    private void sendControlMessageToAll(byte controlType, String data) {
        Set<String> clients = connectionManager.getConnectedClients();
        for (String clientId : clients) {
            try {
                sendControlMessage(clientId, controlType, data);
            } catch (IOException e) {
                logger.error("Error enviando mensaje de control a " + clientId, e);
            }
        }
    }

    /**
     * Procesa un mensaje de control recibido
     */
    public void handleControlMessage(byte[] data, String source) {
        try {
            logger.info("Procesando mensaje de control recibido de " + source + ", tamaño: " + data.length);
            // Parsear header
            byte[] headerBytes = new byte[MessageHeader.HEADER_SIZE];
            System.arraycopy(data, 0, headerBytes, 0, MessageHeader.HEADER_SIZE);
            MessageHeader header = MessageHeader.fromBytes(headerBytes);
            logger.info("Header parseado - Tipo: " + header.getTipo() + ", Longitud: " + header.getLongitud());

            if (header.getTipo() == MessageHeader.MessageType.CONTROL) {
                // Leer tipo de control
                byte controlType = data[MessageHeader.HEADER_SIZE];
                
                // Leer datos
                byte[] dataBytes = new byte[header.getLongitud() - 1]; // -1 porque el tipo de control ya se leyó
                System.arraycopy(data, MessageHeader.HEADER_SIZE + 1, dataBytes, 0, dataBytes.length);
                String controlData = new String(dataBytes, StandardCharsets.UTF_8);
                logger.info("Tipo de control: " + controlType + ", Datos: " + controlData);

                // Verificar checksum
                int calculatedChecksum = calculateChecksum(dataBytes);
                if (calculatedChecksum != header.getChecksum()) {
                    logger.warn("Checksum inválido en mensaje de control recibido");
                    return;
                }

                // Procesar según el tipo de control
                switch (controlType) {
                    case CONTROL_USER_LIST:
                        // Publicar evento con la lista de usuarios
                        logger.info("Lista de usuarios recibida: " + controlData);
                        eventAggregator.publish(new NetworkEvent(
                            NetworkEvent.EventType.CONNECTED,
                            controlData, // JSON con la lista de usuarios
                            "SERVER"
                        ));
                        logService.logInfo("Lista de usuarios recibida: " + controlData, "ControlService", traceId, null);
                        break;
                    case CONTROL_USER_CONNECTED:
                        eventAggregator.publish(new NetworkEvent(
                            NetworkEvent.EventType.CONNECTED,
                            controlData, // ID del usuario conectado
                            "SERVER"
                        ));
                        break;
                    case CONTROL_USER_DISCONNECTED:
                        eventAggregator.publish(new NetworkEvent(
                            NetworkEvent.EventType.DISCONNECTED,
                            controlData, // ID del usuario desconectado
                            "SERVER"
                        ));
                        break;
                    case CONTROL_USER_ALIAS:
                        if (connectionManager.isServerMode()) {
                            aliasRegistry.registerAlias(source, controlData);
                            broadcastUserList();
                        } else {
                            aliasRegistry.registerAlias(source, controlData);
                        }
                        break;
                    case CONTROL_AUTH_REQUEST:
                        if (connectionManager.isServerMode()) {
                            handleAuthRequest(controlData, source);
                        }
                        break;
                    case CONTROL_AUTH_RESPONSE:
                        OperationResultPayload authResult = OperationResultPayload.fromPayload(controlData);
                        eventAggregator.publish(new NetworkEvent(
                            NetworkEvent.EventType.AUTH_RESULT,
                            authResult,
                            source
                        ));
                        break;
                    case CONTROL_REGISTER_REQUEST:
                        if (connectionManager.isServerMode()) {
                            handleRegisterRequest(controlData, source);
                        }
                        break;
                    case CONTROL_REGISTER_RESPONSE:
                        OperationResultPayload registerResult = OperationResultPayload.fromPayload(controlData);
                        eventAggregator.publish(new NetworkEvent(
                            NetworkEvent.EventType.REGISTER_RESULT,
                            registerResult,
                            source
                        ));
                        break;
                    case CONTROL_ROOM_CREATE_REQUEST:
                        if (connectionManager.isServerMode()) {
                            handleRoomCreateRequest(controlData, source);
                        }
                        break;
                    case CONTROL_ROOM_CREATE_RESPONSE:
                        eventAggregator.publish(new NetworkEvent(
                            NetworkEvent.EventType.ROOM_CREATED,
                            controlData,
                            source
                        ));
                        break;
                    case CONTROL_ROOM_JOIN_REQUEST:
                        if (connectionManager.isServerMode()) {
                            handleRoomJoinRequest(controlData, source);
                        }
                        break;
                    case CONTROL_ROOM_JOIN_RESPONSE:
                        RoomJoinResponse joinResponse = parseRoomJoinResponse(controlData);
                        eventAggregator.publish(new NetworkEvent(
                            NetworkEvent.EventType.ROOM_MEMBER_ADDED,
                            joinResponse,
                            source
                        ));
                        break;
                    case CONTROL_ROOM_LEAVE:
                        if (connectionManager.isServerMode()) {
                            handleRoomLeave(controlData, source);
                        }
                        break;
                    case CONTROL_ROOM_APPROVE:
                        if (!connectionManager.isServerMode()) {
                            eventAggregator.publish(new NetworkEvent(
                                NetworkEvent.EventType.ROOM_APPROVED,
                                parseRoomPayload(controlData, Room.EstadoRoom.ACTIVO),
                                source
                            ));
                        }
                        break;
                    case CONTROL_ROOM_REJECT:
                        if (!connectionManager.isServerMode()) {
                            eventAggregator.publish(new NetworkEvent(
                                NetworkEvent.EventType.ROOM_REJECTED,
                                parseRoomPayload(controlData, Room.EstadoRoom.RECHAZADO),
                                source
                            ));
                        }
                        break;
                    case CONTROL_ROOM_CLOSE:
                        if (!connectionManager.isServerMode()) {
                            eventAggregator.publish(new NetworkEvent(
                                NetworkEvent.EventType.ROOM_CLOSED,
                                parseRoomPayload(controlData, Room.EstadoRoom.CERRADO),
                                source
                            ));
                        }
                        break;
                    case CONTROL_ROOM_LIST:
                        if (connectionManager.isServerMode()) {
                            try {
                                sendRoomList(source);
                            } catch (IOException e) {
                                logger.error("No se pudo enviar lista de rooms a {}", source, e);
                            }
                        } else {
                            List<RoomSummary> summaries = parseRoomListJson(controlData);
                            eventAggregator.publish(new NetworkEvent(
                                NetworkEvent.EventType.ROOM_LIST,
                                summaries,
                                source
                            ));
                        }
                        break;
                    case CONTROL_ADMIN_MUTE:
                    case CONTROL_ADMIN_UNMUTE:
                    case CONTROL_ADMIN_DISABLE_CAMERA:
                    case CONTROL_ADMIN_ENABLE_CAMERA:
                    case CONTROL_ADMIN_BLOCK_MESSAGES:
                    case CONTROL_ADMIN_UNBLOCK_MESSAGES:
                        // Estos se manejan directamente desde el servidor
                        eventAggregator.publish(new NetworkEvent(
                            NetworkEvent.EventType.ERROR,
                            controlData,
                            source
                        ));
                        break;
                }
            }
        } catch (Exception e) {
            logger.error("Error procesando mensaje de control", e);
        }
    }

    /**
     * Construye un JSON simple con la lista de usuarios
     */
    private String buildUserListJson(Set<String> users, String excludeConnectionId) {
        StringBuilder json = new StringBuilder();
        json.append("[");
        boolean first = true;
        for (String user : users) {
            if (excludeConnectionId != null && excludeConnectionId.equals(user)) {
                continue;
            }
            String alias = aliasRegistry.getAliasOrDefault(user);
            if (alias == null || alias.equals(user)) {
                continue;
            }
            if (!first) {
                json.append(",");
            }
            json.append("\"")
                .append(escapeJson(user))
                .append("::")
                .append(escapeJson(alias))
                .append("\"");
            first = false;
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Parsea el JSON de la lista de usuarios
     */
    public static Set<UserDescriptor> parseUserListJson(String json) {
        Set<UserDescriptor> users = new LinkedHashSet<>();
        json = json.trim();
        if (json.length() < 2 || !json.startsWith("[") || !json.endsWith("]")) {
            return users;
        }

        String body = json.substring(1, json.length() - 1).trim();
        if (body.isEmpty()) {
            return users;
        }

        String[] entries = body.split("\",\"");
        for (String entry : entries) {
            String cleaned = entry.replace("\"", "");
            UserDescriptor descriptor = parseUserDescriptor(cleaned);
            if (descriptor != null) {
                users.add(descriptor);
            }
        }
        return users;
    }

    public static UserDescriptor parseUserDescriptor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] pieces = raw.split("::", 2);
        if (pieces.length != 2) {
            return null;
        }
        String connectionId = pieces[0];
        String displayName = pieces[1];
        if (connectionId.isBlank() || displayName.isBlank()) {
            return null;
        }
        return new UserDescriptor(connectionId, displayName);
    }

    private Room parseRoomPayload(String payload, Room.EstadoRoom estado) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        String[] parts = payload.split("\\|");
        if (parts.length < 3) {
            return null;
        }
        try {
            Long id = Long.parseLong(decodeCredential(parts[1]));
            String name = decodeCredential(parts[2]);
            Room room = new Room();
            room.setId(id);
            room.setName(name);
            room.setEstado(estado);
            return room;
        } catch (NumberFormatException e) {
            logger.warn("No se pudo parsear payload de room: {}", payload, e);
            return null;
        }
    }

    private RoomJoinResponse parseRoomJoinResponse(String payload) {
        if (payload == null) {
            return new RoomJoinResponse(false, null, "Respuesta vacía");
        }
        if (payload.startsWith("OK|")) {
            String[] parts = payload.split("\\|", 2);
            if (parts.length == 2) {
                try {
                    Long roomId = Long.parseLong(decodeCredential(parts[1]));
                    return new RoomJoinResponse(true, roomId, "");
                } catch (NumberFormatException e) {
                    return new RoomJoinResponse(false, null, "ID de room inválido");
                }
            }
        }
        OperationResultPayload result = OperationResultPayload.fromPayload(payload);
        return new RoomJoinResponse(false, null, result.getMessage());
    }

    /**
     * Parsea el JSON de lista de rooms enviado por el servidor.
     */
    public static List<RoomSummary> parseRoomListJson(String json) {
        List<RoomSummary> rooms = new ArrayList<>();
        if (json == null) {
            return rooms;
        }
        String trimmed = json.trim();
        if (trimmed.length() < 2 || !trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return rooms;
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return rooms;
        }
        // formato esperado: [{"id":1,"name":"Room","creator":"user"}, ...]
        String[] entries = body.split("\\},\\{");
        for (String entry : entries) {
            String cleaned = entry.replace("{", "").replace("}", "");
            String[] fields = cleaned.split(",");
            Long id = null;
            String name = "";
            String creator = "";
            for (String field : fields) {
                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;
                String key = kv[0].replace("\"", "").trim();
                String value = kv[1].replace("\"", "").trim();
                switch (key) {
                    case "id" -> {
                        try {
                            id = Long.parseLong(value);
                        } catch (NumberFormatException ignored) { }
                    }
                    case "name" -> name = value;
                    case "creator" -> creator = value;
                }
            }
            if (id != null) {
                rooms.add(new RoomSummary(id, name, creator));
            }
        }
        return rooms;
    }

    private String formatUserEntry(String connectionId) {
        String alias = aliasRegistry.getAliasOrDefault(connectionId);
        if (alias == null || alias.equals(connectionId)) {
            return connectionId;
        }
        return formatUserEntry(connectionId, alias);
    }

    private String formatUserEntry(String connectionId, String alias) {
        return connectionId + "::" + alias;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String encodeCredential(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeCredential(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private int calculateChecksum(byte[] data) {
        int checksum = 0;
        for (byte b : data) {
            checksum = (checksum << 1) ^ b;
        }
        return checksum;
    }

    private void handleAuthRequest(String payload, String source) {
        try {
            String[] parts = payload.split("\\|");
            if (parts.length < 2) {
                sendControlMessage(source, CONTROL_AUTH_RESPONSE, OperationResultPayload.error("Payload inválido").toPayload());
                return;
            }
            String username = decodeCredential(parts[0]);
            String password = decodeCredential(parts[1]);

            ServerRuntime.markAsServerProcess();
            AuthService authService = new AuthService();
            java.util.Optional<Usuario> usuarioOpt = authService.autenticar(username, password);
            OperationResultPayload response;
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                aliasRegistry.registerAlias(source, usuario.getUsername());
                broadcastUserList();
                response = OperationResultPayload.success(usuario);
            } else {
                response = OperationResultPayload.error("Credenciales inválidas");
            }
            sendControlMessage(source, CONTROL_AUTH_RESPONSE, response.toPayload());
        } catch (Exception e) {
            logger.error("Error procesando autenticación remota", e);
            try {
                sendControlMessage(source, CONTROL_AUTH_RESPONSE, OperationResultPayload.error("Error interno").toPayload());
            } catch (IOException ioException) {
                logger.error("No se pudo enviar respuesta de error de autenticación", ioException);
            }
        }
    }

    private void handleRegisterRequest(String payload, String source) {
        try {
            String[] parts = payload.split("\\|");
            if (parts.length < 3) {
                sendControlMessage(source, CONTROL_REGISTER_RESPONSE, OperationResultPayload.error("Payload inválido").toPayload());
                return;
            }

            String username = decodeCredential(parts[0]);
            String password = decodeCredential(parts[1]);
            String email = decodeCredential(parts[2]);

            ServerRuntime.markAsServerProcess();
            AuthService authService = new AuthService();
            Usuario usuario = authService.registrar(username, password, email);
            OperationResultPayload response = OperationResultPayload.success(usuario);
            sendControlMessage(source, CONTROL_REGISTER_RESPONSE, response.toPayload());
        } catch (Exception e) {
            logger.error("Error procesando registro remoto", e);
            try {
                sendControlMessage(source, CONTROL_REGISTER_RESPONSE, OperationResultPayload.error(e.getMessage()).toPayload());
            } catch (IOException ioException) {
                logger.error("No se pudo enviar respuesta de error de registro", ioException);
            }
        }
    }

    private void handleRoomCreateRequest(String payload, String source) {
        try {
            // Formato: roomName|creatorUsername|member1,member2,member3
            String[] parts = payload.split("\\|");
            if (parts.length < 2) {
                sendControlMessage(source, CONTROL_ROOM_CREATE_RESPONSE, 
                    OperationResultPayload.error("Payload inválido").toPayload());
                return;
            }

            String roomName = decodeCredential(parts[0]);
            String creatorUsername = decodeCredential(parts[1]);
            Set<String> members = new java.util.HashSet<>();
            
            if (parts.length > 2) {
                String membersStr = decodeCredential(parts[2]);
                if (!membersStr.isEmpty()) {
                    String[] memberIds = membersStr.split(",");
                    for (String memberId : memberIds) {
                        if (!memberId.trim().isEmpty()) {
                            members.add(memberId.trim());
                        }
                    }
                }
            }

            // Obtener el username del servidor desde ServerRuntime o RoomService
            // Por ahora, necesitamos obtenerlo de alguna manera
            // Esto se puede mejorar pasando el serverUsername al ControlService
            Room room = roomService.createRoomRequest(roomName, source, creatorUsername, members);
            
            // Publicar evento para que el servidor vea la solicitud pendiente
            eventAggregator.publish(new NetworkEvent(
                NetworkEvent.EventType.ROOM_CREATED,
                room,
                source
            ));

            // Responder al cliente que la solicitud fue recibida
            String response = "PENDIENTE|" + encodeCredential(String.valueOf(room.getId())) + "|" + encodeCredential(roomName);
            sendControlMessage(source, CONTROL_ROOM_CREATE_RESPONSE, response);
        } catch (Exception e) {
            logger.error("Error procesando solicitud de creación de room", e);
            try {
                sendControlMessage(source, CONTROL_ROOM_CREATE_RESPONSE, 
                    OperationResultPayload.error("Error al crear room: " + e.getMessage()).toPayload());
            } catch (IOException ioException) {
                logger.error("No se pudo enviar respuesta de error de room", ioException);
            }
        }
    }

    private void handleRoomJoinRequest(String payload, String source) {
        try {
            // Formato: roomId
            String roomIdStr = decodeCredential(payload);
            Long roomId = Long.parseLong(roomIdStr);
            
            if (roomService.addMemberToRoom(roomId, source)) {
                String response = "OK|" + encodeCredential(String.valueOf(roomId));
                sendControlMessage(source, CONTROL_ROOM_JOIN_RESPONSE, response);
            } else {
                sendControlMessage(source, CONTROL_ROOM_JOIN_RESPONSE, 
                    OperationResultPayload.error("No se pudo unir al room").toPayload());
            }
        } catch (Exception e) {
            logger.error("Error procesando solicitud de unión a room", e);
            try {
                sendControlMessage(source, CONTROL_ROOM_JOIN_RESPONSE, 
                    OperationResultPayload.error("Error: " + e.getMessage()).toPayload());
            } catch (IOException ioException) {
                logger.error("No se pudo enviar respuesta de error", ioException);
            }
        }
    }

    private void handleRoomLeave(String payload, String source) {
        try {
            String roomIdStr = decodeCredential(payload);
            Long roomId = Long.parseLong(roomIdStr);
            
            roomService.removeMemberFromRoom(roomId, source);
        } catch (Exception e) {
            logger.error("Error procesando salida de room", e);
        }
    }

    public void approveRoom(Long roomId) throws IOException {
        if (roomService.approveRoom(roomId)) {
            Optional<Room> roomOpt = roomService.getRoom(roomId);
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();
                // Notificar a todos los miembros
                for (String memberId : room.getMembers()) {
                    String response = "APPROVED|" + encodeCredential(String.valueOf(roomId)) + "|" + encodeCredential(room.getName());
                    sendControlMessage(memberId, CONTROL_ROOM_APPROVE, response);
                }
            }
        }
    }

    public void rejectRoom(Long roomId) throws IOException {
        if (roomService.rejectRoom(roomId)) {
            Optional<Room> roomOpt = roomService.getRoom(roomId);
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();
                // Notificar al creador
                String response = "REJECTED|" + encodeCredential(String.valueOf(roomId)) + "|" + encodeCredential(room.getName());
                sendControlMessage(room.getCreatorConnectionId(), CONTROL_ROOM_REJECT, response);
            }
        }
    }

    public void closeRoom(Long roomId) throws IOException {
        if (roomService.closeRoom(roomId)) {
            Optional<Room> roomOpt = roomService.getRoom(roomId);
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();
                // Notificar a todos los miembros
                for (String memberId : room.getMembers()) {
                    String response = "CLOSED|" + encodeCredential(String.valueOf(roomId)) + "|" + encodeCredential(room.getName());
                    sendControlMessage(memberId, CONTROL_ROOM_CLOSE, response);
                }
            }
        }
    }

    public void sendRoomList(String connectionId) throws IOException {
        List<Room> activeRooms = roomService.getActiveRooms();
        // Serializar lista de rooms a JSON simple
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Room room : activeRooms) {
            if (!first) {
                json.append(",");
            }
            json.append("{")
                .append("\"id\":").append(room.getId()).append(",")
                .append("\"name\":\"").append(escapeJson(room.getName())).append("\",")
                .append("\"creator\":\"").append(escapeJson(room.getCreatorUsername())).append("\"")
                .append("}");
            first = false;
        }
        json.append("]");
        sendControlMessage(connectionId, CONTROL_ROOM_LIST, json.toString());
    }

    public void sendAdminControl(String targetConnectionId, byte controlType) throws IOException {
        sendControlMessage(targetConnectionId, controlType, "");
    }

    public static class UserDescriptor {
        private final String connectionId;
        private final String displayName;

        public UserDescriptor(String connectionId, String displayName) {
            this.connectionId = connectionId;
            this.displayName = displayName;
        }

        public String getConnectionId() {
            return connectionId;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static class RoomSummary {
        private final Long id;
        private final String name;
        private final String creatorUsername;

        public RoomSummary(Long id, String name, String creatorUsername) {
            this.id = id;
            this.name = name;
            this.creatorUsername = creatorUsername;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCreatorUsername() {
            return creatorUsername;
        }
    }

    public static class RoomJoinResponse {
        private final boolean success;
        private final Long roomId;
        private final String message;

        public RoomJoinResponse(boolean success, Long roomId, String message) {
            this.success = success;
            this.roomId = roomId;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public Long getRoomId() {
            return roomId;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class OperationResultPayload {
        private final boolean success;
        private final String message;
        private final Long userId;
        private final String username;
        private final String email;

        private OperationResultPayload(boolean success, String message, Long userId, String username, String email) {
            this.success = success;
            this.message = message;
            this.userId = userId;
            this.username = username;
            this.email = email;
        }

        public static OperationResultPayload success(Usuario usuario) {
            return new OperationResultPayload(
                true,
                "",
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail()
            );
        }

        public static OperationResultPayload error(String message) {
            return new OperationResultPayload(false, message == null ? "" : message, null, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String toPayload() {
            String encodedMessage = encodeValue(message);
            String encodedUsername = encodeValue(username);
            String encodedEmail = encodeValue(email);
            String userIdValue = userId == null ? "" : userId.toString();
            return (success ? "OK" : "ERROR") + "|"
                + encodedMessage + "|"
                + userIdValue + "|"
                + encodedUsername + "|"
                + encodedEmail;
        }

        public static OperationResultPayload fromPayload(String payload) {
            String[] parts = payload.split("\\|", -1);
            if (parts.length < 5) {
                return error("Respuesta inválida");
            }
            boolean success = "OK".equals(parts[0]);
            String message = decodeValue(parts[1]);
            Long userId = parts[2].isBlank() ? null : Long.parseLong(parts[2]);
            String username = decodeValue(parts[3]);
            String email = decodeValue(parts[4]);
            return new OperationResultPayload(success, message, userId, username, email);
        }

        private static String encodeValue(String value) {
            if (value == null) {
                return "";
            }
            return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        private static String decodeValue(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        }
    }
}

