package com.eternal_search.deskchan.jython_support;

import com.eternal_search.deskchan.core.MessageListener;
import com.eternal_search.deskchan.core.PluginProxy;
import com.eternal_search.deskchan.core.ResponseListener;

import java.util.List;

public class MethodProxy {
    private PluginProxy pluginProxy;
    private List<Runnable> cleanupHandlers;

    MethodProxy(PluginProxy pluginProxy, List<Runnable> cleanupHandlers) {
        this.pluginProxy = pluginProxy;
        this.cleanupHandlers = cleanupHandlers;
    }

    public void sendMessage (String tag, Object data) {
        pluginProxy.sendMessage(tag, data);
    }

    public void sendMessage(String tag, Object data, ResponseListener responseListener) {
        pluginProxy.sendMessage(tag, data, responseListener);
    }

    public void addMessageListener(String tag, MessageListener listener) {
        pluginProxy.addMessageListener(tag, listener);
    }

    public void removeMessageListener(String tag, MessageListener listener) {
        pluginProxy.removeMessageListener(tag, listener);
    }

    public void addCleanupHandler(Runnable handler) {
        cleanupHandlers.add(handler);
    }
}