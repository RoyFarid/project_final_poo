package com.whatsapp.repository;

import com.whatsapp.database.DatabaseManager;
import com.whatsapp.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository implements IRepository<Usuario, Long> {
    private static final Logger logger = LoggerFactory.getLogger(UsuarioRepository.class);
    private final DatabaseManager dbManager;

    public UsuarioRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        String sql = "SELECT * FROM Usuario WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToUsuario(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar usuario por ID", e);
        }
        return Optional.empty();
    }

    public Optional<Usuario> findByUsername(String username) {
        String sql = "SELECT * FROM Usuario WHERE Username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToUsuario(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar usuario por username", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Usuario> findAll() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM Usuario";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                usuarios.add(mapResultSetToUsuario(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todos los usuarios", e);
        }
        return usuarios;
    }

    @Override
    public Usuario save(Usuario usuario) {
        String sql = "INSERT INTO Usuario (Username, PasswordHash, Salt, Email, FechaCreacion, Estado) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, usuario.getUsername());
            pstmt.setString(2, usuario.getPasswordHash());
            pstmt.setString(3, usuario.getSalt());
            pstmt.setString(4, usuario.getEmail());
            pstmt.setTimestamp(5, java.sql.Timestamp.valueOf(usuario.getFechaCreacion()));
            pstmt.setString(6, usuario.getEstado().name());
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                usuario.setId(rs.getLong(1));
            }
            return usuario;
        } catch (SQLException e) {
            logger.error("Error al guardar usuario", e);
            throw new RuntimeException("Error al guardar usuario", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM Usuario WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al eliminar usuario", e);
        }
    }

    @Override
    public void update(Usuario usuario) {
        String sql = "UPDATE Usuario SET Username = ?, PasswordHash = ?, Salt = ?, Email = ?, " +
                     "UltimoLogin = ?, Estado = ? WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario.getUsername());
            pstmt.setString(2, usuario.getPasswordHash());
            pstmt.setString(3, usuario.getSalt());
            pstmt.setString(4, usuario.getEmail());
            pstmt.setTimestamp(5, usuario.getUltimoLogin() != null ? java.sql.Timestamp.valueOf(usuario.getUltimoLogin()) : null);
            pstmt.setString(6, usuario.getEstado().name());
            pstmt.setLong(7, usuario.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al actualizar usuario", e);
        }
    }

    public void updateLastLogin(Long userId) {
        String sql = "UPDATE Usuario SET UltimoLogin = ? WHERE Id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al actualizar último login", e);
        }
    }

    private Usuario mapResultSetToUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("Id"));
        usuario.setUsername(rs.getString("Username"));
        usuario.setPasswordHash(rs.getString("PasswordHash"));
        usuario.setSalt(rs.getString("Salt"));
        usuario.setEmail(rs.getString("Email"));
        
        java.sql.Timestamp fechaCreacion = rs.getTimestamp("FechaCreacion");
        if (fechaCreacion != null) {
            usuario.setFechaCreacion(fechaCreacion.toLocalDateTime());
        }
        
        java.sql.Timestamp ultimoLogin = rs.getTimestamp("UltimoLogin");
        if (ultimoLogin != null) {
            usuario.setUltimoLogin(ultimoLogin.toLocalDateTime());
        }
        
        usuario.setEstado(Usuario.EstadoUsuario.valueOf(rs.getString("Estado")));
        return usuario;
    }
}

