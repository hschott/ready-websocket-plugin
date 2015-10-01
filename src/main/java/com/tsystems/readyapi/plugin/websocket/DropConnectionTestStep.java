package com.tsystems.readyapi.plugin.websocket;

import javax.swing.ImageIcon;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.support.IconAnimator;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

@PluginTestStep(typeName = "WebsocketDropConnectionTestStep", name = "Drop Websocket Connection",
        description = "Disconnects from the websocket server", iconPath = "com/smartbear/assets/drop_step.png")
public class DropConnectionTestStep extends ConnectedTestStep {
    private static final String DROP_METHOD_SETTING_NAME = "DropMethod";

    private static boolean actionGroupAdded = false;
    public final static String DROP_METHOD_BEAN_PROP_NAME = "dropMethod";
    private ImageIcon disabledStepIcon;
    private ImageIcon unknownStepIcon;

    private IconAnimator<DropConnectionTestStep> iconAnimator;

    private DropMethod dropMethod = DropMethod.SendDisconnect;

    public DropConnectionTestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest) {
        super(testCase, config, true, forLoadTest);
        if (!actionGroupAdded) {
            SoapUI.getActionRegistry().addActionGroup(new DropConnectionTestStepActionGroup());
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
    protected ExecutableTestStepResult doExecute(PropertyExpansionContext testRunContext,
            CancellationToken cancellationToken) {
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

                if (client.isConnected()) {
                    switch (dropMethod) {
                    case SendDisconnect:
                        client.disconnect(false);

                        break;
                    case Drop:
                        client.disconnect(true);
                        break;
                    }

                    result.setStatus(TestStepResult.TestStepStatus.OK);

                } else if (client.isFaulty()) {
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                    result.setError(client.getThrowable());
                    return result;

                } else {
                    result.addMessage("Already disconnected from the websocket server");
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                }
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
                return "Unable to drop connection (" + StringUtils.join(executionResult.getMessages(), " ") + ")";
            else
                return "Error during drop connection: " + Utils.getExceptionMessage(executionResult.getError());
        default:
            return String.format("The connection has been dropped within %d ms", executionResult.getTimeTaken());

        }
    }

    @Override
    public ExecutableTestStepResult execute(PropertyExpansionContext runContext, CancellationToken cancellationToken) {
        updateState();
        try {
            return doExecute(runContext, cancellationToken);
        } finally {
            cleanAfterExecution(runContext);
        }
    }

    public DropMethod getDropMethod() {
        return dropMethod;
    }

    protected void initIcons() {
        unknownStepIcon = UISupport.createImageIcon("com/smartbear/assets/unknown_drop_step.png");
        disabledStepIcon = UISupport.createImageIcon("com/smartbear/assets/disabled_drop_step.png");

        iconAnimator = new IconAnimator<DropConnectionTestStep>(this, "com/smartbear/assets/unknown_drop_step.png",
                "com/smartbear/assets/drop_step.png", 5);
    }

    @Override
    protected void readData(XmlObjectConfigurationReader reader) {
        super.readData(reader);
        try {
            dropMethod = DropMethod.valueOf(reader.readString(DROP_METHOD_SETTING_NAME,
                    DropMethod.SendDisconnect.toString()));
        } catch (IllegalArgumentException | NullPointerException e) {
            dropMethod = DropMethod.SendDisconnect;
        }
    }

    public void setDropMethod(DropMethod newValue) {
        setProperty(DROP_METHOD_BEAN_PROP_NAME, null, newValue);
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

    @Override
    protected void writeData(XmlObjectBuilder builder) {
        super.writeData(builder);
        builder.add(DROP_METHOD_SETTING_NAME, dropMethod.name());
    }

    public enum DropMethod implements ConnectedTestStepPanel.UIOption {
        SendDisconnect("Send Normal Close message"), Drop("Send Protocol Error message");
        private String title;

        DropMethod(String title) {
            this.title = title;
        }

        public static DropMethod fromString(String str) {
            if (str == null)
                return null;
            for (DropMethod m : DropMethod.values())
                if (m.toString().equals(str))
                    return m;
            return null;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }

}
