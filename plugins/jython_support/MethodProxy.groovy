import info.deskchan.core.MessageListener
import info.deskchan.core.ResponseListener

import java.nio.charset.Charset
import java.nio.file.Path

class MethodProxy {
    private JythonPlugin plugin

    MethodProxy(JythonPlugin plugin) {
        this.plugin = plugin
    }

    void sendMessage (String tag, Object data) {
        plugin.getPluginProxy().sendMessage(tag, data)
    }

    void sendMessage(String tag, Object data, ResponseListener responseListener) {
        plugin.getPluginProxy().sendMessage(tag, data, responseListener)
    }

    void addMessageListener(String tag, MessageListener listener) {
        plugin.getPluginProxy().addMessageListener(tag, listener)
    }

    void removeMessageListener(String tag, MessageListener listener) {
        plugin.getPluginProxy().removeMessageListener(tag, listener)
    }

    void addCleanupHandler(Runnable handler) {
        plugin.getCleanupHandlers().add(handler)
    }

    Path getPluginDirPath() {
        return plugin.getPluginDirPath()
    }

    Path getDataDirPath() {
        return plugin.getPluginProxy().getDataDirPath()
    }

    def log(obj) {
        plugin.getPluginProxy().log(obj.toString())
    }

    def log(Throwable e) {
        plugin.getPluginProxy().log(e)
    }

    def say(String message) {
        byte[] bytes = message.getBytes(Charset.defaultCharset())
        String converted = new String(bytes, Charset.forName("UTF-8"))
        sendMessage("DeskChan:say", [ text: converted ])
    }
}


interface MethodProxyGetter {
    MethodProxy get()
}