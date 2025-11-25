package com.whatsapp.command;

import com.whatsapp.service.NetworkFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SendMessageCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(SendMessageCommand.class);
    private final NetworkFacade networkFacade;
    private final String connectionId;
    private final String message;
    private final Long userId;
    private final String peerIp;
    private String lastSentMessage;

    public SendMessageCommand(NetworkFacade networkFacade, String connectionId, 
                             String message, Long userId, String peerIp) {
        this.networkFacade = networkFacade;
        this.connectionId = connectionId;
        this.message = message;
        this.userId = userId;
        this.peerIp = peerIp;
    }

    @Override
    public void execute() {
        try {
            networkFacade.sendMessage(connectionId, message, userId, peerIp);
            lastSentMessage = message;
        } catch (IOException e) {
            logger.error("Error ejecutando SendMessageCommand", e);
            throw new RuntimeException("Error enviando mensaje", e);
        }
    }

    @Override
    public void undo() {
        // En un chat real, no se puede "desenviar" un mensaje
        // Pero podríamos implementar eliminación si el protocolo lo soporta
        logger.info("Undo no implementado para SendMessageCommand");
    }
}

