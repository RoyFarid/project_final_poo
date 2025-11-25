package com.whatsapp.network;

import java.io.IOException;

public interface INetworkClient {
    void connect(String host, int port) throws IOException;
    void disconnect() throws IOException;
    boolean isConnected();
    ConnectionState getState();
    void send(byte[] data) throws IOException;
    byte[] receive() throws IOException;
    void setStateListener(ConnectionStateListener listener);
}

