package dev.morphia.audits.extraction

import java.nio.file.Files
import java.nio.file.Path
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonValue

/** An example extraction couldn't write a scenario for, and why. */
data class ExtractionFailure(val page: String, val title: String, val detail: String)

/** What one extraction run wrote, and the examples it couldn't. */
data class ExtractionResult(
    /** Scenario paths, relative to the repository, and their contents. */
    val written: Map<String, String>,
    val failures: List<ExtractionFailure>,
    /** [ExtractionResult]'s inventory: the written examples are now owned by their scenarios. */
    val inventory: ExampleInventory,
)

/**
 * Writes a `scenario.json` for every example of the inventory that has none, with its reviewed
 * override applied. A committed scenario is never rewritten: moving one to a new docs commit is a
 * docs bump, reviewed like any other change.
 */
object Extraction {
    /** The operator families core's tests cover. */
    val FAMILIES =
        setOf("aggregation/expressions", "aggregation/stages", "query/filters", "query/updates")

    fun extract(
        repository: Path,
        inventory: ExampleInventory,
        overrides: ExampleOverrides,
    ): ExtractionResult {
        val problems = Overrides.problems(overrides)
        require(problems.isEmpty()) { problems.joinToString("\n") }
        val byIdentity =
            overrides.overrides.associateBy { Triple(it.page, it.title, it.blocksSha256) }
        val used = mutableSetOf<ExampleOverride>()
        val written = sortedMapOf<String, String>()
        val failures = mutableListOf<ExtractionFailure>()
        val pages =
            inventory.pages.map { page ->
                if (page.context !in FAMILIES) return@map page
                page.copy(
                    examples =
                        page.examples.zip(Coverage.exampleKeys(page)).map examples@{ (example, key)
                            ->
                            val override =
                                byIdentity[
                                    Triple(
                                        page.id,
                                        example.title,
                                        Overrides.identity(example.blocks),
                                    )]
                            override?.let { used += it }
                            fun fail(detail: String): DocumentedExample {
                                failures +=
                                    ExtractionFailure(
                                        page.id,
                                        key.removePrefix("${page.id}/"),
                                        detail,
                                    )
                                return example
                            }
                            if (override?.exclude == true) return@examples example
                            // An override for an example that has a scenario was applied when it
                            // was written; one for an example extraction can't read applies to
                            // nothing.
                            if (example.status != ExampleStatus.UNOWNED) {
                                if (override != null && example.status != ExampleStatus.OWNED) {
                                    fail(
                                        "the override can't apply: the example is ${example.status}"
                                    )
                                }
                                return@examples example
                            }
                            val stem = example.stem ?: return@examples fail("no descriptive name")
                            val path = ScenarioWriter.path(page, stem)
                            if (Files.exists(repository.resolve(path))) {
                                return@examples fail(
                                    "$path exists, but the inventory doesn't match it"
                                )
                            }
                            val documented =
                                try {
                                    Documented.documented(
                                        ScenarioWriter.name(page, stem),
                                        null,
                                        example.blocks,
                                        override?.alternatives == true,
                                    )
                                } catch (failure: IllegalArgumentException) {
                                    return@examples fail(failure.message!!)
                                }
                            // An example that seeds nothing and queries a sample collection runs
                            // against that dataset, which the tests don't load.
                            val dataset =
                                overrides.sampleDatasets[documented.collection].takeIf {
                                    example.blocks.none { it.role == BlockRole.DATA }
                                }
                            if (
                                dataset == null &&
                                    documented.data.values.all { it.isEmpty() } &&
                                    documented.printed?.isNotEmpty() == true
                            ) {
                                return@examples fail(
                                    "the page prints results for ${documented.collection}, which nothing seeds"
                                )
                            }
                            val text =
                                try {
                                    ScenarioWriter.render(
                                        ScenarioInput(
                                            page,
                                            stem,
                                            example.name ?: example.title,
                                            documented,
                                            dataset,
                                            override,
                                        )
                                    )
                                } catch (failure: IllegalArgumentException) {
                                    return@examples fail(failure.message!!)
                                }
                            // The runner refuses a stage that reads or writes another database,
                            // since its two behavior runs would share it. The written action is
                            // checked, patches and alternatives included.
                            val action = BsonDocument.parse(text).getDocument("action")
                            (listOfNotNull(action["pipeline"]) +
                                    action.getArray("alternatives", BsonArray()).mapNotNull {
                                        it.asDocument()["pipeline"]
                                    })
                                .flatMap { it.asArray() }
                                .firstOrNull(::namesADatabase)
                                ?.let {
                                    return@examples fail(
                                        "a stage naming another database: ${it.asDocument().firstKey}"
                                    )
                                }
                            // A page that retitles an example would otherwise get a second
                            // scenario beside the one its test reads.
                            twin(repository, path, text)?.let { existing ->
                                return@examples fail(
                                    "its action is $existing's: the page retitled it, so review its owner in ${DocumentedExamples.REVIEWED}"
                                )
                            }
                            written[path] = text
                            example.copy(
                                status = ExampleStatus.OWNED,
                                owner = ScenarioWriter.name(page, stem),
                                stem = null,
                            )
                        }
                )
            }
        // An override for an example that is no longer on its page, or has changed, is stale.
        (overrides.overrides - used).forEach {
            failures +=
                ExtractionFailure(it.page, it.title, "the override matches no example on the page")
        }
        val examples = pages.flatMap { it.examples }
        return ExtractionResult(
            written,
            failures.sortedWith(compareBy({ it.page }, { it.title })),
            inventory.copy(
                pages = pages,
                summary =
                    inventory.summary +
                        ExampleStatus.entries.associate { status ->
                            "examples.${status.name.lowercase()}" to
                                examples.count { it.status == status }
                        },
            ),
        )
    }

