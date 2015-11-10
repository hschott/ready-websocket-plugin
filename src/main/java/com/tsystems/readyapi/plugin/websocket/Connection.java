package com.tsystems.readyapi.plugin.websocket;

import static com.tsystems.readyapi.plugin.websocket.Utils.bytesToHexString;
import static com.tsystems.readyapi.plugin.websocket.Utils.hexStringToBytes;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;

import org.apache.commons.ssl.OpenSSL;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;

import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.support.PropertyChangeNotifier;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

public class Connection implements PropertyChangeNotifier {
    private static final Logger LOGGER = Logger.getLogger(PluginConfig.LOGGER_NAME);

    protected static final boolean ARE_NAMES_CASE_INSENSITIVE = true;

    private static final String NAME_SETTING_NAME = "Name";
    private static final String SERVER_URI_SETTING_NAME = "ServerURI";
    private static final String LOGIN_SETTING_NAME = "Login";
    private static final String ENCR_PASSWORD_SETTING_NAME = "EncrPassword";
    private static final String SUB_PROTOCOLS_SETTING_NAME = "SubProtocols";
    private static final String PASSWORD_FOR_ENCODING = "{CB012CCB-6D9C-4c3d-8A82-06B54D546512}";
    public static final String ENCRYPTION_METHOD = "des3";

    public final static String NAME_BEAN_PROP = "name";
    public final static String LOGIN_BEAN_PROP = "login";
    public final static String PASSWORD_BEAN_PROP = "password";
    public final static String SUB_PROTOCOLS_BEAN_PROP = "subprotocols";
    private String name;

    private String originalServerUri;

    private String login;

    private String password;

    private String subprotocols;

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public Connection() {
    }

    public Connection(String name, ConnectionParams params) {
        this();
        this.name = name;
        setParams(params);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        try {
            propertyChangeSupport.addPropertyChangeListener(listener);
        } catch (Throwable t) {
            LOGGER.error(t);
        }
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        try {
            propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
        } catch (Throwable t) {
            LOGGER.error(t);
        }
    }

    public ExpandedConnectionParams expand(PropertyExpansionContext context) {
        ExpandedConnectionParams result = new ExpandedConnectionParams();
        result.setServerUri(context.expand(getServerUri()));
        result.subprotocols = context.expand(getSubprotocols());
        result.setCredentials(context.expand(getLogin()), context.expand(getPassword()));

        return result;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public ConnectionParams getParams() {
        return new ConnectionParams(getServerUri(), getLogin(), getPassword(), getSubprotocols());
    }

    public String getPassword() {
        return password;
    }

    public String getServerUri() {
        return originalServerUri;
    }

    public String getSubprotocols() {
        return subprotocols;
    }

    public boolean hasCredentials() {
        return login != null && !"".equals(login);
    }

    public void load(XmlObject xml) {
        XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(xml);
        name = reader.readString(NAME_SETTING_NAME, null);
        originalServerUri = reader.readString(SERVER_URI_SETTING_NAME, null);
        login = reader.readString(LOGIN_SETTING_NAME, null);
        password = null;
        String encodedPasswordString = reader.readString(ENCR_PASSWORD_SETTING_NAME, null);
        if (encodedPasswordString != null && encodedPasswordString.length() != 0) {
            byte[] encodedPassword = hexStringToBytes(encodedPasswordString);
            try {
                password = new String(OpenSSL.decrypt(ENCRYPTION_METHOD, PASSWORD_FOR_ENCODING.toCharArray(),
                        encodedPassword));
            } catch (Throwable e) {
                LOGGER.error(e);
            }
        }

        subprotocols = reader.readString(SUB_PROTOCOLS_SETTING_NAME, null);
    }

    public void notifyPropertyChanged(String name, Object oldValue, Object newValue) {
        try {
            if (!Objects.equals(oldValue, newValue))
                propertyChangeSupport.firePropertyChange(name, oldValue, newValue);
        } catch (Throwable t) {
            LOGGER.error(t);
        }
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        try {
            propertyChangeSupport.removePropertyChangeListener(listener);
        } catch (Throwable t) {
            LOGGER.error(t);
        }
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        try {
            propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
        } catch (Throwable t) {
            LOGGER.error(t);
        }
    }

    public XmlObject save() {
        XmlObjectBuilder builder = new XmlObjectBuilder();
        builder.add(NAME_SETTING_NAME, name);
        builder.add(SERVER_URI_SETTING_NAME, originalServerUri);
        if (login != null) {
            builder.add(LOGIN_SETTING_NAME, login);
            if (password != null && password.length() != 0) {
                byte[] encodedPassword = null;
                try {
                    encodedPassword = OpenSSL.encrypt(ENCRYPTION_METHOD, PASSWORD_FOR_ENCODING.toCharArray(),
                            password.getBytes());
                } catch (IOException | GeneralSecurityException e) {
                    LOGGER.error(e);
                }
                builder.add(ENCR_PASSWORD_SETTING_NAME, bytesToHexString(encodedPassword));
            }
        }
        if (subprotocols != null && subprotocols.length() != 0)
            builder.add(SUB_PROTOCOLS_SETTING_NAME, subprotocols);
        return builder.finish();
    }

    public void setCredentials(String login, String password) {
        if (login == null || login.length() == 0) {
            setLogin(login);
            setPassword(null);
        } else {
            setLogin(login);
            setPassword(password);
        }
    }

    public void setLogin(String newValue) {
        String old = getLogin();
        if (!Utils.areStringsEqual(old, newValue, false, true)) {
            login = newValue;
            notifyPropertyChanged(LOGIN_BEAN_PROP, old, newValue);
        }
    }

    public void setName(String newName) {
        String oldName = name;
        if (!Utils.areStringsEqual(oldName, newName, ARE_NAMES_CASE_INSENSITIVE, true)) {
            name = newName;
            notifyPropertyChanged(NAME_BEAN_PROP, oldName, newName);
        }
    }

    public void setParams(ConnectionParams params) {
        setServerUri(params.serverUri);
        setCredentials(params.login, params.password);
        setSubprotocols(params.subprotocols);
    }

    public void setPassword(String newValue) {
        String old = getPassword();
        if (!Utils.areStringsEqual(old, newValue, false, true)) {
            password = newValue;
            notifyPropertyChanged(PASSWORD_BEAN_PROP, old, newValue);
        }
    }

    public void setServerUri(String serverUri) {
        String oldServerUri = getServerUri();
        originalServerUri = serverUri == null ? "" : serverUri;
        if (!Utils.areStringsEqual(oldServerUri, originalServerUri))
            notifyPropertyChanged("serverUri", oldServerUri, originalServerUri);
    }

    public void setSubprotocols(String newValue) {
        String old = getSubprotocols();
        if (!Utils.areStringsEqual(old, newValue, false, true)) {
            subprotocols = newValue;
            notifyPropertyChanged(SUB_PROTOCOLS_BEAN_PROP, old, newValue);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((login == null) ? 0 : login.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((originalServerUri == null) ? 0 : originalServerUri.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((subprotocols == null) ? 0 : subprotocols.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Connection other = (Connection) obj;
        if (login == null) {
            if (other.login != null)
                return false;
        } else if (!login.equals(other.login))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (originalServerUri == null) {
            if (other.originalServerUri != null)
                return false;
        } else if (!originalServerUri.equals(other.originalServerUri))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (subprotocols == null) {
            if (other.subprotocols != null)
                return false;
        } else if (!subprotocols.equals(other.subprotocols))
            return false;
        return true;
    }

}
