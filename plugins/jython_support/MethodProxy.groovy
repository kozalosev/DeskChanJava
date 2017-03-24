import info.deskchan.core.MessageListener
import info.deskchan.core.PluginProxy
import info.deskchan.core.ResponseListener

import java.nio.charset.Charset
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

    Path getPluginDirPath() {
        return plugin.getPluginDirPath()
    }

    Path getDataDirPath() {
        return pluginProxy.getDataDirPath()
    }

    def log(obj) {
        pluginProxy.log(obj.toString())
    }

    def log(Throwable e) {
        pluginProxy.log(e)
    }

    def say(message) {
        byte[] text = message.toString().getBytes(Charset.forName("ISO_8859_1"))
        String convertedMessage = new String(text)
        sendMessage("DeskChan:say", [text: convertedMessage])
    }
}