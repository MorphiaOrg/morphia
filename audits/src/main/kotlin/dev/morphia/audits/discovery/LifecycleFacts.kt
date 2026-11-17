package dev.morphia.audits.discovery

import dev.morphia.audits.coverage.*
import dev.morphia.audits.documents.DocumentNode
import dev.morphia.audits.documents.DocumentPrecision

/**
 * Section/context ownership is explicit; unrelated feature additions cannot introduce an operator.
 */
internal object LifecycleFacts {
    private val kinds =
        mapOf(
            "versionadded" to LifecycleEvent.INTRODUCED,
            "versionchanged" to LifecycleEvent.CHANGED,
            "deprecated" to LifecycleEvent.DEPRECATED,
            "versionremoved" to LifecycleEvent.REMOVED,
        )

    fun collect(
        definition: LocatedNode,
        primaryRole: Boolean,
        key: CapabilityKey,
        facts: List<LocatedNode>,
        evidence: SourceEvidenceIndex,
        facets: MutableMap<String, CapabilityFacet>,
        problem: (String, String) -> Unit,
    ): List<VersionEvent> =
        facts
            .filter { located ->
                located.node.kind == "directive" &&
                    located.node.name in kinds &&
                    (located.targets.lastOrNull()?.let { it === definition.node } != false)
            }
            .mapNotNull { located ->
                val scope = located.sections.lastOrNull()
                val definitionScope = definition.sections.lastOrNull()
                val ownsTarget = located.targets.lastOrNull() === definition.node
                val interveningSection = located.sections.size > definition.sections.size
                val root =
                    (ownsTarget && !interveningSection) ||
                        scope == null ||
                        located.sections.size <= 1 ||
                        scope === definitionScope
                var feature: String? = null
                if (!root) {
                    val featureScope = requireNotNull(scope)
                    val titles = located.sections.drop(1).map(DocumentFacts::title)
                    val readable =
                        DocumentFacts.slug(titles.joinToString(" ")).take(80).ifBlank { "feature" }
                    feature = "section-$readable-${AuditJson.hash(titles).take(12)}"
                    val heading = DocumentFacts.heading(featureScope)
                    val proof =
                        heading?.let {
                            evidence.heading(
                                it,
                                EvidenceKind.FEATURE,
                                "feature:${key.id()}:$feature",
                            )
                        }
                    if (proof == null) {
                        problem(
                            "UNVERIFIED_FEATURE_SCOPE",
                            "Version notice has no captured section origin",
                        )
                        return@mapNotNull null
                    }
                    val existing = facets[feature]
                    facets[feature] =
                        CapabilityFacet(
                            feature,
                            DocumentFacts.title(featureScope),
                            (existing?.evidence.orEmpty() + proof.id).distinct().sorted(),
                        )
                }
                val ownsSection =
                    scope === definitionScope &&
                        scope != null &&
                        DocumentFacts.title(scope) == "Definition"
                val nested =
                    located.containers
                        .drop(definition.containers.size + if (ownsTarget) 1 else 0)
                        .any { it in setOf("list", "listItem", "target") }
                val siblingTargets =
                    facts.count {
                        it.node.kind == "target" &&
                            it.node.domain == "mongodb" &&
                            it.node.name in DocumentFacts.prefixes &&
                            it.sections == definition.sections
                    }
                // A feature section outside every target cannot say which of several operators
                // defined on the same page it describes.
                val pageTargets =
                    facts.count {
                        it.node.kind == "target" &&
                            it.node.domain == "mongodb" &&
                            it.node.name in DocumentFacts.prefixes
                    }
                event(
                    located.node,
                    key,
                    feature,
                    primaryRole &&
                        !nested &&
                        (ownsTarget ||
                            (!root && pageTargets == 1) ||
                            (ownsSection && siblingTargets == 1)),
                    evidence,
                    problem,
                )
            }

    fun catalog(
        row: LocatedNode,
        key: CapabilityKey,
        evidence: SourceEvidenceIndex,
        problem: (String, String) -> Unit,
    ): List<VersionEvent> =
        DocumentFacts.walk(row.node, row.page)
            .filter { it.node.kind == "directive" && it.node.name in kinds }
            .mapNotNull { located ->
                // A nested feature section does not establish the row operator's introduction.
                if (
                    located.sections.isNotEmpty() ||
                        located.targets.isNotEmpty() ||
                        "list" in located.containers
                ) {
                    problem(
                        "UNSCOPED_CATALOG_VERSION",
                        "Catalog version occurs inside a feature section; retain source for review",
                    )
                    null
                } else
                    event(
                        located.node,
                        key,
                        null,
                        row.node.attributes["memberCount"] == "1",
                        evidence,
                        problem,
                    )
            }
            .toList()

    private fun event(
        node: DocumentNode,
        key: CapabilityKey,
        feature: String?,
        primaryRole: Boolean,
        evidence: SourceEvidenceIndex,
        problem: (String, String) -> Unit,
    ): VersionEvent? {
        val name = node.name ?: return null
        val kind = kinds[name] ?: return null
        val raw = evidence.directiveArgument(node, name)
        if (raw == null) {
            problem(
                "UNRESOLVED_VERSION_ARGUMENT",
                "Cannot bind the parsed version argument to its raw source; preserve the document evidence for review",
            )
            return null
        }
        if (raw.isBlank()) {
            problem("EMPTY_VERSION_NOTICE", "Lifecycle directive has no version argument")
            return null
        }
        val declaration =
            Regex("^\\s*\\.\\. (?:mongodb:)?${Regex.escape(name)}::\\s*${Regex.escape(raw)}\\s*$")
        val anchor = "version:${key.id()}:${feature ?: "definition"}:$name:$raw"
        val exact = evidence.node(node, EvidenceKind.VERSION, anchor) { declaration.matches(it) }
        val proof = exact ?: evidence.file(node, EvidenceKind.VERSION, anchor)
        if (proof == null) {
            problem("UNVERIFIED_VERSION_SOURCE", "Lifecycle notice has no captured raw source")
            return null
        }
        val parsed = VersionRange.parse(raw)
        val confident =
            primaryRole &&
                parsed != null &&
                node.origin.precision == DocumentPrecision.PARSER_REPORTED &&
                exact != null &&
                node.origin.reportedLine?.toLong() == exact.startLine &&
                exact.startLine == exact.endLine
        if (!confident)
            problem(
                "UNCERTAIN_VERSION_SCOPE",
                "Preserved $name $raw for ${key.id()} with unknown confidence; context/source span is not independently established",
            )
        return VersionEvent(
            kind,
            feature,
            raw,
            parsed,
            if (confident) EvidenceConfidence.EXPLICIT else EvidenceConfidence.UNKNOWN,
            listOf(proof.id),
        )
    }
}
