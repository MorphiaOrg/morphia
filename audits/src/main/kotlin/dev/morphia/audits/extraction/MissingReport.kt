package dev.morphia.audits.extraction

import com.fasterxml.jackson.annotation.JsonInclude
import java.nio.file.Path

/** Who resolves a category: a change to the reader, or a reviewer's decision in the catalog. */
enum class MissingFix {
    /** The reader or extraction can't handle the page's code yet: change the audit's sources. */
    READER,
    /** The reader did its part, and a reviewer must name or assign the example. */
    REVIEW,
}

/**
 * What a category of missing scenarios means, where in the audit's sources it comes from, and how
 * to resolve one.
 */
data class MissingCategory(
    val fix: MissingFix,
    val meaning: String,
    val where: List<String>,
    val resolve: String,
)

/** One missing scenario as the report lists it, with a link to its page at the pinned commit. */
data class MissingReportEntry(
    val key: String,
    val page: String,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val title: String?,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val detail: String?,
    val source: String?,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val blocksSha256: String?,
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val blocks: List<SourceBlock>,
)

/** One category of the report, its entries, and the guidance for resolving them. */
data class MissingReportGroup(
    val category: String,
    /** `PAGE` when the reader couldn't read the page at all, `EXAMPLE` otherwise. */
    val scope: String,
    val count: Int,
    val fix: MissingFix?,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val meaning: String?,
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val where: List<String>,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val resolve: String?,
    val entries: List<MissingReportEntry>,
)

/**
 * The missing-scenario check's work queue: [Coverage.missing]'s entries grouped by what each waits
 * on, with each example's source blocks and a link to its page, so a person or an agent can pick a
 * category and work through it without the docs checkout. Written beside the check's failure, in
 * `audits/target/missing-scenarios.json` and `.md`.
 */
object MissingReport {
    private const val SRC = "audits/src/main/kotlin/dev/morphia/audits"
    private const val EXCLUDE =
        "If the example isn't a data scenario Morphia can express, exclude it in ${Overrides.PATH} " +
            "with `exclude: true`, its `blocksSha256` and a reason instead."

