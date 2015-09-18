package com.tsystems.readyapi.plugin.websocket;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;

public class ClientCache {
    private Map<String, Client> map = new ConcurrentHashMap<String, Client>();

    public Client add(String connectionName, ExpandedConnectionParams params) throws Exception {
        Client result = get(connectionName);
        if (result == null)
            result = register(connectionName, params);
        return result;
    }

    public void assureFinalized() {
        for (Client client : map.values())
            client.dispose();
        map.clear();
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

    public Client get(String connectionName) {
        return map.get(connectionName);
    }

    private ClientUpgradeRequest getDefaultClientUpgradeRequest() {
        ClientUpgradeRequest upgradeRequest = new ClientUpgradeRequest();
        return upgradeRequest;
    }

    private Client register(String connectionName, ExpandedConnectionParams connectionParams) throws Exception {
        WebSocketClient clientObj = new WebSocketClient();
        clientObj.start();
        Client newClient = new Client(clientObj, createClientUpgradeRequest(connectionParams));
        map.put(connectionName, newClient);
        return newClient;
    }

    static ClientCache getCache(PropertyExpansionContext testRunContext) {
        final String CLIENT_CACHE_PROPNAME = "client_cache";
        ClientCache cache = (ClientCache) testRunContext.getProperty(CLIENT_CACHE_PROPNAME);
        if (cache == null) {
            cache = new ClientCache();
            testRunContext.setProperty(CLIENT_CACHE_PROPNAME, cache);
        }
        return cache;
    }

}
