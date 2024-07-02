package org.hschott.readyapi.plugin.websocket;

import com.eviware.soapui.impl.EmptyPanelBuilder;
import com.eviware.soapui.plugins.auto.PluginPanelBuilder;
import com.eviware.soapui.ui.desktop.DesktopPanel;

@PluginPanelBuilder(targetModelItem = RiseConnectionTestStep.class)
public class RiseConnectionTestStepPanelBuilder extends EmptyPanelBuilder<RiseConnectionTestStep> {
    @Override
    public DesktopPanel buildDesktopPanel(RiseConnectionTestStep testStep) {
        return new RiseConnectionTestStepPanel(testStep);
    }

    @Override
    public boolean hasDesktopPanel() {
        return true;
    }

}