    /** The guidance for each category, by the code [MissingScenario.category] holds. */
    val CATEGORIES: Map<String, MissingCategory> =
        mapOf(
            "MISSING_COMMAND_OR_RESULT" to
                MissingCategory(
                    MissingFix.READER,
                    "No block in the section reads as an action (a find, aggregate, update or " +
                        "other command on a collection), so there is nothing to run. Either the " +
                        "page's code isn't a collection command, or every block was skipped as " +
                        "setup, a template or unreadable source. An entry with no blocks means " +
                        "the reader captured nothing it could use.",
                    listOf(
                        "$SRC/scenarios/SectionPrograms.kt (block kinds; the " +
                            "`MISSING_COMMAND_OR_RESULT` assignment)",
                        "$SRC/shell/ShellPrograms.kt (which calls count as actions)",
                    ),
                    "Find the block that should be the action, and teach the block " +
                        "classification or the shell grammar to read it. $EXCLUDE",
                ),
            "AFTER_EARLIER_WRITE" to
                MissingCategory(
                    MissingFix.READER,
                    "A read or write that runs after an earlier write on the page. Run alone, it " +
                        "would miss that write's effect, so it isn't extracted.",
                    listOf("$SRC/scenarios/ScenarioExtraction.kt (`afterWrites`)"),
                    "Carry the earlier writes into the scenario as setup, so that it starts from " +
                        "the data the page's result assumes.",
                ),
            "SYNTAX" to
                MissingCategory(
                    MissingFix.READER,
                    "The shell grammar rejected the code: the program parser (Rhino) or the " +
                        "shell-literal reader failed on it.",
                    listOf("$SRC/shell/ShellPrograms.kt", "$SRC/shell/ShellLiterals.kt"),
                    "Reproduce with the block text in ShellProgramsTest or ShellLiteralsTest, then " +
                        "extend the grammar. If the page's code is itself invalid, exclude it with " +
                        "that reason.",
                ),
            "COLLECTION_REQUIRED" to
                MissingCategory(
                    MissingFix.READER,
                    "A command's receiver isn't a known collection, as in `db.aggregate(...)` " +
                        "with `\$documents`, or `db.getSiblingDB(...)`.",
                    listOf("$SRC/shell/ShellPrograms.kt (the `COLLECTION_REQUIRED` stop)"),
                    "Support the receiver: a database-level aggregate needs no collection.",
                ),
            "DOCUMENT_STREAM_SYNTAX" to
                MissingCategory(
                    MissingFix.READER,
                    "The printed results can't be split into a stream of documents.",
                    listOf("$SRC/shell/ShellResultFraming.kt"),
                    "Reproduce with the EXPECTED block in ShellResultsTest, and teach the framing " +
                        "its layout.",
                ),
            "UNSUPPORTED_SETUP" to
                MissingCategory(
                    MissingFix.READER,
                    "A setup block in the section loads data the grammar can't read, so the " +
                        "actions after it would run without that data.",
                    listOf(
                        "$SRC/scenarios/SectionPrograms.kt (`Kind.Skip(\"UNSUPPORTED_SETUP\")`)",
                        "$SRC/shell/ShellPrograms.kt",
                    ),
                    "Find the setup block that doesn't normalize, and fix whatever the grammar " +
                        "rejects in it.",
                ),
            "UNSUPPORTED_SHARED_SETUP" to
                MissingCategory(
                    MissingFix.READER,
                    "The setup the page shares between sections loads data the grammar can't " +
                        "read, so every section that depends on it is skipped.",
                    listOf(
                        "$SRC/scenarios/SectionPrograms.kt",
                        "$SRC/scenarios/ScenarioExtraction.kt",
                    ),
                    "Find the shared setup block that doesn't normalize, and fix whatever the " +
                        "grammar rejects in it.",
                ),
            "UNSUPPORTED_AST" to
                MissingCategory(
                    MissingFix.READER,
                    "A value uses JavaScript syntax the literal lowering doesn't support, such " +
                        "as a function literal.",
                    listOf("$SRC/shell/ShellValueLowering.kt", "$SRC/shell/ShellResultFraming.kt"),
                    "Lower the syntax to BSON when it has a BSON form, such as a function " +
                        "literal that the server takes as a code string. $EXCLUDE",
                ),
            "CALL_TARGET" to
                MissingCategory(
                    MissingFix.READER,
                    "A call inside a value isn't an explicit BSON constructor, such as a method " +
                        "call or a helper function.",
                    listOf("$SRC/shell/ShellValueLowering.kt (the `CALL_TARGET` check)"),
                    "Support the call when it builds a constant BSON value. $EXCLUDE",
                ),
            "UNOWNED" to
                MissingCategory(
                    MissingFix.READER,
                    "The reader read and named the example, but extraction didn't write its " +
                        "scenario. The detail is extraction's reason. With no detail, extraction " +
                        "would write the scenario now: rerun it.",
                    listOf("$SRC/extraction/Extraction.kt (`extract`)"),
                    "Fix the problem the detail names, or rerun extraction (see the docs bump in " +
                        "audits/README.md). $EXCLUDE",
                ),
            "UNNAMED" to
                MissingCategory(
                    MissingFix.REVIEW,
                    "The example's title is generic (\"main\", \"Example\"), so it needs a " +
                        "reviewed descriptive name before extraction can name its scenario.",
                    listOf(DocumentedExamples.REVIEWED),
                    "Add a name for the example in ${DocumentedExamples.REVIEWED}, then rerun " +
                        "extraction.",
                ),
            "AMBIGUOUS" to
                MissingCategory(
                    MissingFix.REVIEW,
                    "The example matches more than one scenario, or shares its name with another " +
                        "example on the page.",
                    listOf(DocumentedExamples.REVIEWED),
                    "Record its owner or a distinct name in ${DocumentedExamples.REVIEWED}.",
                ),
            "SCENARIO_FILE_MISSING" to
                MissingCategory(
                    MissingFix.REVIEW,
                    "The inventory says a scenario owns the example, but its scenario.json is " +
                        "gone.",
                    listOf("core/src/test/resources/dev/morphia/test"),
                    "Restore the scenario, or rerun extraction to write it again.",
                ),
        )

    /** The guidance for a page the reader couldn't read, whatever the code. */
    val PAGE =
        MissingCategory(
            MissingFix.READER,
            "A page-level check failed, so the reader couldn't read all of the page: some or " +
                "all of its examples are missing from the inventory, and any it did inventory " +
                "appear under their own categories. The detail is the reader's message.",
            listOf(
                "$SRC/scenarios/ScenarioExtraction.kt (the `problem(...)` calls)",
                "audits/catalog/scenario-plan.json (the page's rule)",
            ),
            "Teach the reader the page's layout, or adjust the page's rule in the scenario plan.",
        )

