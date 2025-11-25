package com.whatsapp.protocol;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class MessageHeader implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final int HEADER_SIZE = 13; // 1 + 4 + 4 + 4
    
    private byte tipo;           // 0=Chat, 1=Archivo, 2=Video, 3=Ctrl
    private int longitud;        // tamaño del payload
    private int correlId;        // ID de correlación
    private int checksum;        // CRC32 truncado

    public MessageHeader() {
    }

    public MessageHeader(byte tipo, int longitud, int correlId, int checksum) {
        this.tipo = tipo;
        this.longitud = longitud;
        this.correlId = correlId;
        this.checksum = checksum;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
        buffer.put(tipo);
        buffer.putInt(longitud);
        buffer.putInt(correlId);
        buffer.putInt(checksum);
        return buffer.array();
    }

    public static MessageHeader fromBytes(byte[] data) {
        if (data.length < HEADER_SIZE) {
            throw new IllegalArgumentException("Datos insuficientes para el header");
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte tipo = buffer.get();
        int longitud = buffer.getInt();
        int correlId = buffer.getInt();
        int checksum = buffer.getInt();
        return new MessageHeader(tipo, longitud, correlId, checksum);
    }

    // Getters y Setters
    public byte getTipo() {
        return tipo;
    }

    public void setTipo(byte tipo) {
        this.tipo = tipo;
    }

    public int getLongitud() {
        return longitud;
    }

    public void setLongitud(int longitud) {
        this.longitud = longitud;
    }

    public int getCorrelId() {
        return correlId;
    }

    public void setCorrelId(int correlId) {
        this.correlId = correlId;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public static class MessageType {
        public static final byte CHAT = 0;
        public static final byte ARCHIVO = 1;
        public static final byte VIDEO = 2;
        public static final byte CONTROL = 3;
    }
}

