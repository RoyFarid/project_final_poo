package com.whatsapp.network;

import com.whatsapp.network.factory.SocketFactory;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    private static ConnectionManager instance;
    private final Map<String, Socket> connections;
    private final Map<String, DataOutputStream> outputStreams;
    private final ExecutorService executorService;
    private ServerSocket serverSocket;
    private final AtomicBoolean isRunning;
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

        serverSocket = SocketFactory.createTcpServerSocket(port);
        isRunning.set(true);
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
                    executorService.submit(() -> handleClient(clientId, clientSocket));
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
        Socket socket = SocketFactory.createTcpSocket(host, port);
        String connectionId = socket.getRemoteSocketAddress().toString();
        connections.put(connectionId, socket);
        outputStreams.put(connectionId, new DataOutputStream(socket.getOutputStream()));
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
        } catch (IOException e) {
            logger.error("Error desconectando cliente", e);
        }
    }

    public void stop() {
        isRunning.set(false);
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
        
        executorService.shutdown();
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
}

