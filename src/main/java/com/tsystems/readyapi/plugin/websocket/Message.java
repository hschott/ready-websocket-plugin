package com.tsystems.readyapi.plugin.websocket;

import java.nio.ByteBuffer;

public abstract class Message<T> {

    public abstract T getPayload();

    public static class BinaryMessage extends Message<ByteBuffer> {
        public ByteBuffer buffer;

        public BinaryMessage(byte[] payload) {
            buffer = payload != null ? ByteBuffer.wrap(payload) : null;
        }

        public BinaryMessage(byte[] payload, int offset, int length) {
            buffer = payload != null ? ByteBuffer.wrap(payload, offset, length) : null;
        }

        @Override
        public ByteBuffer getPayload() {
            return buffer;
        }
    }

    public static class TextMessage extends Message<String> {
        private String payload;

        public TextMessage(String payload) {
            this.payload = payload;
        }

        @Override
        public String getPayload() {
            return payload;
        }
    }
}