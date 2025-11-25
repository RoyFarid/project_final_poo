package com.whatsapp.service;

import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.network.observer.NetworkEventObserver;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cliente auxiliar que encapsula las solicitudes de autenticación/registro al servidor central.
 */
public class RemoteAuthClient implements NetworkEventObserver, AutoCloseable {
    private final EventAggregator eventAggregator;
    private final ControlService controlService;
    private final AtomicReference<CompletableFuture<ControlService.OperationResultPayload>> pendingAuth;
    private final AtomicReference<CompletableFuture<ControlService.OperationResultPayload>> pendingRegister;

    public RemoteAuthClient() {
        this.eventAggregator = EventAggregator.getInstance();
        this.controlService = new ControlService();
        this.pendingAuth = new AtomicReference<>();
        this.pendingRegister = new AtomicReference<>();
        eventAggregator.subscribe(this);
    }

    public ControlService.OperationResultPayload authenticate(String serverConnectionId, String username, String password)
        throws IOException, TimeoutException, InterruptedException {
        CompletableFuture<ControlService.OperationResultPayload> future = new CompletableFuture<>();
        pendingAuth.set(future);
        controlService.sendAuthRequest(serverConnectionId, username, password);
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IOException("Error interno autenticando", e);
        }
    }

    public ControlService.OperationResultPayload register(String serverConnectionId, String username, String password, String email)
        throws IOException, TimeoutException, InterruptedException {
        CompletableFuture<ControlService.OperationResultPayload> future = new CompletableFuture<>();
        pendingRegister.set(future);
        controlService.sendRegisterRequest(serverConnectionId, username, password, email);
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IOException("Error interno registrando", e);
        }
    }

    @Override
    public void onNetworkEvent(NetworkEvent event) {
        if (event.getType() == NetworkEvent.EventType.AUTH_RESULT) {
            CompletableFuture<ControlService.OperationResultPayload> future = pendingAuth.getAndSet(null);
            if (future != null && event.getData() instanceof ControlService.OperationResultPayload payload) {
                future.complete(payload);
            }
        } else if (event.getType() == NetworkEvent.EventType.REGISTER_RESULT) {
            CompletableFuture<ControlService.OperationResultPayload> future = pendingRegister.getAndSet(null);
            if (future != null && event.getData() instanceof ControlService.OperationResultPayload payload) {
                future.complete(payload);
            }
        }
    }

    @Override
    public void close() {
        eventAggregator.unsubscribe(this);
    }
}


