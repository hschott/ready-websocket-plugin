package com.tsystems.readyapi.plugin.websocket;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

public class XmlObjectBuilder {

    private XmlObject config;
    private XmlCursor cursor;

    public XmlObjectBuilder(XmlObject config) {
        this.config = config;
        cursor = config.newCursor();
        cursor.toNextToken();
    }

    public XmlObjectBuilder() {
        this(XmlObject.Factory.newInstance());
        cursor = config.newCursor();
        cursor.toNextToken();
    }

    public XmlObjectBuilder add(String name, String value) {
        cursor.insertElementWithText(name, value);
        return this;
    }

    public XmlObjectBuilder add(String name, int value) {
        cursor.insertElementWithText(name, String.valueOf(value));
        return this;
    }

    public XmlObjectBuilder add(String name, long value) {
        cursor.insertElementWithText(name, String.valueOf(value));
        return this;
    }

    public XmlObjectBuilder add(String name, float value) {
        cursor.insertElementWithText(name, String.valueOf(value));
        return this;
    }

    public XmlObjectBuilder addSection(String sectionName, XmlObject section) {
        cursor.beginElement(sectionName);
        cursor.push();
        XmlCursor srcCursor = section.newCursor();
        srcCursor.toNextToken();
        while (srcCursor.currentTokenType() != XmlCursor.TokenType.NONE
                && srcCursor.currentTokenType() != XmlCursor.TokenType.ENDDOC) {
            srcCursor.copyXml(cursor);
            if (srcCursor.currentTokenType() == XmlCursor.TokenType.START)
                srcCursor.toEndToken();
            srcCursor.toNextToken();

        }
        cursor.pop();
        cursor.toEndToken();
        cursor.toNextToken();
        srcCursor.dispose();

        return this;
    }

    public XmlObject finish() {
        cursor.dispose();
        return config;
    }

    public XmlObjectBuilder add(String name, boolean value) {
        cursor.insertElementWithText(name, String.valueOf(value));
        return this;
    }

    public void add(String name, String[] values) {
        for (String value : values) {
            add(name, value);
        }
    }

}
