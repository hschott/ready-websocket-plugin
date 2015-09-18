package com.tsystems.readyapi.plugin.websocket;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.eviware.soapui.support.UISupport;

public class StopTestStepAction extends AbstractAction {
    private RunTestStepAction runAction;

    public StopTestStepAction(RunTestStepAction correspondingRunAction) {
        super();
        this.runAction = correspondingRunAction;
        putValue(Action.SMALL_ICON, UISupport.createImageIcon("com/eviware/soapui/resources/images/stop.png"));
        putValue(Action.SHORT_DESCRIPTION, "Aborts ongoing test step execution");
        putValue(Action.ACCELERATOR_KEY, UISupport.getKeyStroke("alt X"));
    }

    public void actionPerformed(ActionEvent e) {
        runAction.cancel();
    }
}
