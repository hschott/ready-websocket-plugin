package org.hschott.readyapi.plugin.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public abstract class Message<T> {

    public abstract T getPayload();

    public abstract long size();

    public static class BinaryMessage extends Message<ByteBuffer> {
        public ByteBuffer buffer;

        public BinaryMessage(byte[] payload) {
            buffer = payload != null ? ByteBuffer.wrap(payload) : null;
        }

        public BinaryMessage(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        public BinaryMessage(byte[] payload, int offset, int length) {
            buffer = payload != null ? ByteBuffer.wrap(payload, offset, length) : null;
        }

        @Override
        public ByteBuffer getPayload() {
            return buffer;
        }

        @Override
        public long size() {
            return buffer.capacity();
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

        @Override
        public long size() {
            return payload.getBytes(Charset.forName("UTF-8")).length;
        }
    }
}