package com.whatsapp.model;

import java.time.LocalDateTime;

public class Log {
    private Long id;
    private NivelLog nivel;
    private String mensaje;
    private String modulo;
    private LocalDateTime fecha;
    private String traceId;
    private Long userId;

    public enum NivelLog {
        DEBUG, INFO, WARN, ERROR
    }

    public Log() {
    }

    public Log(NivelLog nivel, String mensaje, String modulo, String traceId, Long userId) {
        this.nivel = nivel;
        this.mensaje = mensaje;
        this.modulo = modulo;
        this.fecha = LocalDateTime.now();
        this.traceId = traceId;
        this.userId = userId;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NivelLog getNivel() {
        return nivel;
    }

    public void setNivel(NivelLog nivel) {
        this.nivel = nivel;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}

