package com.tsystems.readyapi.plugin.websocket;

import com.eviware.soapui.model.support.TestRunListenerAdapter;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.plugins.ListenerConfiguration;

@ListenerConfiguration
public class CacheCleanUpListener extends TestRunListenerAdapter {

    @Override
    public void afterRun(TestCaseRunner testRunner, TestCaseRunContext runContext) {
        ClientCache.getCache(runContext).assureFinalized();
    }
}