package dev.morphia.audits.coverage

import java.util.Locale

enum class OperatorContext {
    QUERY_PREDICATE,
    QUERY_PROJECTION,
    UPDATE_OPERATOR,
    UPDATE_MODIFIER,
    PIPELINE_STAGE,
    AGGREGATION_EXPRESSION,
    GROUP_ACCUMULATOR,
    WINDOW_FUNCTION;

    fun key(): String = name.lowercase(Locale.ROOT).replace('_', '-')
}

data class CapabilityKey(val product: String, val context: OperatorContext, val token: String) {
    fun id(): String = "$product/${context.key()}/$token"
}

data class CapabilityRef(val key: CapabilityKey, val facet: String?) {
    fun id(): String = key.id() + (facet?.let { "/facet/$it" } ?: "")
}

enum class SourceKind {
    GIT,
    CONTENT_ADDRESSED,
}

enum class SourceRole {
    CURRENT,
    HISTORICAL,
}

enum class SourceChannel {
    RELEASED,
    UPCOMING,
    UNKNOWN,
}

data class ParserPin(val name: String, val version: String, val sha256: String)

data class DocumentationSource(
    val id: String,
    val kind: SourceKind,
    val uri: String,
    val revision: String?,
    val contentSha256: String,
    val root: String,
    val label: String,
    val role: SourceRole,
    val channel: SourceChannel,
    val parser: ParserPin,
    val normalizerVersion: String,
)

enum class EvidenceKind {
    DEFINITION,
    FEATURE,
    VERSION,
    EXAMPLE,
    DISPOSITION,
    CONSTRAINT,
}

data class SourceEvidence(
    val id: String,
    val source: String,
    val path: String,
    val anchor: String,
    val startLine: Long,
    val endLine: Long,
    val textSha256: String,
    val kind: EvidenceKind,
)

/** Source observations are not release introductions. */
data class CapabilityObservation(val source: String, val evidence: List<String>)

enum class LifecycleEvent {
    INTRODUCED,
    CHANGED,
    DEPRECATED,
    REMOVED,
}

enum class EvidenceConfidence {
    EXPLICIT,
    INFERRED,
    UNKNOWN,
}

data class VersionEvent(
    val kind: LifecycleEvent,
    val facet: String?,
    val rawVersion: String,
    val version: VersionRange?,
    val confidence: EvidenceConfidence,
    val evidence: List<String>,
)

data class CapabilityFacet(val id: String, val title: String, val evidence: List<String>)

enum class ConstraintKind {
    FCV,
    EDITION,
    TOPOLOGY,
    STABLE_API,
}

data class CapabilityConstraint(
    val kind: ConstraintKind,
    val expression: String,
    val evidence: List<String>,
)

enum class ExtractionState {
    NOT_ASSESSED,
    COMPLETE,
    INCOMPLETE,
    UNRESOLVED,
}

data class Capability(
    val key: CapabilityKey,
    val observations: List<CapabilityObservation>,
    val facets: List<CapabilityFacet>,
    val versions: List<VersionEvent>,
    val constraints: List<CapabilityConstraint>,
    val examples: ExtractionState,
    val exampleProblems: List<String>,
)

data class AuditProblem(
    val code: String,
    val message: String,
    val context: OperatorContext?,
    val subject: String?,
)

data class CapabilityCatalog(
    val schemaVersion: Int = 1,
    val product: String,
    val sources: List<DocumentationSource>,
    val evidence: List<SourceEvidence>,
    val capabilities: List<Capability>,
    val discoveryProblems: List<AuditProblem>,
)

/** Independent accepted expectations: new keys are allowed; lost keys require a disposition. */
data class ContextExpectation(
    val context: OperatorContext,
    val minimumCapabilities: Int,
    val expected: List<CapabilityRef>,
    val intentionalEmptyEvidence: List<String>,
)

data class ApiExpectation(
    val sourceRoots: List<String>,
    val minimumDeclarations: Int,
    val minimumOperatorTags: Int,
)

enum class BindingKind {
    TYPED,
    RAW_BSON,
}

