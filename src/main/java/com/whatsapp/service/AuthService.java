package com.whatsapp.service;

import com.whatsapp.model.Usuario;
import com.whatsapp.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UsuarioRepository usuarioRepository;
    private static final int BCRYPT_ROUNDS = 12;
    private String serverUsername;

    public AuthService() {
        this.usuarioRepository = new UsuarioRepository();
    }

    public AuthService(String serverUsername) {
        this.serverUsername = serverUsername;
        this.usuarioRepository = new UsuarioRepository();
    }

    public Usuario registrar(String username, String password, String email) {
        // Validaciones
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El username no puede estar vacío");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Email inválido");
        }

        // Verificar si el usuario ya existe
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("El username ya está en uso");
        }

        // Generar salt y hash de contraseña
        String salt = BCrypt.gensalt(BCRYPT_ROUNDS);
        String passwordHash = BCrypt.hashpw(password, salt);

        // Crear y guardar usuario
        Usuario usuario = new Usuario(username, passwordHash, salt, email);
        usuario = usuarioRepository.save(usuario);
        
        logger.info("Usuario registrado: {}", username);
        return usuario;
    }

    public Optional<Usuario> autenticar(String username, String password) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        
        if (usuarioOpt.isEmpty()) {
            logger.warn("Intento de login con username inexistente: {}", username);
            return Optional.empty();
        }

        Usuario usuario = usuarioOpt.get();
        
        // Verificar estado
        if (usuario.getEstado() != Usuario.EstadoUsuario.ACTIVO) {
            logger.warn("Intento de login con usuario inactivo: {}", username);
            return Optional.empty();
        }

        // Verificar contraseña
        if (BCrypt.checkpw(password, usuario.getPasswordHash())) {
            // Actualizar último login
            usuario.setUltimoLogin(java.time.LocalDateTime.now());
            usuarioRepository.updateLastLogin(usuario.getId());
            logger.info("Usuario autenticado: {}", username);
            return Optional.of(usuario);
        } else {
            logger.warn("Contraseña incorrecta para usuario: {}", username);
            return Optional.empty();
        }
    }

    public boolean cambiarPassword(Long userId, String oldPassword, String newPassword) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(userId);
        if (usuarioOpt.isEmpty()) {
            return false;
        }

        Usuario usuario = usuarioOpt.get();
        if (!BCrypt.checkpw(oldPassword, usuario.getPasswordHash())) {
            return false;
        }

        String salt = BCrypt.gensalt(BCRYPT_ROUNDS);
        String newPasswordHash = BCrypt.hashpw(newPassword, salt);
        usuario.setPasswordHash(newPasswordHash);
        usuario.setSalt(salt);
        usuarioRepository.update(usuario);
        
        logger.info("Contraseña cambiada para usuario ID: {}", userId);
        return true;
    }
}

