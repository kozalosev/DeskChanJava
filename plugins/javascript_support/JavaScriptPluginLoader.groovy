import info.deskchan.core.PluginLoader
import info.deskchan.core.PluginManager

import java.nio.file.Files
import java.nio.file.Path


class JavaScriptPluginLoader implements PluginLoader {
    private Closure logger

    JavaScriptPluginLoader(Closure logger) {
        this.logger = logger
    }

    @Override
    boolean matchPath(Path path) {
        if (Files.isDirectory(path)) {
            return Files.isReadable(path.resolve("plugin.js"))
        } else {
            return path.getFileName().toString().endsWith(".js")
        }
    }

    @Override
    void loadByPath(Path path) throws Throwable {
        String id = path.getFileName().toString()
        if (Files.isDirectory(path)) {
            path = path.resolve("plugin.js")
        }
        JavaScriptPlugin plugin = new JavaScriptPlugin(path, logger)
        PluginManager.getInstance().initializePlugin(id, plugin)
    }
}
