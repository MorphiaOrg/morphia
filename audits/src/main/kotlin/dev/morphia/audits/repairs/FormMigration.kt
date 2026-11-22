package dev.morphia.audits.repairs

import dev.morphia.audits.model.Operator
import dev.morphia.audits.model.OperatorType
import dev.morphia.audits.published.PublishedComparison
import dev.morphia.audits.published.PublishedDocument
import dev.morphia.audits.rst.RstDocument
import dev.morphia.audits.semantics.BsonComparison
import dev.morphia.audits.semantics.BsonComparisonState
import dev.morphia.audits.semantics.BsonDifference
import dev.morphia.audits.semantics.BsonDifferenceKind
import dev.morphia.audits.shell.AggregateOperation
import dev.morphia.audits.shell.FindOperation
import dev.morphia.audits.shell.InsertOperation
import dev.morphia.audits.shell.LegacyDocuments
import dev.morphia.audits.shell.ProgramBlock
import dev.morphia.audits.shell.ProgramFormat
import dev.morphia.audits.shell.ProgramRole
import dev.morphia.audits.shell.ShellDialect
import dev.morphia.audits.shell.ShellLiteralState
import dev.morphia.audits.shell.ShellProgramRequest
import dev.morphia.audits.shell.ShellPrograms
import dev.morphia.audits.shell.UpdateOperation
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonValue

/** How a source page's commit relates to the Morphia commit that wrote the fixture. */
enum class SourceRelation {
    /** The page as of the fixture's origin commit (or just before the manual move removed it). */
    AT_ORIGIN,
    /**
     * The fixture predates every page at that path, so the source is the commit that first added
     * the page. It pins the documented values a declaration changes; it is not where the fixture
     * came from.
     */
    AFTER_ORIGIN,
}

/** A page of the documentation repository at an exact commit. */
data class FormSource(
    val commit: String,
    val path: String,
    val relation: SourceRelation = SourceRelation.AT_ORIGIN,
) {
    init {
        require(Regex("[0-9a-f]{40}").matches(commit)) {
            "Source commit must be a full SHA: $commit"
        }
        require(
            (path.startsWith("source/") || path.startsWith("content/manual/manual/source/")) &&
                path.endsWith(".txt") &&
                ".." !in path.split('/')
        ) {
            "Unsupported source path: $path"
        }
    }
}

/**
 * Reviewed claim that a locked fixture is the documented example at [source] under [rules].
 * [origin] is the Morphia commit that created the fixture; [source] is the page as of that commit.
 */
data class FormDeclaration(
    val fixture: String,
    val origin: String,
    val source: FormSource,
    /** Exact text of the lock this declaration retires. */
    val lock: String,
    val rules: List<FormRule>,
    /** Why the commands are equivalent; required exactly when [rules] has ALTERNATIVE_COMMANDS. */
    val justification: String? = null,
    /**
     * Git blob id of the reviewed page, and of each include it splices in. Pages are not committed
     * (they are licensed separately), so these pins are what a networked recapture is checked
     * against.
     */
    val pageBlob: String = "",
    val includes: List<FormInclude> = emptyList(),
    /** Number type changes; allowed only when the lock is filed as NUMERIC_TYPE. */
    val retypes: List<NumericRetype> = emptyList(),
    /** Value corrections; allowed only when the lock is filed as DOCS_ERROR or FIXTURE_CURATION. */
    val corrections: List<FormCorrection> = emptyList(),
    /** File name of the lock marker: `lock`, or `lockd` for the one fixture that spells it so. */
    val marker: String = "lock",
)

/**
 * What the repair inventory knows about a fixture's test: its lock categories, which decide what an
 * override may declare (form rules for MORPHIA_FORM, retypes for NUMERIC_TYPE), the extra
 * collections it loads, and the helpers it runs, which decide the kind of action it executes.
 */
data class FixtureFacts(
    val categories: List<String>,
    val collections: Map<String, String> = emptyMap(),
    val helpers: List<String> = emptyList(),
) {
    /** The kind of action the test runs, when it runs exactly one helper. */
    val tested: ActionShape?
        get() =
            when (helpers.singleOrNull()) {
                "testPipeline" -> ActionShape.PIPELINE
                "testQuery" -> ActionShape.FIND
                "testUpdate" -> ActionShape.UPDATE
                else -> null
            }
}

/** Why a locked fixture cannot (yet) be reproduced from its documented source. */
enum class FormGap {
    /** The legacy extractor cannot read the page, for example because of an include. */
    PAGE_UNSUPPORTED,
    /** No example on the page carries the fixture's recorded name. */
    NO_MATCHING_EXAMPLE,
    /** The documented command or data uses syntax the shell normalizer does not lower. */
    SOURCE_UNSUPPORTED,
    /** The fixture holds files or command options this comparison does not cover. */
    FIXTURE_UNSUPPORTED,
    /** Every declared rule applied, and the fixture still differs from the source. */
    RESIDUAL_DIFFERENCE,
    /**
     * The tool reproduces the fixture, but a reviewer rejected the declaration it drafts: its
     * corrections are not the edits the lock describes. The detail is the reviewer's reason; only a
     * reviewer sets this gap, and capture checks that the fixture still reproduces.
     */
    REJECTED,
}

data class FormUnresolved(
    val fixture: String,
    val origin: String,
    val source: FormSource,
    val gap: FormGap,
    val detail: String,
)

data class FormReview(
    val schemaVersion: Int,
    val repository: String,
    val declarations: List<FormDeclaration>,
    val unresolved: List<FormUnresolved>,
    /**
     * Declarations whose fixtures moved onto the operator case runner. They keep their reviewed
     * pins; `FixtureMigration.verify` checks the migration ledger against them, and capture does
     * not re-examine them, because their legacy fixtures are gone.
     */
    val migrated: List<FormDeclaration> = emptyList(),
)

enum class BlockRole {
    ACTION,
    DATA,
    EXPECTED,
    INDEX,
}

/** Source text as extracted from the page, so the claim can be rechecked without the page. */
data class SourceBlock(val role: BlockRole, val text: String, val sha256: String)

/** A documented operation in BSON: seed data by collection, the action, and its printed result. */
data class DocumentedScenario(
    val collection: String,
    val data: Map<String, List<BsonDocument>>,
    val shape: ActionShape,
    val action: List<BsonValue>,
    /**
     * What the documented results prove, or null when the page shows none. A page's results are
     * what mongosh printed, even when they read as JSON: a printed `10` is an Int32 or the same
     * Double, so they are constraints, never guessed BSON values.
     */
    val printed: List<PublishedDocument>?,
    /** For an update, whether the page uses `updateMany` rather than `updateOne`. */
    val many: Boolean = false,
    /**
     * The results read as documents, when every block reads that way: what a correction's pointer
     * addresses. The scenario never carries them; see [printed].
     */
    val documents: List<BsonValue>? = null,
)

data class FormOverride(
    val fixture: String,
    val origin: String,
    val source: FormSource,
    /** Git blob id of the page, checkable against the repository at [FormSource.commit]. */
    val pageBlob: String,
    /** Pages spliced in by `.. include::`, in the order they were read. */
    val includes: List<FormInclude>,
    /** Include targets left in place because they are not tracked files (generated extracts). */
    val unexpandedIncludes: List<String>,
    val example: String,
    val shape: ActionShape,
    val blocks: List<SourceBlock>,
    val rules: List<FormRule>,
    /** The lock this override retired; null for a never-locked fixture's pinned reference. */
    val retiredLock: RepairMarker?,
    /** SHA-256 of every remaining fixture file, by file name. */
    val fixtureFiles: Map<String, String>,
    /** The test's extra collections by data file, as the repair inventory records them. */
    val collections: Map<String, String> = emptyMap(),
    val retypes: List<NumericRetype> = emptyList(),
    val corrections: List<FormCorrection> = emptyList(),
)

