package dev.morphia.audits.sources

import dev.morphia.audits.coverage.ContractValidation
import java.nio.file.Path
import java.util.TreeMap

/** Dependency truth is derived from raw records, rather than trusting a cached READY flag. */
internal object SourceGraph {
    const val MAX_LINK_DEPTH = 128

    fun validPath(path: String, root: Boolean = false): Boolean =
        path.length <= 4096 &&
            path.toByteArray(Charsets.UTF_8).size <= 4096 &&
            path.count { it == '/' } < 256 &&
            path.none { it.code < 32 } &&
            ContractValidation.relative(path, root)

    fun within(path: String, root: String): Boolean =
        root == "." || path == root || path.startsWith("$root/")

    fun target(path: String, link: String): String? {
        if (
            link.isEmpty() ||
                link.startsWith('/') ||
                '\\' in link ||
                ':' in link ||
                '\u0000' in link
        )
            return null
        // Embedded navigation can cross a file, absent component or another symlink.
        // Keep that unsupported rather than silently applying lexical filesystem semantics.
        if (link.endsWith('/')) return null
        var named = false
        for (part in link.split('/')) {
            if (part in setOf("", ".", "..")) {
                if (named && part in setOf(".", "..")) return null
            } else named = true
        }
        val resolved = (Path.of(path).parent ?: Path.of("")).resolve(link).normalize()
        val normalized = resolved.joinToString("/") { it.toString() }.ifEmpty { "." }
        return normalized.takeIf { validPath(it, true) }
    }

    fun validateStructure(resources: List<GitResource>) {
        require(
            resources.all { validPath(it.path) } &&
                resources.map { it.path }.distinct().size == resources.size
        ) {
            "Source paths must be bounded, valid and unique"
        }
        val paths = resources.map { it.path }.toSet()
        resources.forEach { resource ->
            val parts = resource.path.split('/')
            for (index in 1 until parts.size) require(
                parts.take(index).joinToString("/") !in paths
            ) {
                "A source file or symlink cannot also be a directory ancestor: ${resource.path}"
            }
        }
    }

    fun inspect(resources: List<GitResource>, request: GitSourceRequest): List<SourceProblem> {
        val byPath = TreeMap(resources.associateBy { it.path })
        val listed = mutableMapOf<String, List<GitResource>>()
        fun list(root: String): List<GitResource> =
            listed.getOrPut(root) {
                if (root == ".") byPath.values.toList()
                else
                    byPath[root]?.let(::listOf)
                        ?: byPath.tailMap("$root/").values.takeWhile {
                            it.path.startsWith("$root/")
                        }
            }
        val visited = mutableSetOf<String>()
        val problems = mutableListOf<SourceProblem>()
        fun visit(resource: GitResource, chain: List<String>) {
            if (resource.path in chain) {
                problems +=
                    SourceProblem(
                        "SOURCE_CYCLE",
                        resource.path,
                        chain + resource.path,
                        "Repository symlink dependency cycle",
                    )
                return
            }
            if (!visited.add(resource.path) || resource.kind != GitResourceKind.SYMLINK) return
            val next = chain + resource.path
            if (chain.size >= MAX_LINK_DEPTH) {
                problems +=
                    SourceProblem(
                        "SOURCE_DEPTH",
                        resource.path,
                        next,
                        "Source dependency depth exceeds $MAX_LINK_DEPTH links",
                    )
                return
            }
            val target = target(resource.path, resource.linkTarget.orEmpty())
            when {
                target == null ->
                    problems +=
                        SourceProblem(
                            "UNRESOLVED_SYMLINK",
                            resource.path,
                            next,
                            "Symlink target is unsupported or leaves the pinned repository",
                        )
                request.allowedSymlinkRoots.none { within(target, it) } ->
                    problems +=
                        SourceProblem(
                            "UNDECLARED_DEPENDENCY",
                            resource.path,
                            next,
                            "Symlink target $target is outside the declared dependency roots",
                        )
                else -> {
                    val dependencies = list(target)
                    if (dependencies.isEmpty())
                        problems +=
                            SourceProblem(
                                "UNRESOLVED_SYMLINK",
                                resource.path,
                                next,
                                "Pinned target $target is absent or unresolved in this bundle",
                            )
                    else dependencies.forEach { visit(it, next) }
                }
            }
        }
        request.roots.sorted().forEach {
            list(it).forEach { resource -> visit(resource, emptyList()) }
        }
        return problems
            .distinct()
            .sortedWith(
                compareBy(
                    { it.path },
                    { it.code },
                    { it.chain.joinToString("\u0000") },
                    { it.message },
                )
            )
    }
}
