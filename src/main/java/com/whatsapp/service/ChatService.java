package com.whatsapp.service;

import com.whatsapp.model.Transferencia;
import com.whatsapp.network.ConnectionManager;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.protocol.MessageHeader;
import com.whatsapp.repository.TransferenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final ConnectionManager connectionManager;
    private final TransferenciaRepository transferenciaRepository;
    private final EventAggregator eventAggregator;
    private final LogService logService;
    private final AtomicInteger correlIdGenerator;
    private String traceId;

    public ChatService() {
        this.connectionManager = ConnectionManager.getInstance();
        this.transferenciaRepository = ServerRuntime.isServerProcess() ? new TransferenciaRepository() : null;
        this.eventAggregator = EventAggregator.getInstance();
        this.logService = LogService.getInstance();
        this.correlIdGenerator = new AtomicInteger(0);
        this.traceId = logService.generateTraceId();
    }

    public void sendMessage(String connectionId, String message, Long userId, String peerIp) throws IOException {
        try {
            // Crear header
            int correlId = correlIdGenerator.incrementAndGet();
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            int checksum = calculateChecksum(messageBytes);
            
            // Serializar mensaje completo
            byte[] fullMessage = buildChatPacket(messageBytes, correlId, checksum);

            // Enviar
            connectionManager.send(connectionId, fullMessage);

            // Registrar transferencia
            if (transferenciaRepository != null) {
                Transferencia transferencia = new Transferencia(
                    Transferencia.TipoTransferencia.TEXTO,
                    "Mensaje",
                    (long) messageBytes.length,
                    String.valueOf(checksum),
                    userId,
                    peerIp
                );
                transferencia.setEstado(Transferencia.EstadoTransferencia.COMPLETADA);
                transferenciaRepository.save(transferencia);
            }

            logService.logInfo("Mensaje enviado a " + connectionId, "ChatService", traceId, userId);
        } catch (Exception e) {
            logger.error("Error enviando mensaje", e);
            logService.logError("Error enviando mensaje: " + e.getMessage(), "ChatService", traceId, userId);
            throw new IOException("Error enviando mensaje", e);
        }
    }

    public void handleReceivedMessage(byte[] data, String source) {
        try {
            // Parsear header
            byte[] headerBytes = new byte[MessageHeader.HEADER_SIZE];
            System.arraycopy(data, 0, headerBytes, 0, MessageHeader.HEADER_SIZE);
            MessageHeader header = MessageHeader.fromBytes(headerBytes);

            if (header.getTipo() == MessageHeader.MessageType.CHAT) {
                // Extraer mensaje
                byte[] messageBytes = new byte[header.getLongitud()];
                System.arraycopy(data, MessageHeader.HEADER_SIZE, messageBytes, 0, header.getLongitud());
                String message = new String(messageBytes, StandardCharsets.UTF_8);

                // Verificar checksum
                int calculatedChecksum = calculateChecksum(messageBytes);
                if (calculatedChecksum != header.getChecksum()) {
                    logger.warn("Checksum inválido en mensaje recibido");
                    return;
                }

                if (message.startsWith("TO:")) {
                    if (connectionManager.isServerMode()) {
                        routeChatMessage(source, message, header.getCorrelId());
                    }
                    return;
                }

                ChatMessage chatMessage = buildChatMessageFromPayload(message, header.getCorrelId(), source);
                if (chatMessage != null) {
                    eventAggregator.publish(new NetworkEvent(
                        NetworkEvent.EventType.MESSAGE_RECEIVED,
                        chatMessage,
                        chatMessage.getSource()
                    ));
                }

                logService.logInfo("Mensaje recibido de " + source, "ChatService", traceId, null);
            }
        } catch (Exception e) {
            logger.error("Error procesando mensaje recibido", e);
        }
    }

    private void routeChatMessage(String source, String payload, int correlId) {
        int separatorIndex = payload.indexOf('|');
        if (separatorIndex <= 3) {
            logger.warn("Payload inválido para enrutamiento: " + payload);
            return;
        }

        String targetId = payload.substring(3, separatorIndex);
        String encodedMessage = payload.substring(separatorIndex + 1);

        try {
            String forwardPayload = "FROM:" + source + "|" + encodedMessage;
            byte[] forwardBytes = forwardPayload.getBytes(StandardCharsets.UTF_8);
            byte[] packet = buildChatPacket(forwardBytes, correlId, calculateChecksum(forwardBytes));
            connectionManager.send(targetId, packet);
            logger.info("Mensaje reenviado de " + source + " a " + targetId);
        } catch (IOException e) {
            logger.error("Error reenviando mensaje a " + targetId, e);
        }
    }

    private ChatMessage buildChatMessageFromPayload(String payload, int correlId, String fallbackSource) {
        if (!payload.startsWith("FROM:")) {
            return new ChatMessage(fallbackSource, payload, correlId);
        }

        int separatorIndex = payload.indexOf('|');
        if (separatorIndex <= 5) {
            logger.warn("Payload inválido para mensaje entrante: " + payload);
            return null;
        }

        String senderId = payload.substring(5, separatorIndex);
        String encodedMessage = payload.substring(separatorIndex + 1);
        try {
            String message = new String(Base64.getDecoder().decode(encodedMessage), StandardCharsets.UTF_8);
            return new ChatMessage(senderId, message, correlId);
        } catch (IllegalArgumentException e) {
            logger.error("Error decodificando mensaje Base64", e);
            return null;
        }
    }

    private byte[] buildChatPacket(byte[] messageBytes, int correlId, int checksum) throws IOException {
        MessageHeader header = new MessageHeader(
            MessageHeader.MessageType.CHAT,
            messageBytes.length,
            correlId,
            checksum
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(header.toBytes());
        baos.write(messageBytes);
        return baos.toByteArray();
    }

    private int calculateChecksum(byte[] data) {
        int checksum = 0;
        for (byte b : data) {
            checksum = (checksum << 1) ^ b;
        }
        return checksum;
    }

    public static class ChatMessage {
        private final String source;
        private final String message;
        private final int correlId;

        public ChatMessage(String source, String message, int correlId) {
            this.source = source;
            this.message = message;
            this.correlId = correlId;
        }

        public String getSource() {
            return source;
        }

        public String getMessage() {
            return message;
        }

        public int getCorrelId() {
            return correlId;
        }
    }
}

