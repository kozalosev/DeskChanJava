package info.deskchan.jar_loader

import info.deskchan.core.Plugin
import info.deskchan.core.PluginLoader
import info.deskchan.core.PluginManager
import info.deskchan.core.PluginProxyInterface

import java.io.File
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult
import java.nio.file.attribute.BasicFileAttributes
import java.net.URLClassLoader
import java.util.jar.JarFile


class Main : Plugin, PluginLoader {

    private lateinit var pluginProxy: PluginProxyInterface

    override fun initialize(pluginProxy: PluginProxyInterface): Boolean {
        this.pluginProxy = pluginProxy
        PluginManager.getInstance().registerPluginLoader(this)
        return true
    }

    override fun unload() = PluginManager.getInstance().unregisterPluginLoader(this)

    override fun matchPath(path: Path) = when {
        Files.isDirectory(path) -> path.toFile().listFiles { _, name -> name.endsWith(".jar") }.isNotEmpty()
        Files.isRegularFile(path) -> path.fileName.toString().endsWith(".jar")
        else -> false
    }

    override fun loadByPath(path: Path) {
        val jars = when {
            Files.isDirectory(path) -> scanDirectory(path)
            else -> listOf(path.toFile())
        }
        val urls = jars.map { it.toURI().toURL() }.toTypedArray()
        val loader = URLClassLoader(urls, javaClass.classLoader)
        jars.forEach { loadPlugin(it, loader) }
    }

    private fun loadPlugin(file: File, loader: ClassLoader) {
        val manifest = JarFile(file).manifest
        val isPlugin = manifest.entries.containsKey("DeskChan-Plugin")

        if (!isPlugin) {
            return
        }

        val id = file.name.toString()
        val mainClass = manifest.mainAttributes.getValue("Main-Class")
        val className = mainClass ?: "Main"

        val cls: Class<*>
        try {
            cls = Class.forName(className, true, loader)
        } catch (e: ClassNotFoundException) {
            log("Unable to load plugin \"$id\"! Couldn't find class \"$className\".")
            return
        }

        val plugin = cls.newInstance() as? Plugin
        if (plugin != null) {
            PluginManager.getInstance().initializePlugin(id, plugin)
        } else {
            log("The class \"$id\" is not an instance of Plugin!")
        }
    }

    private fun scanDirectory(path: Path): List<File> {
        val jars = mutableListOf<File>()
        Files.walkFileTree(path, JarFinder(jars, this::log))
        return jars
    }

    fun log(obj: Any) = when (obj) {
        is Throwable -> pluginProxy.log(obj)
        else -> pluginProxy.log(obj.toString())
    }


    private class JarFinder(val jars: MutableList<File>, val logger: (Any) -> Unit) : SimpleFileVisitor<Path>() {

        override fun visitFile(path: Path?, attributes: BasicFileAttributes?): FileVisitResult {
            if (path != null && attributes != null) {
                if (attributes.isRegularFile && path.endsWith(".jar")) {
                    jars.add(path.toFile())
                }
            } else {
                logger("Couldn't load file \"${path?.fileName.toString()}\"!")
            }
            return FileVisitResult.CONTINUE
        }

    }

}
