package com.whatsapp.command;

import com.whatsapp.service.NetworkFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SendFileCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(SendFileCommand.class);
    private final NetworkFacade networkFacade;
    private final String serverConnectionId;
    private final String targetConnectionId;
    private final String filePath;
    private final Long userId;

    public SendFileCommand(NetworkFacade networkFacade, String serverConnectionId,
                           String targetConnectionId, String filePath, Long userId) {
        this.networkFacade = networkFacade;
        this.serverConnectionId = serverConnectionId;
        this.targetConnectionId = targetConnectionId;
        this.filePath = filePath;
        this.userId = userId;
    }

    @Override
    public void execute() {
        try {
            networkFacade.sendFile(serverConnectionId, targetConnectionId, filePath, userId);
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

