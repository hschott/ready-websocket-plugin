package com.tsystems.readyapi.plugin.websocket;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class ClientCache {
    private static final int FIRST_UNTITLED_CLIENT = 0;

    private static class CacheKey {
        public ExpandedConnectionParams params;
        public int clientId;

        public CacheKey(ExpandedConnectionParams params, int clientId) {
            this.params = params;
            this.clientId = clientId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + clientId;
            result = prime * result + (params == null ? 0 : params.hashCode());
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
            CacheKey other = (CacheKey) obj;
            if (clientId != other.clientId)
                return false;
            if (params == null) {
                if (other.params != null)
                    return false;
            } else if (!params.equals(other.params))
                return false;
            return true;
        }

    }

    public Client get(String connectionName) {
        return map.get(connectionName);
    }

    public Client add(String connectionName, ExpandedConnectionParams params) throws Exception {
        Client result = get(connectionName);
        if (result == null)
            result = register(connectionName, params);
        return result;
    }

    private Client register(String connectionName, ExpandedConnectionParams connectionParams) throws Exception {
        WebSocketClient clientObj = new WebSocketClient();
        clientObj.start();
        Client newClient = new Client(clientObj, createClientUpgradeRequest(connectionParams));
        map.put(connectionName, newClient);
        return newClient;
    }

    private ClientUpgradeRequest createClientUpgradeRequest(ExpandedConnectionParams connectionParams) throws Exception {
        ClientUpgradeRequest upgradeRequest;
        if (connectionParams == null)
            upgradeRequest = getDefaultClientUpgradeRequest();
        else {
            upgradeRequest = new ClientUpgradeRequest();
            if (connectionParams.hasCredentials()) {
                String basicAuthHeader = new String(
                        Base64.encodeBase64((connectionParams.login + ":" + connectionParams.password).getBytes()));
                upgradeRequest.setHeader("Authorization", "Basic " + basicAuthHeader);
            }
            upgradeRequest.setRequestURI(new URI(connectionParams.getNormalizedServerUri()));
        }
        return upgradeRequest;
    }

    private ClientUpgradeRequest getDefaultClientUpgradeRequest() {
        ClientUpgradeRequest upgradeRequest = new ClientUpgradeRequest();
        return upgradeRequest;
    }

    public void assureFinalized() {
        for (Client client : map.values())
            client.dispose();
        map.clear();
    }

    private Map<String, Client> map = new ConcurrentHashMap<String, Client>();

}
