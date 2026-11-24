package dev.morphia.audits.generate

import com.fasterxml.jackson.annotation.JsonInclude
import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.documents.DocumentReport
import dev.morphia.audits.naming.DescriptiveNames
import dev.morphia.audits.repairs.BlockRole
import dev.morphia.audits.repairs.CaseManifests
import dev.morphia.audits.repairs.FormInclude
import dev.morphia.audits.repairs.FormSource
import dev.morphia.audits.repairs.SourceBlock
import dev.morphia.audits.scenarios.ExtractedScenario
import dev.morphia.audits.scenarios.ScenarioCode
import dev.morphia.audits.scenarios.ScenarioExtraction
import dev.morphia.audits.scenarios.ScenarioExtractionPlan
import dev.morphia.audits.scenarios.ScenarioPageRule
import dev.morphia.audits.scenarios.ScenarioVariant
import dev.morphia.audits.shell.ProgramRole
import dev.morphia.audits.shell.ShellLiteralState
import dev.morphia.audits.sources.VerifiedSourceBundle
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

enum class ExampleStatus {
    /** A legacy fixture or a manifest case already owns the example. */
    OWNED,
    /** No test owns the example, and it has a descriptive title: a generation candidate. */
    UNOWNED,
    /** No test owns the example, and its title is generic, so it needs a reviewed name. */
    UNNAMED,
    /**
     * The example matches more than one owner, or shares its name with another example; it keeps
     * its source blocks for the review that resolves it.
     */
    AMBIGUOUS,
    /** No test owns the example, and the extractor found no program to generate it from. */
    UNSUPPORTED,
}

/**
 * One example on a current page. [owner] is the owning fixture, by title or, when the title changed
 * since the fixture was written, by its action text or its descriptive name. An unowned example
 * keeps its source [blocks], so generation never needs the page.
 */
data class DocumentedExample(
    val title: String,
    val status: ExampleStatus,
    val stem: String? = null,
    val owner: String? = null,
    val detail: String? = null,
    val blocks: List<SourceBlock> = emptyList(),
    /** The reviewed name of an example whose title is generic; omitted when there is none. */
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val name: String? = null,
)

/**
 * A reviewed decision about one example, identified by its page (`context/operator`), its title and
 * the SHA-256 of its action block, so a changed example comes back for review. Exactly one of
 * [name] (a descriptive name for a generic title) and [owner] (the fixture that owns an example
 * several fixtures match) is set, with the [reason].
 */
data class ReviewedExample(
    val page: String,
    val title: String,
    val actionSha256: String,
    val name: String? = null,
    val owner: String? = null,
    val reason: String,
)

data class ReviewedExamples(val schemaVersion: Int = 1, val examples: List<ReviewedExample>)

/**
 * An operator page at the inventory's commit: `context` is the legacy package
 * (`aggregation/expressions`, ...).
 */
data class DocumentedPage(
    val context: String,
    val operator: String,
    val source: FormSource,
    val pageBlob: String,
    val includes: List<FormInclude>,
    val examples: List<DocumentedExample>,
)

/** A page that is skipped: not an operator page, or one the legacy extractor cannot read. */
data class SkippedPage(val path: String, val detail: String)

/**
 * Every documented example on the operator pages of one documentation commit, with its owner. It is
 * the reviewed input of generation: pages are not committed, so it records the unowned examples'
 * source text, as the legacy references do.
 */
data class ExampleInventory(
    val schemaVersion: Int = 1,
    val sourceRepository: String,
    val commit: String,
    val pages: List<DocumentedPage>,
    val skipped: List<SkippedPage>,
    val summary: Map<String, Int>,
)

object DocumentedExamples {
    const val PATH = "audits/catalog/documented-examples.json"
    private const val RESOURCES = "core/src/test/resources/dev/morphia/test"
    private const val LEDGER = "audits/catalog/fixture-migration.json"
    const val REVIEWED = "audits/catalog/example-names.json"

    /** An owned example: its fixture, its title when recorded, and its action text. */
    private data class Owned(val fixture: String, val title: String?, val action: String?)

