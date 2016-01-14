package com.tsystems.readyapi.plugin.websocket;

import javax.swing.ImageIcon;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.support.IconAnimator;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

@PluginTestStep(typeName = "WebsocketOpenConnectionTestStep", name = "Open Websocket Connection",
        description = "Connects to the websocket server",
        iconPath = "com/tsystems/readyapi/plugin/websocket/rise_step.png")
public class RiseConnectionTestStep extends ConnectedTestStep {

    private static boolean actionGroupAdded = false;
    private ImageIcon disabledStepIcon;
    private ImageIcon unknownStepIcon;

    private IconAnimator<RiseConnectionTestStep> iconAnimator;

    public RiseConnectionTestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest) {
        super(testCase, config, true, forLoadTest);
        if (!actionGroupAdded) {
            SoapUI.getActionRegistry().addActionGroup(new RiseConnectionTestStepActionGroup());
            actionGroupAdded = true;
        }
        if (config != null && config.getConfig() != null) {
            XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(config.getConfig());
            readData(reader);
        }

        if (!forLoadTest)
            initIcons();
        setIcon(unknownStepIcon);
    }

    @Override
    protected ExecutableTestStepResult doExecute(SubmitContext testRunContext, CancellationToken cancellationToken) {
        ExecutableTestStepResult result = new ExecutableTestStepResult(this);
        result.startTimer();
        result.setStatus(TestStepResult.TestStepStatus.UNKNOWN);
        if (iconAnimator != null)
            iconAnimator.start();
        try {
            try {
                Client client = getClient(testRunContext, result);
                if (client == null) {
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                    return result;
                }

                long starTime = System.nanoTime();
                long maxTime = getTimeout() == 0 ? Long.MAX_VALUE : starTime + (long) getTimeout() * 1000 * 1000;

                if (!waitForConnection(client, cancellationToken, result, maxTime))
                    return result;

            } catch (Exception e) {
                result.setStatus(TestStepResult.TestStepStatus.FAILED);
                result.setError(e);
            }

            return result;

        } finally {
            result.stopTimer();
            if (iconAnimator != null)
                iconAnimator.stop();
            result.setOutcome(formOutcome(result));
            SoapUI.log(String.format("%s - [%s test step]", result.getOutcome(), getName()));
            notifyExecutionListeners(result);
        }
    }

    private String formOutcome(WsdlTestStepResult executionResult) {
        switch (executionResult.getStatus()) {
        case CANCELED:
            return "CANCELED";
        case FAILED:
            if (executionResult.getError() == null)
                return "Unable to open connection (" + StringUtils.join(executionResult.getMessages(), " ") + ")";
            else
                return "Error during open connection: " + Utils.getExceptionMessage(executionResult.getError());
        default:
            return String.format("The connection has been opened within %d ms", executionResult.getTimeTaken());

        }
    }

    protected void initIcons() {
        unknownStepIcon = UISupport.createImageIcon("com/tsystems/readyapi/plugin/websocket/unknown_rise_step.png");
        disabledStepIcon = UISupport.createImageIcon("com/tsystems/readyapi/plugin/websocket/disabled_rise_step.png");

        iconAnimator = new IconAnimator<RiseConnectionTestStep>(this,
                "com/tsystems/readyapi/plugin/websocket/unknown_rise_step.png",
                "com/tsystems/readyapi/plugin/websocket/rise_step.png", 5);
    }

    @Override
    protected void updateState() {
        if (iconAnimator == null)
            return;
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null
                && (testMonitor.hasRunningLoadTest(getTestCase()) || testMonitor.hasRunningSecurityTest(getTestCase())))
            setIcon(disabledStepIcon);
        else
            setIcon(unknownStepIcon);
    }

}
