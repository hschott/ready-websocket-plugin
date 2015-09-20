package com.tsystems.readyapi.plugin.websocket;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.CloseException;
import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

@WebSocket
public class Client {
    private final static Logger LOGGER = Logger.getLogger(PluginConfig.LOGGER_NAME);

    private AtomicReference<Session> session = new AtomicReference<Session>();
    private AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
    private WebSocketClient webSocketClient;
    private ClientUpgradeRequest upgradeRequest;
    private MessageQueue messageQueue = new MessageQueue();
    private AtomicReference<Future<?>> future = new AtomicReference<Future<?>>();

    public Client(WebSocketClient webSocketClient, ClientUpgradeRequest upgradeRequest) {
        this.webSocketClient = webSocketClient;
        this.upgradeRequest = upgradeRequest;
    }

    public void cancel() {
        Future<?> future;
        if ((future = this.future.get()) != null)
            future.cancel(true);
    }

    public void connect() {
        if (isConnected())
            return;
        try {
            throwable.set(null);
            future.set(webSocketClient.connect(this, upgradeRequest.getRequestURI(), upgradeRequest));
        } catch (Exception e) {
            Throwable th = ExceptionUtils.getRootCause(e);
            throwable.set(th != null ? th : e);
            LOGGER.error(th != null ? th : e);
        }
    }

    public void disconnect(boolean harshDisconnect) throws Exception {
        Session session;
        if ((session = this.session.get()) != null)
            if (!harshDisconnect)
                session.close(new CloseStatus(StatusCode.NORMAL, "drop connection test step"));
            else {
                session.disconnect();
                this.session.set(null);
            }
    }

    public void dispose() {
        try {
            Session session;
            if ((session = this.session.get()) != null)
                session.disconnect();
            webSocketClient.stop();
            this.session.set(null);
            throwable.set(null);
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public MessageQueue getMessageQueue() {
        return messageQueue;
    }

    public Throwable getThrowable() {
        return throwable.get();
    }

    public boolean isAvailable() {
        Future<?> future;
        if ((future = this.future.get()) != null)
            return future.isDone();
        else
            return true;
    }

    public boolean isConnected() {
        Session session;
        if ((session = this.session.get()) != null)
            return session.isOpen();
        else
            return false;
    }

    public boolean isFaulty() {
        return throwable.get() != null;
    }

    @OnWebSocketMessage
    public void onWebSocketBinary(byte[] payload, int offset, int length) {
        Message.BinaryMessage message = new Message.BinaryMessage(payload, offset, length);
        messageQueue.addMessage(message);
    }

    @OnWebSocketClose
    public void onWebSocketClose(int statusCode, String reason) {
        LOGGER.info("WebSocketClose statusCode=" + statusCode + " reason=" + reason);
        messageQueue = new MessageQueue();
        if (statusCode > StatusCode.NORMAL)
            throwable.set(new CloseException(statusCode, reason));
        session.set(null);
    }

    @OnWebSocketConnect
    public void onWebSocketConnect(Session session) {
        LOGGER.info("WebSocketConnect success="
                + (session.getUpgradeResponse() != null ? session.getUpgradeResponse().isSuccess() : false)
                + " accepted protocol="
                + (session.getUpgradeResponse() != null ? session.getUpgradeResponse().getAcceptedSubProtocol() : ""));
        this.session.set(session);
    }

    @OnWebSocketError
    public void onWebSocketError(Session session, Throwable cause) {
        LOGGER.error("WebSocketError", cause);
        throwable.set(cause);
    }

    @OnWebSocketFrame
    public void onWebSocketFrame(Frame frame) {
        LOGGER.debug("WebSocketFrame type=" + frame.getType() + " payload=" + frame.getPayload());
    }

    @OnWebSocketMessage
    public void onWebSocketText(String payload) {
        Message.TextMessage message = new Message.TextMessage(payload);
        messageQueue.addMessage(message);
    }

    public void sendMessage(Message<?> message) {
        if (!isConnected())
            return;
        Session session;
        if ((session = this.session.get()) != null) {
            throwable.set(null);
            if (message instanceof Message.TextMessage) {
                Message.TextMessage text = (Message.TextMessage) message;
                future.set(session.getRemote().sendStringByFuture(text.getPayload()));
            }
            if (message instanceof Message.BinaryMessage) {
                Message.BinaryMessage binary = (Message.BinaryMessage) message;
                future.set(session.getRemote().sendBytesByFuture(binary.getPayload()));
            }
        }
    }
}
