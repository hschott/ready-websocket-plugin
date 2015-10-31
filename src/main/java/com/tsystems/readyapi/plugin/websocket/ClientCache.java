package com.tsystems.readyapi.plugin.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.testsuite.TestRunContext;

public class ClientCache {
    private static final String CLIENT_CACHE_PROPNAME = "client_cache";

    private Map<String, Client> map = new ConcurrentHashMap<String, Client>();

    public Client add(String connectionName, ExpandedConnectionParams params) throws Exception {
        Client result = get(connectionName);
        if (result == null)
            result = register(connectionName, params);
        return result;
    }

    public static void assureFinalized(PropertyExpansionContext testRunContext) {
        lazyCache(testRunContext).assureFinalized();
    }

    private void assureFinalized() {
        for (Client client : map.values())
            client.dispose();
        map.clear();
    }

    public Client get(String connectionName) {
        return map.get(connectionName);
    }

    private Client register(String connectionName, ExpandedConnectionParams connectionParams) throws Exception {
        Client newClient = new TyrusClient(connectionParams);
        map.put(connectionName, newClient);
        return newClient;
    }

    public static ClientCache getCache(PropertyExpansionContext testRunContext) {

        PropertyExpansionContext cacheContext = testRunContext.hasProperty(TestRunContext.LOAD_TEST_CONTEXT) ? (PropertyExpansionContext) testRunContext
                .getProperty(TestRunContext.LOAD_TEST_CONTEXT) : testRunContext;

        return lazyCache(cacheContext);
    }

    private static ClientCache lazyCache(PropertyExpansionContext cacheContext) {
        ClientCache cache;
        synchronized (cacheContext) {
            cache = (ClientCache) cacheContext.getProperty(CLIENT_CACHE_PROPNAME);
            if (cache == null) {
                cache = new ClientCache();
                cacheContext.setProperty(CLIENT_CACHE_PROPNAME, cache);
            }
        }
        return cache;
    }
}
