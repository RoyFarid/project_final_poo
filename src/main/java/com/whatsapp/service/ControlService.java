package com.whatsapp.service;

import com.whatsapp.network.ConnectionManager;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.protocol.MessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio para manejar mensajes de control, como la lista de usuarios conectados
 */
public class ControlService {
    private static final Logger logger = LoggerFactory.getLogger(ControlService.class);
    private final ConnectionManager connectionManager;
    private final EventAggregator eventAggregator;
    private final LogService logService;
    private final AtomicInteger correlIdGenerator;
    private String traceId;
    
    // Tipos de mensajes de control
    public static final byte CONTROL_USER_LIST = 1;
    public static final byte CONTROL_USER_CONNECTED = 2;
    public static final byte CONTROL_USER_DISCONNECTED = 3;

    public ControlService() {
        this.connectionManager = ConnectionManager.getInstance();
        this.eventAggregator = EventAggregator.getInstance();
        this.logService = LogService.getInstance();
        this.correlIdGenerator = new AtomicInteger(0);
        this.traceId = logService.generateTraceId();
    }

    /**
     * Envía la lista de usuarios conectados a un cliente específico
     */
    public void sendUserList(String connectionId) throws IOException {
        Set<String> connectedUsers = connectionManager.getConnectedClients();
        logger.info("Enviando lista de usuarios a " + connectionId + ". Usuarios: " + connectedUsers);
        String userListJson = buildUserListJson(connectedUsers);
        logger.info("JSON generado: " + userListJson);
        sendControlMessage(connectionId, CONTROL_USER_LIST, userListJson);
        logger.info("Mensaje de control enviado exitosamente");
    }

    /**
     * Notifica a todos los clientes que un usuario se conectó
     */
    public void notifyUserConnected(String userId) throws IOException {
        sendControlMessageToAll(CONTROL_USER_CONNECTED, userId);
    }

    /**
     * Notifica a todos los clientes que un usuario se desconectó
     */
    public void notifyUserDisconnected(String userId) throws IOException {
        sendControlMessageToAll(CONTROL_USER_DISCONNECTED, userId);
    }

    /**
     * Envía un mensaje de control a un cliente específico
     */
    public void sendControlMessage(String connectionId, byte controlType, String data) throws IOException {
        try {
            int correlId = correlIdGenerator.incrementAndGet();
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            int checksum = calculateChecksum(dataBytes);
            
            // Crear header con tipo CONTROL
            MessageHeader header = new MessageHeader(
                MessageHeader.MessageType.CONTROL,
                dataBytes.length + 1, // +1 para el tipo de control
                correlId,
                checksum
            );

            // Serializar mensaje completo: header + tipoControl + datos
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(header.toBytes());
            baos.write(controlType); // Tipo de control
            baos.write(dataBytes);
            byte[] fullMessage = baos.toByteArray();

            connectionManager.send(connectionId, fullMessage);
            logger.info("Mensaje de control enviado a " + connectionId + " (tipo: " + controlType + ", datos: " + data + ")");
            logService.logInfo("Mensaje de control enviado a " + connectionId, "ControlService", traceId, null);
        } catch (Exception e) {
            logger.error("Error enviando mensaje de control", e);
            throw new IOException("Error enviando mensaje de control", e);
        }
    }

    /**
     * Envía un mensaje de control a todos los clientes
     */
    private void sendControlMessageToAll(byte controlType, String data) {
        Set<String> clients = connectionManager.getConnectedClients();
        for (String clientId : clients) {
            try {
                sendControlMessage(clientId, controlType, data);
            } catch (IOException e) {
                logger.error("Error enviando mensaje de control a " + clientId, e);
            }
        }
    }

    /**
     * Procesa un mensaje de control recibido
     */
    public void handleControlMessage(byte[] data, String source) {
        try {
            logger.info("Procesando mensaje de control recibido de " + source + ", tamaño: " + data.length);
            // Parsear header
            byte[] headerBytes = new byte[MessageHeader.HEADER_SIZE];
            System.arraycopy(data, 0, headerBytes, 0, MessageHeader.HEADER_SIZE);
            MessageHeader header = MessageHeader.fromBytes(headerBytes);
            logger.info("Header parseado - Tipo: " + header.getTipo() + ", Longitud: " + header.getLongitud());

            if (header.getTipo() == MessageHeader.MessageType.CONTROL) {
                // Leer tipo de control
                byte controlType = data[MessageHeader.HEADER_SIZE];
                
                // Leer datos
                byte[] dataBytes = new byte[header.getLongitud() - 1]; // -1 porque el tipo de control ya se leyó
                System.arraycopy(data, MessageHeader.HEADER_SIZE + 1, dataBytes, 0, dataBytes.length);
                String controlData = new String(dataBytes, StandardCharsets.UTF_8);
                logger.info("Tipo de control: " + controlType + ", Datos: " + controlData);

                // Verificar checksum
                int calculatedChecksum = calculateChecksum(dataBytes);
                if (calculatedChecksum != header.getChecksum()) {
                    logger.warn("Checksum inválido en mensaje de control recibido");
                    return;
                }

                // Procesar según el tipo de control
                switch (controlType) {
                    case CONTROL_USER_LIST:
                        // Publicar evento con la lista de usuarios
                        logger.info("Lista de usuarios recibida: " + controlData);
                        eventAggregator.publish(new NetworkEvent(
                            NetworkEvent.EventType.CONNECTED,
                            controlData, // JSON con la lista de usuarios
                            "SERVER"
                        ));
                        logService.logInfo("Lista de usuarios recibida: " + controlData, "ControlService", traceId, null);
                        break;
                    case CONTROL_USER_CONNECTED:
                        eventAggregator.publish(new NetworkEvent(
                            NetworkEvent.EventType.CONNECTED,
                            controlData, // ID del usuario conectado
                            "SERVER"
                        ));
                        break;
                    case CONTROL_USER_DISCONNECTED:
                        eventAggregator.publish(new NetworkEvent(
                            NetworkEvent.EventType.DISCONNECTED,
                            controlData, // ID del usuario desconectado
                            "SERVER"
                        ));
                        break;
                }
            }
        } catch (Exception e) {
            logger.error("Error procesando mensaje de control", e);
        }
    }

    /**
     * Construye un JSON simple con la lista de usuarios
     */
    private String buildUserListJson(Set<String> users) {
        StringBuilder json = new StringBuilder();
        json.append("[");
        boolean first = true;
        for (String user : users) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(user).append("\"");
            first = false;
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Parsea el JSON de la lista de usuarios
     */
    public static Set<String> parseUserListJson(String json) {
        java.util.Set<String> users = new java.util.HashSet<>();
        // Parseo simple: ["user1","user2"] -> ["user1", "user2"]
        json = json.trim();
        if (json.startsWith("[") && json.endsWith("]")) {
            json = json.substring(1, json.length() - 1);
            String[] parts = json.split(",");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("\"") && part.endsWith("\"")) {
                    users.add(part.substring(1, part.length() - 1));
                }
            }
        }
        return users;
    }

    private int calculateChecksum(byte[] data) {
        int checksum = 0;
        for (byte b : data) {
            checksum = (checksum << 1) ^ b;
        }
        return checksum;
    }
}

