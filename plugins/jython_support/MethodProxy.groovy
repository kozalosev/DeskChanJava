import com.eternal_search.deskchan.core.MessageListener
import com.eternal_search.deskchan.core.PluginProxy
import com.eternal_search.deskchan.core.ResponseListener

class MethodProxy {
    private PluginProxy pluginProxy
    private List<Runnable> cleanupHandlers

    MethodProxy(PluginProxy pluginProxy, List<Runnable> cleanupHandlers) {
        this.pluginProxy = pluginProxy
        this.cleanupHandlers = cleanupHandlers
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
        cleanupHandlers.add(handler)
    }
}