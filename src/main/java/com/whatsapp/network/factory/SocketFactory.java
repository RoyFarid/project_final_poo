package com.whatsapp.network.factory;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

public class SocketFactory {
    public static Socket createTcpSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }

    public static ServerSocket createTcpServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }

    public static java.net.DatagramSocket createUdpSocket() throws IOException {
        return new java.net.DatagramSocket();
    }

    public static java.net.DatagramSocket createUdpSocket(int port) throws IOException {
        return new java.net.DatagramSocket(port);
    }
}

