package dev.morphia.audits.discovery

import dev.morphia.audits.coverage.CapabilityCatalog
import dev.morphia.audits.coverage.CapabilityKey
import dev.morphia.audits.coverage.OperatorContext
import dev.morphia.audits.documents.DocumentState

/** Reviewed logical selectors, independent of the candidate's observed operator counts. */
data class DiscoveryPlan(
    val schemaVersion: Int = 1,
    val product: String,
    val definitionRoots: List<String>,
    val catalogs: List<CatalogRule>,
)

/**
 * Only first-column names count as catalog members. Sections refer to AST heading anchors; an empty
 * section list selects the whole page. Other operator mentions remain prose.
 */
enum class CatalogFormat {
    TABLE_FIRST_COLUMN,
    OPERATOR_REFERENCE_LIST,
}

data class CatalogRule(
    val id: String,
    val context: OperatorContext,
    val format: CatalogFormat,
    val page: String,
    val includeDescendants: Boolean,
    val sections: List<String>,
    val excludedSections: List<String>,
    val headers: List<String>,
    val ignoredHeaders: List<String>,
    val referenceRoles: List<String>,
    val primaryDefinitionRole: String,
    val fallbackDefinitionRoles: List<String>,
    val minimumMembers: Int,
)

enum class DiscoveryDomain {
    CATALOG,
    DEFINITION,
    VERSION,
    EXAMPLE,
    REFERENCE,
    OUTSIDE_SCOPE,
}

data class DiscoveryDiagnostic(
    val code: String,
    val domain: DiscoveryDomain,
    val context: OperatorContext?,
    val page: String?,
    val message: String,
)

data class UnassignedDefinition(
    val role: String,
    val target: String,
    val page: String,
    val evidence: List<String>,
)

data class DiscoveryContext(
    val context: OperatorContext,
    val healthy: Boolean,
    val catalogMembers: List<String>,
    val observed: Int,
    val problems: List<DiscoveryDiagnostic>,
)

data class DiscoveredPage(
    val page: String,
    val capabilities: List<CapabilityKey>,
    val resources: List<String>,
    val exampleProblems: List<String>,
)

enum class DiscoveryState {
    READY,
    PARTIAL,
}

/** Discovery health is independent of unresolved inventories, examples and version metadata. */
data class DiscoveryReport(
    val schemaVersion: Int = 1,
    val planSha256: String,
    val documentInputSha256: String,
    val state: DiscoveryState,
    val sourceState: DocumentState,
    val catalog: CapabilityCatalog,
    val contexts: List<DiscoveryContext>,
    val pages: List<DiscoveredPage>,
    val unassignedDefinitions: List<UnassignedDefinition>,
    val diagnostics: List<DiscoveryDiagnostic>,
) {
    init {
        require(
            schemaVersion == 1 &&
                planSha256.matches(Regex("[0-9a-f]{64}")) &&
                documentInputSha256.matches(Regex("[0-9a-f]{64}"))
        )
        require(
            contexts.map { it.context }.toSet() == OperatorContext.entries.toSet() &&
                contexts.size == OperatorContext.entries.size
        )
        val discoveryProblems =
            diagnostics.filter {
                it.domain in setOf(DiscoveryDomain.CATALOG, DiscoveryDomain.DEFINITION)
            }
        require(diagnostics.distinct().size == diagnostics.size)
        require(catalog.discoveryProblems.distinct().size == catalog.discoveryProblems.size)
        require(
            catalog.discoveryProblems.toSet() ==
                discoveryProblems
                    .map {
                        dev.morphia.audits.coverage.AuditProblem(
                            it.code,
                            it.message,
                            it.context,
                            it.page,
                        )
                    }
                    .toSet()
        )
        require(
            contexts.all { context ->
                context.problems.toSet() ==
                    discoveryProblems
                        .filter { it.context == null || it.context == context.context }
                        .toSet() && context.problems.distinct().size == context.problems.size
            }
        )
        require(
            catalog.sources.size == 1 &&
                catalog.sources.single().normalizerVersion ==
                    "${CatalogDiscovery.VERSION}:plan:$planSha256:parse-input:$documentInputSha256"
        ) {
            "Discovery provenance must match the report plan and parse-input pins"
        }
        val evidenceIds = catalog.evidence.map { it.id }.toSet()
        require(
            catalog.capabilities.all { cap ->
                (cap.observations.flatMap { it.evidence } +
                        cap.facets.flatMap { it.evidence } +
                        cap.versions.flatMap { it.evidence })
                    .all { it in evidenceIds }
            } && unassignedDefinitions.all { evidenceIds.containsAll(it.evidence) }
        ) {
            "Discovery evidence references must resolve"
        }
        val keys = catalog.capabilities.map { it.key }.toSet()
        require(pages.all { keys.containsAll(it.capabilities) })
        require((state == DiscoveryState.READY) == contexts.all { it.healthy })
        require(
            contexts.all { context ->
                context.catalogMembers == context.catalogMembers.distinct().sorted() &&
                    context.catalogMembers.toSet() ==
                        catalog.capabilities
                            .filter { it.key.context == context.context }
                            .map { it.key.token }
                            .toSet()
            }
        ) {
            "Context catalog members must match the context's capabilities"
        }
        require(
            contexts.all {
                it.healthy == it.problems.isEmpty() &&
                    it.observed ==
                        catalog.capabilities.count { cap -> cap.key.context == it.context }
            }
        )
    }
}
