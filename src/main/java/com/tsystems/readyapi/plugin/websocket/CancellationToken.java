package com.tsystems.readyapi.plugin.websocket;

public interface CancellationToken {
    String cancellationReason();

    boolean cancelled();
}
