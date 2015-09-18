package com.tsystems.readyapi.plugin.websocket;

public interface ExecutionListener {
    void afterExecution(ExecutableTestStep testStep, ExecutableTestStepResult executionResult);
}
