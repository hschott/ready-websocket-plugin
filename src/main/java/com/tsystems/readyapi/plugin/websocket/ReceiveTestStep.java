package com.tsystems.readyapi.plugin.websocket;

import static com.tsystems.readyapi.plugin.websocket.Utils.bytesToHexString;

import java.beans.PropertyChangeEvent;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.WsdlSubmitContext;
import com.eviware.soapui.impl.wsdl.support.AbstractNonHttpMessageExchange;
import com.eviware.soapui.impl.wsdl.support.IconAnimator;
import com.eviware.soapui.impl.wsdl.support.assertions.AssertableConfig;
import com.eviware.soapui.impl.wsdl.support.assertions.AssertionsSupport;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.TestAssertionRegistry;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.iface.Response;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.support.DefaultTestStepProperty;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionsListener;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.google.common.base.Charsets;

@PluginTestStep(typeName = "WebsocketReceiveTestStep", name = "Receive Websocket Message",
        description = "Waits for a Websocket message of a specific topic.",
        iconPath = "com/smartbear/assets/receive_step.png")
public class ReceiveTestStep extends ConnectedTestStep implements Assertable {
    private final static Logger LOGGER = Logger.getLogger(PluginConfig.LOGGER_NAME);

    private final static String EXPECTED_MESSAGE_TYPE_PROP_NAME = "ExpectedMessageType";

    private final static String RECEIVED_MESSAGE_PROP_NAME = "ReceivedMessage";
    private final static String ASSERTION_SECTION = "assertion";

    private static boolean actionGroupAdded = false;

    private MessageType expectedMessageType = MessageType.Text;

    private String receivedMessage = null;

    private AssertionsSupport assertionsSupport;
    private AssertionStatus assertionStatus = AssertionStatus.UNKNOWN;
    private ArrayList<TestAssertionConfig> assertionConfigs = new ArrayList<TestAssertionConfig>();
    private ImageIcon validStepIcon;

    private ImageIcon failedStepIcon;
    private ImageIcon disabledStepIcon;
    private ImageIcon unknownStepIcon;
    private IconAnimator<ReceiveTestStep> iconAnimator;

    private MessageExchangeImpl messageExchange;

