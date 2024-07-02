package org.hschott.readyapi.plugin.websocket;

import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.support.IconAnimator;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.support.DefaultTestStepProperty;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

@PluginTestStep(typeName = "websocketPublishTestStep", name = "Publish using Websocket",
        description = "Publishes a specified message through websocket protocol.",
        iconPath = "org/hschott/readyapi/plugin/websocket/publish_step.png")
public class PublishTestStep extends ConnectedTestStep {

    private final static String MESSAGE_KIND_SETTING_NAME = "MessageKind";
    private final static String MESSAGE_SETTING_NAME = "Message";

    private final static String MESSAGE_TYPE_PROP_NAME = "MessageType";
    private final static String MESSAGE_PROP_NAME = "Message";
    public final static PublishedMessageType DEFAULT_MESSAGE_TYPE = PublishedMessageType.Json;

    private static boolean actionGroupAdded = false;
    private PublishedMessageType messageKind = DEFAULT_MESSAGE_TYPE;

    private String message;

    private ImageIcon disabledStepIcon;
    private ImageIcon unknownStepIcon;
    private IconAnimator<PublishTestStep> iconAnimator;

    public PublishTestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest) {
        super(testCase, config, true, forLoadTest);
        if (!actionGroupAdded) {
            SoapUI.getActionRegistry().addActionGroup(new PublishTestStepActionGroup());
            actionGroupAdded = true;
        }
        if (config != null && config.getConfig() != null) {
            XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(config.getConfig());
            readData(reader);
        }

        addProperty(new DefaultTestStepProperty(MESSAGE_TYPE_PROP_NAME, false,
                new DefaultTestStepProperty.PropertyHandler() {
                    @Override
                    public String getValue(DefaultTestStepProperty property) {
                        return messageKind.toString();
                    }

                    @Override
                    public void setValue(DefaultTestStepProperty property, String value) {
                        PublishedMessageType messageType = PublishedMessageType.fromString(value);
                        if (messageType != null)
                            setMessageKind(messageType);
                    }
                }, this));
        addProperty(new TestStepBeanProperty(MESSAGE_PROP_NAME, false, this, "message", this));

        addProperty(new DefaultTestStepProperty(TIMEOUT_PROP_NAME, false,
                new DefaultTestStepProperty.PropertyHandler() {
                    @Override
                    public String getValue(DefaultTestStepProperty property) {
                        return Integer.toString(getTimeout());
                    }

                    @Override
                    public void setValue(DefaultTestStepProperty property, String value) {
                        int newTimeout;
                        try {
                            newTimeout = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            return;
                        }
                        setTimeout(newTimeout);
                    }

                }, this));

        if (!forLoadTest)
            initIcons();
        setIcon(unknownStepIcon);
    }

    private boolean checkProperties(WsdlTestStepResult result, PublishedMessageType messageTypeToCheck,
            String messageToCheck) {
        boolean ok = true;
        if (messageTypeToCheck == null) {
            result.addMessage("The message format is not specified.");
            ok = false;
        }
        if (StringUtils.isNullOrEmpty(messageToCheck) && messageTypeToCheck != PublishedMessageType.Text) {
            if (messageTypeToCheck == PublishedMessageType.BinaryFile)
                result.addMessage("A file which contains a message is not specified");
            else
                result.addMessage("A message content is not specified.");
            ok = false;
        }

        return ok;
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
                String expandedMessage = testRunContext.expand(message);

                if (!checkProperties(result, messageKind, expandedMessage)) {
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                    return result;
                }
                long starTime = System.nanoTime();
                long maxTime = getTimeout() == 0 ? Long.MAX_VALUE : starTime + (long) getTimeout() * 1000 * 1000;

                Message<?> message;
                try {
                    message = messageKind.toMessage(expandedMessage, getOwningProject());
                } catch (RuntimeException e) {
                    result.addMessage(e.getMessage());
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                    return result;
                }

                if (!waitForConnection(client, cancellationToken, result, maxTime))
                    return result;

                if (!sendMessage(client, message, cancellationToken, result, maxTime))
                    return result;
                else
                    result.setStatus(TestStepResult.TestStepStatus.OK);

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
                return "Unable to publish the message (" + StringUtils.join(executionResult.getMessages(), " ") + ")";
            else
                return "Error during message publishing: " + Utils.getExceptionMessage(executionResult.getError());
        default:
            return String.format("The message has been published within %d ms", executionResult.getTimeTaken());

        }
    }

    public String getMessage() {
        return message;
    }

    public PublishedMessageType getMessageKind() {
        return messageKind;
    }

    protected void initIcons() {
        unknownStepIcon = UISupport.createImageIcon("org/hschott/readyapi/plugin/websocket/unknown_publish_step.png");
        disabledStepIcon = UISupport.createImageIcon("org/hschott/readyapi/plugin/websocket/disabled_publish_step.png");

        iconAnimator = new IconAnimator<PublishTestStep>(this,
                                                         "org/hschott/readyapi/plugin/websocket/unknown_publish_step.png",
                                                         "org/hschott/readyapi/plugin/websocket/publish_step.png", 5);
    }

    @Override
    protected void readData(XmlObjectConfigurationReader reader) {
        super.readData(reader);
        messageKind = PublishedMessageType.valueOf(reader.readString(MESSAGE_KIND_SETTING_NAME,
                DEFAULT_MESSAGE_TYPE.name()));
        message = reader.readString(MESSAGE_SETTING_NAME, "");
    }

    protected boolean sendMessage(Client client, Message<?> message, CancellationToken cancellationToken,
            WsdlTestStepResult testStepResult, long maxTime) {
        long timeout;
        if (maxTime == Long.MAX_VALUE)
            timeout = -1;
        else {
            timeout = maxTime - System.nanoTime();
            if (timeout <= 0) {
                testStepResult.addMessage(TIMEOUT_EXPIRED_MSG);
                testStepResult.setStatus(TestStepResult.TestStepStatus.FAILED);
                return false;
            }
        }

        if (timeout <= -1)
            client.sendMessage(message, timeout);
        else
            client.sendMessage(message, TimeUnit.NANOSECONDS.toMillis(timeout));

        testStepResult.setSize(message.size());

        return waitInternal(client, cancellationToken, testStepResult, maxTime,
                "Unable send message to websocket server.");
    }

    public void setMessage(String value) {
        setProperty("message", MESSAGE_PROP_NAME, value);
    }

    public void setMessageKind(PublishedMessageType newValue) {
        if (messageKind == newValue)
            return;
        PublishedMessageType old = messageKind;
        messageKind = newValue;
        updateData();
        notifyPropertyChanged("messageKind", old, newValue);
        firePropertyValueChanged(MESSAGE_TYPE_PROP_NAME, old.toString(), newValue.toString());
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
        if (messageKind != null)
            builder.add(MESSAGE_KIND_SETTING_NAME, messageKind.name());
        builder.add(MESSAGE_SETTING_NAME, message);
    }

}
