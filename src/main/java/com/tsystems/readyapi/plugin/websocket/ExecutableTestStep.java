package com.tsystems.readyapi.plugin.websocket;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.iface.SubmitContext;

public interface ExecutableTestStep extends ModelItem {
    public void addExecutionListener(ExecutionListener listener);

    public ExecutableTestStepResult execute(SubmitContext context, CancellationToken cancellationToken);

    public void removeExecutionListener(ExecutionListener listener);
}
