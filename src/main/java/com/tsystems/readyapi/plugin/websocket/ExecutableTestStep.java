package com.tsystems.readyapi.plugin.websocket;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;

public interface ExecutableTestStep extends ModelItem {
    public void addExecutionListener(ExecutionListener listener);

    public ExecutableTestStepResult execute(PropertyExpansionContext context, CancellationToken cancellationToken);

    public void removeExecutionListener(ExecutionListener listener);
}
