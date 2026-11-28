package dev.morphia.audits.extraction

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.documents.DocumentReport
import dev.morphia.audits.naming.DescriptiveNames
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
import org.bson.BsonDocument

enum class ExampleStatus {
    /** A committed `scenario.json` holds the example. */
    OWNED,
    /** No scenario holds the example yet, and it has a descriptive title: extraction writes one. */
    UNOWNED,
    /** No test owns the example, and its title is generic, so it needs a reviewed name. */
    UNNAMED,
    /**
     * The example matches more than one owner, or shares its name with another example; it keeps
     * its source blocks for the review that resolves it.
     */
    AMBIGUOUS,
    /** No scenario holds the example, and the reader found no program to extract it from. */
    UNSUPPORTED,
}

/**
 * One example on a current page. [owner] is the scenario that holds it
 * (`<context>/<operator>/<stem>`), matched by title or, when the title changed since the scenario
 * was written, by its descriptive name. An example without one keeps its source [blocks], so
 * extraction and review never need the page.
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
    /**
     * Whether its action block holds the page's equivalent spellings of one command, folded in from
     * the examples a review declared alternatives of it.
     */
    @get:JsonInclude(JsonInclude.Include.NON_DEFAULT) val alternatives: Boolean = false,
)

/**
 * A reviewed decision about one example, identified by its page (`context/operator`), its title and
 * the SHA-256 of its action block, so a changed example comes back for review. Exactly one of
 * [name] (a descriptive name for a generic title), [owner] (the scenario that holds an example
 * several scenarios match) and [alternativeOf] is set, with the [reason].
 */
data class ReviewedExample(
    val page: String,
    val title: String,
    val actionSha256: String,
    val name: String? = null,
    val owner: String? = null,
    /**
     * The action SHA-256 of the example on the same page that this one spells another way, as
     * `$cond`'s array form spells its object form. Its action becomes one of that example's
     * alternatives, and it is no longer an example of its own.
     */
    val alternativeOf: String? = null,
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
    /**
     * What the reader couldn't read on the page, such as a missing section: the page's examples are
     * incomplete, so the audit fails until the plan or the reader is fixed.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val problems: List<String> = emptyList(),
) {
    /** The page's identity, `<context>/<operator>`, as reviews and overrides name it. */
    @get:JsonIgnore
    val id: String
        get() = "$context/$operator"
}

/** A page that is skipped: not an operator page, or one the legacy extractor cannot read. */
data class SkippedPage(val path: String, val detail: String)

/**
 * Every documented example on the operator pages of one documentation commit, with the scenario
 * that holds it. Pages are not committed, so it records the source text of every example without
 * one: that is what extraction writes from, and what an exclusion's digest names.
 */
data class ExampleInventory(
    val schemaVersion: Int = 1,
    val sourceRepository: String,
    val commit: String,
    val pages: List<DocumentedPage>,
    val skipped: List<SkippedPage>,
    val summary: Map<String, Int>,
    /**
     * Every operator the pages document, those without examples included: what Morphia's API
     * coverage is checked against.
     */
    val operators: List<DocumentedOperator> = emptyList(),
)

/**
 * An operator a page documents: its [context] (`aggregation/expressions`, ...), its [token]
 * (`$maxN`, which `maxN-array-element.txt` documents), and its [page] (`<context>/<operator>`).
 */
data class DocumentedOperator(val context: String, val token: String, val page: String)

object DocumentedExamples {
    const val PATH = "audits/catalog/documented-examples.json"
    private const val RESOURCES = "core/src/test/resources/dev/morphia/test"
    const val REVIEWED = "audits/catalog/example-names.json"

    /** A committed scenario: its name (`<context>/<operator>/<stem>`) and its example's title. */
    internal data class Owned(val fixture: String, val title: String?)

