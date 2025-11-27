package com.whatsapp.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Modelo para representar un room/grupo en el sistema.
 * Patrón: Entity/Model
 */
public class Room {
    private Long id;
    private String name;
    private String creatorConnectionId;
    private String creatorUsername;
    private EstadoRoom estado;
    private LocalDateTime fechaCreacion;
    private Set<String> members; // Connection IDs de los miembros
    private String serverUsername; // Username del servidor que gestiona este room
    private String requestMessage; // Mensaje opcional enviado con la solicitud

    public enum EstadoRoom {
        PENDIENTE, // Esperando aprobación del servidor
        ACTIVO,    // Room activo y funcionando
        RECHAZADO, // Rechazado por el servidor
        CERRADO    // Cerrado por el servidor o creador
    }

    public Room() {
        this.members = new HashSet<>();
        this.estado = EstadoRoom.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Room(String name, String creatorConnectionId, String creatorUsername, String serverUsername) {
        this();
        this.name = name;
        this.creatorConnectionId = creatorConnectionId;
        this.creatorUsername = creatorUsername;
        this.serverUsername = serverUsername;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatorConnectionId() {
        return creatorConnectionId;
    }

    public void setCreatorConnectionId(String creatorConnectionId) {
        this.creatorConnectionId = creatorConnectionId;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public EstadoRoom getEstado() {
        return estado;
    }

    public void setEstado(EstadoRoom estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void setMembers(Set<String> members) {
        this.members = members;
    }

    public void addMember(String connectionId) {
        this.members.add(connectionId);
    }

    public void removeMember(String connectionId) {
        this.members.remove(connectionId);
    }

    public boolean hasMember(String connectionId) {
        return this.members.contains(connectionId);
    }

    public String getServerUsername() {
        return serverUsername;
    }

    public void setServerUsername(String serverUsername) {
        this.serverUsername = serverUsername;
    }

    public String getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(String requestMessage) {
        this.requestMessage = requestMessage;
    }
}