/** Select all public overloads of a deliberately mapped factory/builder entry point. */
data class ApiSelector(val owner: String, val member: String)

data class ApiBinding(
    val id: String,
    val capability: CapabilityRef,
    val selector: ApiSelector,
    val kind: BindingKind,
)

data class PolicyExclusion(val capability: CapabilityRef, val reason: String)

data class ExpectedOmission(
    val capability: CapabilityRef,
    val reason: String,
    val evidence: List<String>,
)

data class CoveragePlan(
    val schemaVersion: Int = 1,
    val product: String,
    val contexts: List<ContextExpectation>,
    val api: ApiExpectation,
    val bindings: List<ApiBinding>,
    val exclusions: List<PolicyExclusion>,
    val omissions: List<ExpectedOmission>,
)

enum class ReportState {
    COMPARED,
    PARTIAL,
    INVALID,
}

enum class DocumentationState {
    OBSERVED,
    UNRESOLVED,
    REVIEWED_OMISSION,
}

enum class ApiState {
    TYPED_PRESENT,
    PARTIAL,
    RAW_BSON_ONLY,
    MISSING_TYPED_API,
    INVALID_BINDING,
    UNKNOWN_MAPPING,
    EXCLUDED,
    NOT_ASSESSED,
}

enum class IntroductionState {
    EXPLICIT,
    UNKNOWN,
    CONFLICTING,
}

enum class AnnotationState {
    MINIMUM_MATCH,
    INCORRECT_MINIMUM,
    MISSING,
    AMBIGUOUS,
    UNKNOWN_EXPECTATION,
    NOT_APPLICABLE,
}

enum class CheckState {
    NOT_ASSESSED
}

data class Introduction(
    val state: IntroductionState,
    val version: ReleaseVersion?,
    val evidence: List<String>,
)

data class DeclaredAnnotation(val declaringId: String, val value: String)

data class OverloadVersionCheck(
    val declaration: String,
    val state: AnnotationState,
    val serverRequirements: List<DeclaredAnnotation>,
    val morphiaSince: List<DeclaredAnnotation>,
)

data class ContextHealth(
    val context: OperatorContext,
    val healthy: Boolean,
    val observed: Int,
    val expectedMinimum: Int,
    val problems: List<AuditProblem>,
)

data class CapabilityCoverage(
    val capability: CapabilityRef,
    val documentation: DocumentationState,
    val newlyObserved: Boolean,
    val api: ApiState,
    val declarations: List<String>,
    val introduction: Introduction,
    val lifecycle: List<VersionEvent>,
    val annotations: List<OverloadVersionCheck>,
    val serialization: CheckState,
    val behavior: CheckState,
    val examples: ExtractionState,
    val exampleProblems: List<String>,
    val constraints: List<CapabilityConstraint>,
    val notes: List<String>,
)

data class CoverageReport(
    val schemaVersion: Int = 1,
    val state: ReportState,
    val catalogSha256: String?,
    val planSha256: String?,
    val apiSha256: String?,
    val contexts: List<ContextHealth>,
    val capabilities: List<CapabilityCoverage>,
    val problems: List<AuditProblem>,
)

internal val problemOrder =
    compareBy<AuditProblem>({ it.context?.name }, { it.code }, { it.subject }, { it.message })

/** Current facets require current feature evidence, independently of their parent definition. */
internal fun CapabilityCatalog.currentRefs(): Set<CapabilityRef> {
    val sourceRoles = sources.associate { it.id to it.role }
    val byEvidence = evidence.associateBy { it.id }
    return capabilities
        .filter { cap -> cap.observations.any { sourceRoles[it.source] == SourceRole.CURRENT } }
        .flatMap { cap ->
            listOf(CapabilityRef(cap.key, null)) +
                cap.facets
                    .filter { feature ->
                        feature.evidence.any { id ->
                            byEvidence[id]?.let { item ->
                                item.kind == EvidenceKind.FEATURE &&
                                    sourceRoles[item.source] == SourceRole.CURRENT
                            } == true
                        }
                    }
                    .map { CapabilityRef(cap.key, it.id) }
        }
        .toSet()
}
