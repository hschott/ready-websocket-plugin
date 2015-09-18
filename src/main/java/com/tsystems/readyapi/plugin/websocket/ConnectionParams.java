package com.tsystems.readyapi.plugin.websocket;

public class ConnectionParams {
    public String serverUri;
    public String login;
    public String password;
    public String subprotocols;

    public ConnectionParams() {
    }

    public ConnectionParams(String serverUri, String login, String password, String subprotocols) {
        this.serverUri = serverUri;
        this.login = login;
        this.password = password;
        this.subprotocols = subprotocols;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConnectionParams other = (ConnectionParams) obj;
        if (login == null) {
            if (other.login != null)
                return false;
        } else if (!login.equals(other.login))
            return false;
        if (serverUri == null) {
            if (other.serverUri != null)
                return false;
        } else if (!serverUri.equals(other.serverUri))
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

    public boolean hasCredentials() {
        return login != null && !"".equals(login);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (login == null ? 0 : login.hashCode());
        result = prime * result + (serverUri == null ? 0 : serverUri.hashCode());
        result = prime * result + (password == null ? 0 : password.hashCode());
        result = prime * result + (subprotocols == null ? 0 : subprotocols.hashCode());
        return result;
    }

    public void setCredentials(String login, String password) {
        if (login == null || login.length() == 0) {
            this.login = login;
            password = null;
        } else {
            this.login = login;
            this.password = password;
        }
    }

}
