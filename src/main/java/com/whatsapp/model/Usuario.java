package com.whatsapp.model;

import java.time.LocalDateTime;

public class Usuario {
    private Long id;
    private String username;
    private String passwordHash;
    private String salt;
    private String email;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoLogin;
    private EstadoUsuario estado;

    public enum EstadoUsuario {
        ACTIVO, INACTIVO, BLOQUEADO
    }

    public Usuario() {
    }

    public Usuario(String username, String passwordHash, String salt, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.email = email;
        this.fechaCreacion = LocalDateTime.now();
        this.estado = EstadoUsuario.ACTIVO;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getUltimoLogin() {
        return ultimoLogin;
    }

    public void setUltimoLogin(LocalDateTime ultimoLogin) {
        this.ultimoLogin = ultimoLogin;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }
}

