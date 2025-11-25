package com.whatsapp.ui;

import com.whatsapp.model.Usuario;
import com.whatsapp.service.NetworkFacade;

public class LoginResult {
    public enum Mode {
        SERVER,
        CLIENT
    }

    private final Mode mode;
    private final Usuario usuario;
    private final NetworkFacade networkFacade;
    private final String serverHost;
    private final int serverPort;

    public LoginResult(Mode mode, Usuario usuario, NetworkFacade networkFacade, String serverHost, int serverPort) {
        this.mode = mode;
        this.usuario = usuario;
        this.networkFacade = networkFacade;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public Mode getMode() {
        return mode;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public NetworkFacade getNetworkFacade() {
        return networkFacade;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }
}


