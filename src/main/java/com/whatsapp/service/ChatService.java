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
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
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
        this.transferenciaRepository = new TransferenciaRepository();
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
            
            MessageHeader header = new MessageHeader(
                MessageHeader.MessageType.CHAT,
                messageBytes.length,
                correlId,
                checksum
            );

            // Serializar mensaje completo
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(header.toBytes());
            baos.write(messageBytes);
            byte[] fullMessage = baos.toByteArray();

            // Enviar
            connectionManager.send(connectionId, fullMessage);

            // Registrar transferencia
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

                // Publicar evento
                eventAggregator.publish(new NetworkEvent(
                    NetworkEvent.EventType.MESSAGE_RECEIVED,
                    new ChatMessage(source, message, header.getCorrelId()),
                    source
                ));

                logService.logInfo("Mensaje recibido de " + source, "ChatService", traceId, null);
            }
        } catch (Exception e) {
            logger.error("Error procesando mensaje recibido", e);
        }
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

