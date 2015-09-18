package com.tsystems.readyapi.plugin.websocket;

public class MessageQueue {

    private static class Node {
        public Message<?> data;
        public Node next = null;
    }

    private Node first = null, last = null, cur = null, prev = null;

    public MessageQueue() {
    }

    public void addMessage(Message<?> item) {
        synchronized (this) {
            if (first == null) {
                first = new Node();
                first.data = item;
                first.next = null;
                last = first;
            } else {
                last.next = new Node();
                last = last.next;
                last.data = item;
            }
        }
    }

    public void setCurrentMessageToHead() {
        synchronized (this) {
            cur = null;
            prev = null;
        }
    }

    public Message<?> getMessage() {
        synchronized (this) {
            if (cur == null) {
                cur = first;
                prev = null;
            } else {
                if (cur.next == null)
                    return null;
                prev = cur;
                cur = cur.next;
            }
            if (cur == null)
                return null;
            else
                return cur.data;
        }
    }

    public void removeCurrentMessage() {
        synchronized (this) {
            if (cur == null)
                throw new IllegalStateException("There is no current message in the message queue.");
            if (cur == first) {
                if (last == first)
                    last = null;
                cur = cur.next;
                first.next = null;
                prev = null;
                first = cur;
            } else {
                if (last == cur)
                    last = prev;
                prev.next = cur.next;
                cur.next = null;
                cur = prev.next;
            }
        }
    }
}
