package dev.morphia.audits.discovery

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.coverage.OperatorTokens
import dev.morphia.audits.documents.DocumentNode
import java.util.Locale

internal data class LocatedNode(
    val node: DocumentNode,
    val page: String,
    val sections: List<DocumentNode>,
    val targets: List<DocumentNode>,
    val containers: List<String> = emptyList(),
)

internal object DocumentFacts {
    val prefixes =
        mapOf(
            "expression" to "exp.",
            "group" to "grp.",
            "pipeline" to "pipe.",
            "query" to "op.",
            "projection" to "proj.",
            "update" to "up.",
            "window" to "window.",
        )

    fun text(node: DocumentNode): String = node.text ?: node.children.joinToString("") { text(it) }

    fun value(node: DocumentNode, name: String): String? =
        node.attributes[name]?.let(AuditJson::textValue)

    fun heading(section: DocumentNode): DocumentNode? =
        section.children.singleOrNull { it.kind == "heading" }

    fun anchor(section: DocumentNode): String? = heading(section)?.let { value(it, "id") }

    fun title(section: DocumentNode): String = heading(section)?.let(::text) ?: ""

    fun walk(
        root: DocumentNode,
        page: String,
        sections: List<DocumentNode> = emptyList(),
        targets: List<DocumentNode> = emptyList(),
        containers: List<String> = emptyList(),
    ): Sequence<LocatedNode> = sequence {
        yield(LocatedNode(root, page, sections, targets, containers))
        val nextSections = if (root.kind == "section") sections + root else sections
        val nextTargets =
            if (root.kind == "target" && root.domain == "mongodb") targets + root else targets
        (root.children + root.arguments).forEach {
            yieldAll(walk(it, page, nextSections, nextTargets, containers + root.kind))
        }
    }

    fun token(role: String, target: String): String? {
        val prefix = prefixes[role] ?: return null
        return target
            .takeIf { it.startsWith(prefix) }
            ?.removePrefix(prefix)
            ?.takeIf(OperatorTokens::valid)
    }

    fun rows(table: DocumentNode): List<List<DocumentNode>>? {
        val list = table.children.singleOrNull()?.takeIf { it.kind == "list" } ?: return null
        return list.children.map { row ->
            require(row.kind == "listItem") { "Unknown table row kind ${row.kind}" }
            val cells = row.children.singleOrNull()?.takeIf { it.kind == "list" } ?: return null
            if (cells.children.any { it.kind != "listItem" }) return null
            cells.children
        }
    }

    fun within(page: String, root: String): Boolean = page == root || page.startsWith("$root/")

    fun catalogPage(page: String, rule: CatalogRule): Boolean =
        page == rule.page ||
            (rule.includeDescendants && page.startsWith(rule.page.removeSuffix(".txt") + "/"))

    fun slug(value: String): String =
        value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
