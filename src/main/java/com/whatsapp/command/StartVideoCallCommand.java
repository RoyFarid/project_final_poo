package com.whatsapp.command;

import com.whatsapp.service.NetworkFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartVideoCallCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(StartVideoCallCommand.class);
    private final NetworkFacade networkFacade;
    private final String serverConnectionId;
    private final String targetConnectionId;

    public StartVideoCallCommand(NetworkFacade networkFacade, String serverConnectionId, String targetConnectionId) {
        this.networkFacade = networkFacade;
        this.serverConnectionId = serverConnectionId;
        this.targetConnectionId = targetConnectionId;
    }

    @Override
    public void execute() {
        try {
            networkFacade.startVideoCall(serverConnectionId, targetConnectionId);
        } catch (Exception e) {
            logger.error("Error ejecutando StartVideoCallCommand", e);
            throw new RuntimeException("Error iniciando videollamada", e);
        }
    }

    @Override
    public void undo() {
        networkFacade.stopVideoCall();
    }
}

