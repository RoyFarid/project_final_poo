package com.whatsapp.network.observer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventAggregator {
    private static EventAggregator instance;
    private final List<NetworkEventObserver> observers;

    private EventAggregator() {
        this.observers = new CopyOnWriteArrayList<>();
    }

    public static synchronized EventAggregator getInstance() {
        if (instance == null) {
            instance = new EventAggregator();
        }
        return instance;
    }

    public void subscribe(NetworkEventObserver observer) {
        observers.add(observer);
    }

    public void unsubscribe(NetworkEventObserver observer) {
        observers.remove(observer);
    }

    public void publish(NetworkEvent event) {
        for (NetworkEventObserver observer : observers) {
            try {
                observer.onNetworkEvent(event);
            } catch (Exception e) {
                System.err.println("Error notificando observer: " + e.getMessage());
            }
        }
    }
}

