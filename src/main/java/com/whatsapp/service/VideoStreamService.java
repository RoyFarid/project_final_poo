package com.whatsapp.service;

import com.whatsapp.network.ConnectionManager;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.protocol.MessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class VideoStreamService {
    private static final Logger logger = LoggerFactory.getLogger(VideoStreamService.class);
    private final ConnectionManager connectionManager;
    private final EventAggregator eventAggregator;
    private final LogService logService;
    private final AtomicInteger frameIdGenerator;
    private DatagramSocket udpSocket;
    private final AtomicBoolean isStreaming;
    private static final int UDP_PORT = 8888;
    private static final int MAX_PACKET_SIZE = 65507; // Max UDP packet size
    private String traceId;

    public VideoStreamService() {
        this.connectionManager = ConnectionManager.getInstance();
        this.eventAggregator = EventAggregator.getInstance();
        this.logService = LogService.getInstance();
        this.frameIdGenerator = new AtomicInteger(0);
        this.isStreaming = new AtomicBoolean(false);
        this.traceId = logService.generateTraceId();
    }

    public void startStreaming(String targetHost, int targetPort) throws IOException {
        if (isStreaming.get()) {
            throw new IllegalStateException("Ya se está transmitiendo video");
        }

        udpSocket = new java.net.DatagramSocket();
        isStreaming.set(true);

        // Iniciar captura y envío de frames
        new Thread(() -> {
            try {
                InetAddress targetAddress = InetAddress.getByName(targetHost);
                while (isStreaming.get()) {
                    // Capturar frame (esto se implementaría con la cámara real)
                    byte[] frameData = captureFrame();
                    if (frameData != null && frameData.length > 0) {
                        sendVideoFrame(targetAddress, targetPort, frameData);
                    }
                    Thread.sleep(33); // ~30 FPS
                }
            } catch (Exception e) {
                logger.error("Error en streaming de video", e);
            } finally {
                if (udpSocket != null && !udpSocket.isClosed()) {
                    udpSocket.close();
                }
            }
        }).start();

        logService.logInfo("Streaming de video iniciado", "VideoStreamService", traceId, null);
    }

    public void startReceiving(int port) throws IOException {
        if (isStreaming.get()) {
            throw new IllegalStateException("Ya se está recibiendo video");
        }

        udpSocket = new java.net.DatagramSocket(port);
        isStreaming.set(true);

        new Thread(() -> {
            byte[] buffer = new byte[MAX_PACKET_SIZE];
            while (isStreaming.get()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    
                    byte[] frameData = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, frameData, 0, packet.getLength());
                    
                    // Publicar evento con frame recibido
                    eventAggregator.publish(new NetworkEvent(
                        NetworkEvent.EventType.VIDEO_FRAME,
                        frameData,
                        packet.getAddress().toString()
                    ));
                } catch (IOException e) {
                    if (isStreaming.get()) {
                        logger.error("Error recibiendo frame de video", e);
                    }
                }
            }
        }).start();

        logService.logInfo("Recepción de video iniciada en puerto " + port, "VideoStreamService", traceId, null);
    }

    private void sendVideoFrame(InetAddress targetAddress, int targetPort, byte[] frameData) throws IOException {
        int frameId = frameIdGenerator.incrementAndGet();
        
        // Dividir frame en paquetes si es necesario
        int offset = 0;
        int packetNumber = 0;
        
        while (offset < frameData.length) {
            int chunkSize = Math.min(MAX_PACKET_SIZE - 20, frameData.length - offset); // -20 para header
            byte[] chunk = new byte[chunkSize];
            System.arraycopy(frameData, offset, chunk, 0, chunkSize);
            
            // Crear paquete con metadata
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(frameId);
            dos.writeInt(packetNumber);
            dos.writeInt(frameData.length);
            dos.writeInt(offset);
            dos.write(chunk);
            dos.flush();
            
            byte[] packetData = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(
                packetData,
                packetData.length,
                targetAddress,
                targetPort
            );
            
            udpSocket.send(packet);
            offset += chunkSize;
            packetNumber++;
        }
    }

    private byte[] captureFrame() {
        // Placeholder - esto se implementaría con la cámara real usando JavaFX MediaCapture
        // Por ahora retornamos null
        return null;
    }

    public void stopStreaming() {
        isStreaming.set(false);
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        logService.logInfo("Streaming de video detenido", "VideoStreamService", traceId, null);
    }

    public boolean isStreaming() {
        return isStreaming.get();
    }
}

