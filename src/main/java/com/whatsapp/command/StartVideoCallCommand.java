package com.whatsapp.command;

import com.whatsapp.service.NetworkFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StartVideoCallCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(StartVideoCallCommand.class);
    private final NetworkFacade networkFacade;
    private final String targetHost;
    private final int targetPort;

    public StartVideoCallCommand(NetworkFacade networkFacade, String targetHost, int targetPort) {
        this.networkFacade = networkFacade;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
    }

    @Override
    public void execute() {
        try {
            networkFacade.startVideoCall(targetHost, targetPort);
        } catch (IOException e) {
            logger.error("Error ejecutando StartVideoCallCommand", e);
            throw new RuntimeException("Error iniciando videollamada", e);
        }
    }

    @Override
    public void undo() {
        networkFacade.stopVideoCall();
    }
}

