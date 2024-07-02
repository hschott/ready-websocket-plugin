package org.hschott.readyapi.plugin.websocket;

import com.eviware.soapui.model.testsuite.LoadTestRunContext;
import com.eviware.soapui.model.testsuite.LoadTestRunListener;
import com.eviware.soapui.model.testsuite.LoadTestRunner;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestRunListener;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.plugins.ListenerConfiguration;

@ListenerConfiguration
public class ClientCacheCleanUpListener implements LoadTestRunListener, TestRunListener {

    @Override
    public void afterRun(TestCaseRunner testRunner, TestCaseRunContext runContext) {
        ClientCache.assureFinalized(runContext);
    }

    @Override
    public void beforeLoadTest(LoadTestRunner loadTestRunner, LoadTestRunContext context) {
        //
    }

    @Override
    public void loadTestStarted(LoadTestRunner loadTestRunner, LoadTestRunContext context) {
        //
    }

    @Override
    public void beforeTestCase(LoadTestRunner loadTestRunner, LoadTestRunContext context, TestCaseRunner testRunner,
            TestCaseRunContext runContext) {
        //
    }

    @Override
    public void beforeTestStep(LoadTestRunner loadTestRunner, LoadTestRunContext context, TestCaseRunner testRunner,
            TestCaseRunContext runContext, TestStep testStep) {
        //
    }

    @Override
    public void afterTestStep(LoadTestRunner loadTestRunner, LoadTestRunContext context, TestCaseRunner testRunner,
            TestCaseRunContext runContext, TestStepResult testStepResult) {
        //
    }

    @Override
    public void afterTestCase(LoadTestRunner loadTestRunner, LoadTestRunContext context, TestCaseRunner testRunner,
            TestCaseRunContext runContext) {
        //
    }

    @Override
    public void loadTestStopped(LoadTestRunner loadTestRunner, LoadTestRunContext context) {
        //
    }

    @Override
    public void afterLoadTest(LoadTestRunner loadTestRunner, LoadTestRunContext context) {
        ClientCache.assureFinalized(context);
    }

    @Override
    public void beforeRun(TestCaseRunner testRunner, TestCaseRunContext runContext) {
        //
    }

    @Override
    public void beforeStep(TestCaseRunner testRunner, TestCaseRunContext runContext) {
        //
    }

    @Override
    public void beforeStep(TestCaseRunner testRunner, TestCaseRunContext runContext, TestStep testStep) {
        //
    }

    @Override
    public void afterStep(TestCaseRunner testRunner, TestCaseRunContext runContext, TestStepResult result) {
        //
    }
}