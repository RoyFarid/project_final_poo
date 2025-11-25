package com.whatsapp.service;

import com.whatsapp.network.ConnectionManager;
import com.whatsapp.protocol.MessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio encargado de capturar, transmitir y reproducir audio en una videollamada.
 */
public class AudioStreamService {
    private static final Logger logger = LoggerFactory.getLogger(AudioStreamService.class);
    private static final byte DIRECTION_CLIENT_TO_SERVER = 0;
    private static final byte DIRECTION_SERVER_TO_CLIENT = 1;
    private static final int CHUNK_MILLIS = 20;

    private final ConnectionManager connectionManager;
    private final LogService logService;
    private final AtomicBoolean isStreaming;
    private final AtomicBoolean micMuted;
    private final AtomicBoolean speakerMuted;
    private final AtomicInteger frameIdGenerator;
    private final AudioFormat audioFormat;

    private ExecutorService executorService;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private String currentServerConnectionId;
    private String currentTargetConnectionId;
    private String traceId;

    public AudioStreamService() {
        this.connectionManager = ConnectionManager.getInstance();
        this.logService = LogService.getInstance();
        this.isStreaming = new AtomicBoolean(false);
        this.micMuted = new AtomicBoolean(false);
        this.speakerMuted = new AtomicBoolean(false);
        this.frameIdGenerator = new AtomicInteger(0);
        this.audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16000.0f,
            16,
            1,
            2,
            16000.0f,
            false
        );
        this.traceId = logService.generateTraceId();
    }

    public void startStreaming(String serverConnectionId, String targetConnectionId) {
        if (isStreaming.get()) {
            logger.warn("Audio ya se encuentra transmitiéndose");
            return;
        }

        this.currentServerConnectionId = serverConnectionId;
        this.currentTargetConnectionId = targetConnectionId;
        this.executorService = Executors.newSingleThreadExecutor();

        try {
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(micInfo)) {
                throw new LineUnavailableException("Micrófono no soportado para el formato requerido");
            }
            microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
            microphone.open(audioFormat);
            microphone.start();

            isStreaming.set(true);
            executorService.submit(this::captureLoop);
            logService.logInfo("Streaming de audio iniciado", "AudioStreamService", traceId, null);
        } catch (LineUnavailableException e) {
            logger.error("No se pudo iniciar el streaming de audio", e);
            stopStreaming();
        }
    }

    public void stopStreaming() {
        isStreaming.set(false);
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }
        if (speakers != null) {
            speakers.stop();
            speakers.close();
            speakers = null;
        }
        logService.logInfo("Streaming de audio detenido", "AudioStreamService", traceId, null);
    }

    public void setMicrophoneMuted(boolean muted) {
        micMuted.set(muted);
    }

    public void setSpeakerMuted(boolean muted) {
        speakerMuted.set(muted);
    }

    private void captureLoop() {
        int bufferSize = (int) (audioFormat.getFrameSize() * (audioFormat.getSampleRate() * (CHUNK_MILLIS / 1000.0)));
        bufferSize = Math.max(bufferSize, 1024);
        byte[] buffer = new byte[bufferSize];

        while (isStreaming.get()) {
            try {
                if (micMuted.get() || microphone == null) {
                    Thread.sleep(CHUNK_MILLIS);
                    continue;
                }
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    byte[] data = new byte[bytesRead];
                    System.arraycopy(buffer, 0, data, 0, bytesRead);
                    sendAudioChunk(data);
                }
            } catch (Exception e) {
                logger.error("Error en captura de audio", e);
                break;
            }
        }
    }

    private void sendAudioChunk(byte[] chunk) throws IOException {
        if (currentServerConnectionId == null || currentTargetConnectionId == null) {
            return;
        }

        byte[] routedPayload = wrapPayload(DIRECTION_CLIENT_TO_SERVER, currentTargetConnectionId, chunk);
        int frameId = frameIdGenerator.incrementAndGet();
        MessageHeader header = new MessageHeader(
            MessageHeader.MessageType.AUDIO,
            routedPayload.length,
            frameId,
            calculateChecksum(routedPayload)
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(header.toBytes());
        baos.write(routedPayload);
        connectionManager.send(currentServerConnectionId, baos.toByteArray());
    }

    public void handleIncomingPacket(byte[] data, String source) {
        try {
            byte[] headerBytes = new byte[MessageHeader.HEADER_SIZE];
            System.arraycopy(data, 0, headerBytes, 0, headerBytes.length);
            MessageHeader header = MessageHeader.fromBytes(headerBytes);
            if (header.getTipo() != MessageHeader.MessageType.AUDIO) {
                return;
            }

            byte[] payload = new byte[header.getLongitud()];
            System.arraycopy(data, MessageHeader.HEADER_SIZE, payload, 0, payload.length);
            AudioFrame frame = parseFrame(payload);
            if (frame == null) {
                return;
            }

            if (frame.direction == DIRECTION_CLIENT_TO_SERVER && connectionManager.isServerMode()) {
                forwardFrame(frame, source, header.getCorrelId());
            } else if (frame.direction == DIRECTION_SERVER_TO_CLIENT && !connectionManager.isServerMode()) {
                playAudio(frame.payload);
            }
        } catch (Exception e) {
            logger.error("Error procesando paquete de audio", e);
        }
    }

    private void forwardFrame(AudioFrame frame, String originalSender, int correlId) throws IOException {
        byte[] routedPayload = wrapPayload(DIRECTION_SERVER_TO_CLIENT, originalSender, frame.payload);
        MessageHeader header = new MessageHeader(
            MessageHeader.MessageType.AUDIO,
            routedPayload.length,
            correlId,
            calculateChecksum(routedPayload)
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(header.toBytes());
        baos.write(routedPayload);
        connectionManager.send(frame.peerId, baos.toByteArray());
    }

    private void playAudio(byte[] payload) {
        if (speakerMuted.get()) {
            return;
        }
        try {
            if (speakers == null) {
                DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
                speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
                speakers.open(audioFormat);
                speakers.start();
            }
            speakers.write(payload, 0, payload.length);
        } catch (Exception e) {
            logger.error("No se pudo reproducir audio entrante", e);
        }
    }

    private byte[] wrapPayload(byte direction, String peerId, byte[] payload) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeByte(direction);
            dos.writeUTF(peerId);
            dos.writeInt(payload.length);
            dos.write(payload);
            dos.flush();
        }
        return baos.toByteArray();
    }

    private AudioFrame parseFrame(byte[] payload) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload))) {
            byte direction = dis.readByte();
            String peerId = dis.readUTF();
            int length = dis.readInt();
            byte[] data = new byte[length];
            dis.readFully(data);
            return new AudioFrame(direction, peerId, data);
        }
    }

    private int calculateChecksum(byte[] data) {
        int checksum = 0;
        for (byte b : data) {
            checksum = (checksum << 1) ^ b;
        }
        return checksum;
    }

    private static class AudioFrame {
        final byte direction;
        final String peerId;
        final byte[] payload;

        AudioFrame(byte direction, String peerId, byte[] payload) {
            this.direction = direction;
            this.peerId = peerId;
            this.payload = payload;
        }
    }
}


