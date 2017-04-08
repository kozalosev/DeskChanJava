import info.deskchan.core.Plugin
import info.deskchan.core.PluginProxy
import org.python.core.Py
import org.python.core.PyCode
import org.python.core.PyStringMap
import org.python.core.PySystemState
import org.python.util.PythonInterpreter

import java.nio.file.Path


class JythonPlugin implements Plugin {
    private List<Runnable> cleanupHandlers = new ArrayList<>()
    private Path pluginPath
    private Path pythonModulesDirPath
    private PluginProxy pluginProxy

    JythonPlugin(Path pluginPath, Path pythonModulesDirPath) {
        this.pluginPath = pluginPath
        this.pythonModulesDirPath = pythonModulesDirPath
    }

    @Override
    boolean initialize(PluginProxy pluginProxy) {
        this.pluginProxy = pluginProxy

        PythonInterpreter interpreter = new PythonInterpreter()
        PySystemState systemState = interpreter.getSystemState()
        systemState.path.append(Py.java2py(pythonModulesDirPath.toString()))
        systemState.path.append(Py.java2py(pluginPath.getParent().toString()))
        systemState.path.append(Py.java2py(pluginPath.getParent().resolve("__dependencies__").toString()))
        PyCode script = interpreter.compile(new FileReader(pluginPath.toFile()))

        MethodProxy methodProxy = new MethodProxy(this)
        systemState.builtins.__setitem__("bus", Py.java2py(methodProxy))

        PyStringMap globals = new PyStringMap()
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

    PluginProxy getPluginProxy() {
        return pluginProxy
    }
}