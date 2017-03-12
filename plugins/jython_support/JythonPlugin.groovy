import com.eternal_search.deskchan.core.Plugin
import com.eternal_search.deskchan.core.PluginProxy
import org.python.core.Py
import org.python.core.PyCode
import org.python.core.PyStringMap

class JythonPlugin implements Plugin {
    private List<Runnable> cleanupHandlers = new ArrayList<>()
    private PyCode compiledCode

    JythonPlugin(PyCode compiledCode) {
        this.compiledCode = compiledCode
    }

    @Override
    boolean initialize(PluginProxy pluginProxy) {
        PyStringMap globals = new PyStringMap()
        globals.__setitem__("bus", Py.java2py(new MethodProxy(pluginProxy, cleanupHandlers)))
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
}