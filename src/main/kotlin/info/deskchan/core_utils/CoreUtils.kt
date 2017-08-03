package info.deskchan.core_utils

import info.deskchan.core.*
import info.deskchan.core_utils.Main.log
import org.apache.commons.io.IOUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path


private const val TEMPLATE_INVALID_PROPERTY = "Manifest of plugin \"%s\" has invalid property: %s"

private fun readManifestJsonFile(path: Path): JSONObject? {
    if (!Files.isReadable(path) || Files.isDirectory(path)) {
        return null
    }

    var manifestStr: String = ""
    try {
        Files.newInputStream(path).use { manifestInputStream ->
            manifestStr = IOUtils.toString(manifestInputStream, "UTF-8")
        }
    } catch (e: IOException) {
        log("Couldn't read file: $path")
        log(e)
    }

    return try {
        JSONObject(manifestStr)
    } catch (e: JSONException) {
        log("Invalid manifest file: $path")
        log(e)
        null
    }
}

private fun parseManifest(name: String, manifest: JSONObject): Manifest {
    val map = mutableMapOf<String, Any?>("name" to name)
    listOf("name", "version", "description", "keywords", "license").forEach {
        if (manifest.has(it)) {
            try {
                map[it] = manifest.getString(it)
            } catch (e: JSONException) {
                log(TEMPLATE_INVALID_PROPERTY.format(name, it))
                log(e)
            }
        }
    }
    if (manifest.has("authors")) {
        try {
            val authors = manifest.getJSONArray("authors")
            map["authors"] = authors
                .mapIndexed { i, _ -> authors.getJSONObject(i) }
                .filter { it.has("name") }
                .map {
                        val email = if (it.has("email")) it.getString("email") else null
                        val website = if (it.has("website")) it.getString("website") else null
                        Author(
                                it.getString("name"),
                                email,
                                website
                        )
                }
        } catch (e: JSONException) {
            log(TEMPLATE_INVALID_PROPERTY.format(name, "authors"))
            log(e)
        }
    }
    return Manifest(map)
}

fun parsePluginManifest(id: String, path: Path): PluginManifest {
    val json = readManifestJsonFile(path) ?: return PluginManifest(id)

    val dependencies = mutableListOf<String>()
    listOf("deps", "dependencies").forEach {
        if (json.has(it)) {
            try {
                val deps = json.getJSONArray(it)
                dependencies.addAll(deps.filter { it is String }.map { it.toString() })
            } catch (e: JSONException) {
                log(TEMPLATE_INVALID_PROPERTY.format(id, it))
                log(e)
            }
        }
    }

    val repositories = mutableListOf<String>()
    listOf("reps", "repositories").forEach {
        if (json.has(it)) {
            try {
                val reps = json.getJSONArray(it)
                repositories.addAll(reps.filter { it is String }.map { it.toString() })
            } catch (e: JSONException) {
                log(TEMPLATE_INVALID_PROPERTY.format(id, it))
                log(e)
            }
        }
    }

    val platform: Platform = if (json.has("platform")) {
        try {
            when (json.getString("platform").toLowerCase()) {
                "mobile" -> Platform.MOBILE
                "windows" -> Platform.WINDOWS
                "linux" -> Platform.LINUX
                "mac" -> Platform.MAC
                "android" -> Platform.ANDROID
                "ios" -> Platform.IOS
                else -> DEFAULT_PLATFORM
            }
        } catch (e: JSONException) {
            log(TEMPLATE_INVALID_PROPERTY.format(id, "platform"))
            log(e)
            DEFAULT_PLATFORM
        }
    } else {
        DEFAULT_PLATFORM
    }

    val manifest = parseManifest(id, json)
    return PluginManifest.fromManifest(manifest, dependencies.toSet(), repositories.toSet(), platform)
}


object AuthorParser {
    enum class Part { NAME, EMAIL, WEBSITE, PART_ENDED }

    fun parse(authorStr: String): Author {
        var part = Part.NAME
        val name = StringBuilder()
        val email = StringBuilder()
        val website = StringBuilder()
        for (char in authorStr) {
            when {
                char == '<'                -> part = Part.EMAIL
                char == '>' || char == ')' -> part = Part.PART_ENDED
                char == '('                -> part = Part.WEBSITE
                part == Part.NAME          -> name.append(char)
                part == Part.EMAIL         -> email.append(char)
                part == Part.WEBSITE       -> website.append(char)
            }
        }

        val (nameStr, emailStr, websiteStr) = listOf(name, email, website).map {
            val s = it.toString().trimEnd()
            if (s.isEmpty()) {
                null
            } else {
                s
            }
        }
        return if (nameStr != null) {
            Author(nameStr, emailStr, websiteStr)
        } else {
            UNKNOWN_AUTHOR
        }
    }
}


// TODO: Implement dependencies resolving from remote repositories.
// TODO: Or implement a way to delegate resolving to another plugin.
fun resolveDependencies(plugin: PluginProxyInterface, manifest: PluginManifest?) = manifest?.dependencies
        ?.map { val r = PluginManager.getInstance().tryLoadPluginByName(it)
            if (!r) log("Failed to load dependency $it of plugin ${plugin.id}")
            r
        }
        ?.all { it }    // true if all plugins were loaded
        ?: true         // or if there is no dependencies to load

fun resolveDependencies(plugin: PluginEntity) = resolveDependencies(plugin, plugin.manifest)