data class FormInclude(val path: String, val blob: String)

data class FormRuleDescription(val rule: FormRule, val description: String)

data class FormMigrationReport(
    val schemaVersion: Int = 1,
    val repositoryRevision: String,
    val sourceRepository: String,
    val rules: List<FormRuleDescription>,
    val overrides: List<FormOverride>,
    val unresolved: List<FormUnresolved>,
    val summary: Map<String, Int>,
)

/**
 * The documented reference of each never-locked fixture: the page as of the commit that created the
 * fixture, which reproduces the fixture exactly, or, where the page prints its results as mongosh
 * prints them, satisfies what those printed results prove (counted as `references.printed`).
 * [references] are [FormOverride]s without a retired lock, rules, retypes or corrections;
 * [unresolved] holds the fixtures no candidate reproduces, with the gap of their first candidate.
 * This file is the reviewed artifact: pages are not committed, so offline checks read its recorded
 * source text, and `recheckReferences` re-derives every pin from the fetched pages.
 */
data class LegacyReferenceReport(
    val schemaVersion: Int = 1,
    val repositoryRevision: String,
    val sourceRepository: String,
    val references: List<FormOverride>,
    val unresolved: List<FormUnresolved>,
    val summary: Map<String, Int>,
)

/**
 * Replaces MORPHIA_FORM locks with declared equivalences. The documented example is extracted from
 * the page as it was when the fixture was created; the declared rules are applied to it and to the
 * curated fixture, and the two must then be BSON-identical in action, data, expected results and
 * index. A lock is retired only when the rules are sufficient and each one is necessary.
 */
object FormMigration {
    /** Lock marker names; one fixture spells its lock `lockd`. */
    private val markers = setOf("lock", "lockd")

    private val fixtureFileNames =
        setOf("action.json", "data.json", "expected.json", "index.json", "name") + markers

    /** The fixture's lock marker, or where a `lock` would be if it has none. */
    private fun lock(folder: Path): Path =
        markers.map { folder.resolve(it) }.firstOrNull { it.isRegularFile() }
            ?: folder.resolve("lock")

    /** A locked fixture and the page it is believed to come from. */
    data class Candidate(val fixture: String, val origin: String, val source: FormSource)

    data class Candidates(val schemaVersion: Int, val candidates: List<Candidate>)

    /**
     * Proposes a review: the minimal rules for each reproducible fixture, the gap for the rest.
     * Each fixture uses the first candidate (in the order given) whose page reproduces it; a
     * fixture nothing reproduces reports the gap of its first candidate. The proposal is input for
     * a reviewer, not an accepted declaration.
     */
    fun draft(
        repository: Path,
        pages: Path,
        sourceRepository: String,
        candidates: List<List<Candidate>>,
        facts: Map<String, FixtureFacts>,
    ): FormReview {
        val root = repository.toRealPath()
        val declarations = mutableListOf<FormDeclaration>()
        val unresolved = mutableListOf<FormUnresolved>()
        candidates
            .flatten()
            .groupBy { it.fixture }
            .toSortedMap()
            .forEach { (fixture, choices) ->
                val lock = lock(folder(root, fixture))
                require(lock.isRegularFile()) { "$fixture has no lock" }
                val known = facts(facts, fixture)
                val outcomes = choices.map { it to examine(root, pages, fixture, it.source, known) }
                val reproduced = outcomes.firstOrNull { it.second is Examined.Reproduced }
                if (reproduced != null) {
                    val (candidate, outcome) = reproduced
                    outcome as Examined.Reproduced
                    declarations +=
                        FormDeclaration(
                            fixture,
                            candidate.origin,
                            candidate.source,
                            Files.readString(lock),
                            outcome.rules,
                            pageBlob = outcome.pageBlob,
                            includes = outcome.includes,
                            retypes = outcome.retypes,
                            corrections = outcome.corrections,
                            marker = lock.name,
                        )
                } else {
                    val (candidate, outcome) = outcomes.first()
                    unresolved +=
                        FormUnresolved(
                            fixture,
                            candidate.origin,
                            candidate.source,
                            (outcome as Examined.Gap).gap,
                            outcome.detail,
                        )
                }
            }
        return FormReview(1, sourceRepository, declarations, unresolved)
    }

