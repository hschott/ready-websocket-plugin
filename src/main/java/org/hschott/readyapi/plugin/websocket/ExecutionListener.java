package org.hschott.readyapi.plugin.websocket;

public interface ExecutionListener {
    void afterExecution(ExecutableTestStep testStep, ExecutableTestStepResult executionResult);
}
