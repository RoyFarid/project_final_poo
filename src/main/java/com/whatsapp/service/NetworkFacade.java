package com.whatsapp.service;

import com.whatsapp.network.*;
import com.whatsapp.network.observer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

/**
 * Facade para orquestar todos los servicios de red
 */
public class NetworkFacade implements NetworkEventObserver {
    private static final Logger logger = LoggerFactory.getLogger(NetworkFacade.class);
    private final ConnectionManager connectionManager;
    private final ChatService chatService;
    private final FileTransferService fileTransferService;
    private final VideoStreamService videoStreamService;
    private final AudioStreamService audioStreamService;
    private final EventAggregator eventAggregator;
    private final LogService logService;
    private final NetworkEventObserver incomingObserver;

    public NetworkFacade() {
        this.connectionManager = ConnectionManager.getInstance();
        this.chatService = new ChatService();
        this.fileTransferService = new FileTransferService();
        this.videoStreamService = new VideoStreamService();
        this.audioStreamService = new AudioStreamService();
        this.eventAggregator = EventAggregator.getInstance();
        this.logService = LogService.getInstance();
        
        // Suscribirse a eventos de red
        eventAggregator.subscribe(this);

        // Configurar listener para mensajes recibidos
        this.incomingObserver = event -> {
            if (event.getType() != NetworkEvent.EventType.MESSAGE_RECEIVED) {
                return;
            }

            if (!(event.getData() instanceof byte[])) {
                return;
            }

            byte[] data = (byte[]) event.getData();
            if (data.length < com.whatsapp.protocol.MessageHeader.HEADER_SIZE) {
                return;
            }

            try {
                byte[] headerBytes = new byte[com.whatsapp.protocol.MessageHeader.HEADER_SIZE];
                System.arraycopy(data, 0, headerBytes, 0, headerBytes.length);
                com.whatsapp.protocol.MessageHeader header =
                    com.whatsapp.protocol.MessageHeader.fromBytes(headerBytes);

                switch (header.getTipo()) {
                    case com.whatsapp.protocol.MessageHeader.MessageType.CONTROL:
                        logger.info("NetworkFacade detectó mensaje de CONTROL, procesando...");
                        ControlService controlService = new ControlService();
                        controlService.handleControlMessage(data, event.getSource());
                        break;
                    case com.whatsapp.protocol.MessageHeader.MessageType.ARCHIVO:
                        fileTransferService.handleIncomingPacket(data, event.getSource());
                        break;
                    case com.whatsapp.protocol.MessageHeader.MessageType.VIDEO:
                        videoStreamService.handleIncomingPacket(data, event.getSource());
                        break;
                    case com.whatsapp.protocol.MessageHeader.MessageType.AUDIO:
                        audioStreamService.handleIncomingPacket(data, event.getSource());
                        break;
                    case com.whatsapp.protocol.MessageHeader.MessageType.CHAT:
                    default:
                        chatService.handleReceivedMessage(data, event.getSource());
                        break;
                }
            } catch (Exception e) {
                logger.warn("Error procesando mensaje entrante", e);
            }
        };
        eventAggregator.subscribe(incomingObserver);
    }

    // Métodos de conexión
    public void startServer(int port) throws IOException {
        connectionManager.startServer(port);
        logService.logInfo("Servidor iniciado vía NetworkFacade", "NetworkFacade", 
                          logService.generateTraceId(), null);
    }

    public void connectToServer(String host, int port) throws IOException {
        connectionManager.connectToServer(host, port);
        logService.logInfo("Conectado a servidor vía NetworkFacade", "NetworkFacade", 
                          logService.generateTraceId(), null);
    }

    public void disconnectClients() {
        connectionManager.disconnectAllClients();
        videoStreamService.stopStreaming();
        audioStreamService.stopStreaming();
    }

    public String getPrimaryConnectionId() {
        return connectionManager.getPrimaryConnectionId();
    }

    public void disconnect() {
        connectionManager.stop();
        videoStreamService.stopStreaming();
        audioStreamService.stopStreaming();
    }

