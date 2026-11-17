package dev.morphia.audits.scenarios

import dev.morphia.audits.documents.*
import dev.morphia.audits.naming.*
import dev.morphia.audits.semantics.ResultOrder
import dev.morphia.audits.shell.*

/** Reviewed selection policy, not a baseline inferred from this candidate's observed counts. */
data class ScenarioExtractionPlan(
    val schemaVersion: Int = 1,
    val catalogSha256: String,
    val pages: List<ScenarioPageRule>,
)

data class ScenarioPageRule(
    val id: String,
    val page: String,
    val scope: ScenarioScope,
    val destination: ExampleDestination,
    val sections: List<String>,
    val excludedSections: List<String>,
    val minimumReferenceGroups: Int,
    val referenceLanguages: List<String>,
    val referenceTabs: List<String>,
    val foreignLanguages: List<String>,
    val foreignTabs: List<String>,
    val dialect: ShellDialect,
    val resultOrder: ResultOrder,
    val sectionAssembly: SectionAssemblyPolicy? = null,
)

/** Reviewed ownership contract; absent means native IO ownership only. */
data class SectionAssemblyPolicy(
    val layout: SectionLayout,
    val sharedSetupSections: List<String> = emptyList(),
    val scenarioTabs: List<String> = emptyList(),
    /** Explicit legacy highlighting aliases, still parsed with the closed shell grammar. */
    val additionalShellLanguages: List<String> = emptyList(),
)

enum class SectionLayout {
    SOURCE_ONLY,
    COMMANDS_THEN_RESULT,
}

data class ScenarioSection(
    val title: String,
    val anchor: String?,
    val origin: DocumentOrigin,
    val explicitLabels: List<String> = emptyList(),
    val leaf: Boolean = true,
)

/** Canonical JSON options/attributes remain unchanged, including include-range selections. */
data class ScenarioContainer(
    val kind: String,
    val name: String?,
    val domain: String?,
    val argument: String,
    val options: Map<String, String>,
    val attributes: Map<String, String>,
    val origin: DocumentOrigin,
)

data class ScenarioSnippet(
    val reference: String,
    val canonicalPath: String?,
    val sha256: String?,
    val identity: String?,
    val parserDigest: String?,
    val directive: ScenarioContainer,
)

data class ScenarioCode(
    /** Report-local lookup only. Never a test name, registry identity or matching preference. */
    val locator: List<Int>,
    val source: String,
    val sha256: String,
    val language: String?,
    val attributes: Map<String, String>,
    val origin: DocumentOrigin,
    val role: ProgramRole?,
    val containers: List<ScenarioContainer>,
    val snippets: List<ScenarioSnippet>,
)

enum class ScenarioVariant {
    REFERENCE,
    FOREIGN,
    UNRESOLVED,
}

data class ExtractedScenario(
    /** Content-addressed candidate handle; persisted descriptive names are separate. */
    val id: String,
    val rule: String,
    val page: String,
    val variant: ScenarioVariant,
    val sections: List<ScenarioSection>,
    val containers: List<ScenarioContainer>,
    val source: DocumentOrigin,
    val explicitLabels: List<String>,
    val blocks: List<ScenarioCode>,
    val program: ShellProgramResult?,
    val normalizationProblem: String?,
    val naming: NamingScenario?,
)

data class ScenarioExtractionProblem(
    val code: String,
    val rule: String,
    val candidate: String?,
    val message: String,
)

data class ScenarioSourceDiagnostic(
    val diagnostic: DocumentDiagnostic,
    val blocksNaming: Boolean,
    val reason: String,
)

data class ExtractedScenarioPage(
    val rule: String,
    val page: String,
    val referenceGroups: Int,
    val foreignGroups: Int,
    val unassigned: List<ScenarioCode>,
    val unassignedReferenceBlocks: Int,
    val dependencies: List<DocumentDependency>,
    val dependenciesComplete: Boolean,
    val sourceDiagnostics: List<ScenarioSourceDiagnostic>,
    /** Completeness of selected source grouping; never behavioral or serialization coverage. */
    val namingComplete: Boolean,
    /** Declared setup is retained even if all consumers disappear. */
    val sharedSetup: List<ScenarioCode> = emptyList(),
)

enum class ScenarioExtractionState {
    READY,
    PARTIAL,
}

data class ScenarioExtractionReport(
    val schemaVersion: Int = 1,
    val normalizer: String,
    val sourceSha256: String,
    val documentInputSha256: String,
    val parser: DocumentParser,
    val plan: ScenarioExtractionPlan,
    val planSha256: String,
    val state: ScenarioExtractionState,
    val pages: List<ExtractedScenarioPage>,
    val scenarios: List<ExtractedScenario>,
    val diagnostics: List<DocumentDiagnostic>,
    val problems: List<ScenarioExtractionProblem>,
)

data class ScenarioExtractionLimits(
    val maximumNodes: Int = 500_000,
    val maximumDepth: Int = 128,
    val maximumGroups: Int = 10_000,
    val maximumCodeCharacters: Int = 16_777_216,
    val maximumMetadataCharacters: Int = 16_777_216,
    val maximumReportBytes: Int = 67_108_864,
) {
    init {
        require(maximumNodes in 1..1_000_000 && maximumDepth in 1..256)
        require(maximumGroups in 1..20_000 && maximumCodeCharacters in 1..67_108_864)
        require(maximumMetadataCharacters in 1..67_108_864)
        require(maximumReportBytes in 1..134_217_728)
    }
}
