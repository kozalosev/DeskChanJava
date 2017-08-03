package info.deskchan.core


data class Author(val name: String, val email: String? = null, val website: String? = null)
val UNKNOWN_AUTHOR = Author("Unknown")
val UNKNOWN_AUTHOR_SET = setOf(UNKNOWN_AUTHOR)

enum class Platform { ALL, DESKTOP, MOBILE, WINDOWS, LINUX, MAC, ANDROID, IOS }
val DEFAULT_PLATFORM = Platform.DESKTOP   // TODO: change to ALL before the final release version

open class LocalizedManifestStrings(val name: String, val version: String, val description: String,
                                    val authors: String, val license: String)
val MANIFEST_STRINGS_ENGLISH = LocalizedManifestStrings("Name", "Version", "Description", "Authors", "License")


open class Manifest {

    val name: String
    val version: String?
    val description: String?    // TODO: make this field localizable
    val keywords: Set<String>
    val authors: Set<Author>
    val license: String?

    constructor(name: String, version: String? = null, description: String? = null, keywords: Set<String> = emptySet(),
                authors: Set<Author> = UNKNOWN_AUTHOR_SET, license: String? = null) {
        this.name = name
        this.version = version
        this.description = description
        this.keywords = keywords
        this.authors = authors
        this.license = license
    }

    constructor(map: Map<String, Any?>) {
        name = map["name"] as String
        version = map.getOrDefault("version", null) as String?
        description = map.getOrDefault("description", null) as String?
        license = map.getOrDefault("license", null) as String?

        keywords = (map.getOrDefault("keywords", emptySet<String>()) as Collection<*>)
                .map { it.toString() }.toSet()
        authors = (map.getOrDefault("authors", UNKNOWN_AUTHOR_SET) as Collection<*>)
                .filter { it is Author }.map { it as Author }.toSet()
    }

    override fun toString() = toString(MANIFEST_STRINGS_ENGLISH)

    fun toString(labels: LocalizedManifestStrings): String {
        val builder = StringBuilder()
        mapOf(labels.name to name, labels.version to version, labels.license to license).forEach { title, value ->
            if (value != null) {
                builder.append("$title: $value\n")
            }
        }
        if (description != null) {
            builder.append("\n${labels.description}: $description\n")
        }
        if (authors != UNKNOWN_AUTHOR_SET) {
            builder.append("\n${labels.authors}:\n")
            authors.forEach {
                builder.append("- ${it.name}")
                if (it.email != null) {
                    builder.append(" <${it.email}>")
                }
                if (it.website != null) {
                    builder.append(" (${it.website})")
                }
                builder.append("\n")
            }
        }
        return builder.toString().trim()
    }

}

class PluginManifest : Manifest {

    val dependencies: Set<String>?
    val repositories: Set<String>?
    val platform: Platform

    constructor(name: String, version: String? = null, description: String? = null, keywords: Set<String> = emptySet(),
                authors: Set<Author> = UNKNOWN_AUTHOR_SET, license: String? = null,
                dependencies: Set<String>? = null, repositories: Set<String>? = null,
                platform: Platform = Platform.ALL) : super(name, version, description, keywords, authors, license) {
        this.dependencies = dependencies
        this.repositories = repositories
        this.platform = platform
    }

    constructor(map: Map<String, Any?>) : super(map) {
        dependencies = (map.getOrDefault("dependencies", null) as? Collection<*>?)
                ?.map { it.toString() }?.toSet()
        repositories = (map.getOrDefault("repositories", null) as? Collection<*>?)
                ?.map { it.toString() }?.toSet()
        platform = map.getOrDefault("platform", null) as Platform
    }

    companion object {
        internal fun fromManifest(manifest: Manifest, dependencies: Set<String>? = null, repositories: Set<String>? = null,
                         platform: Platform = Platform.ALL): PluginManifest {
            return PluginManifest(manifest.name, manifest.version, manifest.description, manifest.keywords,
                    manifest.authors, manifest.license, dependencies, repositories, platform)
        }
    }

}
