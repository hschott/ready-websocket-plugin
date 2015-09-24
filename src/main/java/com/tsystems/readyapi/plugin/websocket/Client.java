package com.tsystems.readyapi.plugin.websocket;

import java.util.Queue;

public interface Client {

    public void cancel();

    public void connect();

    public void disconnect(boolean harshDisconnect) throws Exception;

    public void dispose();

    public Queue<Message<?>> getMessageQueue();

    public Throwable getThrowable();

    public boolean isAvailable();

    public boolean isConnected();

    public boolean isFaulty();

    public void sendMessage(Message<?> message);

}