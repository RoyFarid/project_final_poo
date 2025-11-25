package com.whatsapp.command;

import com.whatsapp.service.NetworkFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SendFileCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(SendFileCommand.class);
    private final NetworkFacade networkFacade;
    private final String connectionId;
    private final String filePath;
    private final Long userId;
    private final String peerIp;

    public SendFileCommand(NetworkFacade networkFacade, String connectionId, 
                          String filePath, Long userId, String peerIp) {
        this.networkFacade = networkFacade;
        this.connectionId = connectionId;
        this.filePath = filePath;
        this.userId = userId;
        this.peerIp = peerIp;
    }

    @Override
    public void execute() {
        try {
            networkFacade.sendFile(connectionId, filePath, userId, peerIp);
        } catch (IOException e) {
            logger.error("Error ejecutando SendFileCommand", e);
            throw new RuntimeException("Error enviando archivo", e);
        }
    }

    @Override
    public void undo() {
        // No se puede deshacer el envío de un archivo
        logger.info("Undo no implementado para SendFileCommand");
    }
}

