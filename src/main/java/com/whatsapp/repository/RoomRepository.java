package com.whatsapp.repository;

import com.whatsapp.database.DatabaseManager;
import com.whatsapp.model.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository para gestionar rooms en la base de datos.
 * Patrón: Repository
 */
public class RoomRepository implements IRepository<Room, Long> {
    private static final Logger logger = LoggerFactory.getLogger(RoomRepository.class);
    private final DatabaseManager dbManager;

    public RoomRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public Optional<Room> findById(Long id) {
        String sql = "SELECT * FROM Room WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Room room = mapResultSetToRoom(rs);
                loadMembers(room);
                return Optional.of(room);
            }
        } catch (SQLException e) {
            logger.error("Error al buscar room por ID", e);
        }
        return Optional.empty();
    }

    public Optional<Room> findByName(String name) {
        String sql = "SELECT * FROM Room WHERE Name = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Room room = mapResultSetToRoom(rs);
                loadMembers(room);
                return Optional.of(room);
            }
        } catch (SQLException e) {
            logger.error("Error al buscar room por nombre", e);
        }
        return Optional.empty();
    }

    public List<Room> findByServerUsername(String serverUsername) {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM Room WHERE ServerUsername = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, serverUsername);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Room room = mapResultSetToRoom(rs);
                loadMembers(room);
                rooms.add(room);
            }
        } catch (SQLException e) {
            logger.error("Error al buscar rooms por servidor", e);
        }
        return rooms;
    }

    public List<Room> findActiveRooms(String serverUsername) {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM Room WHERE ServerUsername = ? AND Estado = 'ACTIVO'";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, serverUsername);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Room room = mapResultSetToRoom(rs);
                loadMembers(room);
                rooms.add(room);
            }
        } catch (SQLException e) {
            logger.error("Error al buscar rooms activos", e);
        }
        return rooms;
    }

    @Override
    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM Room";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Room room = mapResultSetToRoom(rs);
                loadMembers(room);
                rooms.add(room);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todos los rooms", e);
        }
        return rooms;
    }

    @Override
    public Room save(Room room) {
        // Intentar con columnas extendidas; si falla por esquema antiguo, usar fallback.
        String sql = "INSERT INTO Room (Name, CreatorConnectionId, CreatorUsername, Estado, FechaCreacion, ServerUsername, RequestMessage, IncludeServer) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, room.getName());
            pstmt.setString(2, room.getCreatorConnectionId());
            pstmt.setString(3, room.getCreatorUsername());
            pstmt.setString(4, room.getEstado().name());
            pstmt.setTimestamp(5, java.sql.Timestamp.valueOf(room.getFechaCreacion()));
            pstmt.setString(6, room.getServerUsername());
            pstmt.setString(7, room.getRequestMessage());
            pstmt.setBoolean(8, room.isIncludeServer());
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                room.setId(rs.getLong(1));
            }
            
            saveMembers(room);
            logger.info("Room guardado: ID={}, Name={}, Estado={}", room.getId(), room.getName(), room.getEstado());
            return room;
        } catch (SQLException primary) {
            logger.warn("Insert extendido falló, intentando fallback (esquema antiguo)", primary);
            String legacySql = "INSERT INTO Room (Name, CreatorConnectionId, CreatorUsername, Estado, FechaCreacion, ServerUsername) " +
                               "VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(legacySql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, room.getName());
                pstmt.setString(2, room.getCreatorConnectionId());
                pstmt.setString(3, room.getCreatorUsername());
                pstmt.setString(4, room.getEstado().name());
                pstmt.setTimestamp(5, java.sql.Timestamp.valueOf(room.getFechaCreacion()));
                pstmt.setString(6, room.getServerUsername());
                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    room.setId(rs.getLong(1));
                }
                saveMembers(room);
                logger.info("Room guardado (fallback): ID={}, Name={}, Estado={}", room.getId(), room.getName(), room.getEstado());
                return room;
            } catch (SQLException e) {
                logger.error("Error al guardar room (fallback)", e);
                throw new RuntimeException("Error al guardar room", e);
            }
        }
    }

    @Override
    public void delete(Long id) {
        // Primero eliminar miembros
        String deleteMembersSql = "DELETE FROM RoomMember WHERE RoomId = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteMembersSql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al eliminar miembros del room", e);
        }
        
        // Luego eliminar el room
        String sql = "DELETE FROM Room WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al eliminar room", e);
        }
    }

    @Override
    public void update(Room room) {
        String sql = "UPDATE Room SET Name = ?, Estado = ?, RequestMessage = ?, IncludeServer = ? WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, room.getName());
            pstmt.setString(2, room.getEstado().name());
            pstmt.setString(3, room.getRequestMessage());
            pstmt.setBoolean(4, room.isIncludeServer());
            pstmt.setLong(5, room.getId());
            pstmt.executeUpdate();

            updateMembers(room);
        } catch (SQLException primary) {
            logger.warn("Update extendido falló, probando SQL compatible", primary);
            String legacySql = "UPDATE Room SET Name = ?, Estado = ? WHERE Id = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(legacySql)) {
                pstmt.setString(1, room.getName());
                pstmt.setString(2, room.getEstado().name());
                pstmt.setLong(3, room.getId());
                pstmt.executeUpdate();
                updateMembers(room);
            } catch (SQLException e) {
                logger.error("Error al actualizar room", e);
            }
        }
    }

    public void addMember(Long roomId, String connectionId) {
        String sql = "INSERT INTO RoomMember (RoomId, ConnectionId) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE RoomId = RoomId";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, roomId);
            pstmt.setString(2, connectionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al agregar miembro al room", e);
        }
    }

    public void removeMember(Long roomId, String connectionId) {
        String sql = "DELETE FROM RoomMember WHERE RoomId = ? AND ConnectionId = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, roomId);
            pstmt.setString(2, connectionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al eliminar miembro del room", e);
        }
    }

    private void loadMembers(Room room) {
        String sql = "SELECT ConnectionId FROM RoomMember WHERE RoomId = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, room.getId());
            ResultSet rs = pstmt.executeQuery();
            Set<String> members = new HashSet<>();
            while (rs.next()) {
                members.add(rs.getString("ConnectionId"));
            }
            room.setMembers(members);
        } catch (SQLException e) {
            logger.error("Error al cargar miembros del room", e);
        }
    }

    private void saveMembers(Room room) {
        if (room.getId() == null) {
            return;
        }
        
        // Eliminar miembros existentes
        String deleteSql = "DELETE FROM RoomMember WHERE RoomId = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setLong(1, room.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al eliminar miembros existentes", e);
        }
        
        // Insertar nuevos miembros
        String insertSql = "INSERT INTO RoomMember (RoomId, ConnectionId) VALUES (?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (String memberId : room.getMembers()) {
                pstmt.setLong(1, room.getId());
                pstmt.setString(2, memberId);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            logger.error("Error al guardar miembros del room", e);
        }
    }

    private void updateMembers(Room room) {
        saveMembers(room);
    }

    private Room mapResultSetToRoom(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setId(rs.getLong("Id"));
        room.setName(rs.getString("Name"));
        room.setCreatorConnectionId(rs.getString("CreatorConnectionId"));
        room.setCreatorUsername(rs.getString("CreatorUsername"));
        room.setEstado(Room.EstadoRoom.valueOf(rs.getString("Estado")));
        
        java.sql.Timestamp fechaCreacion = rs.getTimestamp("FechaCreacion");
        if (fechaCreacion != null) {
            room.setFechaCreacion(fechaCreacion.toLocalDateTime());
        }
        
        room.setServerUsername(rs.getString("ServerUsername"));
        
        // Cargar RequestMessage si existe
        try {
            room.setRequestMessage(rs.getString("RequestMessage"));
        } catch (SQLException ignored) {
            // Columna no existe en BD antigua
        }
        
        // Cargar IncludeServer si existe
        try {
            room.setIncludeServer(rs.getBoolean("IncludeServer"));
        } catch (SQLException ignored) {
            // Columna no existe en BD antigua
        }
        
        return room;
    }

    /**
     * Elimina todos los rooms de un servidor específico
     */
    public void deleteAllByServerUsername(String serverUsername) {
        // Primero obtener IDs de rooms a eliminar
        List<Long> roomIds = new ArrayList<>();
        String selectSql = "SELECT Id FROM Room WHERE ServerUsername = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, serverUsername);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                roomIds.add(rs.getLong("Id"));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener rooms para eliminar", e);
        }
        
        // Eliminar miembros de cada room
        for (Long roomId : roomIds) {
            String deleteMembersSql = "DELETE FROM RoomMember WHERE RoomId = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(deleteMembersSql)) {
                pstmt.setLong(1, roomId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.error("Error al eliminar miembros del room " + roomId, e);
            }
        }
        
        // Eliminar rooms
        String deleteRoomsSql = "DELETE FROM Room WHERE ServerUsername = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteRoomsSql)) {
            pstmt.setString(1, serverUsername);
            int deleted = pstmt.executeUpdate();
            logger.info("Eliminados {} rooms del servidor {}", deleted, serverUsername);
        } catch (SQLException e) {
            logger.error("Error al eliminar rooms del servidor", e);
        }
    }
}

