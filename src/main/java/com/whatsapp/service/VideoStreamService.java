package com.whatsapp.service;

import com.github.sarxos.webcam.Webcam;
import com.whatsapp.network.ConnectionManager;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.protocol.MessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.whatsapp.service.ControlService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class VideoStreamService {
    private static final Logger logger = LoggerFactory.getLogger(VideoStreamService.class);
    private final ConnectionManager connectionManager;
    private final EventAggregator eventAggregator;
    private final LogService logService;
    private final AtomicInteger frameIdGenerator;
    private final AtomicBoolean isStreaming;
    private ScheduledExecutorService executorService;
    private String currentServerConnectionId;
    private String currentTargetConnectionId;
    private String traceId;
    private static final byte DIRECTION_CLIENT_TO_SERVER = 0;
    private static final byte DIRECTION_SERVER_TO_CLIENT = 1;
    private static final int FRAME_WIDTH = 320;
    private static final int FRAME_HEIGHT = 240;
    private Webcam webcam;

    public VideoStreamService() {
        this.connectionManager = ConnectionManager.getInstance();
        this.eventAggregator = EventAggregator.getInstance();
        this.logService = LogService.getInstance();
        this.frameIdGenerator = new AtomicInteger(0);
        this.isStreaming = new AtomicBoolean(false);
        this.traceId = logService.generateTraceId();
    }

    public void startStreaming(String serverConnectionId, String targetConnectionId) {
        if (isStreaming.get()) {
            throw new IllegalStateException("Ya se está transmitiendo video");
        }
        this.currentServerConnectionId = serverConnectionId;
        this.currentTargetConnectionId = targetConnectionId;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        isStreaming.set(true);

        executorService.scheduleAtFixedRate(this::sendFrameHeartbeat, 0, 300, TimeUnit.MILLISECONDS);
        logService.logInfo("Streaming de video iniciado", "VideoStreamService", traceId, null);
    }

    private void sendFrameHeartbeat() {
        if (!isStreaming.get() || currentServerConnectionId == null || currentTargetConnectionId == null) {
            return;
        }

        if (ControlService.isAdminCameraDisabled()) {
            return;
        }

        try {
            int frameId = frameIdGenerator.incrementAndGet();
            byte[] frameData = captureFrame();
            if (frameData == null || frameData.length == 0) {
                return;
            }

            byte[] routedPayload = wrapPayload(DIRECTION_CLIENT_TO_SERVER, currentTargetConnectionId, frameData);
            MessageHeader header = new MessageHeader(
                MessageHeader.MessageType.VIDEO,
                routedPayload.length,
                frameId,
                calculateChecksum(routedPayload)
            );

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(header.toBytes());
            baos.write(routedPayload);
            connectionManager.send(currentServerConnectionId, baos.toByteArray());
        } catch (IOException e) {
            logger.error("Error enviando frame de video", e);
        }
    }

    public void handleIncomingPacket(byte[] data, String source) {
        try {
            byte[] headerBytes = new byte[MessageHeader.HEADER_SIZE];
            System.arraycopy(data, 0, headerBytes, 0, headerBytes.length);
            MessageHeader header = MessageHeader.fromBytes(headerBytes);
            if (header.getTipo() != MessageHeader.MessageType.VIDEO) {
                return;
            }

            byte[] payload = new byte[header.getLongitud()];
            System.arraycopy(data, MessageHeader.HEADER_SIZE, payload, 0, payload.length);

            VideoFrame frame = parseFrame(payload);
            if (frame == null) {
                return;
            }

            if (frame.direction == DIRECTION_CLIENT_TO_SERVER && connectionManager.isServerMode()) {
                forwardFrame(frame, source, header.getCorrelId());
            } else if (frame.direction == DIRECTION_SERVER_TO_CLIENT && !connectionManager.isServerMode()) {
                eventAggregator.publish(new NetworkEvent(
                    NetworkEvent.EventType.VIDEO_FRAME,
                    new VideoFramePayload(header.getCorrelId(), frame.peerId, frame.payload),
                    frame.peerId
                ));
            }
        } catch (Exception e) {
            logger.error("Error procesando paquete de video", e);
        }
    }

    private void forwardFrame(VideoFrame frame, String originalSender, int correlId) {
        try {
            byte[] routedPayload = wrapPayload(DIRECTION_SERVER_TO_CLIENT, originalSender, frame.payload);
            MessageHeader header = new MessageHeader(
                MessageHeader.MessageType.VIDEO,
                routedPayload.length,
                correlId,
                calculateChecksum(routedPayload)
            );

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(header.toBytes());
            baos.write(routedPayload);
            connectionManager.send(frame.peerId, baos.toByteArray());
        } catch (IOException e) {
            logger.error("Error reenviando frame de video", e);
        }
    }

    public void stopStreaming() {
        isStreaming.set(false);
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        logService.logInfo("Streaming de video detenido", "VideoStreamService", traceId, null);
    }

    private byte[] captureFrame() {
        try {
            if (webcam == null) {
                webcam = Webcam.getDefault();
                if (webcam == null) {
                    logger.warn("No se encontr\u00f3 webcam; usando captura de pantalla como fallback");
                    return captureScreenFallback();
                }
                webcam.setViewSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
                webcam.open(true);
            }

            if (!webcam.isOpen()) {
                webcam.open(true);
            }

            BufferedImage image = webcam.getImage();
            if (image == null) {
                logger.warn("Webcam no entreg\u00f3 imagen, usando captura de pantalla fallback");
                return captureScreenFallback();
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "jpg", baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            logger.error("No se pudo capturar frame de video", e);
            return captureScreenFallback();
        }
    }

    private byte[] captureScreenFallback() {
        try {
            Rectangle screenRect = new Rectangle(
                0,
                0,
                Math.min(FRAME_WIDTH, (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth()),
                Math.min(FRAME_HEIGHT, (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight())
            );
            BufferedImage image = new Robot().createScreenCapture(screenRect);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "jpg", baos);
                return baos.toByteArray();
            }
        } catch (Exception ex) {
            logger.error("Tampoco se pudo capturar pantalla como fallback", ex);
            return null;
        }
    }

    private byte[] wrapPayload(byte direction, String peerId, byte[] payload) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte(direction);
        dos.writeUTF(peerId);
        dos.writeInt(payload.length);
        dos.write(payload);
        dos.flush();
        return baos.toByteArray();
    }

    private VideoFrame parseFrame(byte[] payload) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload))) {
            byte direction = dis.readByte();
            String peerId = dis.readUTF();
            int length = dis.readInt();
            byte[] data = new byte[length];
            dis.readFully(data);
            return new VideoFrame(direction, peerId, data);
        }
    }

    private int calculateChecksum(byte[] data) {
        int checksum = 0;
        for (byte b : data) {
            checksum = (checksum << 1) ^ b;
        }
        return checksum;
    }

    private static class VideoFrame {
        final byte direction;
        final String peerId;
        final byte[] payload;

        VideoFrame(byte direction, String peerId, byte[] payload) {
            this.direction = direction;
            this.peerId = peerId;
            this.payload = payload;
        }
    }

    public static class VideoFramePayload {
        private final int frameId;
        private final String peerId;
        private final byte[] data;

        public VideoFramePayload(int frameId, String peerId, byte[] data) {
            this.frameId = frameId;
            this.peerId = peerId;
            this.data = data;
        }

        public int getFrameId() {
            return frameId;
        }

        public String getPeerId() {
            return peerId;
        }

        public byte[] getData() {
            return data;
        }
    }
}

