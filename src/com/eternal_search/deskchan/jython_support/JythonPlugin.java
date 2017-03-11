package com.eternal_search.deskchan.jython_support;

import com.eternal_search.deskchan.core.Plugin;
import com.eternal_search.deskchan.core.PluginProxy;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyStringMap;

import java.util.ArrayList;
import java.util.List;

public class JythonPlugin implements Plugin {
    private List<Runnable> cleanupHandlers = new ArrayList<>();
    private PyCode compiledCode;

    public JythonPlugin(PyCode compiledCode) {
        this.compiledCode = compiledCode;
    }

    @Override
    public boolean initialize(PluginProxy pluginProxy) {
        PyStringMap globals = new PyStringMap();
        globals.__setitem__("bus", Py.java2py(new MethodProxy(pluginProxy, cleanupHandlers)));
        try {
            Py.runCode(compiledCode, globals, globals);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void unload() {
        for (Runnable runnable : cleanupHandlers) {
            runnable.run();
        }
    }
}