package dev.morphia.audits.repairs

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * Resolves Giza-generated `/includes/extracts/<ref>.rst` files from the `extracts*.yaml` sources
 * they are built from: each YAML document has a `ref` and `content`, may `inherit` another entry's
 * fields (`file` and `ref`), and may define `replacement` values substituted for `{{key}}`.
 */
class GizaExtracts(private val includes: Path) {
    /** One extract, with the YAML files it was assembled from. */
    data class Extract(val content: String, val files: List<Path>)

    private val entries: Map<Pair<String, String>, List<JsonNode>> by lazy {
        if (!Files.isDirectory(includes)) emptyMap()
        else
            Files.list(includes)
                .use { stream ->
                    stream
                        .filter {
                            it.isRegularFile() &&
                                it.name.startsWith("extracts") &&
                                it.name.endsWith(".yaml")
                        }
                        .sorted()
                        .toList()
                }
                .flatMap { file ->
                    mapper
                        .readerFor(JsonNode::class.java)
                        .readValues<JsonNode>(Files.readAllBytes(file))
                        .readAll()
                        .filter { it.isObject && it.hasNonNull("ref") }
                        .map { (file.name to it["ref"].asText()) to it }
                }
                .groupBy({ it.first }, { it.second })
    }

    /** The extract named [ref], or null when no YAML file defines it exactly once. */
    fun resolve(ref: String): Extract? {
        val key = entries.keys.filter { it.second == ref }.singleOrNull() ?: return null
        val entry = assemble(key, mutableSetOf()) ?: return null
        var text = entry.content
        entry.replacements.forEach { (name, value) -> text = text.replace("{{$name}}", value) }
        // An unreplaced placeholder means the inheritance chain is not understood.
        if (Regex("""\{\{[^}]+}}""").containsMatchIn(text)) return null
        return Extract(text, entry.files)
    }

    private class Entry(
        val content: String,
        val replacements: Map<String, String>,
        val files: List<Path>,
    )

    /** [key]'s content and replacements, inherited fields overridden by its own. */
    private fun assemble(
        key: Pair<String, String>,
        seen: MutableSet<Pair<String, String>>,
    ): Entry? {
        require(seen.add(key)) { "Extract inheritance cycle at $key" }
        // A ref defined twice in one file is ambiguous; fields that render beyond the content
        // (a heading, an `only` directive, text before or after) are not reproduced.
        val node = entries[key]?.singleOrNull() ?: return null
        if (rendering.any { node.has(it) }) return null
        val parent =
            (node["inherit"] ?: node["source"])?.let {
                val file = it["file"]?.asText() ?: return null
                val ref = it["ref"]?.asText() ?: return null
                assemble(file to ref, seen) ?: return null
            }
        val content = node["content"]?.asText() ?: parent?.content ?: return null
        val replacements = parent?.replacements.orEmpty().toMutableMap()
        (node["replacement"] ?: node["replacements"])?.fields()?.forEach { (name, value) ->
            replacements[name] = value.asText()
        }
        return Entry(
            content,
            replacements,
            listOf(includes.resolve(key.first)) + parent?.files.orEmpty(),
        )
    }

    private companion object {
        val mapper = YAMLMapper()
        val rendering = listOf("title", "style", "only", "pre", "post", "append")
    }
}
