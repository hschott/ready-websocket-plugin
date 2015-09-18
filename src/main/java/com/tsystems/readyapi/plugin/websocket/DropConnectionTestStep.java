package com.tsystems.readyapi.plugin.websocket;

import javax.swing.ImageIcon;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.support.IconAnimator;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

@PluginTestStep(typeName = "WebsocketDropConnectionTestStep", name = "Drop Websocket Connection",
        description = "Disconnects from the websocket server", iconPath = "com/smartbear/assets/drop_step.png")
public class DropConnectionTestStep extends ConnectedTestStep {
    private static final String DROP_METHOD_SETTING_NAME = "DropMethod";

    private static boolean actionGroupAdded = false;
    private ImageIcon disabledStepIcon;
    private ImageIcon unknownStepIcon;
    private IconAnimator<DropConnectionTestStep> iconAnimator;

    private DropMethod dropMethod = DropMethod.SendDisconnect;

    public enum DropMethod implements ConnectedTestStepPanel.UIOption {
        SendDisconnect("Send Disconnect message to websocket server"), Drop("Close network connection");
        private String title;

        DropMethod(String title) {
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        public static DropMethod fromString(String str) {
            if (str == null)
                return null;
            for (DropMethod m : DropMethod.values())
                if (m.toString().equals(str))
                    return m;
            return null;
        }
    }

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
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null)
            testMonitor.addTestMonitorListener(this);

    }

    protected void initIcons() {
        unknownStepIcon = UISupport.createImageIcon("com/smartbear/assets/unknown_drop_step.png");
        disabledStepIcon = UISupport.createImageIcon("com/smartbear/assets/disabled_drop_step.png");

        iconAnimator = new IconAnimator<DropConnectionTestStep>(this, "com/smartbear/assets/unknown_drop_step.png",
                "com/smartbear/assets/drop_step.png", 5);
    }

    @Override
    public void release() {
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null)
            testMonitor.removeTestMonitorListener(this);
        super.release();
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

    @Override
    protected void writeData(XmlObjectBuilder builder) {
        super.writeData(builder);
        builder.add(DROP_METHOD_SETTING_NAME, dropMethod.name());
    }

    public final static String DROP_METHOD_BEAN_PROP_NAME = "dropMethod";

    public DropMethod getDropMethod() {
        return dropMethod;
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
    public ExecutableTestStepResult execute(PropertyExpansionContext runContext, CancellationToken cancellationToken) {
        updateState();
        try {
            return doExecute(runContext, cancellationToken);
        } finally {
            cleanAfterExecution(runContext);
        }
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
            Client client = getClient(testRunContext, result);
            if (client == null)
                return result;
            if (client.isConnected())
                try {
                    switch (dropMethod) {
                    case SendDisconnect:
                        client.disconnect(false);
                        break;
                    case Drop:
                        client.disconnect(true);
                        break;
                    }
                } catch (Exception e) {
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                    result.setError(e);
                }
            else {
                result.addMessage("Already disconnected from the websocket server");
                result.setStatus(TestStepResult.TestStepStatus.FAILED);
            }

            return result;

        } finally {
            result.stopTimer();
            if (iconAnimator != null)
                iconAnimator.stop();
        }

    }

}
