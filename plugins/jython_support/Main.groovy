@Grab('org.python:jython-standalone:2.7.1b3')

import info.deskchan.core.Plugin
import info.deskchan.core.PluginLoader
import info.deskchan.core.PluginManager
import info.deskchan.core.PluginProxy

import java.nio.file.Files
import java.nio.file.Path


class Main implements Plugin, PluginLoader {
    private Path pluginDirPath

    Main(Path pluginDirPath) {
        this.pluginDirPath = pluginDirPath
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

        Path pythonModulesDirPath = pluginDirPath.resolve("python_modules")
        JythonPlugin plugin = new JythonPlugin(path, pythonModulesDirPath)

        PluginManager.getInstance().initializePlugin(id, plugin)
    }
}