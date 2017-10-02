import info.deskchan.core.Plugin
import info.deskchan.core.PluginProxyInterface

import javax.script.Compilable
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import java.nio.charset.StandardCharsets
import java.nio.file.Path


class JavaScriptPlugin implements Plugin {
    private List<Runnable> cleanupHandlers = new ArrayList<>()
    private Path pluginPath
    private PluginProxyInterface pluginProxy
    private Closure logger

    JavaScriptPlugin(Path pluginPath, Closure logger) {
        this.pluginPath = pluginPath
        this.logger = logger
    }

    @Override
    boolean initialize(PluginProxyInterface pluginProxy) {
        logger.call("Trying to load plugin \"${pluginProxy.getId()}\"...")
        this.pluginProxy = pluginProxy

        def scriptEngine = new ScriptEngineManager().getEngineByName("nashorn")
        def compiler = (Compilable) scriptEngine

        def scriptStream = new FileInputStream(pluginPath.toFile())
        def scriptReader = new InputStreamReader(scriptStream, StandardCharsets.UTF_8)
        def script = compiler.compile(scriptReader)

        def bindings = scriptEngine.createBindings()
        bindings.put("bus", new MethodProxy(this))

        try {
            script.eval(bindings)
        } catch (ScriptException e) {
            e.printStackTrace()
            return false
        }
        return true
    }

    @Override
    void unload() {
        for (Runnable runnable : cleanupHandlers) {
            runnable.run()
        }
    }

    Path getPluginDirPath() { pluginPath.getParent() }

    List<Runnable> getCleanupHandlers() { cleanupHandlers }

    PluginProxyInterface getPluginProxy() { pluginProxy }
}