    /**
     * Extracts every operator page [plan] names from the parsed [document] of [bundle], and matches
     * each example with the fixtures of [repository]'s legacy packages and migration ledger.
     */
    fun capture(
        repository: Path,
        bundle: VerifiedSourceBundle,
        document: DocumentReport,
        plan: ScenarioExtractionPlan,
    ): ExampleInventory {
        val root = repository.toRealPath()
        val owned = owned(root).groupBy { it.fixture.substringBeforeLast('/') }
        val reviewed =
            root
                .resolve(REVIEWED)
                .takeIf { Files.isRegularFile(it) }
                ?.let { AuditJson.reviewedExamples(Files.readString(it)).examples }
                .orEmpty()
        reviewed.forEach {
            require((it.name == null) != (it.owner == null) && it.reason.isNotBlank()) {
                "${it.page} ${it.title}: a review names an example or its owner, with a reason"
            }
        }
        val manifest = bundle.manifest
        val commit = manifest.request.revision
        val blobs = manifest.resources.associate { it.path to it.gitObject }
        val report = ScenarioExtraction.extract(bundle, document, plan)
        val rules = plan.pages.associateBy { it.id }
        val used = mutableSetOf<ReviewedExample>()
        val documented = mutableListOf<DocumentedPage>()
        val operators = ScenarioPlans.operatorPages(document)
        val skipped =
            document.pages
                .filter { page -> operators.none { it.page.id == page.id } }
                .map { SkippedPage(it.id, "no operator directive") }
                .toMutableList()
        operators.forEach { operator ->
            val rule = rules[operator.id]
            val path = operator.page.expanded?.origin?.path
            if (rule == null || path == null) {
                skipped += SkippedPage(operator.page.id, "no examples section")
                return@forEach
            }
            val scenarios =
                report.scenarios
                    .filter { it.rule == rule.id && it.variant == ScenarioVariant.REFERENCE }
                    .sortedWith { a, b -> compareLocators(position(a), position(b)) }
            documented +=
                DocumentedPage(
                    operator.context,
                    operator.operator,
                    FormSource(commit, path),
                    blobs.getValue(path),
                    operator.page.dependencies
                        .mapNotNull { it.origin.path }
                        .distinct()
                        .filter { it in blobs }
                        .map { FormInclude(it, blobs.getValue(it)) },
                    examples(
                        // A page that repeats an example, say for a second user, shows it once.
                        scenarios
                            .map { title(it, rule) to blocks(it) }
                            .distinctBy { (title, example) ->
                                // The same example: the same title, source and state. Examples
                                // without an action are never the same one.
                                if (example.blocks.none { it.role == BlockRole.ACTION }) Any()
                                else Triple(title, example.blocks, example.problem)
                            },
                        owned[operator.id].orEmpty(),
                        reviewed.filter { it.page == operator.id },
                        used,
                    ),
                )
        }
        val examples = documented.flatMap { it.examples }
        return ExampleInventory(
            sourceRepository = "mongodb/docs",
            commit = commit,
            pages = documented.sortedWith(compareBy({ it.context }, { it.operator })),
            skipped = skipped.sortedBy { it.path },
            summary =
                ExampleStatus.entries.associate { status ->
                    "examples.${status.name.lowercase()}" to examples.count { it.status == status }
                } +
                    mapOf(
                        "pages" to documented.size,
                        "pages.skipped" to skipped.size,
                        // A review whose example changed or left no longer applies; it is redone.
                        "reviews.stale" to (reviewed - used).size,
                    ),
        )
    }

    /**
     * An example's title as the legacy inventory spelled it: its section's, `main` for a generic
     * one, and `<section> :: <tab>` for an example a scenario tab shows.
     */
    private fun title(scenario: ExtractedScenario, rule: ScenarioPageRule): String {
        // The first section is the page's own.
        val section =
            scenario.sections
                .drop(1)
                .lastOrNull()
                ?.title
                ?.trim()
                ?.takeUnless(DescriptiveNames::isGeneric) ?: "main"
        val tab =
            scenario.containers.lastOrNull {
                it.name == "tab" &&
                    it.options["tabid"]?.let(AuditJson::textValue) in
                        rule.sectionAssembly?.scenarioTabs.orEmpty()
            }
        return if (tab == null) section else "$section :: ${tab.argument.trim()}"
    }

