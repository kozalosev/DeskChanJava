@Grab('org.python:jython-standalone:2.7.1')

import info.deskchan.core.Plugin
import info.deskchan.core.PluginProxyInterface
import org.python.core.Py
import org.python.core.PyCode
import org.python.core.PyStringMap
import org.python.util.PythonInterpreter

import java.nio.charset.Charset
import java.nio.file.Path


class JythonPlugin implements Plugin {
    private List<Runnable> cleanupHandlers = new ArrayList<>()
    private Path pluginPath
    private Path pythonModulesDirPath
    private PluginProxyInterface pluginProxy
    private Closure logger

    JythonPlugin(Path pluginPath, Path pythonModulesDirPath, Closure logger) {
        this.pluginPath = pluginPath
        this.pythonModulesDirPath = pythonModulesDirPath
        this.logger = logger
    }

    @Override
    boolean initialize(PluginProxyInterface pluginProxy) {
        logger.call("Trying to load plugin \"${pluginProxy.getId()}\"...")
        this.pluginProxy = pluginProxy

        def interpreter = new PythonInterpreter()
        def systemState = interpreter.getSystemState()
        systemState.path.append(Py.java2py(pythonModulesDirPath.toString()))
        systemState.path.append(Py.java2py(pluginPath.getParent().toString()))
        systemState.path.append(Py.java2py(pluginPath.getParent().resolve("__dependencies__").toString()))

        def scriptStream = new FileInputStream(pluginPath.toFile())
        def scriptReader = new InputStreamReader(scriptStream, Charset.forName("UTF-8"))
        PyCode script = interpreter.compile(scriptReader)

        def methodProxy = new MethodProxy(this)
        def globals = new PyStringMap()
        globals.__setitem__("bus", Py.java2py(methodProxy))
        try {
            Py.runCode(script, globals, globals)
        }
        catch (Exception e) {
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

    PluginProxyInterface getPluginProxy() {
        return pluginProxy
    }
}