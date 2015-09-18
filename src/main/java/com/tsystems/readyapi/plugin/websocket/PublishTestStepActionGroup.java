package com.tsystems.readyapi.plugin.websocket;

import com.eviware.soapui.impl.wsdl.actions.teststep.WsdlTestStepSoapUIActionGroup;

// @ActionGroup(defaultTargetType = PublishTestStep.class)
public class PublishTestStepActionGroup extends WsdlTestStepSoapUIActionGroup {
    public PublishTestStepActionGroup() {
        super("PublishTestStepActions", "Publish Using Websocket TestStep Actions");
    }
}
