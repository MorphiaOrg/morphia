package dev.morphia.audits.generate

import com.fasterxml.jackson.annotation.JsonInclude
import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.model.OperatorType
import dev.morphia.audits.naming.DescriptiveNames
import dev.morphia.audits.repairs.BlockRole
import dev.morphia.audits.repairs.FormInclude
import dev.morphia.audits.repairs.FormMigration
import dev.morphia.audits.repairs.FormSource
import dev.morphia.audits.repairs.SourceBlock
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
}

/**
 * One example on a current page. [owner] is the owning fixture, by title or, when the title changed
 * since the fixture was written, by its action text. An unowned example keeps its source [blocks],
 * so generation never needs the page.
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
    private val DIRECTIVES =
        mapOf(
            "expression" to "aggregation/expressions",
            // Accumulator and window operator pages; Morphia's tests keep them with the
            // expressions.
            "group" to "aggregation/expressions",
            "pipeline" to "aggregation/stages",
            "query" to "query/filters",
            "update" to "query/updates",
        )
    private val directive = Regex("""^\.\. (expression|group|pipeline|query|update):: \$\S*\s*$""")

    /** An owned example: its fixture, its title when recorded, and its action text. */
    private data class Owned(val fixture: String, val title: String?, val action: String?)

    /**
     * Reads every page of [sources] (a `fetch.py current` file) from [pages] and matches each
     * example with the fixtures of [repository]'s legacy packages and migration ledger.
     */
    fun capture(repository: Path, pages: Path, sources: CurrentPages): ExampleInventory {
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
        val used = mutableSetOf<ReviewedExample>()
        val documented = mutableListOf<DocumentedPage>()
        val skipped = mutableListOf<SkippedPage>()
        sources.pages.forEach { entry ->
            val source = entry.source
            require(source.commit == sources.commit) {
                "${source.path} is not at ${sources.commit}"
            }
            val file = pages.resolve(source.commit).resolve(source.path)
            val declared =
                file.readText().lines().firstNotNullOfOrNull { directive.matchEntire(it.trimEnd()) }
            if (declared == null) {
                skipped += SkippedPage(source.path, "no operator directive")
                return@forEach
            }
            val context = DIRECTIVES.getValue(declared.groupValues[1])
            // The page's file name, as the legacy fixture directories use it: `$[]` is
            // `positional-all`.
            val operator = source.path.substringAfterLast('/').removeSuffix(".txt")
            val type =
                when (context) {
                    "aggregation/expressions" -> OperatorType.EXPRESSION
                    "aggregation/stages" -> OperatorType.STAGE
                    "query/filters" -> OperatorType.FILTER
                    else -> OperatorType.UPDATE
                }
            val read =
                try {
                    FormMigration.pageExamples(pages, source, type)
                } catch (failure: RuntimeException) {
                    skipped += SkippedPage(source.path, failure.message ?: "$failure")
                    return@forEach
                }
            val candidates = owned["$context/$operator"].orEmpty()
            documented +=
                DocumentedPage(
                    context,
                    operator,
                    source,
                    read.pageBlob,
                    read.includes,
                    examples(
                        read.examples.map { it.name to it.blocks },
                        candidates,
                        reviewed.filter { it.page == "$context/$operator" },
                        used,
                    ),
                )
        }
        val examples = documented.flatMap { it.examples }
        return ExampleInventory(
            sourceRepository = "mongodb/docs",
            commit = sources.commit,
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
     * Assigns each of a page's examples its owner or status; first by title, then by action text.
     */
    private fun examples(
        read: List<Pair<String, List<SourceBlock>>>,
        owners: List<Owned>,
        reviewed: List<ReviewedExample>,
        used: MutableSet<ReviewedExample>,
    ): List<DocumentedExample> {
        val reviews =
            read.map { (title, blocks) ->
                val action = blocks.firstOrNull { it.role == BlockRole.ACTION }?.sha256
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
        return read.mapIndexed { index, (title, blocks) ->
            val review = reviews[index]
            val name = review?.name
            val action = blocks.firstOrNull { it.role == BlockRole.ACTION }?.text?.let(::squeeze)
            val byTitle = owners.filter { it.title == title || name != null && it.title == name }
            val byAction =
                if (byTitle.isNotEmpty() || action == null) emptyList()
                else owners.filter { it.action != null && it.action in action }
            val matched = byTitle.ifEmpty { byAction }
            val stem = stems[index]
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
                names.count { it == names[index] } > 1 ->
                    DocumentedExample(
                        title,
                        ExampleStatus.AMBIGUOUS,
                        detail = "the page has several examples titled \"$title\"",
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
                        detail = if (byTitle.isEmpty()) "matched by action text" else null,
                        name = name,
                    )
                stem == null ->
                    DocumentedExample(
                        title,
                        ExampleStatus.UNNAMED,
                        detail = "a generic title needs a reviewed name",
                        blocks = blocks,
                    )
                stems.count { it == stem } > 1 ->
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
            DIRECTIVES.values.distinct().flatMap { context ->
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

/** A `fetch.py current` sources file: every operator page at one commit. */
data class CurrentPages(
    val schemaVersion: Int = 1,
    val commit: String,
    val pages: List<CurrentPage>,
)

data class CurrentPage(val source: FormSource)
