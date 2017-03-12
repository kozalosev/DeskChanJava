import com.eternal_search.deskchan.core.MessageListener
import com.eternal_search.deskchan.core.PluginProxy
import com.eternal_search.deskchan.core.ResponseListener

import java.nio.file.Path

class MethodProxy {
    private PluginProxy pluginProxy
    private JythonPlugin plugin

    MethodProxy(PluginProxy pluginProxy, JythonPlugin plugin) {
        this.pluginProxy = pluginProxy
        this.plugin = plugin
    }

    void sendMessage (String tag, Object data) {
        pluginProxy.sendMessage(tag, data)
    }

    void sendMessage(String tag, Object data, ResponseListener responseListener) {
        pluginProxy.sendMessage(tag, data, responseListener)
    }

    void addMessageListener(String tag, MessageListener listener) {
        pluginProxy.addMessageListener(tag, listener)
    }

    void removeMessageListener(String tag, MessageListener listener) {
        pluginProxy.removeMessageListener(tag, listener)
    }

    void addCleanupHandler(Runnable handler) {
        plugin.getCleanupHandlers().add(handler)
    }

    Path getPluginDir() {
        return plugin.getPluginDir()
    }

    Path getDataDir() {
        return pluginProxy.getDataDir()
    }
}