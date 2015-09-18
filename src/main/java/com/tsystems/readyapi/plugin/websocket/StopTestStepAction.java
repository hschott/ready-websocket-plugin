package com.tsystems.readyapi.plugin.websocket;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.eviware.soapui.support.UISupport;

public class StopTestStepAction extends AbstractAction {
    /** serialVersionUID description. */
    private static final long serialVersionUID = -2956917601726341516L;
    private RunTestStepAction runAction;

    public StopTestStepAction(RunTestStepAction correspondingRunAction) {
        super();
        runAction = correspondingRunAction;
        putValue(Action.SMALL_ICON, UISupport.createImageIcon("com/eviware/soapui/resources/images/stop.png"));
        putValue(Action.SHORT_DESCRIPTION, "Aborts ongoing test step execution");
        putValue(Action.ACCELERATOR_KEY, UISupport.getKeyStroke("alt X"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        runAction.cancel();
    }
}
