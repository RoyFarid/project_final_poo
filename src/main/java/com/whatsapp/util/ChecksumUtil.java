package com.whatsapp.util;

/**
 * Utilidad para calcular checksums de datos.
 * Centraliza la lógica de cálculo de checksum para evitar duplicación de código.
 */
public final class ChecksumUtil {
    private ChecksumUtil() {
        // Clase utilitaria, no instanciable
    }

    /**
     * Calcula un checksum simple usando operaciones de bits.
     * 
     * @param data Los datos para los cuales calcular el checksum
     * @return El checksum calculado
     */
    public static int calculateChecksum(byte[] data) {
        int checksum = 0;
        for (byte b : data) {
            checksum = (checksum << 1) ^ b;
        }
        return checksum;
    }
}

