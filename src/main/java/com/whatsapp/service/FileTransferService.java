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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
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
    private final Map<Integer, IncomingTransfer> incomingTransfers;
    private static final byte DIRECTION_CLIENT_TO_SERVER = 0;
    private static final byte DIRECTION_SERVER_TO_CLIENT = 1;
    private static final byte FRAME_METADATA = 1;
    private static final byte FRAME_CHUNK = 2;
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
        this.incomingTransfers = new ConcurrentHashMap<>();
        this.traceId = logService.generateTraceId();
    }

    public void sendFile(String serverConnectionId, String targetConnectionId, String filePath, Long userId) throws IOException {
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
            targetConnectionId
        );
        transferencia.setEstado(Transferencia.EstadoTransferencia.EN_PROGRESO);
        transferencia = transferenciaRepository.save(transferencia);

        int transferId = transferencia.getId().intValue();
        FileTransfer fileTransfer = new FileTransfer(transferId, path, fileSize, fileName, checksum);
        activeTransfers.put(transferId, fileTransfer);

        // Enviar metadata primero
        sendFileMetadata(serverConnectionId, targetConnectionId, fileName, fileSize, checksum, transferId);

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

                sendFileChunk(serverConnectionId, targetConnectionId, transferId, chunkNumber, chunk, totalSent, fileSize);
                
                totalSent += bytesRead;
                chunkNumber++;

                // Notificar progreso
                double progress = (double) totalSent / fileSize * 100;
                eventAggregator.publish(new NetworkEvent(
                    NetworkEvent.EventType.FILE_PROGRESS,
                    new FileProgress(transferId, fileName, progress, totalSent, fileSize, false, null),
                    targetConnectionId
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

    private void sendFileMetadata(String serverConnectionId, String targetConnectionId, String fileName, long fileSize, String checksum, int transferId) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (DataOutputStream dos = new DataOutputStream(baos)) {
                dos.writeUTF(fileName);
                dos.writeLong(fileSize);
                dos.writeUTF(checksum);
                dos.writeInt(transferId);
                dos.flush();
            }

            byte[] metadata = baos.toByteArray();
            byte[] routedPayload = wrapRoutedPayload(DIRECTION_CLIENT_TO_SERVER, FRAME_METADATA, targetConnectionId, metadata);
            int correlId = correlIdGenerator.incrementAndGet();
            int checksumInt = calculateChecksum(routedPayload);
            MessageHeader header = new MessageHeader(
                MessageHeader.MessageType.ARCHIVO,
                routedPayload.length,
                correlId,
                checksumInt
            );

            ByteArrayOutputStream fullMessage = new ByteArrayOutputStream();
            fullMessage.write(header.toBytes());
            fullMessage.write(routedPayload);

            connectionManager.send(serverConnectionId, fullMessage.toByteArray());
        } catch (IOException e) {
            logger.error("Error enviando metadata de archivo", e);
            throw e;
        }
    }

    private void sendFileChunk(String serverConnectionId, String targetConnectionId, int transferId, int chunkNumber, byte[] chunk,
                              long offset, long totalSize) throws IOException {
        int attempts = 0;
        while (attempts < 5) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (DataOutputStream dos = new DataOutputStream(baos)) {
                    dos.writeInt(transferId);
                    dos.writeInt(chunkNumber);
                    dos.writeLong(offset);
                    dos.writeInt(chunk.length);
                    dos.write(chunk);
                    dos.flush();
                }
                byte[] chunkData = baos.toByteArray();

                byte[] routedPayload = wrapRoutedPayload(DIRECTION_CLIENT_TO_SERVER, FRAME_CHUNK, targetConnectionId, chunkData);
                int correlId = correlIdGenerator.incrementAndGet();
                int checksum = calculateChecksum(routedPayload);
                MessageHeader header = new MessageHeader(
                    MessageHeader.MessageType.ARCHIVO,
                    routedPayload.length,
                    correlId,
                    checksum
                );

                ByteArrayOutputStream fullMessage = new ByteArrayOutputStream();
                fullMessage.write(header.toBytes());
                fullMessage.write(routedPayload);

                connectionManager.send(serverConnectionId, fullMessage.toByteArray());
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

    public void handleIncomingPacket(byte[] data, String source) {
        try {
            byte[] headerBytes = new byte[MessageHeader.HEADER_SIZE];
            System.arraycopy(data, 0, headerBytes, 0, MessageHeader.HEADER_SIZE);
            MessageHeader header = MessageHeader.fromBytes(headerBytes);

            if (header.getTipo() != MessageHeader.MessageType.ARCHIVO) {
                return;
            }

            byte[] payload = new byte[header.getLongitud()];
            System.arraycopy(data, MessageHeader.HEADER_SIZE, payload, 0, payload.length);

            FileRouteFrame frame = parseFrame(payload);
            if (frame == null) {
                logger.warn("No se pudo parsear frame de archivo");
                return;
            }

            if (frame.direction == DIRECTION_CLIENT_TO_SERVER && connectionManager.isServerMode()) {
                forwardFrameToTarget(frame, source);
                return;
            }

            if (frame.direction == DIRECTION_SERVER_TO_CLIENT && !connectionManager.isServerMode()) {
                processIncomingFrame(frame);
            }
        } catch (Exception e) {
            logger.error("Error manejando paquete de archivo", e);
        }
    }

    private void processIncomingFrame(FileRouteFrame frame) throws IOException {
        if (frame.frameType == FRAME_METADATA) {
            handleIncomingMetadata(frame);
        } else if (frame.frameType == FRAME_CHUNK) {
            handleIncomingChunk(frame);
        }
    }

    private void handleIncomingMetadata(FileRouteFrame frame) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(frame.payload))) {
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();
            String checksum = dis.readUTF();
            int transferId = dis.readInt();

            Path downloadDir = getDownloadDirectory();
            Files.createDirectories(downloadDir);

            Path targetPath = downloadDir.resolve(generateSafeFileName(fileName));
            RandomAccessFile raf = new RandomAccessFile(targetPath.toFile(), "rw");
            incomingTransfers.put(transferId, new IncomingTransfer(
                transferId,
                frame.peerId,
                targetPath,
                checksum,
                fileSize,
                null,
                raf
            ));

            logService.logInfo("Recibiendo archivo " + fileName + " desde " + frame.peerId,
                "FileTransferService", traceId, null);
        }
    }

    private void handleIncomingChunk(FileRouteFrame frame) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(frame.payload))) {
            int transferId = dis.readInt();
            dis.readInt(); // chunkNumber (no usamos)
            long offset = dis.readLong();
            int length = dis.readInt();
            byte[] chunk = new byte[length];
            dis.readFully(chunk);

            IncomingTransfer transfer = incomingTransfers.get(transferId);
            if (transfer == null) {
                logger.warn("Chunk recibido para transferencia desconocida: " + transferId);
                return;
            }

            transfer.raf.seek(offset);
            transfer.raf.write(chunk);
            long transferred = transfer.transferred.addAndGet(length);
            double progress = (double) transferred / transfer.fileSize * 100;

            boolean completed = transferred >= transfer.fileSize;
            if (completed) {
                finalizeIncomingTransfer(transfer);
                progress = 100.0;
            }

            eventAggregator.publish(new NetworkEvent(
                NetworkEvent.EventType.FILE_PROGRESS,
                new FileProgress(
                    transferId,
                    transfer.outputPath.getFileName().toString(),
                    progress,
                    transferred,
                    transfer.fileSize,
                    true,
                    transfer.outputPath.toString()
                ),
                transfer.senderId
            ));

            if (completed) {
                incomingTransfers.remove(transfer.transferId);
            }
        }
    }

    private void finalizeIncomingTransfer(IncomingTransfer transfer) throws IOException {
        try {
            transfer.raf.getChannel().force(true);
        } finally {
            transfer.raf.close();
        }

        String calculatedChecksum = calculateSHA256(transfer.outputPath);
        boolean checksumOk = calculatedChecksum.equalsIgnoreCase(transfer.checksum);

        // Guardar la transferencia solo si tenemos un userId (en el cliente receptor no siempre lo hay)
        Long userId = transfer.userId;
        if (userId != null) {
            Transferencia transferencia = new Transferencia(
                Transferencia.TipoTransferencia.ARCHIVO,
                transfer.outputPath.getFileName().toString(),
                transfer.fileSize,
                calculatedChecksum,
                userId,
                transfer.senderId
            );
            transferencia.setEstado(checksumOk ? Transferencia.EstadoTransferencia.COMPLETADA :
                Transferencia.EstadoTransferencia.ERROR);
            transferencia.setFin(LocalDateTime.now());
            transferenciaRepository.save(transferencia);
        } else {
            logger.info("Transferencia recibida sin userId; se omite persistencia");
        }

        if (checksumOk) {
            logService.logInfo("Archivo recibido correctamente desde " + transfer.senderId,
                "FileTransferService", traceId, null);
        } else {
            logService.logWarning("Checksum inválido para archivo desde " + transfer.senderId,
                "FileTransferService", traceId, null);
        }

        incomingTransfers.remove(transfer.transferId);
    }

    private void forwardFrameToTarget(FileRouteFrame frame, String source) {
        try {
            byte[] forwardedPayload = wrapRoutedPayload(
                DIRECTION_SERVER_TO_CLIENT,
                frame.frameType,
                source,
                frame.payload
            );

            int correlId = correlIdGenerator.incrementAndGet();
            MessageHeader header = new MessageHeader(
                MessageHeader.MessageType.ARCHIVO,
                forwardedPayload.length,
                correlId,
                calculateChecksum(forwardedPayload)
            );

            ByteArrayOutputStream fullMessage = new ByteArrayOutputStream();
            fullMessage.write(header.toBytes());
            fullMessage.write(forwardedPayload);

            connectionManager.send(frame.peerId, fullMessage.toByteArray());
            logger.info("Frame de archivo reenviado de " + source + " a " + frame.peerId);
        } catch (IOException e) {
            logger.error("Error reenviando frame de archivo", e);
        }
    }

    private byte[] wrapRoutedPayload(byte direction, byte frameType, String peerId, byte[] payload) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte(direction);
        dos.writeByte(frameType);
        dos.writeUTF(peerId);
        dos.writeInt(payload.length);
        dos.write(payload);
        dos.flush();
        return baos.toByteArray();
    }

    private FileRouteFrame parseFrame(byte[] payload) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload))) {
            byte direction = dis.readByte();
            byte frameType = dis.readByte();
            String peerId = dis.readUTF();
            int length = dis.readInt();
            byte[] data = new byte[length];
            dis.readFully(data);
            return new FileRouteFrame(direction, frameType, peerId, data);
        }
    }

    private Path getDownloadDirectory() {
        return Paths.get(System.getProperty("user.home"), "Downloads", "whatsapp_clone");
    }

    private String generateSafeFileName(String fileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return timestamp + "_" + fileName;
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

    @SuppressWarnings("unused")
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

    private static class IncomingTransfer {
        final int transferId;
        final String senderId;
        final Path outputPath;
        final String checksum;
        final long fileSize;
        final Long userId;
        final RandomAccessFile raf;
        final AtomicLong transferred = new AtomicLong(0);

        IncomingTransfer(int transferId, String senderId, Path outputPath, String checksum, long fileSize, Long userId, RandomAccessFile raf) {
            this.transferId = transferId;
            this.senderId = senderId;
            this.outputPath = outputPath;
            this.checksum = checksum;
            this.fileSize = fileSize;
            this.userId = userId;
            this.raf = raf;
        }
    }

    private static class FileRouteFrame {
        final byte direction;
        final byte frameType;
        final String peerId;
        final byte[] payload;

        FileRouteFrame(byte direction, byte frameType, String peerId, byte[] payload) {
            this.direction = direction;
            this.frameType = frameType;
            this.peerId = peerId;
            this.payload = payload;
        }
    }

    public static class FileProgress {
        private final int transferId;
        private final String fileName;
        private final double progress;
        private final long bytesTransferred;
        private final long totalBytes;
        private final boolean incoming;
        private final String localPath;

        public FileProgress(int transferId, String fileName, double progress, long bytesTransferred,
                            long totalBytes, boolean incoming, String localPath) {
            this.transferId = transferId;
            this.fileName = fileName;
            this.progress = progress;
            this.bytesTransferred = bytesTransferred;
            this.totalBytes = totalBytes;
            this.incoming = incoming;
            this.localPath = localPath;
        }

        public int getTransferId() {
            return transferId;
        }

        public String getFileName() {
            return fileName;
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

        public boolean isIncoming() {
            return incoming;
        }

        public String getLocalPath() {
            return localPath;
        }
    }
}

