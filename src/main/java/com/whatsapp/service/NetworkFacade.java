package com.whatsapp.service;

import com.whatsapp.network.ConnectionManager;
import com.whatsapp.network.ConnectionState;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.network.observer.NetworkEventObserver;
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
    private final EventAggregator eventAggregator;
    private final LogService logService;

    public NetworkFacade() {
        this.connectionManager = ConnectionManager.getInstance();
        this.chatService = new ChatService();
        this.fileTransferService = new FileTransferService();
        this.videoStreamService = new VideoStreamService();
        this.eventAggregator = EventAggregator.getInstance();
        this.logService = LogService.getInstance();
        
        // Suscribirse a eventos de red
        eventAggregator.subscribe(this);
        
        // Configurar listener para mensajes recibidos
        eventAggregator.subscribe(new NetworkEventObserver() {
            @Override
            public void onNetworkEvent(NetworkEvent event) {
                if (event.getType() == NetworkEvent.EventType.MESSAGE_RECEIVED) {
                    if (event.getData() instanceof byte[]) {
                        byte[] data = (byte[]) event.getData();
                        // Verificar si es un mensaje de control
                        if (data.length >= com.whatsapp.protocol.MessageHeader.HEADER_SIZE) {
                            try {
                                byte[] headerBytes = new byte[com.whatsapp.protocol.MessageHeader.HEADER_SIZE];
                                System.arraycopy(data, 0, headerBytes, 0, headerBytes.length);
                                com.whatsapp.protocol.MessageHeader header = 
                                    com.whatsapp.protocol.MessageHeader.fromBytes(headerBytes);
                                
                                if (header.getTipo() == com.whatsapp.protocol.MessageHeader.MessageType.CONTROL) {
                                    // Es un mensaje de control, procesarlo
                                    com.whatsapp.service.ControlService controlService = 
                                        new com.whatsapp.service.ControlService();
                                    controlService.handleControlMessage(data, event.getSource());
                                    return;
                                }
                            } catch (Exception e) {
                                logger.warn("Error verificando tipo de mensaje", e);
                            }
                        }
                        // Es un mensaje de chat normal
                        chatService.handleReceivedMessage(data, event.getSource());
                    }
                }
            }
        });
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

    public void disconnect() {
        connectionManager.stop();
        videoStreamService.stopStreaming();
    }

    // Métodos de chat
    public void sendMessage(String connectionId, String message, Long userId, String peerIp) throws IOException {
        chatService.sendMessage(connectionId, message, userId, peerIp);
    }

    // Métodos de transferencia de archivos
    public void sendFile(String connectionId, String filePath, Long userId, String peerIp) throws IOException {
        fileTransferService.sendFile(connectionId, filePath, userId, peerIp);
    }

    // Métodos de video
    public void startVideoCall(String targetHost, int targetPort) throws IOException {
        videoStreamService.startStreaming(targetHost, targetPort);
    }

    public void startReceivingVideo(int port) throws IOException {
        videoStreamService.startReceiving(port);
    }

    public void stopVideoCall() {
        videoStreamService.stopStreaming();
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

    @Override
    public void onNetworkEvent(NetworkEvent event) {
        // El facade puede procesar eventos globales si es necesario
        logger.debug("NetworkFacade recibió evento: {}", event.getType());
    }
}

