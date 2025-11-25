package com.whatsapp.repository;

import com.whatsapp.database.DatabaseManager;
import com.whatsapp.model.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LogRepository implements IRepository<Log, Long> {
    private static final Logger logger = LoggerFactory.getLogger(LogRepository.class);
    private final DatabaseManager dbManager;

    public LogRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public Optional<Log> findById(Long id) {
        String sql = "SELECT * FROM Log WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToLog(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar log por ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Log> findAll() {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT * FROM Log ORDER BY Fecha DESC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todos los logs", e);
        }
        return logs;
    }

    public List<Log> findByTraceId(String traceId) {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT * FROM Log WHERE TraceId = ? ORDER BY Fecha DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, traceId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar logs por traceId", e);
        }
        return logs;
    }

    @Override
    public Log save(Log log) {
        String sql = "INSERT INTO Log (Nivel, Mensaje, Modulo, Fecha, TraceId, UserId) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, log.getNivel().name());
            pstmt.setString(2, log.getMensaje());
            pstmt.setString(3, log.getModulo());
            pstmt.setTimestamp(4, java.sql.Timestamp.valueOf(log.getFecha()));
            pstmt.setString(5, log.getTraceId());
            pstmt.setObject(6, log.getUserId());
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                log.setId(rs.getLong(1));
            }
            return log;
        } catch (SQLException e) {
            logger.error("Error al guardar log", e);
            throw new RuntimeException("Error al guardar log", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM Log WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al eliminar log", e);
        }
    }

    @Override
    public void update(Log log) {
        String sql = "UPDATE Log SET Nivel = ?, Mensaje = ?, Modulo = ?, Fecha = ?, " +
                     "TraceId = ?, UserId = ? WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, log.getNivel().name());
            pstmt.setString(2, log.getMensaje());
            pstmt.setString(3, log.getModulo());
            pstmt.setTimestamp(4, java.sql.Timestamp.valueOf(log.getFecha()));
            pstmt.setString(5, log.getTraceId());
            pstmt.setObject(6, log.getUserId());
            pstmt.setLong(7, log.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al actualizar log", e);
        }
    }

    private Log mapResultSetToLog(ResultSet rs) throws SQLException {
        Log log = new Log();
        log.setId(rs.getLong("Id"));
        log.setNivel(Log.NivelLog.valueOf(rs.getString("Nivel")));
        log.setMensaje(rs.getString("Mensaje"));
        log.setModulo(rs.getString("Modulo"));
        
        java.sql.Timestamp fecha = rs.getTimestamp("Fecha");
        if (fecha != null) {
            log.setFecha(fecha.toLocalDateTime());
        }
        
        log.setTraceId(rs.getString("TraceId"));
        if (rs.getObject("UserId") != null) {
            log.setUserId(rs.getLong("UserId"));
        }
        return log;
    }
}