    /**
     * Extracts every operator page [plan] names from the parsed [document] of [bundle], and matches
     * each example with the scenarios committed in [repository].
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
            require(
                listOfNotNull(it.name, it.owner, it.alternativeOf).size == 1 &&
                    it.reason.isNotBlank()
            ) {
                "${it.page} ${it.title}: a review names an example, its owner or the example it" +
                    " is an alternative of, with a reason"
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
                        // Scenarios live under the test class's folder: maxN-array-element is
                        // maxNArrayElement.
                        owned["${operator.context}/${ScenarioWriter.folder(operator.operator)}"]
                            .orEmpty(),
                        reviewed.filter { it.page == operator.id },
                        used,
                    ),
                    report.problems
                        .filter { it.rule == rule.id }
                        .map { "${it.code}: ${it.message}" }
                        .distinct(),
                )
        }
        val examples = documented.flatMap { it.examples }
        return ExampleInventory(
            sourceRepository = "mongodb/docs",
            commit = commit,
            pages = documented.sortedWith(compareBy({ it.context }, { it.operator })),
            skipped = skipped.sortedBy { it.path },
            operators =
                operators
                    .map { DocumentedOperator(it.context, it.token, it.id) }
                    .distinct()
                    .sortedWith(compareBy({ it.context }, { it.token }, { it.page })),
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
        // A second action is the find the page runs after the first, a write, to show its effect.
        val action = scenario.blocks.firstOrNull { it.role == ProgramRole.ACTION }
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
                            ProgramRole.ACTION ->
                                if (code === action) BlockRole.ACTION else BlockRole.READ
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
                    SourceBlock(role, text, Documented.sha256(text.toByteArray()))
                }
        // Where the page shows no read after an update, generation keeps the legacy reading of
        // what it prints: an acknowledgement, or the documents the filter matches.
        val printed =
            scenario.skipped
                .firstOrNull { it.reason == "UPDATE_RESULT" }
                ?.code
                ?.source
                ?.takeIf { action != null && blocks.none { it.role == BlockRole.READ } }
                ?.let { SourceBlock(BlockRole.EXPECTED, it, Documented.sha256(it.toByteArray())) }
        return Unrunnable(
            blocks + listOfNotNull(printed),
            problem ?: "no program".takeIf { scenario.program == null },
        )
    }

    /** A scenario's source blocks, and why it has no program when it hasn't. */
    internal data class Unrunnable(val blocks: List<SourceBlock>, val problem: String?)

    /** Where a scenario's action sits on its page, so examples keep the page's order. */
    private fun position(scenario: ExtractedScenario): List<Int> =
        (scenario.blocks.firstOrNull { it.role == ProgramRole.ACTION } ?: scenario.blocks.first())
            .locator

    private fun compareLocators(a: List<Int>, b: List<Int>): Int {
        a.zip(b).forEach { (x, y) -> if (x != y) return x.compareTo(y) }
        return a.size.compareTo(b.size)
    }

