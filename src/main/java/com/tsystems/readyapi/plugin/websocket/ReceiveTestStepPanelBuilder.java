package com.tsystems.readyapi.plugin.websocket;

import com.eviware.soapui.impl.EmptyPanelBuilder;
import com.eviware.soapui.plugins.auto.PluginPanelBuilder;
import com.eviware.soapui.ui.desktop.DesktopPanel;

@PluginPanelBuilder(targetModelItem = ReceiveTestStep.class)
public class ReceiveTestStepPanelBuilder extends EmptyPanelBuilder<ReceiveTestStep> {

    @Override
    public DesktopPanel buildDesktopPanel(ReceiveTestStep testStep) {
        return new ReceiveTestStepPanel(testStep);
    }

    @Override
    public boolean hasDesktopPanel() {
        return true;
    }
}
