package com.tsystems.readyapi.plugin.websocket;

import java.net.URI;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.codec.binary.Base64;
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
public class JettyClient implements Client {
    private final static Logger LOGGER = Logger.getLogger(PluginConfig.LOGGER_NAME);

    private AtomicReference<Session> session = new AtomicReference<Session>();
    private AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
    private WebSocketClient webSocketClient;
    private ClientUpgradeRequest upgradeRequest;
    private Queue<Message<?>> messageQueue = new LinkedBlockingQueue<Message<?>>();
    private AtomicReference<Future<?>> future = new AtomicReference<Future<?>>();

    public JettyClient(ExpandedConnectionParams connectionParams) throws Exception {
        webSocketClient = new WebSocketClient();
        webSocketClient.start();
        upgradeRequest = createClientUpgradeRequest(connectionParams);
    }

    private ClientUpgradeRequest getDefaultClientUpgradeRequest() {
        ClientUpgradeRequest upgradeRequest = new ClientUpgradeRequest();
        return upgradeRequest;
    }

    private ClientUpgradeRequest createClientUpgradeRequest(ExpandedConnectionParams connectionParams) throws Exception {
        ClientUpgradeRequest upgradeRequest;
        if (connectionParams == null)
            upgradeRequest = getDefaultClientUpgradeRequest();
        else {
            upgradeRequest = new ClientUpgradeRequest();
            if (connectionParams.hasCredentials()) {
                String basicAuthHeader = new String(
                        Base64.encodeBase64((connectionParams.login + ":" + connectionParams.password).getBytes()));
                upgradeRequest.setHeader("Authorization", "Basic " + basicAuthHeader);
            }
            upgradeRequest.setRequestURI(new URI(connectionParams.getNormalizedServerUri()));
            if (connectionParams.hasSubprotocols())
                upgradeRequest.setSubProtocols(connectionParams.subprotocols.split(","));
        }
        return upgradeRequest;
    }

    /**
     * Explain why this is overridden.
     * 
     * @see com.tsystems.readyapi.plugin.websocket.Client#cancel()
     */
    @Override
    public void cancel() {
        Future<?> future;
        if ((future = this.future.get()) != null)
            future.cancel(true);
    }

    /**
     * Explain why this is overridden.
     * 
     * @see com.tsystems.readyapi.plugin.websocket.Client#connect()
     */
    @Override
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

    /**
     * Explain why this is overridden.
     * 
     * @see com.tsystems.readyapi.plugin.websocket.Client#disconnect(boolean)
     */
    @Override
    public void disconnect(boolean harshDisconnect) throws Exception {
        Session session;
        if ((session = this.session.get()) != null)
            if (!harshDisconnect)
                session.close(new CloseStatus(StatusCode.NORMAL, "drop connection test step"));
            else {
                session.close(new CloseStatus(StatusCode.PROTOCOL, "drop connection test step"));
                this.session.set(null);
            }
    }

    /**
     * Explain why this is overridden.
     * 
     * @see com.tsystems.readyapi.plugin.websocket.Client#dispose()
     */
    @Override
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

    /**
     * Explain why this is overridden.
     * 
     * @see com.tsystems.readyapi.plugin.websocket.Client#getMessageQueue()
     */
    @Override
    public Queue<Message<?>> getMessageQueue() {
        return messageQueue;
    }

    /**
     * Explain why this is overridden.
     * 
     * @see com.tsystems.readyapi.plugin.websocket.Client#getThrowable()
     */
    @Override
    public Throwable getThrowable() {
        return throwable.get();
    }

    /**
     * Explain why this is overridden.
     * 
     * @see com.tsystems.readyapi.plugin.websocket.Client#isAvailable()
     */
    @Override
    public boolean isAvailable() {
        Future<?> future;
        if ((future = this.future.get()) != null)
            if (future.isDone())
                try {
                    future.get();
                    return true;
                } catch (Exception e) {
                    throwable.set(e);
                    return false;
                }
            else
                return false;
        else
            return true;
    }

    /**
     * Explain why this is overridden.
     * 
     * @see com.tsystems.readyapi.plugin.websocket.Client#isConnected()
     */
    @Override
    public boolean isConnected() {
        Session session;
        if ((session = this.session.get()) != null)
            return session.isOpen();
        else
            return false;
    }

    /**
     * Explain why this is overridden.
     * 
     * @see com.tsystems.readyapi.plugin.websocket.Client#isFaulty()
     */
    @Override
    public boolean isFaulty() {
        return throwable.get() != null;
    }

    @OnWebSocketMessage
    public void onWebSocketBinary(byte[] payload, int offset, int length) {
        Message.BinaryMessage message = new Message.BinaryMessage(payload, offset, length);
        messageQueue.offer(message);
    }

    @OnWebSocketClose
    public void onWebSocketClose(int statusCode, String reason) {
        LOGGER.info("WebSocketClose statusCode=" + statusCode + " reason=" + reason);
        messageQueue.clear();
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
        messageQueue.offer(message);
    }

    /**
     * Explain why this is overridden.
     * 
     * @see com.tsystems.readyapi.plugin.websocket.Client#sendMessage(com.tsystems.readyapi.plugin.websocket.Message)
     */
    @Override
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