    fun capture(
        repository: Path,
        revision: String,
        pages: Path,
        review: FormReview,
        facts: Map<String, FixtureFacts>,
    ): FormMigrationReport {
        require(review.schemaVersion == 1) { "Unsupported form review ${review.schemaVersion}" }
        val root = repository.toRealPath()
        val problems = mutableListOf<String>()
        val fixtures =
            review.declarations.map { it.fixture } +
                review.unresolved.map { it.fixture } +
                review.migrated.map { it.fixture }
        fixtures
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { problems += "Fixture reviewed more than once: $it" }
        val overrides =
            review.declarations.mapNotNull { declaration ->
                val folder = folder(root, declaration.fixture)
                val lock = lock(folder)
                if (lock.isRegularFile() && Files.readString(lock) != declaration.lock) {
                    problems += "${declaration.fixture}: lock text differs from the declaration"
                }
                if (lock.isRegularFile() && lock.name != declaration.marker) {
                    problems +=
                        "${declaration.fixture}: lock is ${lock.name}, declared ${declaration.marker}"
                }
                val known = facts(facts, declaration.fixture)
                when (
                    val outcome =
                        examine(root, pages, declaration.fixture, declaration.source, known)
                ) {
                    is Examined.Gap -> {
                        problems +=
                            "${declaration.fixture}: declared rules do not reproduce the fixture: " +
                                "${outcome.gap} ${outcome.detail}"
                        null
                    }
                    is Examined.Reproduced -> {
                        val alternatives = FormRule.ALTERNATIVE_COMMANDS in declaration.rules
                        if (alternatives == declaration.justification.isNullOrBlank()) {
                            problems +=
                                "${declaration.fixture}: a justification is required exactly " +
                                    "when ALTERNATIVE_COMMANDS is declared"
                        }
                        if (outcome.rules != declaration.rules) {
                            problems +=
                                "${declaration.fixture}: needs rules ${outcome.rules}, " +
                                    "declared ${declaration.rules}"
                        }
                        if (outcome.retypes != declaration.retypes) {
                            problems +=
                                "${declaration.fixture}: needs retypes ${outcome.retypes}, " +
                                    "declared ${declaration.retypes}"
                        }
                        if (outcome.corrections != declaration.corrections) {
                            problems +=
                                "${declaration.fixture}: needs corrections " +
                                    "${outcome.corrections}, declared ${declaration.corrections}"
                        }
                        if (
                            outcome.pageBlob != declaration.pageBlob ||
                                outcome.includes != declaration.includes
                        ) {
                            problems +=
                                "${declaration.fixture}: page or include blobs differ from the review"
                        }
                        FormOverride(
                            fixture = declaration.fixture,
                            origin = declaration.origin,
                            source = declaration.source,
                            pageBlob = outcome.pageBlob,
                            includes = outcome.includes,
                            unexpandedIncludes = outcome.unexpandedIncludes,
                            example = outcome.example,
                            shape = outcome.shape,
                            blocks = outcome.blocks,
                            rules = declaration.rules,
                            retiredLock =
                                RepairMarker(
                                    declaration.marker,
                                    declaration.lock,
                                    sha256(declaration.lock.toByteArray()),
                                ),
                            fixtureFiles = fixtureFiles(folder),
                            collections = known.collections,
                            retypes = declaration.retypes,
                            corrections = declaration.corrections,
                        )
                    }
                }
            }
        review.unresolved.forEach { unresolved ->
            if (!lock(folder(root, unresolved.fixture)).isRegularFile()) {
                problems += "${unresolved.fixture}: an unresolved fixture must keep its lock"
            }
            val known = facts(facts, unresolved.fixture)
            when (
                val outcome = examine(root, pages, unresolved.fixture, unresolved.source, known)
            ) {
                is Examined.Reproduced ->
                    if (unresolved.gap != FormGap.REJECTED) {
                        problems +=
                            "${unresolved.fixture}: reproducible with ${outcome.rules}; declare it"
                    } else if (unresolved.detail.isBlank()) {
                        problems += "${unresolved.fixture}: a rejection needs the reviewer's reason"
                    }
                is Examined.Gap ->
                    if (outcome.gap != unresolved.gap || outcome.detail != unresolved.detail) {
                        problems +=
                            "${unresolved.fixture}: gap is ${outcome.gap} ${outcome.detail}, " +
                                "reviewed ${unresolved.gap} ${unresolved.detail}"
                    }
            }
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
        return report(revision, review, overrides)
    }

    /**
     * Pins the documented reference of each never-locked fixture in [candidates]: the first
     * candidate (in the order given) whose page reproduces the fixture exactly. A never-locked
     * fixture was written from its page and never edited to match Morphia, so it needs no rule; one
     * that reproduces only under rules, retypes or corrections is left unresolved for review.
     */
    fun references(
        repository: Path,
        revision: String,
        pages: Path,
        sourceRepository: String,
        candidates: List<List<Candidate>>,
        facts: Map<String, FixtureFacts>,
    ): LegacyReferenceReport {
        val root = repository.toRealPath()
        val references = mutableListOf<FormOverride>()
        val unresolved = mutableListOf<FormUnresolved>()
        candidates
            .flatten()
            .groupBy { it.fixture }
            .toSortedMap()
            .forEach { (fixture, choices) ->
                val folder = folder(root, fixture)
                require(!lock(folder).isRegularFile()) { "$fixture is locked" }
                val known = facts(facts, fixture)
                require(known.categories.isEmpty()) { "$fixture has repair categories" }
                val outcomes = choices.map { it to examine(root, pages, fixture, it.source, known) }
                val exact =
                    outcomes.firstOrNull { (_, outcome) ->
                        outcome is Examined.Reproduced &&
                            outcome.rules.isEmpty() &&
                            outcome.retypes.isEmpty() &&
                            outcome.corrections.isEmpty()
                    }
                if (exact != null) {
                    val (candidate, outcome) = exact
                    outcome as Examined.Reproduced
                    references +=
                        FormOverride(
                            fixture = fixture,
                            origin = candidate.origin,
                            source = candidate.source,
                            pageBlob = outcome.pageBlob,
                            includes = outcome.includes,
                            unexpandedIncludes = outcome.unexpandedIncludes,
                            example = outcome.example,
                            shape = outcome.shape,
                            blocks = outcome.blocks,
                            rules = emptyList(),
                            retiredLock = null,
                            fixtureFiles = fixtureFiles(folder),
                            collections = known.collections,
                        )
                } else {
                    val (candidate, outcome) = outcomes.first()
                    unresolved +=
                        when (outcome) {
                            is Examined.Gap ->
                                FormUnresolved(
                                    fixture,
                                    candidate.origin,
                                    candidate.source,
                                    outcome.gap,
                                    outcome.detail,
                                )
                            is Examined.Reproduced ->
                                FormUnresolved(
                                    fixture,
                                    candidate.origin,
                                    candidate.source,
                                    FormGap.RESIDUAL_DIFFERENCE,
                                    "reproduces only with rules ${outcome.rules}",
                                )
                        }
                }
            }
        val summary = sortedMapOf<String, Int>()
        summary["references"] = references.size
        summary["references.afterOrigin"] =
            references.count { it.source.relation == SourceRelation.AFTER_ORIGIN }
        // Printed results prove constraints, not BSON values: a printed 4 is an Int32 or a Double.
        summary["references.printed"] = references.count { printed(it.blocks) }
        summary["unresolved"] = unresolved.size
        FormGap.values().forEach { gap -> summary["gap.$gap"] = unresolved.count { it.gap == gap } }
        return LegacyReferenceReport(
            repositoryRevision = revision,
            sourceRepository = sourceRepository,
            references = references,
            unresolved = unresolved,
            summary = summary,
        )
    }

    /**
     * Rechecks committed references against the tree without the pages: each recorded source text
     * must still reproduce its fixture exactly. Fixtures in [moved] left the legacy layout (the
     * migration ledger checks those against their reference instead).
     */
    fun verifyReferences(
        repository: Path,
        report: LegacyReferenceReport,
        facts: Map<String, FixtureFacts>,
        moved: Set<String>,
    ) {
        val root = repository.toRealPath()
        val problems = mutableListOf<String>()
        (report.references.map { it.fixture } + report.unresolved.map { it.fixture })
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { problems += "Fixture referenced more than once: $it" }
        report.references.forEach { reference ->
            if (
                reference.retiredLock != null ||
                    reference.rules.isNotEmpty() ||
                    reference.retypes.isNotEmpty() ||
                    reference.corrections.isNotEmpty()
            ) {
                problems += "${reference.fixture}: a reference reproduces its fixture exactly"
            }
            reference.blocks.forEach {
                if (sha256(it.text.toByteArray()) != it.sha256) {
                    problems += "${reference.fixture}: ${it.role} source text digest mismatch"
                }
            }
            if (reference.fixture in moved) return@forEach
            val folder = folder(root, reference.fixture)
            if (lock(folder).isRegularFile()) {
                problems += "${reference.fixture}: a referenced fixture is locked"
            }
            if (fixtureFiles(folder) != reference.fixtureFiles) {
                problems += "${reference.fixture}: fixture files changed since the reference"
            }
            val known = facts(facts, reference.fixture)
            if (known.collections != reference.collections) {
                problems += "${reference.fixture}: test collections changed since the reference"
            }
            when (val outcome = reproduce(folder, reference.shape, reference.blocks, known)) {
                is Examined.Gap ->
                    problems += "${reference.fixture}: ${outcome.gap} ${outcome.detail}"
                is Examined.Reproduced ->
                    if (
                        outcome.rules.isNotEmpty() ||
                            outcome.retypes.isNotEmpty() ||
                            outcome.corrections.isNotEmpty()
                    ) {
                        problems += "${reference.fixture}: no longer reproduces exactly"
                    }
            }
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
    }

    /**
     * Re-derives [report] from the pages it pins: each reference's page must still have its blob
     * and yield the same example, blocks and includes (a fixture in [moved] is gone, so its example
     * is selected by its recorded name and not compared with the fixture); each unresolved fixture
     * must still fail with its recorded gap. This is the networked half of `verifyReferences`.
     */
    fun recheckReferences(
        repository: Path,
        pages: Path,
        report: LegacyReferenceReport,
        facts: Map<String, FixtureFacts>,
        moved: Set<String>,
    ) {
        val root = repository.toRealPath()
        val problems = mutableListOf<String>()
        report.references.forEach { reference ->
            val page = pages.resolve(reference.source.commit).resolve(reference.source.path)
            if (!page.isRegularFile() || gitBlob(Files.readAllBytes(page)) != reference.pageBlob) {
                problems += "${reference.fixture}: the pinned page is missing or has another blob"
                return@forEach
            }
            // A moved reference's fixture is gone: its example is re-extracted by its recorded
            // name, so the recorded text is still shown to come from the page.
            val known =
                if (reference.fixture in moved) FixtureFacts(emptyList())
                else facts(facts, reference.fixture)
            val recorded = reference.example.takeIf { reference.fixture in moved }
            when (
                val outcome =
                    examine(root, pages, reference.fixture, reference.source, known, recorded)
            ) {
                is Examined.Gap ->
                    problems += "${reference.fixture}: ${outcome.gap} ${outcome.detail}"
                is Examined.Reproduced ->
                    if (
                        outcome.blocks != reference.blocks ||
                            outcome.example != reference.example ||
                            outcome.includes != reference.includes ||
                            outcome.shape != reference.shape ||
                            outcome.rules.isNotEmpty()
                    ) {
                        problems += "${reference.fixture}: the page no longer yields its reference"
                    }
            }
        }
        report.unresolved.forEach { unresolved ->
            if (unresolved.fixture in moved) {
                problems += "${unresolved.fixture}: an unresolved fixture cannot have moved"
                return@forEach
            }
            val known = facts(facts, unresolved.fixture)
            val outcome = examine(root, pages, unresolved.fixture, unresolved.source, known)
            val gap =
                when (outcome) {
                    is Examined.Gap -> outcome.gap to outcome.detail
                    is Examined.Reproduced ->
                        FormGap.RESIDUAL_DIFFERENCE to "reproduces only with rules ${outcome.rules}"
                }
            if (gap != (unresolved.gap to unresolved.detail)) {
                problems +=
                    "${unresolved.fixture}: gap is ${gap.first} ${gap.second}, " +
                        "recorded ${unresolved.gap} ${unresolved.detail}"
            }
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
    }

    /** Whether [blocks] print their results as mongosh prints them rather than as documents. */
    private fun printed(blocks: List<SourceBlock>): Boolean =
        blocks.filter { it.role == BlockRole.EXPECTED }.any { documents(it.text) == null }

    /**
     * Rechecks a committed report against the tree without the documentation pages: the recorded
     * source text must still reproduce each fixture under exactly its declared rules, and each
     * retired lock must be gone while each unresolved fixture keeps its lock. [facts] come from the
     * repair inventory; each override must record the same collections.
     */
    fun verify(
        repository: Path,
        review: FormReview,
        report: FormMigrationReport,
        facts: Map<String, FixtureFacts>,
    ) {
        val root = repository.toRealPath()
        val problems = mutableListOf<String>()
        val declared = review.declarations.associateBy { it.fixture }
        if (declared.keys != report.overrides.map { it.fixture }.toSet()) {
            problems += "Overrides do not match the reviewed declarations"
        }
        if (review.unresolved != report.unresolved) {
            problems += "Unresolved fixtures do not match the review"
        }
        report.overrides.forEach { override ->
            val declaration = declared[override.fixture] ?: return@forEach
            if (
                declaration.rules != override.rules ||
                    declaration.source != override.source ||
                    declaration.lock != override.retiredLock?.text ||
                    declaration.pageBlob != override.pageBlob ||
                    declaration.includes != override.includes ||
                    declaration.retypes != override.retypes ||
                    declaration.corrections != override.corrections
            ) {
                problems += "${override.fixture}: override differs from its declaration"
            }
            override.blocks.forEach {
                if (sha256(it.text.toByteArray()) != it.sha256) {
                    problems += "${override.fixture}: ${it.role} source text digest mismatch"
                }
            }
            val folder = folder(root, override.fixture)
            if (lock(folder).isRegularFile()) {
                problems += "${override.fixture}: retired lock is still present"
            }
            if (fixtureFiles(folder) != override.fixtureFiles) {
                problems += "${override.fixture}: fixture files changed since the override"
            }
            val known = facts(facts, override.fixture)
            if (known.collections != override.collections) {
                problems += "${override.fixture}: test collections changed since the override"
            }
            when (val outcome = reproduce(folder, override.shape, override.blocks, known)) {
                is Examined.Gap ->
                    problems += "${override.fixture}: ${outcome.gap} ${outcome.detail}"
                is Examined.Reproduced -> {
                    if (outcome.rules != override.rules) {
                        problems +=
                            "${override.fixture}: needs rules ${outcome.rules}, " +
                                "declared ${override.rules}"
                    }
                    if (outcome.retypes != override.retypes) {
                        problems +=
                            "${override.fixture}: needs retypes ${outcome.retypes}, " +
                                "declared ${override.retypes}"
                    }
                    if (outcome.corrections != override.corrections) {
                        problems +=
                            "${override.fixture}: needs corrections ${outcome.corrections}, " +
                                "declared ${override.corrections}"
                    }
                }
            }
        }
        report.unresolved.forEach {
            if (!lock(folder(root, it.fixture)).isRegularFile()) {
                problems += "${it.fixture}: unresolved fixture lost its lock"
            }
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
    }

    /** Each example's lock categories and extra collections, from the repair inventory. */
    fun facts(inventory: CuratedRepairReport): Map<String, FixtureFacts> =
        inventory.examples.associate {
            it.fixture to FixtureFacts(it.categories, it.collections, it.helpers)
        }

    private fun facts(facts: Map<String, FixtureFacts>, fixture: String): FixtureFacts =
        requireNotNull(facts[fixture]) { "$fixture is not in the repair inventory" }

    /** Deletes each retired lock after checking that every one still holds the recorded text. */
    fun apply(repository: Path, report: FormMigrationReport) {
        val root = repository.toRealPath()
        val locks =
            report.overrides.map {
                folder(root, it.fixture).resolve(requireNotNull(it.retiredLock).name) to it
            }
        val problems =
            locks.mapNotNull { (lock, override) ->
                when {
                    !lock.isRegularFile() -> null
                    sha256(Files.readAllBytes(lock)) != override.retiredLock?.sha256 ->
                        "${override.fixture}: lock changed since capture"
                    else -> null
                }
            }
        require(problems.isEmpty()) { problems.joinToString("\n") }
        locks.forEach { (lock, _) -> Files.deleteIfExists(lock) }
    }

    private fun report(
        revision: String,
        review: FormReview,
        overrides: List<FormOverride>,
    ): FormMigrationReport {
        val sorted = overrides.sortedBy { it.fixture }
        val summary = sortedMapOf<String, Int>()
        summary["overrides"] = sorted.size
        summary["overrides.exact"] =
            sorted.count { it.rules.isEmpty() && it.retypes.isEmpty() && it.corrections.isEmpty() }
        summary["overrides.retyped"] = sorted.count { it.retypes.isNotEmpty() }
        summary["overrides.corrected"] = sorted.count { it.corrections.isNotEmpty() }
        summary["unresolved"] = review.unresolved.size
        FormRule.values().forEach { rule ->
            summary["rule.$rule"] = sorted.count { rule in it.rules }
        }
        FormGap.values().forEach { gap ->
            summary["gap.$gap"] = review.unresolved.count { it.gap == gap }
        }
        return FormMigrationReport(
            repositoryRevision = revision,
            sourceRepository = review.repository,
            rules = FormRule.values().map { FormRuleDescription(it, it.description) },
            overrides = sorted,
            unresolved = review.unresolved.sortedBy { it.fixture },
            summary = summary,
        )
    }

    private sealed interface Examined {
        data class Reproduced(
            val rules: List<FormRule>,
            val shape: ActionShape,
            val blocks: List<SourceBlock>,
            val pageBlob: String = "",
            val includes: List<FormInclude> = emptyList(),
            val unexpandedIncludes: List<String> = emptyList(),
            val example: String = "",
            val retypes: List<NumericRetype> = emptyList(),
            val corrections: List<FormCorrection> = emptyList(),
        ) : Examined

        data class Gap(val gap: FormGap, val detail: String) : Examined
    }

    /**
     * Extracts [fixture]'s example from [source] and compares it with the fixture. With [recorded]
     * (an example name), the example is selected by that name and not compared: the fixture may be
     * gone, and the result carries only what the page yields.
     */
    private fun examine(
        root: Path,
        pages: Path,
        fixture: String,
        source: FormSource,
        facts: FixtureFacts,
        recorded: String? = null,
    ): Examined {
        val folder = folder(root, fixture)
        val page = pages.resolve(source.commit).resolve(source.path)
        require(page.isRegularFile()) { "Missing source page $page" }
        val bytes = Files.readAllBytes(page)
        val blob = gitBlob(bytes)
        val type =
            when (fixture.substringBeforeLast('/').substringBeforeLast('/')) {
                "aggregation/expressions" -> OperatorType.EXPRESSION
                "aggregation/stages" -> OperatorType.STAGE
                "query/filters" -> OperatorType.FILTER
                "query/updates" -> OperatorType.UPDATE
                else -> throw IllegalArgumentException("Not an operator fixture: $fixture")
            }
        val includes = mutableListOf<FormInclude>()
        val unexpanded = mutableListOf<String>()
        val examples =
            try {
                val lines =
                    expand(
                        pages.resolve(source.commit),
                        source.path.substring(0, source.path.indexOf("source/") + "source".length),
                        source.path,
                        includes,
                        unexpanded,
                        0,
                    )
                RstDocument(
                        Operator(type, page.toFile()),
                        lines
                            .dropWhile { line -> line.trim() !in listOf("Example", "Examples") }
                            .toMutableList(),
                    )
                    .examples
            } catch (failure: RuntimeException) {
                return Examined.Gap(FormGap.PAGE_UNSUPPORTED, failure.message ?: "$failure")
            }
        if (recorded == null && !folder.resolve("name").isRegularFile()) {
            return Examined.Gap(FormGap.FIXTURE_UNSUPPORTED, "the fixture has no name file")
        }
        val name =
            recorded ?: Files.readAllLines(folder.resolve("name")).firstOrNull()?.trim() ?: ""
        val example =
            examples.singleOrNull { it.name == name }
                ?: return Examined.Gap(FormGap.NO_MATCHING_EXAMPLE, name)
        fun block(role: BlockRole, lines: List<String>): SourceBlock {
            val text = lines.joinToString("\n")
            return SourceBlock(role, text, sha256(text.toByteArray()))
        }
        val blocks =
            listOfNotNull(example.actionBlock?.let { block(BlockRole.ACTION, it.code()) }) +
                example.dataBlock.map { block(BlockRole.DATA, it.code()) } +
                listOfNotNull(
                    example.expectedBlock?.let { block(BlockRole.EXPECTED, it.code()) },
                    example.indexBlock?.let { block(BlockRole.INDEX, it.code()) },
                )
        val shape =
            when (val documented = documented(blocks)) {
                is Examined.Gap -> return documented
                else -> (documented as Documented).alternatives.first().shape
            }
        if (recorded != null) {
            return Examined.Reproduced(
                emptyList(),
                shape,
                blocks,
                pageBlob = blob,
                includes = includes.distinct(),
                example = example.name,
            )
        }
        return when (val outcome = reproduce(folder, shape, blocks, facts)) {
            is Examined.Gap -> outcome
            is Examined.Reproduced ->
                outcome.copy(
                    pageBlob = blob,
                    example = example.name,
                    includes = includes.distinct(),
                )
        }
    }

    /**
     * The documented operation an override was proven against, read from its recorded source text.
     * Its declared rules travel with it to the runner, and its retypes and corrections are applied
     * by `FixtureMigration`; alternative commands are not supported.
     */
    fun documented(override: FormOverride): DocumentedScenario {
        // The documented data names every collection, so a test's extra collections need nothing
        // more; their legacy data files were loaded under those same names.
        require(FormRule.ALTERNATIVE_COMMANDS !in override.rules) {
            "${override.fixture}: alternative commands are not supported"
        }
        override.blocks.forEach {
            require(sha256(it.text.toByteArray()) == it.sha256) {
                "${override.fixture}: ${it.role} source text digest mismatch"
            }
        }
        val documented =
            when (val parsed = documented(override.blocks)) {
                is Examined.Gap ->
                    throw IllegalArgumentException("${override.fixture}: ${parsed.detail}")
                else -> parsed as Documented
            }
        val action = documented.alternatives.single()
        require(action.shape == override.shape) { "${override.fixture}: action is ${action.shape}" }
        val expected = override.blocks.filter { it.role == BlockRole.EXPECTED }
        require(override.blocks.none { it.role == BlockRole.INDEX }) {
            "${override.fixture}: indexes are not supported"
        }
        val printed =
            if (expected.isEmpty()) null
            else
                when (val read = published(documented.collection, expected)) {
                    is Examined.Gap ->
                        throw IllegalArgumentException("${override.fixture}: ${read.detail}")
                    else -> @Suppress("UNCHECKED_CAST") (read as List<PublishedDocument>)
                }
        val listed = expected.map { documents(it.text) }
        return DocumentedScenario(
            documented.collection,
            documented.data.mapValues { (_, values) -> values.map { it.asDocument() } },
            action.shape,
            action.values,
            printed,
            action.many,
            listed.takeIf { it.isNotEmpty() && it.all { rows -> rows != null } }?.flatMap { it!! },
        )
    }

    private data class Action(
        val shape: ActionShape,
        val values: List<BsonValue>,
        val many: Boolean,
    )

    /**
     * The documented example lowered to commands; it never executes anything. [data] holds the
     * documented documents by collection; [collection] is the one the action runs against.
     */
    private class Documented(
        val alternatives: List<Action>,
        val collection: String,
        val data: Map<String, List<BsonValue>>,
    )

    private fun documented(blocks: List<SourceBlock>): Any {
        fun gap(gap: FormGap, detail: String) = Examined.Gap(gap, detail)
        val action =
            blocks.singleOrNull { it.role == BlockRole.ACTION }
                ?: return gap(FormGap.SOURCE_UNSUPPORTED, "No single action block")
        // A data block is either a shell insert program or a plain listing of documents.
        val (programs, listings) =
            blocks
                .filter { it.role == BlockRole.DATA }
                .partition { ".insertOne(" in it.text || ".insertMany(" in it.text }
        val program =
            ShellPrograms.normalize(
                ShellProgramRequest(
                    dialect = ShellDialect.MONGOSH,
                    blocks =
                        programs.mapIndexed { index, block ->
                            ProgramBlock("data$index", block.text, ProgramRole.SETUP)
                        } + ProgramBlock("action", action.text, ProgramRole.ACTION),
                )
            )
        if (program.state != ShellLiteralState.NORMALIZED) {
            return gap(FormGap.SOURCE_UNSUPPORTED, program.problems.joinToString { it.code })
        }
        val inserts =
            program.commands
                .filter { it.role == ProgramRole.SETUP }
                .map {
                    it.operation as? InsertOperation
                        ?: return gap(FormGap.SOURCE_UNSUPPORTED, "setup ${it.operation.kind}")
                }
        val commands = program.commands.filter { it.role == ProgramRole.ACTION }
        if (commands.isEmpty() || commands.map { it.namespace }.toSet().size != 1) {
            return gap(FormGap.SOURCE_UNSUPPORTED, "${commands.size} action commands")
        }
        val namespaces =
            (commands + program.commands.filter { it.role == ProgramRole.SETUP }).map {
                it.namespace
            }
        if (namespaces.any { it == null || it.database != null }) {
            return gap(FormGap.SOURCE_UNSUPPORTED, "command outside the current database")
        }
        val collection = commands.first().namespace!!.collection
        val inserted =
            program.commands
                .filter { it.role == ProgramRole.SETUP }
                .zip(inserts)
                .groupBy({ it.first.namespace!!.collection }) { (_, insert) ->
                    insert.documents.map { it.value() }
                }
                .mapValues { (_, documents) -> documents.flatten() }
        // A listing names no collection; it is the action's only when nothing else is documented.
        if (listings.isNotEmpty() && (inserted.keys - collection).isNotEmpty()) {
            return gap(FormGap.SOURCE_UNSUPPORTED, "document listing beside several collections")
        }
        val listed =
            listings.flatMap {
                documents(it.text) ?: return gap(FormGap.SOURCE_UNSUPPORTED, "data syntax")
            }
        val data =
            if (listings.isEmpty()) inserted
            else inserted + (collection to inserted[collection].orEmpty() + listed)
        val alternatives =
            commands.map { command ->
                when (val operation = command.operation) {
                    is AggregateOperation ->
                        if (operation.options != null)
                            return gap(FormGap.FIXTURE_UNSUPPORTED, "aggregate options")
                        else
                            Action(
                                ActionShape.PIPELINE,
                                operation.pipeline.value().asArray().values,
                                false,
                            )
                    is FindOperation ->
                        if (operation.modifiers.isNotEmpty())
                            return gap(FormGap.FIXTURE_UNSUPPORTED, "find modifiers")
                        else
                            Action(
                                ActionShape.FIND,
                                listOfNotNull(
                                    operation.filter.value(),
                                    operation.projection?.value(),
                                ),
                                false,
                            )
                    is UpdateOperation ->
                        if (operation.options != null)
                            return gap(FormGap.FIXTURE_UNSUPPORTED, "update options")
                        else
                            Action(
                                ActionShape.UPDATE,
                                listOf(operation.filter.value(), operation.update.value()),
                                operation.sourceMethod == "updateMany",
                            )
                    else -> return gap(FormGap.SOURCE_UNSUPPORTED, "${operation.kind} action")
                }
            }
        if (alternatives.map { it.shape to it.many }.toSet().size != 1) {
            return gap(FormGap.SOURCE_UNSUPPORTED, "action commands of different kinds")
        }
        return Documented(alternatives, collection, data)
    }

    private fun reproduce(
        folder: Path,
        shape: ActionShape,
        blocks: List<SourceBlock>,
        facts: FixtureFacts,
    ): Examined {
        val collections = facts.collections
        val files = Files.list(folder).use { stream -> stream.map { it.name }.sorted().toList() }
        (files - fixtureFileNames - collections.keys)
            .takeIf { it.isNotEmpty() }
            ?.let {
                return Examined.Gap(FormGap.FIXTURE_UNSUPPORTED, "files $it")
            }
        val documented =
            when (val parsed = documented(blocks)) {
                is Examined.Gap -> return parsed
                else -> parsed as Documented
            }
        val first = documented.alternatives.first()
        // The fixture's action is read as the kind its test runs, never as the page's kind.
        val tested =
            facts.tested ?: return Examined.Gap(FormGap.FIXTURE_UNSUPPORTED, "not one test helper")
        if (first.shape != shape || first.shape != tested) {
            return Examined.Gap(
                FormGap.RESIDUAL_DIFFERENCE,
                "action is ${first.shape}, the test runs $tested",
            )
        }
        val curatedText = Files.readString(folder.resolve("action.json"))
        if (shape == ActionShape.UPDATE && ("updateMany" in curatedText) != first.many) {
            return Examined.Gap(FormGap.RESIDUAL_DIFFERENCE, "updateOne/updateMany differs")
        }
        val curated =
            documents(unwrap(curatedText.trim()))
                ?: return Examined.Gap(FormGap.FIXTURE_UNSUPPORTED, "action.json syntax")

        // data.json is loaded into the action's collection; every other documented collection
        // must be a data file the test loads into a collection of that name.
        if (documented.collection in collections.values) {
            return Examined.Gap(
                FormGap.FIXTURE_UNSUPPORTED,
                "a data file targets the action collection",
            )
        }
        (documented.data.keys - documented.collection - collections.values.toSet())
            .sorted()
            .firstOrNull()
            ?.let {
                return Examined.Gap(FormGap.FIXTURE_UNSUPPORTED, "no data file for collection $it")
            }
        // Each data, expected and index file paired with its documented documents, in the order
        // they are compared.
        val compared = mutableListOf<Triple<String, List<BsonValue>, List<BsonValue>>>()
        fun pair(file: String, source: List<BsonValue>?): Examined.Gap? {
            val path = folder.resolve(file)
            if (!path.isRegularFile()) {
                return if (source == null) null
                else Examined.Gap(FormGap.RESIDUAL_DIFFERENCE, "$file is missing")
            }
            if (source == null) {
                return Examined.Gap(FormGap.RESIDUAL_DIFFERENCE, "$file has no documented source")
            }
            val text = Files.readString(path)
            val parsed =
                documents(text) ?: return Examined.Gap(FormGap.FIXTURE_UNSUPPORTED, "$file syntax")
            // The tests read these files one document per line, so a layout that only parses as a
            // whole (wrapped in an array, or a document over several lines) is not equivalent.
            if (!readsByLine(text, parsed)) {
                return Examined.Gap(
                    FormGap.FIXTURE_UNSUPPORTED,
                    "$file is not one document per line",
                )
            }
            compared += Triple(file, source, parsed)
            return null
        }
        (mapOf("data.json" to documented.collection) + collections).toSortedMap().forEach {
            (file, collection) ->
            pair(file, documented.data[collection])?.let {
                return it
            }
        }
        val expected = blocks.filter { it.role == BlockRole.EXPECTED }
        val listed = expected.map { documents(it.text) }
        if (listed.all { it != null }) {
            pair(
                    "expected.json",
                    expected.takeIf { it.isNotEmpty() }?.flatMap { documents(it.text)!! },
                )
                ?.let {
                    return it
                }
        } else {
            printed(folder, documented.collection, expected, facts)?.let {
                return it
            }
        }
        val index = blocks.filter { it.role == BlockRole.INDEX }
        pair(
                "index.json",
                index
                    .takeIf { it.isNotEmpty() }
                    ?.flatMap {
                        documents(unwrap(it.text.trim()))
                            ?: return Examined.Gap(FormGap.SOURCE_UNSUPPORTED, "index syntax")
                    },
            )
            ?.let {
                return it
            }

        /** The first difference under [rules] and [retypes], with the values compared. */
        class Mismatch(
            val detail: String,
            val file: String? = null,
            val difference: BsonDifference? = null,
            val documented: List<BsonValue> = emptyList(),
            val curated: List<BsonValue> = emptyList(),
        )
        fun compare(
            file: String,
            source: List<BsonValue>,
            target: List<BsonValue>,
            retypes: List<NumericRetype>,
            corrections: List<FormCorrection>,
        ): Mismatch? {
            val own = retypes.filter { it.file == file }
            val (retyped, counts) =
                NumericRetypes.apply(source, own) ?: return Mismatch("$file retypes do not apply")
            own.zip(counts)
                .firstOrNull { it.second == 0 }
                ?.let {
                    return Mismatch("$file retype at ${it.first.pointer} changes nothing")
                }
            val values =
                when (
                    val corrected =
                        FormCorrections.apply(retyped, corrections.filter { it.file == file })
                ) {
                    is FormCorrections.Result.Applied -> corrected.values
                    is FormCorrections.Result.Stale ->
                        return Mismatch("$file correction at ${corrected.pointer} is stale")
                }
            val result = BsonComparison.compare(BsonArray(values), BsonArray(target))
            val label = if (file == "action.json") "action" else file
            return when (result.state) {
                BsonComparisonState.EQUAL -> null
                BsonComparisonState.DIFFERENT ->
                    Mismatch(
                        "$label ${result.difference!!.kind} at ${result.difference.path.pointer}",
                        file,
                        result.difference,
                        values,
                        target,
                    )
                BsonComparisonState.INVALID -> Mismatch("$label ${result.problem}")
            }
        }
        fun mismatch(
            rules: Set<FormRule>,
            retypes: List<NumericRetype>,
            corrections: List<FormCorrection> = emptyList(),
        ): Mismatch? {
            compared.forEach { (file, source, target) ->
                compare(file, source, target, retypes, corrections)?.let {
                    return it
                }
            }
            val alternatives =
                when {
                    documented.alternatives.size == 1 -> documented.alternatives
                    FormRule.ALTERNATIVE_COMMANDS in rules -> documented.alternatives
                    else -> return Mismatch("action has ${documented.alternatives.size} commands")
                }
            val target = FormEquivalence.canonical(curated, shape, rules)
            val results =
                alternatives.map {
                    compare(
                        "action.json",
                        FormEquivalence.canonical(it.values, shape, rules),
                        target,
                        retypes,
                        corrections,
                    )
                }
            return if (results.any { it == null }) null else results.first()
        }
        // A NUMERIC_TYPE lock may change number types, a MORPHIA_FORM lock may change form;
        // neither may stand in for the other.
        val allowed =
            if ("MORPHIA_FORM" in facts.categories) FormRule.values().toSet() else emptySet()
        val retyping = "NUMERIC_TYPE" in facts.categories
        val correcting = facts.categories.any { it == "DOCS_ERROR" || it == "FIXTURE_CURATION" }

        class Changes(val retypes: List<NumericRetype>, val corrections: List<FormCorrection>)
        /**
         * The retypes and corrections that reproduce the fixture under [rules], or the first
         * difference neither explains. A difference is retyped when it can be, and corrected only
         * otherwise. Action pointers depend on the rules, so each rule set derives its own.
         */
        fun discover(rules: Set<FormRule>): Pair<Changes?, Mismatch?> {
            var retypes = listOf<NumericRetype>()
            var corrections = listOf<FormCorrection>()
            while (true) {
                val found = mismatch(rules, retypes, corrections) ?: break
                val difference =
                    found.difference?.takeIf { retypes.size + corrections.size < 1000 }
                        ?: return null to found
                val leaf =
                    difference
                        .takeIf { retyping && it.kind == BsonDifferenceKind.TYPE }
                        ?.let {
                            val pointer = it.path.pointer
                            val from = NumericRetypes.at(found.documented, pointer)
                            val to = NumericRetypes.at(found.curated, pointer)
                            val type = NumericRetypes.type(to)
                            if (from == null || to == null || type == null) null
                            else
                                NumericRetypes.convert(from, type)
                                    ?.takeIf { converted -> converted == to }
                                    ?.let {
                                        NumericRetype(
                                            found.file!!,
                                            pointer,
                                            NumericRetypes.type(from)!!,
                                            type,
                                        )
                                    }
                        }
                if (leaf != null) {
                    retypes = retypes + leaf
                    continue
                }
                val pointer = difference.path.pointer
                val before = NumericRetypes.at(found.documented, pointer)
                val after = NumericRetypes.at(found.curated, pointer)
                val correction =
                    when {
                        !correcting -> null
                        // Never a whole file or a whole row or stage: every other difference in
                        // it would go unreviewed.
                        pointer.count { it == '/' } < 2 -> null
                        // Key order is form, and a type change at the same value is a retype.
                        difference.kind == BsonDifferenceKind.KEY_ORDER -> null
                        difference.kind == BsonDifferenceKind.TYPE &&
                            before != null &&
                            after != null &&
                            NumericRetypes.type(after)?.let {
                                NumericRetypes.convert(before, it)
                            } == after -> null
                        difference.kind == BsonDifferenceKind.MISSING_FIELD ->
                            before?.let { FormCorrection.of(found.file!!, pointer, it, null) }
                        difference.kind == BsonDifferenceKind.EXTRA_FIELD -> {
                            val holder =
                                NumericRetypes.at(found.curated, pointer.substringBeforeLast('/'))
                                    as? BsonDocument
                            val key = pointer.substringAfterLast('/')
                            holder
                                ?.keys
                                ?.indexOf(key.replace("~1", "/").replace("~0", "~"))
                                ?.takeIf { it >= 0 }
                                ?.let { FormCorrection.of(found.file!!, pointer, null, after, it) }
                        }
                        before == null || after == null -> null
                        else -> FormCorrection.of(found.file!!, pointer, before, after)
                    } ?: return null to found
                corrections = corrections + correction
            }
            // One retype per file and type change, at the deepest pointer covering its numbers,
            // when that still reproduces the fixture.
            retypes
                .groupBy { Triple(it.file, it.from, it.to) }
                .filterValues { it.size > 1 }
                .forEach { (key, group) ->
                    val merged =
                        retypes - group.toSet() +
                            NumericRetype(
                                key.first,
                                NumericRetypes.common(group.map { it.pointer }),
                                key.second,
                                key.third,
                            )
                    if (mismatch(rules, merged, corrections) == null) retypes = merged
                }
            return Changes(
                retypes.sortedWith(compareBy({ it.file }, { it.pointer })),
                corrections,
            ) to null
        }
        val (found, difference) = discover(allowed)
        var changes = found ?: return Examined.Gap(FormGap.RESIDUAL_DIFFERENCE, difference!!.detail)
        // Drop every rule the comparison does not need, so each declared rule is necessary. A rule
        // is never traded for a correction: dropping it must not need more corrections.
        val needed = allowed.toMutableSet()
        FormRule.values().forEach { rule ->
            if (rule in needed) {
                discover(needed - rule).first?.let {
                    if (it.corrections.size <= changes.corrections.size) {
                        needed -= rule
                        changes = it
                    }
                }
            }
        }
        changes.retypes.forEach { retype ->
            check(mismatch(needed, changes.retypes - retype, changes.corrections) != null) {
                "Unneeded retype $retype"
            }
        }
        changes.corrections.forEach { correction ->
            check(mismatch(needed, changes.retypes, changes.corrections - correction) != null) {
                "Unneeded correction $correction"
            }
        }
        return Examined.Reproduced(
            FormRule.values().filter { it in needed },
            shape,
            blocks,
            retypes = changes.retypes,
            corrections = changes.corrections,
        )
    }

    private val include = Regex("""^(\s*)\.\. include::\s+(/\S+)\s*$""")

    /**
     * Splices `.. include::` targets into the page, as Sphinx does, re-indented to the directive.
     * Includes with options, relative or untracked (generated) targets and other directives are
     * left in place for the extractor, which rejects them only inside an example.
     */
    private fun expand(
        tree: Path,
        sourceRoot: String,
        path: String,
        includes: MutableList<FormInclude>,
        unexpanded: MutableList<String>,
        depth: Int,
        extracts: GizaExtracts = GizaExtracts(tree.resolve("$sourceRoot/includes")),
    ): List<String> {
        require(depth <= 8) { "Includes nest too deeply at $path" }
        val bytes = Files.readAllBytes(tree.resolve(path))
        if (depth > 0) includes += FormInclude(path, gitBlob(bytes))
        val lines = bytes.decodeToString(throwOnInvalidSequence = true).lines()
        return splice(tree, sourceRoot, lines, includes, unexpanded, depth, extracts)
    }

    private fun splice(
        tree: Path,
        sourceRoot: String,
        lines: List<String>,
        includes: MutableList<FormInclude>,
        unexpanded: MutableList<String>,
        depth: Int,
        extracts: GizaExtracts,
    ): List<String> =
        lines.flatMapIndexed { index, line ->
            val match = include.matchEntire(line)
            val next = lines.getOrNull(index + 1)
            val options =
                next != null &&
                    next.isNotBlank() &&
                    next.indexOfFirst { !it.isWhitespace() } > line.indexOf('.') &&
                    next.trim().startsWith(":")
            val target = match?.let { sourceRoot + it.groupValues[2] }
            val indent = match?.groupValues?.get(1) ?: ""
            fun indented(spliced: List<String>) =
                spliced.map { if (it.isBlank()) it else indent + it }
            when {
                target == null || options -> listOf(line)
                tree.resolve(target).isRegularFile() -> {
                    require(".." !in target.split('/')) { "Invalid include $target" }
                    indented(
                        expand(tree, sourceRoot, target, includes, unexpanded, depth + 1, extracts)
                    )
                }
                else -> {
                    // A generated extract is assembled from the YAML Giza builds it from.
                    val prefix = "$sourceRoot/includes/extracts/"
                    val extract =
                        target
                            .takeIf { it.startsWith(prefix) && it.endsWith(".rst") }
                            ?.let { extracts.resolve(it.removePrefix(prefix).removeSuffix(".rst")) }
                    if (extract == null || depth >= 8) {
                        unexpanded += target
                        listOf(line)
                    } else {
                        extract.files.forEach {
                            includes +=
                                FormInclude(
                                    tree.relativize(it).toString(),
                                    gitBlob(Files.readAllBytes(it)),
                                )
                        }
                        indented(
                            splice(
                                tree,
                                sourceRoot,
                                extract.content.lines(),
                                includes,
                                unexpanded,
                                depth + 1,
                                extracts,
                            )
                        )
                    }
                }
            }
        }

    /**
     * Compares expected.json with results printed as mongosh prints them (`Decimal128("1.5")`,
     * `Timestamp({ t, i })`, comments), which the document reader cannot read. Printed text proves
     * less than BSON: a printed `4` may be an Int32 or a Double, so the fixture must satisfy what
     * the text proves ([PublishedComparison]), not equal a guessed value. Printed results take no
     * retype or correction; any difference is a gap.
     */
    private fun printed(
        folder: Path,
        collection: String,
        expected: List<SourceBlock>,
        facts: FixtureFacts,
    ): Examined.Gap? {
        val published =
            when (val read = published(collection, expected)) {
                is Examined.Gap -> return read
                else -> @Suppress("UNCHECKED_CAST") (read as List<PublishedDocument>)
            }
        val path = folder.resolve("expected.json")
        if (!path.isRegularFile()) {
            return Examined.Gap(FormGap.RESIDUAL_DIFFERENCE, "expected.json is missing")
        }
        val text = Files.readString(path)
        val curated =
            documents(text)
                ?: return Examined.Gap(FormGap.FIXTURE_UNSUPPORTED, "expected.json syntax")
        if (!readsByLine(text, curated)) {
            return Examined.Gap(
                FormGap.FIXTURE_UNSUPPORTED,
                "expected.json is not one document per line",
            )
        }
        val result = PublishedComparison.compareResults(published, curated.map { it.asDocument() })
        return when (result.state) {
            // mongosh prints an integral Double as it prints an Int32, so a match cannot show
            // the type a NUMERIC_TYPE lock changed.
            BsonComparisonState.EQUAL ->
                if ("NUMERIC_TYPE" in facts.categories)
                    Examined.Gap(
                        FormGap.SOURCE_UNSUPPORTED,
                        "printed results cannot prove number types",
                    )
                else null
            BsonComparisonState.DIFFERENT ->
                result.difference!!.let {
                    Examined.Gap(
                        FormGap.RESIDUAL_DIFFERENCE,
                        "printed expected ${it.kind} at ${it.path.pointer}",
                    )
                }
            else -> Examined.Gap(FormGap.SOURCE_UNSUPPORTED, "expected ${result.problem}")
        }
    }

    /**
     * The constraints that results printed as mongosh prints them prove, or a gap. Printed text
     * proves less than BSON, so this is a [PublishedDocument] per result, never a guessed value.
     */
    private fun published(collection: String, expected: List<SourceBlock>): Any {
        // The results are read back as the default mongosh profile reads them; a find on the
        // action's collection certifies that profile for every action kind.
        if (collection.any { it == '"' || it == '\\' || it.isISOControl() }) {
            return Examined.Gap(FormGap.SOURCE_UNSUPPORTED, "collection name $collection")
        }
        val owner = "db.getCollection(\"$collection\").find({})"
        val program =
            ShellPrograms.normalize(
                ShellProgramRequest(
                    dialect = ShellDialect.MONGOSH,
                    blocks =
                        listOf(ProgramBlock("owner", owner, ProgramRole.ACTION)) +
                            expected.mapIndexed { index, block ->
                                ProgramBlock(
                                    "expected$index",
                                    block.text,
                                    ProgramRole.EXPECTED,
                                    ProgramFormat.SHELL_RESULTS,
                                )
                            },
                )
            )
        if (program.state != ShellLiteralState.NORMALIZED) {
            return Examined.Gap(FormGap.SOURCE_UNSUPPORTED, "expected syntax")
        }
        return program.expectations.flatMap { it.published.orEmpty() }
    }

    /**
     * Whether `TemplatedTestBase.loadJson` reads [text] as [documents]: each line is one document,
     * after stripping a `[`...`]` that wraps the whole line.
     */
    private fun readsByLine(text: String, documents: List<BsonValue>): Boolean {
        val lines = text.split('\n').let { if (it.last().isEmpty()) it.dropLast(1) else it }
        val rows =
            lines.map { line ->
                val json =
                    if (line.startsWith("[") && line.endsWith("]"))
                        line.substring(1, line.length - 1)
                    else line
                runCatching { BsonDocument.parse(json) }.getOrNull() ?: return false
            }
        return BsonComparison.compare(BsonArray(rows), BsonArray(documents)).state ==
            BsonComparisonState.EQUAL
    }

    /** The template helpers drop a leading `db.<collection>.<method>(` and its closing paren. */
    private fun unwrap(text: String): String =
        if (text.startsWith("db.")) text.substringAfter("(").substringBeforeLast(")") else text

    private fun documents(text: String): List<BsonValue>? =
        LegacyDocuments.parse(text).takeIf { it.state == ShellLiteralState.NORMALIZED }?.documents

    private fun folder(root: Path, fixture: String): Path {
        require(".." !in fixture.split('/')) { "Invalid fixture $fixture" }
        return root.resolve("core/src/test/resources/dev/morphia/test/$fixture")
    }

    private fun fixtureFiles(folder: Path): Map<String, String> =
        Files.list(folder).use { stream ->
            stream
                .filter { it.isRegularFile() && it.name !in markers }
                .toList()
                .sortedBy { it.name }
                .associate { it.name to sha256(Files.readAllBytes(it)) }
        }

    private fun gitBlob(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1")
            .apply {
                update("blob ${bytes.size}\u0000".toByteArray())
                update(bytes)
            }
            .digest()
            .let { HexFormat.of().formatHex(it) }

    private fun sha256(bytes: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
}
