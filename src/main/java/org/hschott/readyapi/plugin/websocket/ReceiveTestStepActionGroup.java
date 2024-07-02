package org.hschott.readyapi.plugin.websocket;

import com.eviware.soapui.impl.wsdl.actions.teststep.WsdlTestStepSoapUIActionGroup;

public class ReceiveTestStepActionGroup extends WsdlTestStepSoapUIActionGroup {
    public ReceiveTestStepActionGroup() {
        super("ReceiveTestStepActions", "Receive Websocket Message TestStep Actions");
    }
}
