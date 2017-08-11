package info.deskchan.jar_loader

import info.deskchan.core.*
import info.deskchan.core_utils.AuthorParser
import info.deskchan.core_utils.getLocalString

import java.io.File
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult
import java.nio.file.attribute.BasicFileAttributes
import java.net.URLClassLoader
import java.util.*
import java.util.jar.Attributes
import java.util.jar.JarFile


class Main : Plugin, PluginLoader {

    companion object {
        const val MANIFEST_PLUGIN_ATTRIBUTE = "DeskChan-Plugin"
    }

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
        val jars = when {
            Files.isDirectory(path) -> scanDirectory(path)
            else -> listOf(path.toFile())
        }
        val urls = jars.map { it.toURI().toURL() }.toTypedArray()
        val loader = URLClassLoader(urls, javaClass.classLoader)
        jars.forEach { loadPlugin(it, loader) }
    }

    private fun loadPlugin(file: File, loader: ClassLoader) {
        val id = file.name.toString()
        val manifestAttributes = JarFile(file).manifest.mainAttributes ?: Attributes()
        val isPlugin = (manifestAttributes.getValue(MANIFEST_PLUGIN_ATTRIBUTE) ?: "false").toBoolean()
        val mainClass = manifestAttributes.getValue("Main-Class")
        val className = mainClass ?: "Main"

        if (!isPlugin) {
            log("\"$id\" is not a plugin according to the \"$MANIFEST_PLUGIN_ATTRIBUTE\" attribute in the manifest.")
            return
        }

        val cls: Class<*>
        try {
            cls = Class.forName(className, true, loader)
        } catch (e: ClassNotFoundException) {
            log("Unable to load plugin \"$id\"! Couldn't find class \"$className\".")
            return
        }

        val plugin = cls.newInstance() as? Plugin
        if (plugin == null) {
            log("The class \"$id\" is not an instance of Plugin!")
            return
        }

        val manifest = PluginManifest(
                manifestAttributes.getValue("Plugin-Name") ?: id,
                manifestAttributes.getValue("Plugin-Version"),
                getLocalString(manifestAttributes.groupValueByKey("Plugin-Description")),
                manifestAttributes.groupValues("Plugin-Keywords"),
                manifestAttributes.groupValues("Plugin-Author").map { AuthorParser.parse(it) }.toSet(),
                manifestAttributes.getValue("Plugin-License")
        )
        val resourceFilePath = manifestAttributes.getValue("Plugin-Resource-File")
        val resourceBundle = extractResources(id, resourceFilePath, loader)
        val data = when {
            resourceBundle != null -> mapOf("resources" to resourceBundle)
            else -> null
        }
        val config = PluginConfig(file.toPath().parent, loader, data)
        PluginManager.getInstance().initializePlugin(id, plugin, manifest, config)
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

    fun Attributes.groupValueByKey(attribute: String): Map<String, String> {
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
    }

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
