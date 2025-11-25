package com.whatsapp.repository;

import com.whatsapp.database.DatabaseManager;
import com.whatsapp.model.Transferencia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransferenciaRepository implements IRepository<Transferencia, Long> {
    private static final Logger logger = LoggerFactory.getLogger(TransferenciaRepository.class);
    private final DatabaseManager dbManager;

    public TransferenciaRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public Optional<Transferencia> findById(Long id) {
        String sql = "SELECT * FROM Transferencia WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToTransferencia(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar transferencia por ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Transferencia> findAll() {
        List<Transferencia> transferencias = new ArrayList<>();
        String sql = "SELECT * FROM Transferencia ORDER BY Inicio DESC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                transferencias.add(mapResultSetToTransferencia(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todas las transferencias", e);
        }
        return transferencias;
    }

    public List<Transferencia> findByUserId(Long userId) {
        List<Transferencia> transferencias = new ArrayList<>();
        String sql = "SELECT * FROM Transferencia WHERE UserId = ? ORDER BY Inicio DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                transferencias.add(mapResultSetToTransferencia(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar transferencias por userId", e);
        }
        return transferencias;
    }

    @Override
    public Transferencia save(Transferencia transferencia) {
        String sql = "INSERT INTO Transferencia (Tipo, Nombre, Tamano, Checksum, Estado, Inicio, UserId, PeerIp) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, transferencia.getTipo().name());
            pstmt.setString(2, transferencia.getNombre());
            pstmt.setLong(3, transferencia.getTamano());
            pstmt.setString(4, transferencia.getChecksum());
            pstmt.setString(5, transferencia.getEstado().name());
            pstmt.setTimestamp(6, java.sql.Timestamp.valueOf(transferencia.getInicio()));
            pstmt.setLong(7, transferencia.getUserId());
            pstmt.setString(8, transferencia.getPeerIp());
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                transferencia.setId(rs.getLong(1));
            }
            return transferencia;
        } catch (SQLException e) {
            logger.error("Error al guardar transferencia", e);
            throw new RuntimeException("Error al guardar transferencia", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM Transferencia WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al eliminar transferencia", e);
        }
    }

    @Override
    public void update(Transferencia transferencia) {
        String sql = "UPDATE Transferencia SET Tipo = ?, Nombre = ?, Tamano = ?, Checksum = ?, " +
                     "Estado = ?, Inicio = ?, Fin = ?, UserId = ?, PeerIp = ? WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transferencia.getTipo().name());
            pstmt.setString(2, transferencia.getNombre());
            pstmt.setLong(3, transferencia.getTamano());
            pstmt.setString(4, transferencia.getChecksum());
            pstmt.setString(5, transferencia.getEstado().name());
            pstmt.setTimestamp(6, java.sql.Timestamp.valueOf(transferencia.getInicio()));
            pstmt.setTimestamp(7, transferencia.getFin() != null ? java.sql.Timestamp.valueOf(transferencia.getFin()) : null);
            pstmt.setLong(8, transferencia.getUserId());
            pstmt.setString(9, transferencia.getPeerIp());
            pstmt.setLong(10, transferencia.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al actualizar transferencia", e);
        }
    }

    private Transferencia mapResultSetToTransferencia(ResultSet rs) throws SQLException {
        Transferencia transferencia = new Transferencia();
        transferencia.setId(rs.getLong("Id"));
        transferencia.setTipo(Transferencia.TipoTransferencia.valueOf(rs.getString("Tipo")));
        transferencia.setNombre(rs.getString("Nombre"));
        transferencia.setTamano(rs.getLong("Tamano"));
        transferencia.setChecksum(rs.getString("Checksum"));
        transferencia.setEstado(Transferencia.EstadoTransferencia.valueOf(rs.getString("Estado")));
        
        java.sql.Timestamp inicio = rs.getTimestamp("Inicio");
        if (inicio != null) {
            transferencia.setInicio(inicio.toLocalDateTime());
        }
        
        java.sql.Timestamp fin = rs.getTimestamp("Fin");
        if (fin != null) {
            transferencia.setFin(fin.toLocalDateTime());
        }
        
        transferencia.setUserId(rs.getLong("UserId"));
        transferencia.setPeerIp(rs.getString("PeerIp"));
        return transferencia;
    }
}