    public ReceiveTestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest) {
        super(testCase, config, true, forLoadTest);
        if (!actionGroupAdded) {
            SoapUI.getActionRegistry().addActionGroup(new ReceiveTestStepActionGroup());
            actionGroupAdded = true;
        }
        if ((config != null) && (config.getConfig() != null)) {
            XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(config.getConfig());
            readData(reader);
        }
        initAssertions(config);

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

        addProperty(new TestStepBeanProperty(RECEIVED_MESSAGE_PROP_NAME, true, this, "receivedMessage", this));

        if (!forLoadTest)
            initIcons();

        messageExchange = new MessageExchangeImpl(this);

        updateState();
    }

    @Override
    public TestAssertion addAssertion(String selection) {

        try {
            WsdlMessageAssertion assertion = assertionsSupport.addWsdlAssertion(selection);
            if (assertion == null)
                return null;

            if (receivedMessage != null) {
                applyAssertion(assertion, new WsdlSubmitContext(this));
                updateState();
            }

            return assertion;
        } catch (Exception e) {
            LOGGER.error(e);
            throw e;
        }
    }

    @Override
    public void addAssertionsListener(AssertionsListener listener) {
        assertionsSupport.addAssertionsListener(listener);
    }

    private void applyAssertion(WsdlMessageAssertion assertion, SubmitContext context) {
        assertion.assertProperty(this, RECEIVED_MESSAGE_PROP_NAME, messageExchange, context);
    }

    private void assertReceivedMessage() {
        if (getReceivedMessage() != null)
            for (WsdlMessageAssertion assertion : assertionsSupport.getAssertionList())
                applyAssertion(assertion, new WsdlSubmitContext(this));
        updateState();
    }

    private String bytesToString(byte[] buf, int startPos, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        ByteBuffer byteBuf = ByteBuffer.wrap(buf, startPos, buf.length - startPos);
        try {
            CharBuffer charBuf = decoder.decode(byteBuf);
            return charBuf.toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    @Override
    public TestAssertion cloneAssertion(TestAssertion source, String name) {
        return assertionsSupport.cloneAssertion(source, name);
    }

    @Override
    protected ExecutableTestStepResult doExecute(SubmitContext runContext, CancellationToken cancellationToken) {
        ExecutableTestStepResult result = new ExecutableTestStepResult(this);
        result.startTimer();
        result.setStatus(TestStepResult.TestStepStatus.UNKNOWN);
        if (iconAnimator != null)
            iconAnimator.start();
        try {
            try {
                Client client = getClient(runContext, result);
                if (client == null) {
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                    return result;
                }

                long starTime = System.nanoTime();
                long maxTime = getTimeout() == 0 ? Long.MAX_VALUE : starTime + (long) getTimeout() * 1000 * 1000;

                if (!waitForConnection(client, cancellationToken, result, maxTime))
                    return result;

                Message<?> msg = null;
                boolean failed = false;
                while (System.nanoTime() <= maxTime && !cancellationToken.isCancelled())
                    if ((msg = client.nextMessage(10)) != null) {

                        if (!storeMessage(msg, result)) {
                            result.addMessage(String
                                    .format("Unable to extract a content of \"%s\" type from the message.",
                                            expectedMessageType));
                            result.setStatus(TestStepResult.TestStepStatus.FAILED);
                            return result;
                        }

                        for (WsdlMessageAssertion assertion : assertionsSupport.getAssertionList()) {
                            applyAssertion(assertion, runContext);
                            failed = assertion.isFailed();
                        }

                        if (!failed)
                            break;

                    } else if (client.isFaulty()) {
                        result.setStatus(TestStepResult.TestStepStatus.FAILED);
                        result.setError(client.getThrowable());
                        return result;
                    } else if (!client.isConnected()) {
                        result.setStatus(TestStepResult.TestStepStatus.CANCELED);
                        return result;
                    }

                if (msg == null || failed) {
                    if (cancellationToken.isCancelled())
                        result.setStatus(TestStepResult.TestStepStatus.CANCELED);
                    else {
                        result.addMessage("The test step's timeout has expired");
                        result.setStatus(TestStepResult.TestStepStatus.FAILED);
                    }
                } else {
                    result.setStatus(TestStepResult.TestStepStatus.OK);
                    if (msg != null)
                        result.setSize(msg.size());
                }
            } catch (Exception e) {
                result.setError(e);
                result.setStatus(TestStepResult.TestStepStatus.FAILED);
            }

            return result;
        } finally {
            result.stopTimer();
            if (iconAnimator != null)
                iconAnimator.stop();
            updateState();
            if (result.getStatus() == TestStepResult.TestStepStatus.UNKNOWN) {
                assertReceivedMessage();

                switch (getAssertionStatus()) {
                case FAILED:
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                    break;
                case VALID:
                    result.setStatus(TestStepResult.TestStepStatus.OK);
                    break;
                }
            }
            result.setOutcome(formOutcome(result));
            SoapUI.log(String.format("%s - [%s test step]", result.getOutcome(), getName()));
            notifyExecutionListeners(result);
        }

    }

    private String formOutcome(ExecutableTestStepResult executionResult) {
        if (executionResult.getStatus() == TestStepResult.TestStepStatus.CANCELED)
            return "CANCELED";
        else if (executionResult.getStatus() == TestStepResult.TestStepStatus.FAILED) {
            if (executionResult.getError() == null)
                return "Unable to receive a valid message (" + StringUtils.join(executionResult.getMessages(), " ")
                        + ")";
            else
                return "Error during message receiving: " + Utils.getExceptionMessage(executionResult.getError());
        } else
            return String.format("Message has been received within %d ms", executionResult.getTimeTaken());

    }

    @Override
    public String getAssertableContent() {
        return getReceivedMessage();
    }

    @Override
    public String getAssertableContentAsXml() {
        // XmlObject.Factory.parse(receivedMessage)
        return getReceivedMessage();
    }

    @Override
    public TestAssertionRegistry.AssertableType getAssertableType() {
        return TestAssertionRegistry.AssertableType.BOTH;
    }

    @Override
    public TestAssertion getAssertionAt(int c) {
        return assertionsSupport.getAssertionAt(c);
    }

    @Override
    public TestAssertion getAssertionByName(String name) {
        return assertionsSupport.getAssertionByName(name);
    }

    @Override
    public int getAssertionCount() {
        return assertionsSupport.getAssertionCount();
    }

    @Override
    public List<TestAssertion> getAssertionList() {
        return new ArrayList<TestAssertion>(assertionsSupport.getAssertionList());
    }

    @Override
    public Map<String, TestAssertion> getAssertions() {
        return assertionsSupport.getAssertions();
    }

    @Override
    public AssertionStatus getAssertionStatus() {
        return assertionStatus;
    }

    @Override
    public String getDefaultAssertableContent() {
        return "";
    }

    public MessageType getExpectedMessageType() {
        return expectedMessageType;
    }

    @Override
    public Interface getInterface() {
        return null;
    }

    public String getReceivedMessage() {
        return receivedMessage;
    }

    @Override
    public TestStep getTestStep() {
        return this;
    }

    private void initAssertions(TestStepConfig testStepData) {
        if (testStepData != null && testStepData.getConfig() != null) {
            XmlObject config = testStepData.getConfig();
            XmlObject[] assertionsSections = config.selectPath("$this/" + ASSERTION_SECTION);
            for (XmlObject assertionSection : assertionsSections) {
                TestAssertionConfig assertionConfig;
                try {
                    assertionConfig = TestAssertionConfig.Factory.parse(assertionSection.toString());
                } catch (XmlException e) {
                    LOGGER.error(e);
                    continue;
                }
                assertionConfigs.add(assertionConfig);
            }
        }
        assertionsSupport = new AssertionsSupport(this, new AssertableConfigImpl());
    }

    protected void initIcons() {
        validStepIcon = UISupport.createImageIcon("com/smartbear/assets/valid_receive_step.png");
        failedStepIcon = UISupport.createImageIcon("com/smartbear/assets/invalid_receive_step.png");
        unknownStepIcon = UISupport.createImageIcon("com/smartbear/assets/unknown_receive_step.png");
        disabledStepIcon = UISupport.createImageIcon("com/smartbear/assets/disabled_receive_step.png");

        iconAnimator = new IconAnimator<ReceiveTestStep>(this, "com/smartbear/assets/receive_step_base.png",
                "com/smartbear/assets/receive_step.png", 5);
    }

    @Override
    public TestAssertion moveAssertion(int ix, int offset) {
        WsdlMessageAssertion assertion = assertionsSupport.getAssertionAt(ix);
        try {
            return assertionsSupport.moveAssertion(ix, offset);
        } finally {
            assertion.release();
            updateState();
        }
    }

    private void onInvalidPayload(byte[] payload, WsdlTestStepResult errors) {
        if (payload == null || payload.length == 0) {
            setReceivedMessage(null);
            errors.addMessage(String.format(
                    "Unable to extract a content of \"%s\" type from the message, because its payload is empty.",
                    expectedMessageType));
            return;
        }
        String text;
        String actualFormat = "hexadecimal digits sequence";
        if (payload.length >= 3 && payload[0] == (byte) 0xef && payload[1] == (byte) 0xbb && payload[2] == (byte) 0xbf) {
            text = bytesToString(payload, 3, Charsets.UTF_8);
            if (text == null)
                text = bytesToHexString(payload);
            else
                actualFormat = "UTF-8 text";
        } else if (payload.length >= 2
                && ((payload[1] & 0xff) * 256 + (payload[0] & 0xff) == 0xfffe || (payload[1] & 0xff) * 256
                        + (payload[0] & 0xff) == 0xfeff)) {
            text = bytesToString(payload, 2, Charsets.UTF_16);
            if (text == null)
                text = bytesToHexString(payload);
            else
                actualFormat = "UTF-16 text";
        } else
            text = bytesToHexString(payload);
        setReceivedMessage(text);
        errors.addMessage(String.format(
                "Unable to extract a content of \"%s\" type from the message. It is stored as %s.",
                expectedMessageType, actualFormat));
    }

    @Override
    public void prepare(TestCaseRunner testRunner, TestCaseRunContext testRunContext) throws Exception {
        super.prepare(testRunner, testRunContext);
        setReceivedMessage(null);
        for (TestAssertion assertion : assertionsSupport.getAssertionList())
            assertion.prepare(testRunner, testRunContext);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(TestAssertion.CONFIGURATION_PROPERTY)
                || event.getPropertyName().equals(TestAssertion.DISABLED_PROPERTY)) {
            updateData();
            assertReceivedMessage();
        }
    }

    @Override
    protected void readData(XmlObjectConfigurationReader reader) {
        super.readData(reader);
        expectedMessageType = MessageType.valueOf(reader.readString(EXPECTED_MESSAGE_TYPE_PROP_NAME,
                MessageType.Text.name()));
    }

    @Override
    public void removeAssertion(TestAssertion assertion) {
        try {
            assertionsSupport.removeAssertion((WsdlMessageAssertion) assertion);

        } finally {
            ((WsdlMessageAssertion) assertion).release();
        }
        updateState();
    }

    @Override
    public void removeAssertionsListener(AssertionsListener listener) {
        assertionsSupport.removeAssertionsListener(listener);
    }

    public void setExpectedMessageType(MessageType value) {
        setProperty("expectedMessageType", null, value);
    }

    @Override
    public void setIcon(ImageIcon newIcon) {
        if (iconAnimator != null && newIcon == iconAnimator.getBaseIcon())
            return;
        super.setIcon(newIcon);
    }

    public void setReceivedMessage(String value) {
        setProperty("receivedMessage", RECEIVED_MESSAGE_PROP_NAME, value);
    }

    private boolean storeMessage(Message<?> message, WsdlTestStepResult errors) {
        if (message instanceof Message.BinaryMessage) {

            byte[] payload = new byte[0];
            Message.BinaryMessage binary = (Message.BinaryMessage) message;
            payload = binary.getPayload().array();

            switch (expectedMessageType) {
            case IntegerNumber:
                switch (payload.length) {
                case 1:
                    setReceivedMessage(String.valueOf(payload[0]));
                    return true;
                case 2:
                    setReceivedMessage(String.valueOf((payload[0] & 0xff) + (payload[1] << 8)));
                    return true;
                case 4:
                    int ir = 0;
                    for (int i = 0; i < 4; ++i)
                        ir += (payload[i] & 0xff) << 8 * i;
                    setReceivedMessage(String.valueOf(ir));
                    return true;
                case 8:
                    long lr = 0;
                    for (int i = 0; i < 8; ++i)
                        lr += (long) (payload[i] & 0xff) << 8 * i;
                    setReceivedMessage(String.valueOf(lr));
                    return true;
                }

                break;

            case FloatNumber:
                switch (payload.length) {
                case 4:
                    setReceivedMessage(String.valueOf(ByteBuffer.wrap(payload).getFloat()));
                    return true;
                case 8:
                    setReceivedMessage(String.valueOf(ByteBuffer.wrap(payload).getDouble()));
                    return true;
                }

                break;

            case BinaryData:
                setReceivedMessage(bytesToHexString(payload));
                return true;

            case Text:
                setReceivedMessage(new String(payload, Charsets.UTF_8));
                return true;
            }

            onInvalidPayload(payload, errors);
            return true;
        }

        if (message instanceof Message.TextMessage) {
            Message.TextMessage text = (Message.TextMessage) message;
            switch (expectedMessageType) {
            case IntegerNumber:
                try {
                    Long number = Long.parseLong(text.getPayload());
                    setReceivedMessage(String.valueOf(number));
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }

            case FloatNumber:
                try {
                    Double number = Double.parseDouble(text.getPayload());
                    setReceivedMessage(String.valueOf(number));
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }

            case BinaryData:
                setReceivedMessage(bytesToHexString(text.getPayload().getBytes(Charset.forName("utf-8"))));
                return true;

            case Text:
                setReceivedMessage(text.getPayload());
                return true;
            }

            return true;
        }

        return false;
    }

    @Override
    protected void updateState() {
        final AssertionStatus oldAssertionStatus = assertionStatus;
        if (getReceivedMessage() != null) {
            int cnt = getAssertionCount();
            if (cnt == 0)
                assertionStatus = AssertionStatus.UNKNOWN;
            else {
                assertionStatus = AssertionStatus.VALID;
                for (int c = 0; c < cnt; c++)
                    if (getAssertionAt(c).getStatus() == AssertionStatus.FAILED) {
                        assertionStatus = AssertionStatus.FAILED;
                        break;
                    }
            }
        } else
            assertionStatus = AssertionStatus.UNKNOWN;
        if (oldAssertionStatus != assertionStatus) {
            final AssertionStatus newAssertionStatus = assertionStatus;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    notifyPropertyChanged("assertionStatus", oldAssertionStatus, newAssertionStatus);
                }
            });
        }
        if (iconAnimator == null)
            return;
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if ((testMonitor != null)
                && (testMonitor.hasRunningLoadTest(getTestStep().getTestCase()) || testMonitor
                        .hasRunningSecurityTest(getTestStep().getTestCase())))
            setIcon(disabledStepIcon);
        else {
            ImageIcon icon = iconAnimator.getIcon();
            if (icon == iconAnimator.getBaseIcon())
                switch (assertionStatus) {
                case VALID:
                    setIcon(validStepIcon);
                    break;
                case FAILED:
                    setIcon(failedStepIcon);
                    break;
                case UNKNOWN:
                    setIcon(unknownStepIcon);
                    break;
                }
        }
    }

    @Override
    protected void writeData(XmlObjectBuilder builder) {
        super.writeData(builder);
        builder.add(EXPECTED_MESSAGE_TYPE_PROP_NAME, expectedMessageType.name());
        for (TestAssertionConfig assertionConfig : assertionConfigs)
            builder.addSection(ASSERTION_SECTION, assertionConfig);
    }

    private class AssertableConfigImpl implements AssertableConfig {

        @Override
        public TestAssertionConfig addNewAssertion() {
            TestAssertionConfig newConfig = TestAssertionConfig.Factory.newInstance();
            assertionConfigs.add(newConfig);
            return newConfig;
        }

        @Override
        public List<TestAssertionConfig> getAssertionList() {
            return assertionConfigs;
        }

        @Override
        public TestAssertionConfig insertAssertion(TestAssertionConfig source, int ix) {
            TestAssertionConfig conf = TestAssertionConfig.Factory.newInstance();
            conf.set(source);
            assertionConfigs.add(ix, conf);
            updateData();
            return conf;
        }

        @Override
        public void removeAssertion(int ix) {
            assertionConfigs.remove(ix);
            updateData();
        }
    }

    private class MessageExchangeImpl extends AbstractNonHttpMessageExchange<ReceiveTestStep> {

        public MessageExchangeImpl(ReceiveTestStep modelItem) {
            super(modelItem);
        }

        @Override
        public String getEndpoint() {
            return null;
        }

        @Override
        public boolean hasRequest(boolean ignoreEmpty) {
            return false;
        }

        @Override
        public boolean hasResponse() {
            return false;
        }

        @Override
        public Response getResponse() {
            return null;
        }

        @Override
        public String getRequestContent() {
            return null;
        }

        @Override
        public String getResponseContent() {
            return null;
        }

        @Override
        public long getTimeTaken() {
            return 0;
        }

        @Override
        public long getTimestamp() {
            return System.currentTimeMillis();
        }

        @Override
        public boolean isDiscarded() {
            return false;
        }

    }

    protected enum MessageType implements ConnectedTestStepPanel.UIOption {
        Text("Text (UTF-8)"), BinaryData("Raw binary data"), IntegerNumber("Integer number"), FloatNumber(
                "Float number");
        private String title;

        MessageType(String title) {
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }

}
