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
        REGISTER_RESULT,
        ROOM_CREATED,
        ROOM_APPROVED,
        ROOM_REJECTED,
        ROOM_CLOSED,
        ROOM_MEMBER_ADDED,
        ROOM_MEMBER_REMOVED,
        ROOM_MESSAGE,
        ROOM_LIST
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

