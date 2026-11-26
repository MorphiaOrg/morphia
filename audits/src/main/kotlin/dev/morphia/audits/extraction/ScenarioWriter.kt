package dev.morphia.audits.extraction

import dev.morphia.audits.published.PublishedEncoding
import org.bson.BsonArray
import org.bson.BsonBoolean
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonNull
import org.bson.BsonString
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings

/** What a scenario is written from: its page, its example and the example's lowered operation. */
data class ScenarioInput(
    val page: DocumentedPage,
    /** The scenario's folder name, from the example's descriptive name. */
    val stem: String,
    /** The example's title, or its reviewed name when the title is generic. */
    val title: String,
    val documented: DocumentedScenario,
    /** The sample dataset the example queries instead of seeding its own data. */
    val dataset: String? = null,
    val override: ExampleOverride? = null,
)

/**
 * Writes one example's `scenario.json`, in the schema the design's appendix lists and core's
 * `OperatorCaseRunner` checks. The page decides everything but what a reviewed override changes.
 */
object ScenarioWriter {
    private val settings =
        JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).indent(true).build()

    /** The scenario's context, from its page's operator family. */
    private val CONTEXTS =
        mapOf(
            "aggregation/expressions" to "EXPRESSION",
            "aggregation/stages" to "STAGE",
            "query/filters" to "FILTER",
            "query/updates" to "UPDATE",
        )

    /**
     * The operator folder of a page, as core's `TemplatedTestBase.prefix()` derives it from the
     * test class: `maxN-array-element` is `TestMaxNArrayElement`, so `maxNArrayElement`.
     */
    fun folder(operator: String): String =
        operator
            .split('-')
            .mapIndexed { index, part ->
                if (index == 0) part else part.replaceFirstChar { it.uppercase() }
            }
            .joinToString("")

    /** The scenario's name: `<context>/<operator folder>/<stem>`, also its folder. */
    fun name(page: DocumentedPage, stem: String): String =
        "${page.context}/${folder(page.operator)}/$stem"

    /** The scenario's path in the repository. */
    fun path(page: DocumentedPage, stem: String): String =
        "core/src/test/resources/dev/morphia/test/${name(page, stem)}/scenario.json"

    fun render(input: ScenarioInput): String {
        val documented = input.documented
        val override = input.override
        val action =
            BsonDocument("kind", BsonString(kind(documented.shape)))
                .append("collection", BsonString(documented.collection))
        when (documented.shape) {
            ActionShape.PIPELINE -> action["pipeline"] = BsonArray(documented.action)
            ActionShape.FIND -> {
                action["filter"] = documented.action[0]
                documented.action.getOrNull(1)?.let { action["projection"] = it }
            }
            ActionShape.UPDATE -> {
                require(documented.action.size == 2 && documented.action[1].isDocument) {
                    "${name(input.page, input.stem)}: only an update document is supported"
                }
                action["filter"] = documented.action[0]
                action["update"] = documented.action[1]
                action["multi"] = BsonBoolean(documented.many)
            }
        }
        // The find the page runs after a write: both sides run it, and its result is the one the
        // page printed.
        documented.read?.let { read ->
            action["read"] =
                BsonDocument("collection", BsonString(read.collection))
                    .append("filter", read.filter)
                    .also { spec ->
                        read.projection?.let { spec["projection"] = it }
                        read.sort?.let { spec["sort"] = it }
                    }
        }
        if (documented.alternatives.isNotEmpty()) {
            action["alternatives"] =
                BsonArray(
                    documented.alternatives.map { values ->
                        when (documented.shape) {
                            ActionShape.PIPELINE -> BsonDocument("pipeline", BsonArray(values))
                            ActionShape.FIND ->
                                BsonDocument("filter", values[0]).also { alternative ->
                                    values.getOrNull(1)?.let { alternative["projection"] = it }
                                }
                            ActionShape.UPDATE ->
                                BsonDocument("filter", values[0]).append("update", values[1])
                        }
                    }
                )
        }
        val collections = BsonDocument()
        documented.data.toSortedMap().forEach { (name, rows) ->
            collections[name] = BsonArray(rows)
        }
        val skipped = BsonDocument()
        input.dataset?.let { dataset ->
            val reason =
                "the example queries the $dataset sample dataset, which the tests do not load"
            skipped["behavior"] = BsonString(reason)
            skipped["published"] = BsonString(reason)
        }
        override?.skipped?.forEach { (check, reason) -> skipped[check] = BsonString(reason) }
        val scenario =
            BsonDocument()
                .append("schemaVersion", BsonInt32(1))
                .append("name", BsonString(name(input.page, input.stem)))
                // The owning test's @DisplayName must equal it.
                .append("title", BsonString(input.title))
                .append("context", BsonString(CONTEXTS.getValue(input.page.context)))
                .append(
                    "source",
                    BsonDocument()
                        .append("repository", BsonString("mongodb/docs"))
                        .append("commit", BsonString(input.page.source.commit))
                        .append("path", BsonString(input.page.source.path))
                        .append("blob", BsonString(input.page.pageBlob)),
                )
                .append("dataset", input.dataset?.let { BsonString(it) } ?: BsonNull.VALUE)
                .append("collections", collections)
                .append(
                    "indexes",
                    BsonArray(
                        documented.indexes.map { index ->
                            BsonDocument("collection", BsonString(index.collection))
                                .append("keys", index.keys)
                                .also { spec -> index.options?.let { spec["options"] = it } }
                        }
                    ),
                )
                .append("action", action)
                .append(
                    "expected",
                    documented.printed?.let { printed ->
                        BsonDocument(
                            "printed",
                            BsonArray(printed.map { PublishedEncoding.encode(it) }),
                        )
                    } ?: BsonDocument("none", BsonString("the page publishes no result")),
                )
                .append(
                    "comparison",
                    BsonDocument()
                        // The command check proves Morphia sends the documented sort; which order
                        // the server returns ties in is not Morphia's, so results compare as a
                        // multiset unless a review says the page's order is defined.
                        .append("order", BsonString(override?.order ?: "UNORDERED"))
                        .append(
                            "rules",
                            BsonArray(override?.rules.orEmpty().sorted().map { BsonString(it) }),
                        )
                        .append(
                            "ignore",
                            BsonArray(override?.ignore.orEmpty().map { BsonString(it) }),
                        )
                        .also { comparison ->
                            override
                                ?.unorderedArrays
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { paths ->
                                    comparison["unorderedArrays"] =
                                        BsonArray(paths.map { BsonString(it) })
                                }
                        }
                        .append("tolerance", BsonNull.VALUE),
                )
                .append(
                    "requirements",
                    BsonDocument()
                        .append(
                            "minimumServer",
                            override?.minimumServer?.let { BsonString(it) } ?: BsonNull.VALUE,
                        )
                        .append("topology", BsonString("STANDALONE")),
                )
        // Only a scenario with a skipped check carries the key, so a reason is never empty.
        if (!skipped.isEmpty()) scenario["skipped"] = skipped
        override?.let { Overrides.patch(scenario, it) }
        return scenario.toJson(settings) + "\n"
    }

    private fun kind(shape: ActionShape): String =
        when (shape) {
            ActionShape.PIPELINE -> "aggregate"
            ActionShape.FIND -> "find"
            ActionShape.UPDATE -> "update"
        }
}
