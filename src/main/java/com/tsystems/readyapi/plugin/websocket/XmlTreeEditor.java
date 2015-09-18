package com.tsystems.readyapi.plugin.websocket;

import java.awt.event.FocusEvent;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.support.editor.views.xml.outline.support.XmlObjectTree;

public class XmlTreeEditor extends XmlObjectTree {
    private String prevValue = null;

    public XmlTreeEditor(boolean editable, ModelItem modelItem) {
        super(editable, modelItem);
    }

    public void setText(String text) {
        setContent(text);
        detectChange(text);
    }

    public String getText() {
        return getXml();
    }

    @Override
    protected void processFocusEvent(FocusEvent event) {
        super.processFocusEvent(event);
        if (!event.isTemporary())
            detectChange(getText());
    }

    private void detectChange(String newValue) {
        if (!Utils.areStringsEqual(prevValue, newValue)) {
            firePropertyChange("text", prevValue, newValue);
            prevValue = newValue;
        }
    }

}
