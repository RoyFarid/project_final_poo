package com.whatsapp.model;

import java.time.LocalDateTime;

public class Transferencia {
    private Long id;
    private TipoTransferencia tipo;
    private String nombre;
    private Long tamano;
    private String checksum;
    private EstadoTransferencia estado;
    private LocalDateTime inicio;
    private LocalDateTime fin;
    private Long userId;
    private String peerIp;

    public enum TipoTransferencia {
        ARCHIVO, VIDEO, TEXTO
    }

    public enum EstadoTransferencia {
        PENDIENTE, EN_PROGRESO, COMPLETADA, CANCELADA, ERROR
    }

    public Transferencia() {
    }

    public Transferencia(TipoTransferencia tipo, String nombre, Long tamano, String checksum, 
                        Long userId, String peerIp) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.tamano = tamano;
        this.checksum = checksum;
        this.estado = EstadoTransferencia.PENDIENTE;
        this.inicio = LocalDateTime.now();
        this.userId = userId;
        this.peerIp = peerIp;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoTransferencia getTipo() {
        return tipo;
    }

    public void setTipo(TipoTransferencia tipo) {
        this.tipo = tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getTamano() {
        return tamano;
    }

    public void setTamano(Long tamano) {
        this.tamano = tamano;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public EstadoTransferencia getEstado() {
        return estado;
    }

    public void setEstado(EstadoTransferencia estado) {
        this.estado = estado;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPeerIp() {
        return peerIp;
    }

    public void setPeerIp(String peerIp) {
        this.peerIp = peerIp;
    }
}

