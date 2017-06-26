package info.deskchan.jar_loader

import info.deskchan.core.Plugin
import info.deskchan.core.PluginLoader
import info.deskchan.core.PluginManager
import info.deskchan.core.PluginProxy

import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarFile


class Main : Plugin, PluginLoader {

    private var pluginProxy: PluginProxy? = null

    override fun initialize(pluginProxy: PluginProxy): Boolean {
        this.pluginProxy = pluginProxy
        PluginManager.getInstance().registerPluginLoader(this)
        return true
    }

    override fun unload() {
        PluginManager.getInstance().unregisterPluginLoader(this)
    }

    override fun matchPath(path: Path): Boolean {
        return path.fileName.toString().endsWith(".jar")
    }

    @Throws(Throwable::class)
    override fun loadByPath(path: Path) {
        val id = path.fileName.toString()
        val manifest = JarFile(path.toFile()).manifest
        val mainClass = manifest.mainAttributes.getValue("Main-Class")
        val className = mainClass ?: "Main"

        val urls = arrayOf(path.toUri().toURL())
        val loader = URLClassLoader(urls, javaClass.classLoader)
        val cls: Class<*>
        try {
            cls = Class.forName(className, true, loader)
        } catch (e: ClassNotFoundException) {
            pluginProxy?.log("Unable to load plugin \"%s\"! Couldn't find class \"%s\".".format(id, className))
            return
        }
        val plugin = cls.newInstance() as Plugin
        PluginManager.getInstance().initializePlugin(id, plugin)
    }

}