    /** The fields a refresh takes from the page: everything the page documents. */
    private val DOCUMENTED =
        listOf("source", "dataset", "collections", "indexes", "action", "expected")

    /**
     * Re-extracts each owned example whose source changed since [committed] recorded it: what a
     * docs bump, or the weekly run against the docs' head, needs so that the tests run against what
     * the page now says. Only the documented fields are replaced; a scenario's title, comparison,
     * requirements and skips are reviewed decisions, and stay.
     */
    fun refresh(
        repository: Path,
        current: ExampleInventory,
        committed: ExampleInventory,
        overrides: ExampleOverrides,
    ): ExtractionResult {
        val byIdentity =
            overrides.overrides.associateBy { Triple(it.page, it.title, it.blocksSha256) }
        val pairs =
            committed.pages.flatMap { page -> page.examples.zip(Coverage.exampleKeys(page)) }
        val before = pairs.associate { (example, key) -> key to example }
        // A retitled example keeps its scenario, so its owner finds it when its key doesn't.
        val byOwner =
            pairs.mapNotNull { (example, _) -> example.owner?.let { it to example } }.toMap()
        val written = sortedMapOf<String, String>()
        val failures = mutableListOf<ExtractionFailure>()
        current.pages
            .filter { it.context in FAMILIES }
            .forEach { page ->
                page.examples.zip(Coverage.exampleKeys(page)).forEach { (example, key) ->
                    val owner = example.owner
                    val was = owner?.let { byOwner[it] } ?: before[key]
                    // Examples that share a title are told apart by their place, as their keys are.
                    val label = key.removePrefix("${page.id}/")
                    // A page edit that leaves the reader nothing to extract would otherwise keep
                    // the
                    // old scenario passing silently.
                    if (
                        example.status == ExampleStatus.OWNED &&
                            example.blocks.isEmpty() &&
                            was?.blocks.orEmpty().isNotEmpty()
                    ) {
                        failures +=
                            ExtractionFailure(
                                page.id,
                                label,
                                "its source changed, and the reader no longer finds the example's code",
                            )
                        return@forEach
                    }
                    if (
                        example.status != ExampleStatus.OWNED ||
                            owner == null ||
                            example.blocks.isEmpty() ||
                            was == null ||
                            Overrides.identity(was.blocks) == Overrides.identity(example.blocks)
                    ) {
                        return@forEach
                    }
                    val path = "core/src/test/resources/dev/morphia/test/$owner/scenario.json"
                    val file = repository.resolve(path)
                    if (!Files.isRegularFile(file)) return@forEach
                    val override =
                        byIdentity[
                            Triple(page.id, example.title, Overrides.identity(example.blocks))]
                    try {
                        val documented =
                            Documented.documented(
                                owner,
                                null,
                                example.blocks,
                                override?.alternatives == true,
                            )
                        val fresh =
                            BsonDocument.parse(
                                ScenarioWriter.render(
                                    ScenarioInput(
                                        page,
                                        owner.substringAfterLast('/'),
                                        example.name ?: example.title,
                                        documented,
                                        overrides.sampleDatasets[documented.collection].takeIf {
                                            example.blocks.none { it.role == BlockRole.DATA }
                                        },
                                        override,
                                    )
                                )
                            )
                        val scenario = BsonDocument.parse(Files.readString(file))
                        DOCUMENTED.forEach { field -> scenario[field] = fresh[field]!! }
                        written[path] = ScenarioWriter.text(scenario)
                    } catch (failure: IllegalArgumentException) {
                        failures +=
                            ExtractionFailure(
                                page.id,
                                label,
                                "its source changed, and ${failure.message}",
                            )
                    }
                }
            }
        return ExtractionResult(written, failures, current)
    }

    /** The committed scenario in [path]'s operator folder whose action equals [text]'s, if any. */
    private fun twin(repository: Path, path: String, text: String): String? {
        fun action(json: String) =
            BsonDocument.parse(json).getDocument("action").clone().apply { remove("alternatives") }
        val mine = action(text)
        val folder = repository.resolve(path).parent.parent
        if (!Files.isDirectory(folder)) return null
        return Files.list(folder).use { stems ->
            stems
                .map { it.resolve("scenario.json") }
                .filter { Files.isRegularFile(it) }
                .toList()
                .firstOrNull { action(Files.readString(it)) == mine }
                ?.let { BsonDocument.parse(Files.readString(it)).getString("name").value }
        }
    }

    private val NAMESPACED = setOf("\$out", "\$merge", "\$lookup", "\$graphLookup", "\$unionWith")

    /** Whether a stage names another database, by the rule core's `OperatorCaseRunner` applies. */
    private fun namesADatabase(value: BsonValue): Boolean =
        when (value) {
            is BsonDocument ->
                value.entries.any { (key, argument) ->
                    key != "\$literal" &&
                        (key in NAMESPACED &&
                            argument is BsonDocument &&
                            (argument.containsKey("db") ||
                                hasDb(argument["into"]) ||
                                hasDb(argument["from"])) || namesADatabase(argument))
                }
            is BsonArray -> value.any(::namesADatabase)
            else -> false
        }

    private fun hasDb(value: BsonValue?): Boolean = value is BsonDocument && value.containsKey("db")
}
