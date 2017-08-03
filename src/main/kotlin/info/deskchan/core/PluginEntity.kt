package info.deskchan.core

import org.apache.commons.io.FilenameUtils.removeExtension
import java.nio.file.Path


class PluginConfig(val directory: Path, loader: ClassLoader? = null, val data: Map<String, Any>? = null) {
    val loader: ClassLoader = loader ?: this::class.java.classLoader
}


class PluginEntity(private val pluginProxy: PluginProxyInterface, val name: String, manifest: PluginManifest? = null, config: PluginConfig? = null)
    : PluginProxyInterface by pluginProxy {

    constructor(plugin: Plugin, id: String, manifest: PluginManifest?, config: PluginConfig?) : this(PluginProxy(plugin), id, manifest, config)

    override fun getId() = pluginProxy.id ?: name

    val manifest: PluginManifest = manifest ?: PluginManifest(id)
    val config: PluginConfig = config ?: PluginConfig(PluginManager.getDefaultPluginDirPath(id))


    fun isNameMatched(name: String) = name == id || name == manifest.name || name == removeExtension(id) || removeExtension(name) == id

}
