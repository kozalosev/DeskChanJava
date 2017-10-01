package info.deskchan.jar_loader

import info.deskchan.core.*
import java.io.File
import java.net.URLClassLoader
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.jar.Attributes
import java.util.jar.JarFile


class Main : Plugin, PluginLoader {

    private lateinit var pluginProxy: PluginProxyInterface

    override fun initialize(pluginProxy: PluginProxyInterface): Boolean {
        this.pluginProxy = pluginProxy
        PluginManager.getInstance().registerPluginLoader(this, "jar")
        return true
    }

    override fun unload() = PluginManager.getInstance().unregisterPluginLoader(this)

    override fun matchPath(path: Path) = when {
        Files.isDirectory(path) -> path.toFile().listFiles { _, name -> name.endsWith(".jar") }.isNotEmpty()
        Files.isRegularFile(path) -> path.fileName.toString().endsWith(".jar")
        else -> false
    }

    override fun loadByPath(path: Path) {
        var correctedPath = path
        if (!path.toString().endsWith(".jar"))
            correctedPath=path.resolveSibling(path.toString() + ".jar")

        val jars = when {
            Files.isDirectory(correctedPath) -> scanDirectory(correctedPath)
            else -> listOf(correctedPath.toFile())
        }
        val urls = jars.map { it.toURI().toURL() }.toTypedArray()
        val loader = URLClassLoader(urls, javaClass.classLoader)
        jars.forEach { loadPlugin(it, loader) }
    }

    private fun loadPlugin(file: File, loader: ClassLoader) {
        val id = file.nameWithoutExtension

        val jar = JarFile(file)
        val manifestAttributes = jar.manifest.mainAttributes ?: Attributes()
        val mainClass = manifestAttributes.getValue("Main-Class")
        val className = mainClass ?: "Main"

        var cls: Class<*>
        try {
            cls = Class.forName(className, true, loader)
        } catch (e: Exception) {
            try {
                cls = Class.forName(className, false, loader)
            } catch (e2: Exception) {
                log("Unable to load plugin \"$id\"! Couldn't find class \"$className\".")
                return
            }
        }

        val plugin = cls.newInstance() as? Plugin
        if (plugin == null) {
            log("The class \"$id\" is not an instance of Plugin!")
            return
        }

        val config = PluginConfig()
        config.append("type", "Jar")
        manifestAttributes.forEach{
            config.append(it.key.toString(), it.value)
        }
        try {
            val resourceFilePath = manifestAttributes.getValue("resources")
            val resourceBundle = extractResources(id, resourceFilePath, loader)

            if (resourceBundle != null)
                config.append("resources", resourceBundle.baseBundleName)

        } catch (e: Exception){ }

        PluginManager.getInstance().initializePlugin(id, plugin, config)
    }

    private fun scanDirectory(path: Path): List<File> {
        val jars = mutableListOf<File>()
        Files.walkFileTree(path, JarFinder(jars, this::log))
        return jars
    }

    private fun extractResources(id: String, resourceFilePath: String?, loader: ClassLoader) = when {
        resourceFilePath != null -> try {
            ResourceBundle.getBundle(resourceFilePath, Locale.getDefault(), loader)
        } catch (e: Exception) {
            log("Couldn't load a resource file of plugin \"$id\"!")
            log(e)
            null
        }
        else -> null
    }

    fun Attributes.groupValues(attribute: String): Set<String> {
        val value = this.getValue(attribute)
        if (value != null) {
            return setOf(value)
        }

        return (1..1000)
                .map { this.getValue("$attribute-$it") }
                .takeWhile { it != null }
                .toSet()
    }

    /*fun Attributes.groupValueByKey(attribute: String): Map<String, String> {
        val value = this.getValue(attribute)
        if (value != null) {
            return mapOf(DEFAULT_LANGUAGE_KEY to value)
        }

        val regexp = "$attribute-(\\S+)".toRegex()
        return this.entries
                .map {
                    val matches = regexp.matchEntire(it.key.toString())
                    val key = matches?.groups?.get(1)?.value
                    if (key != null) {
                        Pair(key, it.value.toString())
                    } else {
                        null
                    }
                }
                .filterNotNull()
                .toMap()
    }*/

    fun log(obj: Any) = when (obj) {
        is Throwable -> pluginProxy.log(obj)
        else -> pluginProxy.log(obj.toString())
    }


    private class JarFinder(val jars: MutableList<File>, val logger: (Any) -> Unit) : SimpleFileVisitor<Path>() {

        override fun visitFile(path: Path?, attributes: BasicFileAttributes?): FileVisitResult {
            if (path != null && attributes != null) {
                if (attributes.isRegularFile && path.toString().endsWith(".jar")) {
                    jars.add(path.toFile())
                }
            } else {
                logger("Couldn't load file \"${path?.fileName.toString()}\"!")
            }
            return FileVisitResult.CONTINUE
        }

    }

}
