package com.tsystems.readyapi.plugin.websocket;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.support.DefaultTestStepProperty;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

public abstract class ConnectedTestStep extends WsdlTestStepWithProperties implements ExecutableTestStep,
        PropertyChangeListener {
    private final static Logger LOGGER = Logger.getLogger(PluginConfig.LOGGER_NAME);

    final static String SERVER_URI_PROP_NAME = "ServerURI";
    final static String SUB_PROTOCOLS_PROP_NAME = "SubProtocols";

    final static String LOGIN_PROP_NAME = "Login";
    final static String PASSWORD_PROP_NAME = "Password";
    final static String CONNECTION_NAME_PROP_NAME = "ConnectionName";
    protected final static String TIMEOUT_PROP_NAME = "Timeout";
    private final static String TIMEOUT_MEASURE_PROP_NAME = "TimeoutMeasure";
    private final static String TIMEOUT_EXPIRED_MSG = "The test step's timeout has expired.";
    private Connection connection;

    private ArrayList<ExecutionListener> executionListeners;

    private int timeout = 30000;

    private TimeMeasure timeoutMeasure = TimeMeasure.Seconds;

    public ConnectedTestStep(WsdlTestCase testCase, TestStepConfig config, boolean hasEditor, boolean forLoadTest) {
        super(testCase, config, hasEditor, forLoadTest);
        addProperty(new DefaultTestStepProperty(SERVER_URI_PROP_NAME, false,
                new DefaultTestStepProperty.PropertyHandler() {
                    @Override
                    public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                        if (connection == null)
                            return "";
                        else
                            return connection.getServerUri();
                    }

                    @Override
                    public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                        if (connection == null)
                            return;
                        connection.setServerUri(s);
                    }
                }, this));
        addProperty(new DefaultTestStepProperty(SUB_PROTOCOLS_PROP_NAME, false,
                new DefaultTestStepProperty.PropertyHandler() {
                    @Override
                    public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                        if (connection == null)
                            return "";
                        else
                            return connection.getSubprotocols();
                    }

                    @Override
                    public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                        if (connection == null)
                            return;
                        connection.setSubprotocols(s);
                    }
                }, this));
        addProperty(new DefaultTestStepProperty(LOGIN_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if (connection == null)
                    return "";
                else
                    return connection.getLogin();
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if (connection == null)
                    return;
                connection.setLogin(s);
            }
        }, this));
        addProperty(new DefaultTestStepProperty(PASSWORD_PROP_NAME, false,
                new DefaultTestStepProperty.PropertyHandler() {
                    @Override
                    public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                        if (connection == null)
                            return "";
                        else
                            return connection.getPassword();
                    }

                    @Override
                    public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                        if (connection == null)
                            return;
                        connection.setPassword(s);
                    }
                }, this));
    }

    @Override
    public void addExecutionListener(ExecutionListener listener) {
        if (executionListeners == null)
            executionListeners = new ArrayList<ExecutionListener>();
        executionListeners.add(listener);
    }

    private boolean checkConnectionParams(ExpandedConnectionParams connectionParams, WsdlTestStepResult log) {
        String uriCheckResult = Utils.checkServerUri(connectionParams.getServerUri());
        if (uriCheckResult == null)
            return true;
        log.addMessage(uriCheckResult);
        log.setStatus(TestStepResult.TestStepStatus.FAILED);
        return false;
    }

    protected void cleanAfterExecution(PropertyExpansionContext runContext) {
        ClientCache.getCache(runContext).assureFinalized();
    }

    protected abstract ExecutableTestStepResult doExecute(PropertyExpansionContext testRunContext,
            CancellationToken cancellationToken);

    protected Client getClient(PropertyExpansionContext runContext, WsdlTestStepResult log) {
        if (connection == null) {
            log.addMessage("Connection for this test step is not selected or is broken.");
            log.setStatus(TestStepResult.TestStepStatus.FAILED);
            return null;
        }
        ExpandedConnectionParams actualConnectionParams;
        ClientCache cache = ClientCache.getCache(runContext);
        Client result = cache.get(connection.getName());
        if (result == null) {
            try {
                actualConnectionParams = connection.expand(runContext);
            } catch (Exception e) {
                log.addMessage(e.getMessage());
                log.setStatus(TestStepResult.TestStepStatus.FAILED);
                return null;
            }
            if (!checkConnectionParams(actualConnectionParams, log))
                return null;
            try {
                result = cache.add(connection.getName(), actualConnectionParams);
            } catch (Exception e) {
                log.setError(e);
                log.setStatus(TestStepResult.TestStepStatus.FAILED);
                return null;
            }
        }
        return result;
    }

    public Connection getConnection() {
        return connection;
    }

    public String getLogin() {
        return connection == null ? null : connection.getLogin();
    }

    public Project getOwningProject() {
        return ModelSupport.getModelItemProject(this);
    }

    public String getPassword() {
        return connection == null ? null : connection.getPassword();
    }

    public String getServerUri() {
        return connection == null ? null : connection.getServerUri();
    }

    public int getShownTimeout() {
        if (timeoutMeasure == TimeMeasure.Milliseconds)
            return timeout;
        else
            return timeout / 1000;
    }

    public String getSubprotocols() {
        return connection == null ? null : connection.getSubprotocols();
    }

    public int getTimeout() {
        return timeout;
    }

    public TimeMeasure getTimeoutMeasure() {
        return timeoutMeasure;
    }

    protected void notifyExecutionListeners(final ExecutableTestStepResult stepRunResult) {
        if (SwingUtilities.isEventDispatchThread()) {
            if (executionListeners != null)
                for (ExecutionListener listener : executionListeners)
                    try {
                        listener.afterExecution(this, stepRunResult);
                    } catch (Throwable e) {
                        LOGGER.error(e);
                    }
        } else
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    notifyExecutionListeners(stepRunResult);
                }
            });
    }

    @Override
    public void prepare(TestCaseRunner testRunner, TestCaseRunContext testRunContext) throws Exception {
        super.prepare(testRunner, testRunContext);
        updateState();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == connection)
            if (Utils.areStringsEqual(evt.getPropertyName(), "serverUri")) {
                notifyPropertyChanged("serverUri", evt.getOldValue(), evt.getNewValue());
                firePropertyValueChanged(SERVER_URI_PROP_NAME, (String) evt.getOldValue(), (String) evt.getNewValue());
            } else if (Utils.areStringsEqual(evt.getPropertyName(), Connection.SUB_PROTOCOLS_BEAN_PROP)) {
                notifyPropertyChanged("subprotocol", evt.getOldValue(), evt.getNewValue());
                firePropertyValueChanged(SUB_PROTOCOLS_PROP_NAME, (String) evt.getOldValue(),
                        (String) evt.getNewValue());
            } else if (Utils.areStringsEqual(evt.getPropertyName(), Connection.LOGIN_BEAN_PROP)) {
                notifyPropertyChanged("login", evt.getOldValue(), evt.getNewValue());
                firePropertyValueChanged(LOGIN_PROP_NAME, (String) evt.getOldValue(), (String) evt.getNewValue());
            } else if (Utils.areStringsEqual(evt.getPropertyName(), Connection.PASSWORD_BEAN_PROP)) {
                notifyPropertyChanged("password", evt.getOldValue(), evt.getNewValue());
                firePropertyValueChanged(PASSWORD_PROP_NAME, (String) evt.getOldValue(), (String) evt.getNewValue());
            } else if (Utils.areStringsEqual(evt.getPropertyName(), Connection.NAME_BEAN_PROP))
                updateData();

    }

    protected void readData(XmlObjectConfigurationReader reader) {
        if (connection != null) {
            connection.removePropertyChangeListener(this);
            connection = null;
        }
        String connectionName = reader.readString(CONNECTION_NAME_PROP_NAME, "");
        if (StringUtils.hasContent(connectionName))
            connection = ConnectionsManager.getConnection(this, connectionName);
        if (connection != null)
            connection.addPropertyChangeListener(this);

        timeout = reader.readInt(TIMEOUT_PROP_NAME, 30000);
        try {
            timeoutMeasure = TimeMeasure.valueOf(reader.readString(TIMEOUT_MEASURE_PROP_NAME,
                    TimeMeasure.Milliseconds.toString()));
        } catch (NumberFormatException | NullPointerException e) {
            timeoutMeasure = TimeMeasure.Milliseconds;
        }
    }

    @Override
    public void release() {
        if (connection != null)
            connection.removePropertyChangeListener(this);
        super.release();
    }

    @Override
    public void removeExecutionListener(ExecutionListener listener) {
        executionListeners.remove(listener);
    }

    @Override
    public TestStepResult run(final TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        return doExecute(testRunContext, new CancellationToken() {
            @Override
            public boolean cancelled() {
                return !testRunner.isRunning();
            }
        });
    }

    protected boolean sendMessage(Client client, Message<?> message, CancellationToken cancellationToken,
            WsdlTestStepResult testStepResult, long maxTime) {
        long timeout;
        if (maxTime == Long.MAX_VALUE)
            timeout = 0;
        else {
            timeout = maxTime - System.nanoTime();
            if (timeout <= 0) {
                testStepResult.addMessage(TIMEOUT_EXPIRED_MSG);
                testStepResult.setStatus(TestStepResult.TestStepStatus.FAILED);
                return false;
            }
        }
        client.sendMessage(message);
        return waitInternal(client, cancellationToken, testStepResult, maxTime,
                "Unable send message to websocket server.");
    }

    protected void setBooleanProperty(String propName, String publishedPropName, boolean value) {
        boolean old;
        try {
            Field field = null;
            Class<?> curClass = getClass();
            while (field == null && curClass != null)
                try {
                    field = curClass.getDeclaredField(propName);
                } catch (NoSuchFieldException e) {
                    curClass = curClass.getSuperclass();
                }
            if (field == null)
                throw new RuntimeException(String.format(
                        "Error during access to %s bean property (details: unable to find the underlying field)",
                        propName)); // We may not get here
            field.setAccessible(true);
            old = (boolean) field.get(this);

            if (old == value)
                return;
            field.set(this, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("Error during access to %s bean property (details: %s)", propName,
                    e.getMessage() + ")")); // We may not get here
        }
        updateData();
        notifyPropertyChanged(propName, old, value);
        if (StringUtils.hasContent(publishedPropName))
            firePropertyValueChanged(publishedPropName, Boolean.toString(old), Boolean.toString(value));

    }

    public void setConnection(Connection value) {
        if (connection == value)
            return;
        String oldServerUri = null, oldSubprotocols = null, oldLogin = null, oldPassword = null;
        Connection oldConnection = connection;
        if (oldConnection != null) {
            oldServerUri = oldConnection.getServerUri();
            oldSubprotocols = oldConnection.getSubprotocols();
            oldLogin = oldConnection.getLogin();
            oldPassword = oldConnection.getPassword();
            oldConnection.removePropertyChangeListener(this);
        }
        connection = value;
        String newServerUri = null, newSubprotocols = null, newLogin = null, newPassword = null;
        if (value != null) {
            value.addPropertyChangeListener(this);
            newServerUri = value.getServerUri();
            newSubprotocols = value.getSubprotocols();
            newPassword = value.getPassword();
            newLogin = value.getLogin();
        }
        updateData();
        notifyPropertyChanged("connection", oldConnection, value);
        if (!Utils.areStringsEqual(newServerUri, oldServerUri, false, true)) {
            notifyPropertyChanged("serverUri", oldServerUri, newServerUri);
            firePropertyValueChanged(SERVER_URI_PROP_NAME, oldServerUri, newServerUri);
        }
        if (!Utils.areStringsEqual(newSubprotocols, oldSubprotocols, false, true)) {
            notifyPropertyChanged("subprotocols", oldSubprotocols, newSubprotocols);
            firePropertyValueChanged(SUB_PROTOCOLS_PROP_NAME, oldSubprotocols, newSubprotocols);
        }
        if (!Utils.areStringsEqual(newLogin, oldLogin, false, true))
            firePropertyValueChanged(LOGIN_PROP_NAME, oldLogin, newLogin);
        if (!Utils.areStringsEqual(newPassword, oldPassword, false, true))
            firePropertyValueChanged(PASSWORD_PROP_NAME, oldPassword, newPassword);
    }

    protected boolean setIntProperty(String propName, String publishedPropName, int value) {
        int old;
        try {
            Field field = null;
            Class curClass = getClass();
            while (field == null && curClass != null)
                try {
                    field = curClass.getDeclaredField(propName);
                } catch (NoSuchFieldException e) {
                    curClass = curClass.getSuperclass();
                }
            if (field == null)
                throw new RuntimeException(String.format(
                        "Error during access to %s bean property (details: unable to find the underlying field)",
                        propName)); // We may not get here
            field.setAccessible(true);
            old = (int) field.get(this);

            if (old == value)
                return false;
            field.set(this, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("Error during access to %s bean property (details: %s)", propName,
                    e.getMessage() + ")")); // We may not get here
        }
        updateData();
        notifyPropertyChanged(propName, old, value);
        if (StringUtils.hasContent(publishedPropName))
            firePropertyValueChanged(publishedPropName, Integer.toString(old), Integer.toString(value));
        return true;
    }

    protected boolean setIntProperty(String propName, String publishedPropName, int value, int minAllowed,
            int maxAllowed) {
        if (value < minAllowed || value > maxAllowed)
            return false;
        return setIntProperty(propName, publishedPropName, value);
    }

    public void setLogin(String value) {
        if (connection != null)
            connection.setLogin(value);
    }

    public void setPassword(String value) {
        if (connection != null)
            connection.setPassword(value);
    }

    protected boolean setProperty(final String propName, final String publishedPropName, final Object value) {
        Object old;
        synchronized (this) {
            try {
                Field field = null;
                Class curClass = getClass();
                while (field == null && curClass != null)
                    try {
                        field = curClass.getDeclaredField(propName);
                    } catch (NoSuchFieldException e) {
                        curClass = curClass.getSuperclass();
                    }
                if (field == null)
                    throw new RuntimeException(String.format(
                            "Error during access to %s bean property (details: unable to find the underlying field)",
                            propName)); // We may not get here
                field.setAccessible(true);
                old = field.get(this);

                if (value == null) {
                    if (old == null)
                        return false;
                } else if (value.equals(old))
                    return false;
                field.set(this, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format("Error during access to %s bean property (details: %s)",
                        propName, e.getMessage() + ")")); // We may not get here
            }
            updateData();
        }
        final Object oldValue = old;
        if (SwingUtilities.isEventDispatchThread()) {
            notifyPropertyChanged(propName, oldValue, value);
            if (publishedPropName != null)
                firePropertyValueChanged(publishedPropName, oldValue == null ? null : oldValue.toString(),
                        value == null ? null : value.toString());
        } else
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    notifyPropertyChanged(propName, oldValue, value);
                    if (publishedPropName != null)
                        firePropertyValueChanged(publishedPropName, oldValue == null ? null : oldValue.toString(),
                                value == null ? null : value.toString());
                }
            });

        return true;
    }

    public void setServerUri(String value) {
        if (connection != null)
            connection.setServerUri(value);
    }

    public void setShownTimeout(int newValue) {
        if (timeoutMeasure == TimeMeasure.Milliseconds)
            setTimeout(newValue);
        else if (timeoutMeasure == TimeMeasure.Seconds)
            if ((long) newValue * 1000 > Integer.MAX_VALUE)
                setTimeout(Integer.MAX_VALUE / 1000 * 1000);
            else
                setTimeout(newValue * 1000);
    }

    public void setSubprotocols(String value) {
        if (connection != null)
            connection.setSubprotocols(value);
    }

    public void setTimeout(int newValue) {
        int oldShownTimeout = getShownTimeout();
        if (setIntProperty("timeout", TIMEOUT_PROP_NAME, newValue, 0, Integer.MAX_VALUE)) {
            if (timeoutMeasure == TimeMeasure.Seconds && newValue % 1000 != 0) {
                timeoutMeasure = TimeMeasure.Milliseconds;
                notifyPropertyChanged("timeoutMeasure", TimeMeasure.Seconds, TimeMeasure.Milliseconds);
            }
            int newShownTimeout = getShownTimeout();
            if (oldShownTimeout != newShownTimeout)
                notifyPropertyChanged("shownTimeout", oldShownTimeout, newShownTimeout);
        }
    }

    public void setTimeoutMeasure(TimeMeasure newValue) {
        if (newValue == null)
            return;
        TimeMeasure oldValue = timeoutMeasure;
        if (oldValue == newValue)
            return;
        int oldShownTimeout = getShownTimeout();
        timeoutMeasure = newValue;
        notifyPropertyChanged("timeoutMeasure", oldValue, newValue);
        if (newValue == TimeMeasure.Milliseconds)
            setIntProperty("timeout", TIMEOUT_PROP_NAME, oldShownTimeout, 0, Integer.MAX_VALUE);
        else if (newValue == TimeMeasure.Seconds)
            if ((long) oldShownTimeout * 1000 > Integer.MAX_VALUE) {
                setIntProperty("timeout", TIMEOUT_PROP_NAME, Integer.MAX_VALUE / 1000 * 1000);
                notifyPropertyChanged("shownTimeout", oldShownTimeout, getShownTimeout());
            } else
                setIntProperty("timeout", TIMEOUT_PROP_NAME, oldShownTimeout * 1000, 0, Integer.MAX_VALUE);
    }

    protected void updateData() {
        if (getConfig() == null)
            return;
        updateData(getConfig());
    }

    protected void updateData(TestStepConfig config) {
        XmlObjectBuilder builder = new XmlObjectBuilder();
        writeData(builder);
        config.setConfig(builder.finish());
    }

    protected abstract void updateState();

    protected boolean waitForConnection(Client client, CancellationToken cancellationToken,
            WsdlTestStepResult testStepResult, long maxTime) {
        long timeout;
        if (maxTime == Long.MAX_VALUE)
            timeout = 0;
        else {
            timeout = maxTime - System.nanoTime();
            if (timeout <= 0) {
                testStepResult.addMessage(TIMEOUT_EXPIRED_MSG);
                testStepResult.setStatus(TestStepResult.TestStepStatus.FAILED);
                return false;
            }
        }
        client.connect();
        return waitInternal(client, cancellationToken, testStepResult, maxTime,
                "Unable connect to the websocket server.");
    }

    protected boolean waitInternal(Client client, CancellationToken cancellationToken,
            WsdlTestStepResult testStepResult, long maxTime, String errorText) {
        while ((!client.isAvailable() || !client.isConnected()) && !client.isFaulty()) {
            boolean stopped = cancellationToken.cancelled();
            if (stopped || maxTime != Long.MAX_VALUE && System.nanoTime() > maxTime) {
                if (stopped) {
                    testStepResult.setStatus(TestStepResult.TestStepStatus.CANCELED);
                    client.cancel();
                } else {
                    testStepResult.addMessage(errorText);
                    testStepResult.addMessage(TIMEOUT_EXPIRED_MSG);
                    testStepResult.setStatus(TestStepResult.TestStepStatus.FAILED);
                }
                return false;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                //
            }
        }
        if (client.getThrowable() != null) {
            testStepResult.addMessage(errorText);
            testStepResult.setError(client.getThrowable());
            testStepResult.setStatus(TestStepResult.TestStepStatus.FAILED);
            return false;
        }

        return true;
    }

    protected void writeData(XmlObjectBuilder builder) {
        if (connection == null)
            builder.add(CONNECTION_NAME_PROP_NAME, "");
        else
            builder.add(CONNECTION_NAME_PROP_NAME, connection.getName());
        builder.add(TIMEOUT_PROP_NAME, timeout);
        builder.add(TIMEOUT_MEASURE_PROP_NAME, timeoutMeasure.name());
    }

    public enum TimeMeasure {
        Milliseconds("milliseconds"), Seconds("seconds");
        private String title;

        TimeMeasure(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

}
