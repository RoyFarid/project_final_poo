package com.whatsapp.network;

public interface ConnectionStateListener {
    void onStateChanged(ConnectionState oldState, ConnectionState newState);
    void onError(String error);
}

