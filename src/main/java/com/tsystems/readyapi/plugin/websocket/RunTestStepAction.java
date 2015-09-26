package com.tsystems.readyapi.plugin.websocket;

import java.awt.event.ActionEvent;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.WsdlSubmitContext;
import com.eviware.soapui.support.UISupport;

public class RunTestStepAction extends AbstractAction implements Runnable, CancellationToken {
    /** serialVersionUID description. */
    private static final long serialVersionUID = -293379556639527450L;
    private ExecutableTestStep testStep;
    private Future future;
    private StopTestStepAction stopAction;
    private boolean isExecutionInProgress = false;
    private boolean isCancelled = false;

    public RunTestStepAction(ExecutableTestStep testStep) {
        this.testStep = testStep;
        putValue(Action.SMALL_ICON, UISupport.createImageIcon("com/eviware/soapui/resources/images/submit_request.png"));
        if (UISupport.isMac())
            putValue(Action.SHORT_DESCRIPTION, "Run test step (Alt-Ctrl-Enter)");
        else
            putValue(Action.SHORT_DESCRIPTION, "Run test step (Alt-Enter)");
        putValue(Action.ACCELERATOR_KEY, UISupport.getKeyStroke("alt ENTER"));
        stopAction = new StopTestStepAction(this);
        stopAction.setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isExecutionInProgress)
            UISupport.showErrorMessage("Test step is still running. Unable to start it again.");
        isExecutionInProgress = true;
        isCancelled = false;
        setEnabled(false);
        stopAction.setEnabled(true);
        future = SoapUI.getThreadPool().submit(this);
    }

    public void cancel() {
        if (!isExecutionInProgress)
            return;
        isCancelled = true;
        if (future.cancel(false))
            onFinish();
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    public StopTestStepAction getCorrespondingStopAction() {
        return stopAction;
    }

    private void onFinish() {
        isExecutionInProgress = false;
        future = null;
        setEnabled(true);
        stopAction.setEnabled(false);
    }

    @Override
    public void run() {
        try {
            testStep.execute(new WsdlSubmitContext(testStep), this);
        } finally {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    onFinish();
                }
            });
        }
    }
}