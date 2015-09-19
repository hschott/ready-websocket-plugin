package com.tsystems.readyapi.plugin.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class ExpandedConnectionParams {
    private String originalServerUri;
    public String fixedId;
    public String subprotocols;
    public String login;
    public String password;

    public ExpandedConnectionParams() {
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExpandedConnectionParams other = (ExpandedConnectionParams) obj;
        if (login == null) {
            if (other.login != null)
                return false;
        } else if (!login.equals(other.login))
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

    public String getActualServerUri() throws URISyntaxException {
        URI uri = new URI(originalServerUri);
        if (uri.getAuthority() == null)
            uri = new URI("ws://" + originalServerUri);
        if (uri.getPort() == -1)
            if ("ws".equals(uri.getScheme().toLowerCase(Locale.ENGLISH)))
                uri = new URI("ws", uri.getUserInfo(), uri.getHost(), PluginConfig.DEFAULT_TCP_PORT, uri.getPath(),
                        uri.getQuery(), uri.getFragment());
            else if ("wss".equals(uri.getScheme().toLowerCase(Locale.ENGLISH)))
                uri = new URI("wss", uri.getUserInfo(), uri.getHost(), PluginConfig.DEFAULT_SSL_PORT, uri.getPath(),
                        uri.getQuery(), uri.getFragment());
        return uri.toString();
    }

    public String getNormalizedServerUri() throws URISyntaxException {
        return getActualServerUri().toLowerCase(Locale.ENGLISH);
    }

    public String getServerUri() {
        return originalServerUri;
    }

    public boolean hasCredentials() {
        return login != null && !"".equals(login);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (login == null ? 0 : login.hashCode());
        result = prime * result + (originalServerUri == null ? 0 : originalServerUri.hashCode());
        result = prime * result + (password == null ? 0 : password.hashCode());
        result = prime * result + (subprotocols == null ? 0 : subprotocols.hashCode());
        return result;
    }

    public boolean hasSubprotocols() {
        return subprotocols != null && subprotocols.trim().length() > 0;
    }

    public void setCredentials(String login, String password) {
        if (login == null || login.length() == 0) {
            this.login = login;
            this.password = null;
        } else {
            this.login = login;
            this.password = password;
        }
    }

    public void setServerUri(String value) {
        originalServerUri = value;
    }

}
