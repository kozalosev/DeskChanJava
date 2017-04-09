import info.deskchan.core.MessageListener
import info.deskchan.core.ResponseListener

import java.nio.charset.Charset


class MethodProxy {
    private JythonPlugin plugin

    MethodProxy(JythonPlugin plugin) {
        this.plugin = plugin
    }

    String getId() {
        return plugin.getPluginProxy().getId()
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

    String getPluginDirPath() {
        return plugin.getPluginDirPath().toString()
    }

    String getDataDirPath() {
        return plugin.getPluginProxy().getDataDirPath().toString()
    }

    def log(obj) {
        plugin.getPluginProxy().log(obj.toString())
    }

    def log(Throwable e) {
        plugin.getPluginProxy().log(e)
    }

    def say(text, Map<String, Object> parameters) {
        if (text == null)
            return
        if (parameters == null)
            parameters = new HashMap<>()

        byte[] bytes = text.toString().getBytes(Charset.defaultCharset())
        String converted = new String(bytes, Charset.forName("UTF-8"))

        parameters.put("text", converted)
        sendMessage("DeskChan:say", parameters)
    }

    def say(text) {
        say(text, null)
    }
}