    /**
     * The scenario's blocks as generation reads them, or none when it has no program to generate:
     * inserts and seeds are data, index builds are indexes, and declarations travel with the action
     * they serve.
     */
    private fun blocks(scenario: ExtractedScenario): Unrunnable {
        val problem =
            scenario.normalizationProblem
                ?: scenario.program
                    ?.takeIf { it.state != ShellLiteralState.NORMALIZED }
                    ?.let { program -> program.problems.joinToString { it.code } }
        fun declaration(code: ScenarioCode) =
            code.role == ProgramRole.SETUP &&
                code.format == null &&
                ".insert" !in code.source &&
                ".createIndex" !in code.source
        // Each declaration travels with the next block that runs, in page order: the insert or
        // action that reads it.
        val pending = mutableListOf<String>()
        val blocks =
            scenario.blocks
                .filter { it.role != null }
                .mapNotNull { code ->
                    if (declaration(code)) {
                        pending += code.source
                        return@mapNotNull null
                    }
                    val role =
                        when (code.role) {
                            ProgramRole.ACTION -> BlockRole.ACTION
                            ProgramRole.EXPECTED -> BlockRole.EXPECTED
                            else ->
                                if (code.format == null && ".createIndex" in code.source)
                                    BlockRole.INDEX
                                else BlockRole.DATA
                        }
                    val runs = role != BlockRole.EXPECTED && code.format == null
                    val text =
                        if (runs && pending.isNotEmpty())
                            (pending + code.source).joinToString("\n").also { pending.clear() }
                        else code.source
                    SourceBlock(role, text, CaseManifests.sha256(text.toByteArray()))
                }
        // Until an update's case runs the page's own read, generation keeps the legacy reading of
        // what the page prints after it: an acknowledgement, or the documents the filter matches.
        val printed =
            scenario.skipped
                .firstOrNull { it.reason == "UPDATE_RESULT" }
                ?.code
                ?.source
                ?.takeIf { scenario.blocks.any { it.role == ProgramRole.ACTION } }
                ?.let {
                    SourceBlock(BlockRole.EXPECTED, it, CaseManifests.sha256(it.toByteArray()))
                }
        return Unrunnable(
            blocks + listOfNotNull(printed),
            problem ?: "no program".takeIf { scenario.program == null },
        )
    }

    /** A scenario's source blocks, and why it has no program when it hasn't. */
    private data class Unrunnable(val blocks: List<SourceBlock>, val problem: String?)

    /** Where a scenario's action sits on its page, so examples keep the page's order. */
    private fun position(scenario: ExtractedScenario): List<Int> =
        (scenario.blocks.firstOrNull { it.role == ProgramRole.ACTION } ?: scenario.blocks.first())
            .locator

    private fun compareLocators(a: List<Int>, b: List<Int>): Int {
        a.zip(b).forEach { (x, y) -> if (x != y) return x.compareTo(y) }
        return a.size.compareTo(b.size)
    }

