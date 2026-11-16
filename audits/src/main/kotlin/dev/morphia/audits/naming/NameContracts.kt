package dev.morphia.audits.naming

import dev.morphia.audits.coverage.CapabilityRef

/** Equivalent language tabs must already be grouped into one emitted reference scenario. */
data class ScenarioScope(val capability: CapabilityRef, val language: String)

data class ExampleDestination(val testClass: String, val resourceDirectory: String)

/**
 * Logical document/snippet names omit revisions and line numbers; trace evidence pins the bytes.
 */
data class ScenarioLocation(
    val document: String,
    val explicitLabels: List<String>,
    val snippetIds: List<String>,
    val autoAnchor: String?,
    val enclosingContext: String?,
    val fingerprint: String?,
)

data class ScenarioTrace(
    val catalogSha256: String,
    val location: ScenarioLocation,
    val displayTitle: String,
    val evidence: List<String>,
)

enum class NameDecisionAction {
    ASSIGNMENT,
    RETIREMENT,
}

data class NameDecision(
    val action: NameDecisionAction,
    val catalogSha256: String,
    val previousName: String?,
    val newStem: String?,
    val reason: String,
    val evidence: List<String>,
)

data class HistoricalBinding(val destination: ExampleDestination, val stem: String)

data class ExampleNameEntry(
    val scope: ScenarioScope,
    val destination: ExampleDestination,
    val stem: String,
    val aliases: List<String>,
    val observations: List<ScenarioTrace>,
    val current: ScenarioTrace,
    val previousBindings: List<HistoricalBinding>,
    val decisions: List<NameDecision>,
    val active: Boolean,
) {
    fun qualifiedName(): String = scope.capability.id() + "/example/" + stem
}

data class ExampleNameRegistry(val schemaVersion: Int = 1, val entries: List<ExampleNameEntry>)

data class NamingScenario(
    val id: String,
    val scope: ScenarioScope,
    val destination: ExampleDestination,
    val trace: ScenarioTrace,
    val contextTitles: List<String>,
)

enum class OccupiedKind {
    FOLDER,
    METHOD,
}

/** Owner is present only when inventory proved the accepted entry owns this exact binding. */
data class OccupiedName(
    val kind: OccupiedKind,
    val namespace: String,
    val name: String,
    val owner: String?,
)

data class ReviewedName(
    val scenario: String,
    val existingName: String?,
    val newStem: String?,
    val reason: String,
    val evidence: List<String>,
)

data class ReviewedRetirement(
    val qualifiedName: String,
    val reason: String,
    val evidence: List<String>,
)

data class NamingRequest(
    val schemaVersion: Int = 1,
    val catalogSha256: String,
    val completeScopes: List<ScenarioScope>,
    val scenarios: List<NamingScenario>,
    val occupied: List<OccupiedName>,
    val reviewedNames: List<ReviewedName>,
    val retirements: List<ReviewedRetirement>,
)

enum class NameProposalState {
    READY,
    UNRESOLVED,
    INVALID,
}

enum class NameMatch {
    STABLE_ID,
    ANCHOR_AND_CONTEXT,
    FINGERPRINT,
    NEW,
    REVIEWED,
}

data class NameResolution(
    val scenario: String,
    val qualifiedName: String,
    val stem: String,
    val method: String,
    val resourcePath: String,
    val match: NameMatch,
    val previousName: String?,
)

data class NamingProblem(
    val code: String,
    val scenario: String?,
    val qualifiedName: String?,
    val message: String,
)

data class NameProposal(
    val schemaVersion: Int = 1,
    val state: NameProposalState,
    val acceptedSha256: String?,
    val requestSha256: String?,
    val registry: ExampleNameRegistry,
    val resolutions: List<NameResolution>,
    val problems: List<NamingProblem>,
)
