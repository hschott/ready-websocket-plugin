package com.tsystems.readyapi.plugin.websocket;

public interface ConnectionsListener {
    public void connectionChanged(Connection connection, String propertyName, Object oldPropertyValue,
            Object newPropertyValue);

    public void connectionListChanged();
}
