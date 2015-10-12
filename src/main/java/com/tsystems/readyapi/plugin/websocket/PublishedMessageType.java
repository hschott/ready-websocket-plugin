package com.tsystems.readyapi.plugin.websocket;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.eviware.soapui.model.project.Project;

enum PublishedMessageType {
    Json("JSON"), Xml("XML"), Text("Text"), BinaryFile("Content of file"), IntegerValue("Integer (4 bytes)"), LongValue(
            "Long (8 bytes)"), FloatValue("Float"), DoubleValue("Double");
    private String name;

    private PublishedMessageType(String name) {
        this.name = name;
    }

    public static PublishedMessageType fromString(String s) {
        if (s == null)
            return null;
        for (PublishedMessageType m : PublishedMessageType.values())
            if (m.toString().equals(s))
                return m;
        return null;

    }

    public Message<?> toMessage(String payload, Project project) {
        byte[] buf;
        switch (this) {
        case Text:
        case Json:
        case Xml:
            return new Message.TextMessage(payload);
        case IntegerValue:
            int iv;
            try {
                iv = Integer.parseInt(payload);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format(
                        "The specified text (\"%s\") cannot represent an integer value.", payload));
            }
            buf = new byte[4];
            for (int i = 0; i < 4; ++i)
                buf[i] = (byte) (iv >> i * 8 & 0xff);
            return new Message.BinaryMessage(buf);

        case LongValue:
            long lv;
            try {
                lv = Long.parseLong(payload);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format(
                        "The specified text (\"%s\") cannot represent a long value.", payload));
            }
            buf = new byte[8];
            for (int i = 0; i < 8; ++i)
                buf[i] = (byte) (lv >> i * 8 & 0xff);
            return new Message.BinaryMessage(buf);

        case DoubleValue:
            buf = new byte[8];
            double dv;
            try {
                dv = Double.parseDouble(payload);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format(
                        "The specified text (\"%s\") cannot represent a double value.", payload));
            }
            long rawD = Double.doubleToLongBits(dv);
            for (int i = 0; i < 8; ++i)
                buf[i] = (byte) (rawD >> (7 - i) * 8 & 0xff);
            return new Message.BinaryMessage(buf);

        case FloatValue:
            buf = new byte[4];
            float fv;
            try {
                fv = Float.parseFloat(payload);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format(
                        "The specified text (\"%s\") cannot represent a float value.", payload));
            }
            int rawF = Float.floatToIntBits(fv);
            for (int i = 0; i < 4; ++i)
                buf[i] = (byte) (rawF >> (3 - i) * 8 & 0xff);
            return new Message.BinaryMessage(buf);

        case BinaryFile:
            File file = new File(payload);
            if (!file.isAbsolute())
                file = new File(new File(project.getPath()).getParent(), file.getPath());
            if (!file.exists())
                throw new IllegalArgumentException(String.format("Unable to find \"%s\" file which contains a message",
                        file.getPath()));
            try {
                return new Message.BinaryMessage(FileUtils.readFileToByteArray(file));

            } catch (IOException e) {
                throw new RuntimeException(String.format(
                        "Attempt of access to \"%s\" file with a published message has failed.", file.getPath()), e);
            }

        }
        throw new IllegalArgumentException("The format of the published message is not specified or unknown."); // We
        // won't
        // be
        // here
    }

    @Override
    public String toString() {
        return name;
    }

}
