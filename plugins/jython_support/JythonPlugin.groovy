import com.eternal_search.deskchan.core.Plugin
import com.eternal_search.deskchan.core.PluginProxy
import org.python.core.Py
import org.python.core.PyCode
import org.python.core.PyStringMap

import java.nio.file.Path

class JythonPlugin implements Plugin {
    private List<Runnable> cleanupHandlers = new ArrayList<>()
    private PyCode compiledCode
    private Path pluginDir

    JythonPlugin(PyCode compiledCode) {
        this.compiledCode = compiledCode
    }

    @Override
    boolean initialize(PluginProxy pluginProxy) {
        if (pluginDir == null)
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

    void setPluginDir(Path path) {
        if (pluginDir == null)
            pluginDir = path
        else
            System.err.println("Attempt to change plugin directory!")
    }

    List<Runnable> getCleanupHandlers() {
        return cleanupHandlers
    }

    Path getPluginDir() {
        return pluginDir
    }
}