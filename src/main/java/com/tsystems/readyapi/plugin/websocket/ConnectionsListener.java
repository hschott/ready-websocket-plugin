package com.tsystems.readyapi.plugin.websocket;

public interface ConnectionsListener {
    public void connectionListChanged();

    public void connectionChanged(Connection connection, String propertyName, Object oldPropertyValue,
            Object newPropertyValue);
}
