import info.deskchan.core.PluginLoader
import info.deskchan.core.PluginManager

import java.nio.file.Files
import java.nio.file.Path


class JythonPluginLoader implements PluginLoader {
    private Path pluginDirPath
    private Closure logger

    JythonPluginLoader(Path pluginDirPath, Closure logger) {
        this.pluginDirPath = pluginDirPath
        this.logger = logger
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
        logger.call("Trying to load plugin \"$id\"...")
        JythonPlugin plugin = new JythonPlugin(path, pythonModulesDirPath)

        PluginManager.getInstance().initializePlugin(id, plugin)
    }
}
