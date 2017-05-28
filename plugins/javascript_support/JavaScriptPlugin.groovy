import info.deskchan.core.Plugin
import info.deskchan.core.PluginProxy

import javax.script.Bindings
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import java.nio.charset.StandardCharsets
import java.nio.file.Path


class JavaScriptPlugin implements Plugin {
    private List<Runnable> cleanupHandlers = new ArrayList<>()
    private Path pluginPath
    private PluginProxy pluginProxy
    private Closure logger

    JavaScriptPlugin(Path pluginPath, Closure logger) {
        this.pluginPath = pluginPath
        this.logger = logger
    }

    @Override
    boolean initialize(PluginProxy pluginProxy) {
        logger.call("Trying to load plugin \"${pluginProxy.getId()}\"...")
        this.pluginProxy = pluginProxy

        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("nashorn")
        Compilable compiler = (Compilable) scriptEngine

        InputStream scriptStream = new FileInputStream(pluginPath.toFile())
        InputStreamReader scriptReader = new InputStreamReader(scriptStream, StandardCharsets.UTF_8)
        CompiledScript script = compiler.compile(scriptReader)

        Bindings bindings = scriptEngine.createBindings()
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

    Path getPluginDirPath() {
        return pluginPath.getParent()
    }

    List<Runnable> getCleanupHandlers() {
        return cleanupHandlers
    }

    PluginProxy getPluginProxy() {
        return pluginProxy
    }
}