    /**
     * Assigns each of a page's examples its owner or status; first by title, then by action text.
     */
    private fun examples(
        read: List<Pair<String, Unrunnable>>,
        owners: List<Owned>,
        reviewed: List<ReviewedExample>,
        used: MutableSet<ReviewedExample>,
    ): List<DocumentedExample> {
        val reviews =
            read.map { (title, example) ->
                val action = example.blocks.firstOrNull { it.role == BlockRole.ACTION }?.sha256
                reviewed.singleOrNull { it.title == title && it.actionSha256 == action }
            }
        used += reviews.filterNotNull()
        // A reviewed name stands in for a generic title.
        val names = read.mapIndexed { index, (title, _) -> reviews[index]?.name ?: title }
        val stems =
            names.map { title ->
                if (DescriptiveNames.isGeneric(title)) null
                else runCatching { DescriptiveNames.stem(title) }.getOrNull()
            }
        val matched =
            read.mapIndexed { index, (title, example) ->
                val blocks = example.blocks
                val review = reviews[index]
                val name = review?.name
                val action =
                    blocks.firstOrNull { it.role == BlockRole.ACTION }?.text?.let(::squeeze)
                // Examples that share a section's title are told apart by their action text alone.
                // Another runnable example with the title takes it from this one, runnable or not.
                val shared =
                    names.indices.any {
                        it != index && names[it] == names[index] && read[it].second.problem == null
                    }
                // A title several runnable examples share names none of them, even once reviewed
                // names tell them apart.
                // An example without a program yields the title to any other.
                val titled =
                    read.indices.none {
                        it != index &&
                            read[it].first == title &&
                            (read[it].second.problem == null || example.problem != null)
                    }
                val byTitle =
                    if (shared) emptyList()
                    else
                        owners.filter {
                            titled && it.title == title || name != null && it.title == name
                        }
                val byAction =
                    if (byTitle.isNotEmpty() || action == null) emptyList()
                    else owners.filter { it.action != null && it.action in action }
                val stem = stems[index]
                // The descriptive-name backport named each fixture for its example, so a fixture
                // with
                // the example's name owns it even when the title or action text has changed since.
                val byName =
                    if (byTitle.isNotEmpty() || byAction.isNotEmpty() || stem == null || shared)
                        emptyList()
                    else owners.filter { it.fixture.substringAfterLast('/') == stem }
                val matched = byTitle.ifEmpty { byAction }.ifEmpty { byName }
                when {
                    review?.owner != null ->
                        if (matched.any { it.fixture == review.owner })
                            DocumentedExample(
                                title,
                                ExampleStatus.OWNED,
                                owner = review.owner,
                                detail = "reviewed owner: ${review.reason}",
                            )
                        else
                            DocumentedExample(
                                title,
                                ExampleStatus.AMBIGUOUS,
                                detail = "the reviewed owner ${review.owner} no longer matches",
                            )
                    matched.size > 1 ->
                        DocumentedExample(
                            title,
                            ExampleStatus.AMBIGUOUS,
                            detail = "matches ${matched.joinToString { it.fixture }}",
                            blocks = blocks,
                        )
                    matched.size == 1 ->
                        DocumentedExample(
                            title,
                            ExampleStatus.OWNED,
                            owner = matched.single().fixture,
                            detail =
                                when {
                                    byTitle.isNotEmpty() -> null
                                    byAction.isNotEmpty() -> "matched by action text"
                                    else -> "matched by name"
                                },
                            name = name,
                        )
                    example.problem != null ->
                        DocumentedExample(
                            title,
                            ExampleStatus.UNSUPPORTED,
                            stem,
                            detail = example.problem,
                            blocks = blocks,
                            name = name,
                        )
                    shared ->
                        DocumentedExample(
                            title,
                            ExampleStatus.AMBIGUOUS,
                            detail = "the page has several examples titled \"$title\"",
                            blocks = blocks,
                        )
                    stem == null ->
                        DocumentedExample(
                            title,
                            ExampleStatus.UNNAMED,
                            detail = "a generic title needs a reviewed name",
                            blocks = blocks,
                        )
                    stems.indices.any {
                        it != index && stems[it] == stem && read[it].second.problem == null
                    } ->
                        DocumentedExample(
                            title,
                            ExampleStatus.AMBIGUOUS,
                            stem,
                            detail = "another example on the page has the name $stem",
                            name = name,
                        )
                    else ->
                        DocumentedExample(
                            title,
                            ExampleStatus.UNOWNED,
                            stem,
                            blocks = blocks,
                            name = name,
                        )
                }
            }
        // One fixture can't own two examples; the review decides which, as for any other match.
        val claimed =
            matched
                .filter { it.detail?.startsWith("reviewed owner") != true }
                .mapNotNull { it.owner }
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        return matched.mapIndexed { index, example ->
            if (example.owner !in claimed || example.detail?.startsWith("reviewed owner") == true)
                example
            else
                DocumentedExample(
                    example.title,
                    ExampleStatus.AMBIGUOUS,
                    detail = "${example.owner} matches several examples",
                    blocks = read[index].second.blocks,
                )
        }
    }

    /**
     * Every fixture of the legacy packages, with the title its `name` file records and its action
     * text, and every ledger case, with its override's example name and action block.
     */
    private fun owned(root: Path): List<Owned> {
        val ledger = AuditJson.fixtureMigration(Files.readString(root.resolve(LEDGER)))
        val migrated =
            ledger.migrations.map { migrated ->
                Owned(
                    migrated.fixture,
                    migrated.override.example,
                    migrated.override.blocks
                        .firstOrNull { it.role == BlockRole.ACTION }
                        ?.text
                        ?.let(::squeeze),
                )
            }
        val known = migrated.map { it.fixture }.toSet()
        val legacy =
            OperatorPage.CONTEXTS.values.distinct().flatMap { context ->
                val base = root.resolve("$RESOURCES/$context")
                if (!base.isDirectory()) emptyList()
                else
                    Files.list(base).use { operators ->
                        operators
                            .filter { it.isDirectory() }
                            .toList()
                            .flatMap { operator ->
                                Files.list(operator).use { examples ->
                                    examples.filter { it.isDirectory() }.toList()
                                }
                            }
                            .map { folder ->
                                Owned(
                                    "$context/${folder.parent.name}/${folder.name}",
                                    folder
                                        .resolve("name")
                                        .takeIf { it.isRegularFile() }
                                        ?.readText()
                                        ?.lines()
                                        ?.firstOrNull()
                                        ?.trim(),
                                    folder
                                        .resolve("action.json")
                                        .takeIf { it.isRegularFile() }
                                        ?.readText()
                                        ?.let(::squeeze),
                                )
                            }
                            .filter { it.fixture !in known }
                    }
            }
        return (migrated + legacy).sortedBy { it.fixture }
    }

    /** Text without whitespace, so a fixture's copied action is found in its page's block. */
    private fun squeeze(text: String): String = text.filterNot { it.isWhitespace() }
}
