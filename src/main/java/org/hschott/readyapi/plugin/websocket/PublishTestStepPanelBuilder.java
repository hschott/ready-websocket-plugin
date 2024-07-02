package org.hschott.readyapi.plugin.websocket;

import com.eviware.soapui.impl.EmptyPanelBuilder;
import com.eviware.soapui.plugins.auto.PluginPanelBuilder;
import com.eviware.soapui.ui.desktop.DesktopPanel;

@PluginPanelBuilder(targetModelItem = PublishTestStep.class)
public class PublishTestStepPanelBuilder extends EmptyPanelBuilder<PublishTestStep> {

    @Override
    public DesktopPanel buildDesktopPanel(PublishTestStep testStep) {
        return new PublishTestStepPanel(testStep);
    }

    @Override
    public boolean hasDesktopPanel() {
        return true;
    }

}
