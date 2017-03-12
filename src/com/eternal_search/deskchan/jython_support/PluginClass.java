package com.eternal_search.deskchan.jython_support;


import com.eternal_search.deskchan.core.Plugin;
import com.eternal_search.deskchan.core.PluginLoader;
import com.eternal_search.deskchan.core.PluginManager;
import com.eternal_search.deskchan.core.PluginProxy;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.util.PythonInterpreter;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class PluginClass implements Plugin, PluginLoader {
    @Override
    public boolean initialize(PluginProxy proxy) {
        PluginManager.getInstance().registerPluginLoader(this);
        return true;
    }

    @Override
    public void unload() {
        PluginManager.getInstance().unregisterPluginLoader(this);
    }

    @Override
    public boolean matchPath(Path path) {
        if (Files.isDirectory(path))
            return Files.isReadable(path.resolve("plugin.py"));
        else
            return path.getFileName().toString().endsWith(".py");
    }

    @Override
    public void loadByPath(Path path) throws Throwable {
        String id = path.getFileName().toString();
        if (Files.isDirectory(path)) {
            path = path.resolve("plugin.py");
        }
        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.getSystemState().path.append(Py.java2py(path.getParent().toString()));
        PyCode script = interpreter.compile(new FileReader(path.toFile()));
        JythonPlugin plugin = new JythonPlugin(script);
        PluginManager.getInstance().initializePlugin(id, plugin);
    }
}
