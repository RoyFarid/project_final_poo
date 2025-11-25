package com.whatsapp.network;

import com.whatsapp.network.factory.SocketFactory;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    private static ConnectionManager instance;
    private final Map<String, Socket> connections;
    private final Map<String, DataOutputStream> outputStreams;
    private ExecutorService executorService;
    private ServerSocket serverSocket;
    private final AtomicBoolean isRunning;
    private final AtomicBoolean serverMode;
    private ConnectionState state;
    private ConnectionStateListener stateListener;
    private final EventAggregator eventAggregator;
    private final LogService logService;
    private String traceId;

    private ConnectionManager() {
        this.connections = new ConcurrentHashMap<>();
        this.outputStreams = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        this.isRunning = new AtomicBoolean(false);
        this.serverMode = new AtomicBoolean(false);
        this.state = ConnectionState.DESCONECTADO;
        this.eventAggregator = EventAggregator.getInstance();
        this.logService = LogService.getInstance();
        this.traceId = logService.generateTraceId();
    }

    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    public void startServer(int port) throws IOException {
        if (isRunning.get()) {
            throw new IllegalStateException("El servidor ya está en ejecución");
        }

        ensureExecutorService();
        serverSocket = SocketFactory.createTcpServerSocket(port);
        isRunning.set(true);
        serverMode.set(true);
        setState(ConnectionState.ACTIVO);
        
        logService.logInfo("Servidor iniciado en puerto " + port, "ConnectionManager", traceId, null);
        eventAggregator.publish(new NetworkEvent(NetworkEvent.EventType.CONNECTED, "Servidor iniciado", "SERVER"));

        executorService.submit(() -> {
            try {
                while (isRunning.get()) {
                    Socket clientSocket = serverSocket.accept();
                    String clientId = clientSocket.getRemoteSocketAddress().toString();
                    connections.put(clientId, clientSocket);
                    outputStreams.put(clientId, new DataOutputStream(clientSocket.getOutputStream()));
                    
                    logService.logInfo("Cliente conectado: " + clientId, "ConnectionManager", traceId, null);
                    eventAggregator.publish(new NetworkEvent(NetworkEvent.EventType.CONNECTED, clientId, "SERVER"));
                    
                    // Iniciar hilo para recibir mensajes de este cliente
                    ensureExecutorService();
                    executorService.submit(() -> handleClient(clientId, clientSocket));
                    
                    // Enviar lista de usuarios conectados al nuevo cliente (después de iniciar el hilo)
                    // Usar un pequeño retraso para asegurar que el cliente esté listo
                    final String finalClientId = clientId;
                    ensureExecutorService();
                    executorService.submit(() -> {
                        try {
                            Thread.sleep(200); // Aumentar retraso para asegurar que el cliente esté listo
                            com.whatsapp.service.ControlService controlService = new com.whatsapp.service.ControlService();
                            Set<String> allClients = new java.util.HashSet<>(connections.keySet());
                            logger.info("Enviando lista de usuarios a " + finalClientId + ": " + allClients);
                            controlService.sendUserList(finalClientId);
                            // Notificar a todos los demás clientes sobre el nuevo usuario (excluyendo al nuevo)
                            Set<String> otherClients = new java.util.HashSet<>(allClients);
                            otherClients.remove(finalClientId);
                            for (String otherClientId : otherClients) {
                                try {
                                    logger.info("Notificando a " + otherClientId + " sobre nuevo usuario: " + finalClientId);
                                    controlService.sendControlMessage(otherClientId, 
                                        com.whatsapp.service.ControlService.CONTROL_USER_CONNECTED, finalClientId);
                                } catch (Exception e) {
                                    logger.warn("Error notificando a cliente " + otherClientId, e);
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error enviando lista de usuarios al nuevo cliente", e);
                        }
                    });
                }
            } catch (IOException e) {
                if (isRunning.get()) {
                    logger.error("Error en servidor", e);
                    logService.logError("Error en servidor: " + e.getMessage(), "ConnectionManager", traceId, null);
                }
            }
        });
    }

    public Socket connectToServer(String host, int port) throws IOException {
        ensureExecutorService();
        Socket socket = SocketFactory.createTcpSocket(host, port);
        String connectionId = socket.getRemoteSocketAddress().toString();
        connections.put(connectionId, socket);
        outputStreams.put(connectionId, new DataOutputStream(socket.getOutputStream()));
        isRunning.set(true);
        serverMode.set(false);
        setState(ConnectionState.ACTIVO);
        
        logService.logInfo("Conectado a servidor: " + host + ":" + port, "ConnectionManager", traceId, null);
        eventAggregator.publish(new NetworkEvent(NetworkEvent.EventType.CONNECTED, connectionId, "CLIENT"));
        
        // Iniciar hilo para recibir mensajes
        executorService.submit(() -> handleClient(connectionId, socket));
        
        return socket;
    }

    private void handleClient(String clientId, Socket socket) {
        try (DataInputStream input = new DataInputStream(socket.getInputStream())) {
            while (isRunning.get() && !socket.isClosed()) {
                int length = input.readInt();
                if (length > 0 && length < 10 * 1024 * 1024) { // Max 10MB
                    byte[] data = new byte[length];
                    input.readFully(data);
                    logger.debug("Mensaje recibido de " + clientId + ", tamaño: " + length);
                    // Verificar si es mensaje de control para logging
                    if (length >= com.whatsapp.protocol.MessageHeader.HEADER_SIZE) {
                        try {
                            byte[] headerBytes = new byte[com.whatsapp.protocol.MessageHeader.HEADER_SIZE];
                            System.arraycopy(data, 0, headerBytes, 0, headerBytes.length);
                            com.whatsapp.protocol.MessageHeader header = 
                                com.whatsapp.protocol.MessageHeader.fromBytes(headerBytes);
                            if (header.getTipo() == com.whatsapp.protocol.MessageHeader.MessageType.CONTROL) {
                                logger.info("Mensaje de CONTROL recibido de " + clientId);
                            }
                        } catch (Exception e) {
                            // Ignorar errores de parsing para logging
                        }
                    }
                    eventAggregator.publish(new NetworkEvent(NetworkEvent.EventType.MESSAGE_RECEIVED, data, clientId));
                }
            }
        } catch (IOException e) {
            if (isRunning.get()) {
                logger.error("Error manejando cliente " + clientId, e);
                disconnectClient(clientId);
            }
        }
    }

    public void send(String connectionId, byte[] data) throws IOException {
        DataOutputStream out = outputStreams.get(connectionId);
        if (out == null) {
            throw new IOException("Conexión no encontrada: " + connectionId);
        }
        synchronized (out) {
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        }
    }

    public void broadcast(byte[] data) {
        for (String connectionId : outputStreams.keySet()) {
            try {
                send(connectionId, data);
            } catch (IOException e) {
                logger.error("Error enviando a " + connectionId, e);
            }
        }
    }

    public void disconnectClient(String connectionId) {
        try {
            Socket socket = connections.remove(connectionId);
            DataOutputStream out = outputStreams.remove(connectionId);
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            logService.logInfo("Cliente desconectado: " + connectionId, "ConnectionManager", traceId, null);
            eventAggregator.publish(new NetworkEvent(NetworkEvent.EventType.DISCONNECTED, connectionId, "SERVER"));
            
            // Notificar a todos los clientes sobre la desconexión
            if (isRunning.get() && serverSocket != null) {
                try {
                    com.whatsapp.service.ControlService controlService = new com.whatsapp.service.ControlService();
                    controlService.notifyUserDisconnected(connectionId);
                } catch (Exception e) {
                    logger.warn("Error notificando desconexión a clientes", e);
                }
            }
        } catch (IOException e) {
            logger.error("Error desconectando cliente", e);
        }
    }

    public void stop() {
        isRunning.set(false);
        serverMode.set(false);
        setState(ConnectionState.DESCONECTADO);
        
        for (String connectionId : new java.util.ArrayList<>(connections.keySet())) {
            disconnectClient(connectionId);
        }
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("Error cerrando servidor", e);
            }
        }
        
        executorService.shutdownNow();
        ensureExecutorService();
        logService.logInfo("ConnectionManager detenido", "ConnectionManager", traceId, null);
    }

    public void setStateListener(ConnectionStateListener listener) {
        this.stateListener = listener;
    }

    private void setState(ConnectionState newState) {
        ConnectionState oldState = this.state;
        this.state = newState;
        if (stateListener != null) {
            stateListener.onStateChanged(oldState, newState);
        }
    }

    public ConnectionState getState() {
        return state;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public java.util.Set<String> getConnectedClients() {
        return connections.keySet();
    }

    public boolean isServerMode() {
        return serverMode.get();
    }

    public String getPrimaryConnectionId() {
        for (String id : connections.keySet()) {
            return id;
        }
        return null;
    }

    public void disconnectAllClients() {
        for (String connectionId : new ArrayList<>(connections.keySet())) {
            disconnectClient(connectionId);
        }
    }

    private void ensureExecutorService() {
        if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
            executorService = Executors.newCachedThreadPool();
        }
    }
}

