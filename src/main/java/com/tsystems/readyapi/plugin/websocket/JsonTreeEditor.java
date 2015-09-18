package com.tsystems.readyapi.plugin.websocket;

import java.awt.event.FocusEvent;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.support.editor.views.xml.outline.support.JsonObjectTree;

public class JsonTreeEditor extends JsonObjectTree {
    private String prevValue = null;
    private boolean isCurValueNull = false;

    public JsonTreeEditor(boolean editable, ModelItem modelItem) {
        super(editable, modelItem);
    }

    public void setText(String text) {
        isCurValueNull = text == null;
        if (isCurValueNull)
            text = "";
        setContent(text);
        detectChange();
    }

    public String getText() {
        String result = getXml();
        return "".equals(result) && isCurValueNull ? null : result;
    }

    @Override
    protected void processFocusEvent(FocusEvent event) {
        super.processFocusEvent(event);
        if (!event.isTemporary())
            detectChange();
    }

    private void detectChange() {
        String newValue = getText();
        if (!Utils.areStringsEqual(prevValue, newValue)) {
            firePropertyChange("text", prevValue, newValue);
            prevValue = newValue;
        }
    }

}