    /**
     * The report's groups, largest first. For an UNOWNED example, the detail is extraction's reason
     * for refusing it, which the inventory doesn't record.
     */
    fun groups(
        repository: Path,
        inventory: ExampleInventory,
        overrides: ExampleOverrides,
    ): List<MissingReportGroup> {
        val missing = Coverage.missing(repository, inventory, overrides)
        // Extraction refuses to run at all when the overrides have problems; every unowned
        // example then waits on those, not on its own reason.
        var blocked: String? = null
        val refused =
            try {
                Extraction.extract(repository, inventory, overrides).failures.associate {
                    "${it.page}/${it.title}" to it.detail
                }
            } catch (failure: IllegalArgumentException) {
                blocked = "extraction can't run: ${failure.message}"
                emptyMap()
            }
        val sources = inventory.pages.associate { it.id to it.source.path }
        return missing
            .groupBy { (if (it.title == null) "PAGE" else "EXAMPLE") to it.category }
            .map { (group, entries) ->
                val (scope, category) = group
                val guidance = if (scope == "PAGE") PAGE else CATEGORIES[category]
                MissingReportGroup(
                    category,
                    scope,
                    entries.size,
                    guidance?.fix,
                    guidance?.meaning,
                    guidance?.where.orEmpty(),
                    guidance?.resolve,
                    entries
                        .map {
                            MissingReportEntry(
                                it.key,
                                it.page,
                                it.title,
                                reason(
                                    if (it.category == "UNOWNED")
                                        blocked ?: refused[it.key] ?: it.detail
                                    else it.detail
                                ),
                                sources[it.page]?.let { path ->
                                    "https://github.com/${inventory.sourceRepository}/blob/${inventory.commit}/$path"
                                },
                                it.title?.let { _ -> Overrides.identity(it.blocks) },
                                it.blocks,
                            )
                        }
                        .sortedWith(compareBy({ it.detail.orEmpty() }, { it.key })),
                )
            }
            .sortedWith(compareBy({ -it.count }, { it.scope }, { it.category }))
    }

    /** A failure's message without the scenario name extraction puts before it. */
    private fun reason(detail: String?): String? =
        detail?.replace(Regex("^[a-z]+/[a-z]+/[A-Za-z0-9]+/[A-Za-z0-9]+: "), "")

    /** The report as Markdown: a summary table, then each group's guidance and entries. */
    fun markdown(groups: List<MissingReportGroup>): String = buildString {
        val total = groups.sumOf { it.count }
        appendLine("# Missing scenarios: $total")
        appendLine()
        appendLine(
            "Documented examples with no scenario and no exclusion, grouped by what each waits " +
                "on. Resolve each one by fixing the reader or by excluding it in " +
                "`${Overrides.PATH}` with a reason. Rerun with " +
                "`./mvnw test -Paudits -pl :morphia-audits -Dtest=ScenarioCoverageTest`. A " +
                "reader fix shows up only after the inventory is rebuilt: see the docs bump in " +
                "`audits/README.md`."
        )
        appendLine()
        appendLine("| Category | Scope | Fix | Count |")
        appendLine("|---|---|---|---:|")
        groups.forEach {
            appendLine(
                "| [${it.category}](#${anchor(it)}) | ${it.scope} | ${it.fix ?: "?"} | ${it.count} |"
            )
        }
        groups.forEach { group ->
            appendLine()
            appendLine("## ${group.scope.lowercase()}: ${group.category} (${group.count})")
            appendLine()
            group.meaning?.let { appendLine("**What it means:** $it") }
            if (group.where.isNotEmpty()) {
                appendLine()
                appendLine("**Where to look:**")
                group.where.forEach {
                    val path = it.substringBefore(" (")
                    appendLine("- `$path`" + it.removePrefix(path))
                }
            }
            group.resolve?.let {
                appendLine()
                appendLine("**To resolve:** $it")
            }
            if (group.meaning == null) {
                appendLine("No guidance is recorded for this code; add it to `MissingReport`.")
            }
            val reasons = group.entries.groupingBy { it.detail ?: "(no detail)" }.eachCount()
            if (reasons.size > 1) {
                appendLine()
                appendLine("**By detail:**")
                reasons.entries.sortedWith(compareBy({ -it.value }, { it.key })).forEach {
                    (reason, count) ->
                    appendLine("- $reason: $count")
                }
            }
            group.entries.forEach { entry ->
                appendLine()
                append("### `${entry.key}`")
                appendLine()
                appendLine()
                entry.source?.let { appendLine("- Page: $it") }
                entry.detail?.let { appendLine("- Detail: $it") }
                entry.blocksSha256?.let { appendLine("- blocksSha256: `$it`") }
                if (entry.title != null && entry.blocks.isEmpty()) {
                    appendLine("- The reader captured no blocks for this example.")
                }
                entry.blocks.forEach { block ->
                    appendLine()
                    appendLine("${block.role}:")
                    val fence = "`".repeat(maxOf(3, longestRun(block.text) + 1))
                    appendLine("${fence}javascript")
                    appendLine(block.text)
                    appendLine(fence)
                }
            }
        }
    }

    /** The summary the check prints: the missing scenarios by category. */
    fun summary(groups: List<MissingReportGroup>): String = buildString {
        appendLine("Missing scenarios: ${groups.sumOf { it.count }}")
        groups.forEach {
            appendLine("  ${it.scope.lowercase()} ${it.category} (${it.fix ?: "?"}): ${it.count}")
        }
    }

    private fun anchor(group: MissingReportGroup) =
        "${group.scope.lowercase()}-${group.category.lowercase()}-${group.count}"

    private fun longestRun(text: String) =
        Regex("`+").findAll(text).maxOfOrNull { it.value.length } ?: 0
}
