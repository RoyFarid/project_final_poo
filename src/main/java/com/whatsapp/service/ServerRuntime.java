package com.whatsapp.service;

/**
 * Mantiene el estado actual del proceso para decidir si debe acceder a la base de datos.
 * Cuando se ejecuta en modo servidor se marca como true; los clientes lo dejan en false.
 */
public final class ServerRuntime {
    private static volatile boolean serverProcess = false;

    private ServerRuntime() {
    }

    public static void markAsServerProcess() {
        serverProcess = true;
    }

    public static void markAsClientProcess() {
        serverProcess = false;
    }

    public static boolean isServerProcess() {
        return serverProcess;
    }
}