    // Métodos de chat
    public void sendMessage(String connectionId, String message, Long userId, String peerIp) throws IOException {
        chatService.sendMessage(connectionId, message, userId, peerIp);
    }

    // Métodos de transferencia de archivos
    public void sendFile(String serverConnectionId, String targetConnectionId, String filePath, Long userId) throws IOException {
        fileTransferService.sendFile(serverConnectionId, targetConnectionId, filePath, userId);
    }

    // Métodos de video
    public void startVideoCall(String serverConnectionId, String targetConnectionId) {
        videoStreamService.startStreaming(serverConnectionId, targetConnectionId);
        audioStreamService.startStreaming(serverConnectionId, targetConnectionId);
    }

    public void stopVideoCall() {
        videoStreamService.stopStreaming();
        audioStreamService.stopStreaming();
    }

    // Métodos de broadcast para servidor
    public void broadcastMessage(String message, Long userId) throws IOException {
        chatService.broadcastMessage(message, userId);
    }

    public void sendMessageToClients(Set<String> targetConnectionIds, String message, Long userId) throws IOException {
        chatService.sendMessageToClients(targetConnectionIds, message, userId);
    }

    public void broadcastFile(String filePath, Long userId) throws IOException {
        fileTransferService.broadcastFile(filePath, userId);
    }

    public void sendFileToClients(Set<String> targetConnectionIds, String filePath, Long userId) throws IOException {
        fileTransferService.sendFileToClients(targetConnectionIds, filePath, userId);
    }

    public void startBroadcastVideoCall(String serverConnectionId) {
        videoStreamService.startBroadcastStreaming(serverConnectionId);
        audioStreamService.startBroadcastStreaming(serverConnectionId);
    }

    public void startVideoCallToClients(String serverConnectionId, Set<String> targetConnectionIds) {
        videoStreamService.startStreamingToClients(serverConnectionId, targetConnectionIds);
        audioStreamService.startStreamingToClients(serverConnectionId, targetConnectionIds);
    }

    // Métodos de control del servidor
    public void blockClientToClientCommunication() throws IOException {
        ControlService controlService = new ControlService();
        controlService.blockClientToClient();
    }

    public void unblockClientToClientCommunication() throws IOException {
        ControlService controlService = new ControlService();
        controlService.unblockClientToClient();
    }

    public boolean isClientToClientBlocked() {
        return ControlService.isClientToClientBlocked();
    }

    public void setMicrophoneMuted(boolean muted) {
        audioStreamService.setMicrophoneMuted(muted);
    }

    public void setSpeakerMuted(boolean muted) {
        audioStreamService.setSpeakerMuted(muted);
    }

    // Métodos de información
    public ConnectionState getConnectionState() {
        return connectionManager.getState();
    }

    public Set<String> getConnectedClients() {
        return connectionManager.getConnectedClients();
    }

    public boolean isConnected() {
        return connectionManager.isRunning();
    }

    // Métodos de gestión de clientes para el servidor
    public void approveClient(String connectionId) throws IOException {
        ControlService controlService = new ControlService();
        controlService.approveClient(connectionId);
    }

    public void rejectClient(String connectionId) throws IOException {
        ControlService controlService = new ControlService();
        controlService.rejectClient(connectionId);
    }

    public void kickClient(String connectionId) throws IOException {
        ControlService controlService = new ControlService();
        controlService.kickClient(connectionId);
    }

    public Set<String> getPendingClients() {
        ControlService controlService = new ControlService();
        return controlService.getPendingClients();
    }

    public Set<String> getApprovedClients() {
        return ControlService.getApprovedClients();
    }

    @Override
    public void onNetworkEvent(NetworkEvent event) {
        // El facade puede procesar eventos globales si es necesario
        logger.debug("NetworkFacade recibió evento: {}", event.getType());
    }

    public void shutdown() {
        eventAggregator.unsubscribe(this);
        eventAggregator.unsubscribe(incomingObserver);
    }
}

