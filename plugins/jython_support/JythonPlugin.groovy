import info.deskchan.core.Plugin
import info.deskchan.core.PluginProxy
import org.python.core.Py
import org.python.core.PyCode
import org.python.core.PyStringMap

import java.nio.file.Path

class JythonPlugin implements Plugin {
    private List<Runnable> cleanupHandlers = new ArrayList<>()
    private PyCode compiledCode
    private Path pluginDirPath

    JythonPlugin(PyCode compiledCode) {
        this.compiledCode = compiledCode
    }

    @Override
    boolean initialize(PluginProxy pluginProxy) {
        if (pluginDirPath == null)
            throw new NullPointerException("Plugin directory is not set!")

        PyStringMap globals = new PyStringMap()
        globals.__setitem__("bus", Py.java2py(new MethodProxy(pluginProxy, this)))
        try {
            Py.runCode(compiledCode, globals, globals)
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

    void setPluginDirPath(Path path) {
        if (pluginDirPath == null)
            pluginDirPath = path
        else
            System.err.println("Attempt to change plugin directory!")
    }

    Path getPluginDirPath() {
        return pluginDirPath
    }

    List<Runnable> getCleanupHandlers() {
        return cleanupHandlers
    }
}