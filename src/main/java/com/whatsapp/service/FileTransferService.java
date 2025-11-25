package com.whatsapp.service;

import com.whatsapp.model.Transferencia;
import com.whatsapp.network.ConnectionManager;
import com.whatsapp.network.observer.EventAggregator;
import com.whatsapp.network.observer.NetworkEvent;
import com.whatsapp.network.strategy.ExponentialBackoffStrategy;
import com.whatsapp.network.strategy.RetryStrategy;
import com.whatsapp.protocol.MessageHeader;
import com.whatsapp.repository.TransferenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FileTransferService {
    private static final Logger logger = LoggerFactory.getLogger(FileTransferService.class);
    private final ConnectionManager connectionManager;
    private final TransferenciaRepository transferenciaRepository;
    private final EventAggregator eventAggregator;
    private final LogService logService;
    private final RetryStrategy retryStrategy;
    private final AtomicInteger correlIdGenerator;
    private final Map<Integer, FileTransfer> activeTransfers;
    private static final int CHUNK_SIZE = 64 * 1024; // 64 KB
    private String traceId;

    public FileTransferService() {
        this.connectionManager = ConnectionManager.getInstance();
        this.transferenciaRepository = new TransferenciaRepository();
        this.eventAggregator = EventAggregator.getInstance();
        this.logService = LogService.getInstance();
        this.retryStrategy = new ExponentialBackoffStrategy(1000, 30000, 5);
        this.correlIdGenerator = new AtomicInteger(0);
        this.activeTransfers = new ConcurrentHashMap<>();
        this.traceId = logService.generateTraceId();
    }

    public void sendFile(String connectionId, String filePath, Long userId, String peerIp) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Archivo no encontrado: " + filePath);
        }

        long fileSize = Files.size(path);
        String fileName = path.getFileName().toString();
        String checksum = calculateSHA256(path);

        // Crear registro de transferencia
        Transferencia transferencia = new Transferencia(
            Transferencia.TipoTransferencia.ARCHIVO,
            fileName,
            fileSize,
            checksum,
            userId,
            peerIp
        );
        transferencia.setEstado(Transferencia.EstadoTransferencia.EN_PROGRESO);
        transferencia = transferenciaRepository.save(transferencia);

        int transferId = transferencia.getId().intValue();
        FileTransfer fileTransfer = new FileTransfer(transferId, path, fileSize, fileName, checksum);
        activeTransfers.put(transferId, fileTransfer);

        // Enviar metadata primero
        sendFileMetadata(connectionId, fileName, fileSize, checksum, transferId);

        // Enviar archivo en chunks
        try (FileInputStream fis = new FileInputStream(path.toFile());
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            byte[] buffer = new byte[CHUNK_SIZE];
            int chunkNumber = 0;
            long totalSent = 0;

            while (totalSent < fileSize) {
                int bytesRead = bis.read(buffer);
                if (bytesRead == -1) break;

                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);

                sendFileChunk(connectionId, transferId, chunkNumber, chunk, totalSent, fileSize);
                
                totalSent += bytesRead;
                chunkNumber++;

                // Notificar progreso
                double progress = (double) totalSent / fileSize * 100;
                eventAggregator.publish(new NetworkEvent(
                    NetworkEvent.EventType.FILE_PROGRESS,
                    new FileProgress(transferId, progress, totalSent, fileSize),
                    connectionId
                ));
            }

            // Marcar como completada
            transferencia.setEstado(Transferencia.EstadoTransferencia.COMPLETADA);
            transferencia.setFin(java.time.LocalDateTime.now());
            transferenciaRepository.update(transferencia);
            
            activeTransfers.remove(transferId);
            logService.logInfo("Archivo enviado: " + fileName, "FileTransferService", traceId, userId);
        } catch (IOException e) {
            transferencia.setEstado(Transferencia.EstadoTransferencia.ERROR);
            transferenciaRepository.update(transferencia);
            activeTransfers.remove(transferId);
            throw e;
        }
    }

    private void sendFileMetadata(String connectionId, String fileName, long fileSize, String checksum, int transferId) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeUTF(fileName);
            oos.writeLong(fileSize);
            oos.writeUTF(checksum);
            oos.writeInt(transferId);
            oos.flush();
            byte[] metadata = baos.toByteArray();

            int correlId = correlIdGenerator.incrementAndGet();
            int checksumInt = calculateChecksum(metadata);
            MessageHeader header = new MessageHeader(
                MessageHeader.MessageType.ARCHIVO,
                metadata.length,
                correlId,
                checksumInt
            );

            ByteArrayOutputStream fullMessage = new ByteArrayOutputStream();
            fullMessage.write(header.toBytes());
            fullMessage.write(metadata);

            connectionManager.send(connectionId, fullMessage.toByteArray());
        } catch (IOException e) {
            logger.error("Error enviando metadata de archivo", e);
            throw e;
        }
    }

    private void sendFileChunk(String connectionId, int transferId, int chunkNumber, byte[] chunk, 
                              long offset, long totalSize) throws IOException {
        int attempts = 0;
        while (attempts < 5) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeInt(transferId);
                oos.writeInt(chunkNumber);
                oos.writeLong(offset);
                oos.writeInt(chunk.length);
                oos.write(chunk);
                oos.flush();
                byte[] chunkData = baos.toByteArray();

                int correlId = correlIdGenerator.incrementAndGet();
                int checksum = calculateChecksum(chunkData);
                MessageHeader header = new MessageHeader(
                    MessageHeader.MessageType.ARCHIVO,
                    chunkData.length,
                    correlId,
                    checksum
                );

                ByteArrayOutputStream fullMessage = new ByteArrayOutputStream();
                fullMessage.write(header.toBytes());
                fullMessage.write(chunkData);

                connectionManager.send(connectionId, fullMessage.toByteArray());
                return; // Éxito
            } catch (IOException e) {
                attempts++;
                if (retryStrategy.shouldRetry(attempts, e)) {
                    try {
                        Thread.sleep(retryStrategy.getDelay(attempts));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrumpido durante reintento", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    public void receiveFile(byte[] data, String source) {
        // Implementar recepción de archivo
        // Similar a sendFile pero al revés
    }

    private String calculateSHA256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(path.toFile());
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Error calculando SHA-256", e);
        }
    }

    private int calculateChecksum(byte[] data) {
        int checksum = 0;
        for (byte b : data) {
            checksum = (checksum << 1) ^ b;
        }
        return checksum;
    }

    private static class FileTransfer {
        final int transferId;
        final Path filePath;
        final long fileSize;
        final String fileName;
        final String checksum;

        FileTransfer(int transferId, Path filePath, long fileSize, String fileName, String checksum) {
            this.transferId = transferId;
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.fileName = fileName;
            this.checksum = checksum;
        }
    }

    public static class FileProgress {
        private final int transferId;
        private final double progress;
        private final long bytesTransferred;
        private final long totalBytes;

        public FileProgress(int transferId, double progress, long bytesTransferred, long totalBytes) {
            this.transferId = transferId;
            this.progress = progress;
            this.bytesTransferred = bytesTransferred;
            this.totalBytes = totalBytes;
        }

        public int getTransferId() {
            return transferId;
        }

        public double getProgress() {
            return progress;
        }

        public long getBytesTransferred() {
            return bytesTransferred;
        }

        public long getTotalBytes() {
            return totalBytes;
        }
    }
}