    /** Assigns each of a page's examples its owner or status: first by title, then by name. */
    internal fun examples(
        page: List<Pair<String, Unrunnable>>,
        owners: List<Owned>,
        reviewed: List<ReviewedExample>,
        used: MutableSet<ReviewedExample>,
    ): List<DocumentedExample> {
        fun action(example: Unrunnable) =
            example.blocks.singleOrNull { it.role == BlockRole.ACTION }
        val found =
            page.map { (title, example) ->
                val action = example.blocks.firstOrNull { it.role == BlockRole.ACTION }?.sha256
                reviewed.singleOrNull { it.title == title && it.actionSha256 == action }
            }
        // An alternative folds into the example it spells another way: a runnable example whose
        // only block is its action, into one on the page that is itself no alternative. Any other
        // stays an example of its own, and its review is stale.
        val into =
            page.indices
                .mapNotNull { index ->
                    val of = found[index]?.alternativeOf ?: return@mapNotNull null
                    val example = page[index].second
                    if (
                        example.problem != null ||
                            example.blocks.size != 1 ||
                            action(example) == null
                    )
                        return@mapNotNull null
                    page.indices
                        .singleOrNull { other ->
                            other != index &&
                                found[other]?.alternativeOf == null &&
                                page[other].second.problem == null &&
                                action(page[other].second)?.sha256 == of
                        }
                        ?.let { index to it }
                }
                .toMap()
        used +=
            found
                .filterIndexed { index, review ->
                    review != null && (review.alternativeOf == null || index in into)
                }
                .filterNotNull()
        val kept = page.indices.filter { it !in into }
        // The primary's action block joins its alternatives' in page order, so a change to any of
        // them changes the example's digest.
        val read =
            kept.map { index ->
                val (title, example) = page[index]
                val folded = into.filterValues { it == index }.keys.sorted()
                if (folded.isEmpty()) return@map title to example
                val text =
                    (listOf(index) + folded).joinToString("\n") { action(page[it].second)!!.text }
                title to
                    example.copy(
                        blocks =
                            example.blocks.map {
                                if (it.role != BlockRole.ACTION) it
                                else
                                    SourceBlock(
                                        BlockRole.ACTION,
                                        text,
                                        Documented.sha256(text.toByteArray()),
                                    )
                            }
                    )
            }
        val reviews = kept.map { found[it] }
        val alternatives = kept.map { it in into.values }
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
                // A reviewed name stands in for the title, so a legacy scenario that kept the
                // generic title doesn't claim the example.
                val byTitle =
                    if (shared) emptyList()
                    else if (name != null) owners.filter { it.title == name }
                    else owners.filter { titled && it.title == title }
                val stem = stems[index]
                // A scenario is named for its example, so the scenario with the example's name
                // holds
                // it even when the page's title has changed since.
                val byName =
                    if (byTitle.isNotEmpty() || stem == null || shared) emptyList()
                    else owners.filter { it.fixture.substringAfterLast('/') == stem }
                val matched = byTitle.ifEmpty { byName }
                when {
                    // A reviewed owner holds the example even when the page has since retitled it.
                    review?.owner != null ->
                        if (owners.any { it.fixture == review.owner })
                            DocumentedExample(
                                title,
                                ExampleStatus.OWNED,
                                owner = review.owner,
                                detail = "reviewed owner: ${review.reason}",
                                blocks = blocks,
                            )
                        else
                            DocumentedExample(
                                title,
                                ExampleStatus.AMBIGUOUS,
                                detail = "the reviewed owner ${review.owner} no longer exists",
                                blocks = blocks,
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
                            detail = if (byTitle.isNotEmpty()) null else "matched by name",
                            // Kept for the example's identity, which an override's digest names.
                            blocks = blocks,
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
                            blocks = blocks,
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
        // One scenario can't own two examples. A reviewed owner keeps its claim; every other
        // example claiming the same scenario goes back for review.
        val counts = matched.mapNotNull { it.owner }.groupingBy { it }.eachCount()
        val reviewedClaims =
            matched
                .filter { it.detail?.startsWith("reviewed owner") == true }
                .mapNotNull { it.owner }
        return matched.mapIndexed { index, example ->
            val owner = example.owner
            val reviewed = example.detail?.startsWith("reviewed owner") == true
            if (
                owner == null ||
                    (counts.getValue(owner) == 1) ||
                    (reviewed && reviewedClaims.count { it == owner } == 1)
            )
                example.copy(alternatives = alternatives[index])
            else
                DocumentedExample(
                    example.title,
                    ExampleStatus.AMBIGUOUS,
                    detail = "$owner matches several examples",
                    blocks = read[index].second.blocks,
                    alternatives = alternatives[index],
                )
        }
    }

    /** Every committed scenario of the operator families, with its example's title. */
    private fun owned(root: Path): List<Owned> =
        OperatorPage.CONTEXTS.values
            .distinct()
            .flatMap { context ->
                val base = root.resolve("$RESOURCES/$context")
                if (!base.isDirectory()) emptyList()
                else
                    Files.walk(base, 3).use { files ->
                        files
                            .filter { it.name == "scenario.json" && it.isRegularFile() }
                            .toList()
                            .map { file ->
                                val scenario = BsonDocument.parse(file.readText())
                                Owned(
                                    scenario.getString("name").value,
                                    scenario.getString("title", null)?.value,
                                )
                            }
                    }
            }
            .sortedBy { it.fixture }
}
