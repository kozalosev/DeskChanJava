import info.deskchan.core.PluginConfig
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
        def id = path.getFileName().toString()
        def config = new PluginConfig("Jython")
        if (Files.isDirectory(path)) {
            def manifestPath = path.resolve("manifest.json")
            if (Files.exists(manifestPath)) {
                config.appendFromJson(manifestPath)
            }

            path = path.resolve("plugin.py")
        }

        def pythonModulesDirPath = pluginDirPath.resolve("python_modules")
        def plugin = new JythonPlugin(path, pythonModulesDirPath, logger)

        PluginManager.getInstance().initializePlugin(id, plugin, config)
    }
}
