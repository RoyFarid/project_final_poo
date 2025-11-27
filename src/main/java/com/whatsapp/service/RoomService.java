package com.whatsapp.service;

import com.whatsapp.model.Room;
import com.whatsapp.network.ConnectionManager;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para gestionar rooms/grupos en el sistema.
 * Patrón: Singleton
 */
public class RoomService {
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);
    private static RoomService instance;
    private final RoomRepository roomRepository;
    private final ConnectionManager connectionManager;
    private final EventAggregator eventAggregator;
    private final UserAliasRegistry aliasRegistry;
    private final LogService logService;
    private final Map<Long, Room> activeRoomsCache; // Cache de rooms activos en memoria
    private final Map<Long, String> requestMessages; // Mensajes adjuntos por los clientes
    private String serverUsername;
    private String traceId;

    private RoomService() {
        this.roomRepository = new RoomRepository();
        this.connectionManager = ConnectionManager.getInstance();
        this.eventAggregator = EventAggregator.getInstance();
        this.aliasRegistry = UserAliasRegistry.getInstance();
        this.logService = LogService.getInstance();
        this.activeRoomsCache = new ConcurrentHashMap<>();
        this.requestMessages = new ConcurrentHashMap<>();
        this.traceId = logService.generateTraceId();
    }

    public static synchronized RoomService getInstance() {
        if (instance == null) {
            instance = new RoomService();
        }
        return instance;
    }

    public void setServerUsername(String serverUsername) {
        this.serverUsername = serverUsername;
        // Limpiar rooms anteriores y cache
        clearAllRoomsForServer();
    }

    public String getServerUsername() {
        return serverUsername;
    }

    /**
     * Limpia todos los rooms del servidor actual al iniciar.
     * Los rooms son temporales y se eliminan al reiniciar el servidor.
     */
    public void clearAllRoomsForServer() {
        if (serverUsername == null) {
            return;
        }
        System.out.println("[RoomService] Limpiando rooms del servidor: " + serverUsername);
        activeRoomsCache.clear();
        requestMessages.clear();
        roomRepository.deleteAllByServerUsername(serverUsername);
        System.out.println("[RoomService] Rooms limpiados correctamente");
    }

    /**
     * Crea una solicitud de room (pendiente de aprobación)
     */
    public Room createRoomRequest(String roomName, String creatorConnectionId, String creatorUsername, Set<String> memberConnectionIds, String requestMessage) {
        return createRoomRequest(roomName, creatorConnectionId, creatorUsername, memberConnectionIds, requestMessage, false);
    }

    /**
     * Crea una solicitud de room (pendiente de aprobación) con opción de incluir servidor
     */
    public Room createRoomRequest(String roomName, String creatorConnectionId, String creatorUsername, Set<String> memberConnectionIds, String requestMessage, boolean includeServer) {
        System.out.println("[RoomService] createRoomRequest llamado");
        System.out.println("[RoomService] serverUsername: " + serverUsername);
        System.out.println("[RoomService] roomName: " + roomName);
        System.out.println("[RoomService] creatorConnectionId: " + creatorConnectionId);
        System.out.println("[RoomService] creatorUsername: " + creatorUsername);
        System.out.println("[RoomService] includeServer: " + includeServer);
        
        if (serverUsername == null) {
            System.out.println("[RoomService] ERROR: serverUsername es null!");
            throw new IllegalStateException("Server username no está configurado");
        }

        Room room = new Room(roomName, creatorConnectionId, creatorUsername, serverUsername);
        room.setRequestMessage(requestMessage);
        room.setIncludeServer(includeServer);
        room.getMembers().addAll(memberConnectionIds);
        room.getMembers().add(creatorConnectionId); // El creador también es miembro
        
        System.out.println("[RoomService] Room creado en memoria, guardando en BD...");
        try {
            room = roomRepository.save(room);
            System.out.println("[RoomService] Room guardado con ID: " + room.getId());
        } catch (Exception e) {
            System.out.println("[RoomService] ERROR guardando room: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        activeRoomsCache.put(room.getId(), room);
        if (requestMessage != null && !requestMessage.isBlank()) {
            requestMessages.put(room.getId(), requestMessage);
        }
        System.out.println("[RoomService] Room agregado a cache");
        
        logService.logInfo("Solicitud de room creada: " + roomName, "RoomService", traceId, null);
        return room;
    }

    /**
     * Crea un room activo directamente desde el servidor (sin pasar por estado PENDIENTE).
     */
    public Room createRoomByServer(String roomName, Set<String> memberConnectionIds, boolean includeServer) {
        if (serverUsername == null) {
            throw new IllegalStateException("Server username no est\u00e1 configurado");
        }
        if (roomName == null || roomName.isBlank()) {
            throw new IllegalArgumentException("El nombre del room es obligatorio");
        }

        String serverMemberId = "SERVER_" + serverUsername;
        Set<String> members = new java.util.HashSet<>();
        if (memberConnectionIds != null) {
            members.addAll(memberConnectionIds);
        }
        // El servidor siempre es creador y miembro
        members.add(serverMemberId);
        if (!includeServer) {
            // Si no quiere participar, solo se agrega como creador
            members.remove(serverMemberId);
        }

        Room room = new Room(roomName, serverMemberId, serverUsername, serverUsername);
        room.setEstado(Room.EstadoRoom.ACTIVO);
        room.setIncludeServer(includeServer);
        room.getMembers().clear();
        room.getMembers().addAll(members);

        room = roomRepository.save(room);
        activeRoomsCache.put(room.getId(), room);
        requestMessages.remove(room.getId());
        logService.logInfo("Room creado por servidor: " + roomName, "RoomService", traceId, null);
        return room;
    }

    /**
     * Aprueba un room pendiente
     */
    public boolean approveRoom(Long roomId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }

        Room room = roomOpt.get();
        if (room.getEstado() != Room.EstadoRoom.PENDIENTE) {
            return false;
        }

        room.setEstado(Room.EstadoRoom.ACTIVO);
        
        // Si el cliente quería incluir al servidor, agregarlo automáticamente
        if (room.isIncludeServer() && serverUsername != null) {
            String serverMemberId = "SERVER_" + serverUsername;
            room.addMember(serverMemberId);
            System.out.println("[RoomService] Servidor agregado automáticamente al room: " + serverMemberId);
        }
        
        roomRepository.update(room);
        activeRoomsCache.put(room.getId(), room);
        requestMessages.remove(roomId);

        // Notificar a todos los miembros del room
        notifyRoomApproved(room);
        
        logService.logInfo("Room aprobado: " + room.getName(), "RoomService", traceId, null);
        return true;
    }

    /**
     * Rechaza un room pendiente
     */
    public boolean rejectRoom(Long roomId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }

        Room room = roomOpt.get();
        if (room.getEstado() != Room.EstadoRoom.PENDIENTE) {
            return false;
        }

        room.setEstado(Room.EstadoRoom.RECHAZADO);
        roomRepository.update(room);
        activeRoomsCache.remove(room.getId());
        requestMessages.remove(roomId);

        // Notificar al creador
        notifyRoomRejected(room);
        
        logService.logInfo("Room rechazado: " + room.getName(), "RoomService", traceId, null);
        return true;
    }

    /**
     * Cierra un room activo
     */
    public boolean closeRoom(Long roomId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }

        Room room = roomOpt.get();
        room.setEstado(Room.EstadoRoom.CERRADO);
        roomRepository.update(room);
        activeRoomsCache.remove(room.getId());
        requestMessages.remove(roomId);

        // Notificar a todos los miembros
        notifyRoomClosed(room);
        
        logService.logInfo("Room cerrado: " + room.getName(), "RoomService", traceId, null);
        return true;
    }

    /**
     * Agrega un miembro a un room activo
     */
    public boolean addMemberToRoom(Long roomId, String connectionId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }

        Room room = roomOpt.get();
        if (room.getEstado() != Room.EstadoRoom.ACTIVO) {
            return false;
        }

        // Si ya es miembro, evitar duplicar eventos
        if (room.hasMember(connectionId)) {
            return true;
        }

        room.addMember(connectionId);
        roomRepository.addMember(roomId, connectionId);
        activeRoomsCache.put(room.getId(), room);

        // Notificar al nuevo miembro
        notifyMemberAdded(room, connectionId);
        
        return true;
    }

    /**
     * Elimina un miembro de un room
     */
    public boolean removeMemberFromRoom(Long roomId, String connectionId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }

        Room room = roomOpt.get();
        room.removeMember(connectionId);
        roomRepository.removeMember(roomId, connectionId);
        activeRoomsCache.put(room.getId(), room);

        // Notificar al miembro eliminado
        notifyMemberRemoved(room, connectionId);
        
        return true;
    }

    /**
     * Obtiene todos los rooms activos del servidor
     */
    public List<Room> getActiveRooms() {
        if (serverUsername == null) {
            return Collections.emptyList();
        }
        List<Room> rooms = roomRepository.findActiveRooms(serverUsername);
        rooms.forEach(this::hydrateRequestMessage);
        return rooms;
    }

    /**
     * Obtiene todos los rooms pendientes
     */
    public List<Room> getPendingRooms() {
        System.out.println("[RoomService] getPendingRooms llamado");
        System.out.println("[RoomService] serverUsername: " + serverUsername);
        
        if (serverUsername == null) {
            System.out.println("[RoomService] serverUsername es null, retornando lista vacía");
            return Collections.emptyList();
        }
        
        List<Room> allRooms = roomRepository.findByServerUsername(serverUsername);
        System.out.println("[RoomService] Rooms encontrados para servidor '" + serverUsername + "': " + allRooms.size());
        
        List<Room> pending = allRooms.stream()
                .filter(r -> r.getEstado() == Room.EstadoRoom.PENDIENTE)
                .toList();
        pending.forEach(this::hydrateRequestMessage);
        System.out.println("[RoomService] Rooms PENDIENTES: " + pending.size());
        
        return pending;
    }

    /**
     * Obtiene un room por ID
     */
    public Optional<Room> getRoom(Long roomId) {
        Room cached = activeRoomsCache.get(roomId);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        roomOpt.ifPresent(this::hydrateRequestMessage);
        return roomOpt;
    }

    /**
     * Obtiene rooms en los que participa un usuario
     */
    public List<Room> getRoomsForUser(String connectionId) {
        List<Room> userRooms = new ArrayList<>();
        for (Room room : activeRoomsCache.values()) {
            if (room.hasMember(connectionId) && room.getEstado() == Room.EstadoRoom.ACTIVO) {
                userRooms.add(room);
            }
        }
        return userRooms;
    }

    /**
     * Envía un mensaje a todos los miembros de un room
     */
    public void sendMessageToRoom(Long roomId, String senderConnectionId, String message) throws IOException {
        Optional<Room> roomOpt = getRoom(roomId);
        if (roomOpt.isEmpty()) {
            throw new IllegalArgumentException("Room no encontrado");
        }

        Room room = roomOpt.get();
        if (room.getEstado() != Room.EstadoRoom.ACTIVO) {
            throw new IllegalStateException("Room no está activo");
        }

        if (!room.hasMember(senderConnectionId)) {
            throw new IllegalStateException("El usuario no es miembro del room");
        }

        // Enviar mensaje a todos los miembros excepto el remitente
        for (String memberId : room.getMembers()) {
            if (!memberId.equals(senderConnectionId)) {
                // Usar ChatService para enviar el mensaje
                // Esto se implementará en ChatService
            }
        }
    }

    private void refreshActiveRoomsCache() {
        activeRoomsCache.clear();
        if (serverUsername != null) {
            List<Room> activeRooms = roomRepository.findActiveRooms(serverUsername);
            for (Room room : activeRooms) {
                hydrateRequestMessage(room);
                activeRoomsCache.put(room.getId(), room);
            }
        }
    }

    private void hydrateRequestMessage(Room room) {
        if (room == null || room.getId() == null) {
            return;
        }
        String message = requestMessages.get(room.getId());
        if (message != null && (room.getRequestMessage() == null || room.getRequestMessage().isBlank())) {
            room.setRequestMessage(message);
        }
    }

    private void notifyRoomApproved(Room room) {
        // Publicar evento para notificar a los miembros
        eventAggregator.publish(new NetworkEvent(
            NetworkEvent.EventType.ROOM_APPROVED,
            room,
            "SERVER"
        ));
    }

    private void notifyRoomRejected(Room room) {
        eventAggregator.publish(new NetworkEvent(
            NetworkEvent.EventType.ROOM_REJECTED,
            room,
            "SERVER"
        ));
    }

    private void notifyRoomClosed(Room room) {
        eventAggregator.publish(new NetworkEvent(
            NetworkEvent.EventType.ROOM_CLOSED,
            room,
            "SERVER"
        ));
    }

    private void notifyMemberAdded(Room room, String connectionId) {
        eventAggregator.publish(new NetworkEvent(
            NetworkEvent.EventType.ROOM_MEMBER_ADDED,
            new RoomMemberEvent(room.getId(), connectionId),
            "SERVER"
        ));
    }

    private void notifyMemberRemoved(Room room, String connectionId) {
        eventAggregator.publish(new NetworkEvent(
            NetworkEvent.EventType.ROOM_MEMBER_REMOVED,
            new RoomMemberEvent(room.getId(), connectionId),
            "SERVER"
        ));
    }

    public static class RoomMemberEvent {
        private final Long roomId;
        private final String connectionId;

        public RoomMemberEvent(Long roomId, String connectionId) {
            this.roomId = roomId;
            this.connectionId = connectionId;
        }

        public Long getRoomId() {
            return roomId;
        }

        public String getConnectionId() {
            return connectionId;
        }
    }
}

