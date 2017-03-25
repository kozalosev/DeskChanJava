@Grab('org.python:jython-standalone:2.7.1b3')

import info.deskchan.core.Plugin
import info.deskchan.core.PluginLoader
import info.deskchan.core.PluginManager
import info.deskchan.core.PluginProxy
import org.python.core.Py
import org.python.core.PyCode
import org.python.core.PySystemState
import org.python.util.PythonInterpreter

import java.nio.file.Files
import java.nio.file.Path

class Main implements Plugin, PluginLoader {
    private Path pluginDirPath

    Main(Path pluginDirPah) {
        this.pluginDirPath = pluginDirPah
    }

    @Override
    boolean initialize(PluginProxy proxy) {
        PluginManager.getInstance().registerPluginLoader(this)
        return true
    }

    boolean initialize() {
        return initialize(null)
    }

    @Override
    void unload() {
        PluginManager.getInstance().unregisterPluginLoader(this)
    }

    @Override
    boolean matchPath(Path path) {
        if (Files.isDirectory(path))
            return Files.isReadable(path.resolve("plugin.py"))
        else
            return path.getFileName().toString().endsWith(".py")
    }

    @Override
    void loadByPath(Path path) throws Throwable {
        String id = path.getFileName().toString()
        if (Files.isDirectory(path)) {
            path = path.resolve("plugin.py")
        }
        PythonInterpreter interpreter = new PythonInterpreter()
        PySystemState systemState = interpreter.getSystemState()
        systemState.path.append(Py.java2py(pluginDirPath.toString()))
        systemState.path.append(Py.java2py(path.getParent().toString()))
        systemState.path.append(Py.java2py(path.getParent().resolve("__dependencies__").toString()))
        PyCode script = interpreter.compile(new FileReader(path.toFile()))

        // To make the bus work globally, I had to inject it in both ways: as a built-in and a global variable for a plugin.
        MethodProxy methodProxy
        JythonPlugin plugin = new JythonPlugin(script, { -> methodProxy })
        plugin.setPluginDirPath(path.getParent())
        methodProxy = new MethodProxy(plugin)
        systemState.builtins.__setitem__("bus", Py.java2py(methodProxy))

        PluginManager.getInstance().initializePlugin(id, plugin)
    }
}