package com.tsystems.readyapi.plugin.websocket;

import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;
import com.eviware.soapui.support.UISupport;

@PluginConfiguration(groupId = "com.tsystems.readyapi.plugin.websocket", name = "Websocket Support Plugin",
        version = "2.0.8", autoDetect = true, description = "Adds Websocket TestSteps to SoapUI",
        infoUrl = "https://github.com/hschott/ready-websocket-plugin/blob/master/README.md")
public class PluginConfig extends PluginAdapter {

    public final static int DEFAULT_TCP_PORT = 80;
    public final static int DEFAULT_SSL_PORT = 443;
    public final static String LOGGER_NAME = "Websocket Plugin";

    public PluginConfig() {
        super();
        UISupport.addResourceClassLoader(getClass().getClassLoader());
    }
}
