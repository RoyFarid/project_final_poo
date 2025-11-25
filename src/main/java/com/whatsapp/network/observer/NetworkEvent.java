package com.whatsapp.network.observer;

public class NetworkEvent {
    public enum EventType {
        CONNECTED,
        DISCONNECTED,
        MESSAGE_RECEIVED,
        FILE_PROGRESS,
        VIDEO_FRAME,
        ERROR,
        AUTH_RESULT,
        REGISTER_RESULT
    }

    private final EventType type;
    private final Object data;
    private final String source;

    public NetworkEvent(EventType type, Object data, String source) {
        this.type = type;
        this.data = data;
        this.source = source;
    }

    public EventType getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public String getSource() {
        return source;
    }
}

