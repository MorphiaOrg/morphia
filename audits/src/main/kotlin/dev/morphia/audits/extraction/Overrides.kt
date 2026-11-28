package dev.morphia.audits.extraction

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonValue
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings

/**
 * A reviewed change to what extraction writes for one example. Its identity is the example's [page]
 * (`<context>/<operator>`), its [title] as the inventory records it, and [blocksSha256], the digest
 * of its source blocks: an override written for one version of an example doesn't silently apply to
 * a changed one. Every override has a [reason] a reviewer can check.
 */
data class ExampleOverride(
    val page: String,
    val title: String,
    val blocksSha256: String,
    val reason: String,
    /** The page is wrong in a way no patch fixes, or the example is out of scope: no scenario. */
    @get:JsonInclude(JsonInclude.Include.NON_DEFAULT) val exclude: Boolean = false,
    /** Edits to the written scenario, each guarded by the digest of the value it replaces. */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val patches: List<ScenarioPatch> = emptyList(),
    /**
     * Checks the scenario skips: `serialization`, `behavior` or `published`, with their reasons.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val skipped: Map<String, String> = emptyMap(),
    /**
     * `ORDERED` when the page's results are in a defined order; scenarios are unordered otherwise.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val order: String? = null,
    /** Form rules the command check applies, by `FormRules.Rule` name. */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val rules: List<String> = emptyList(),
    /** Whether the action block holds equivalent spellings of one command, as alternatives. */
    @get:JsonInclude(JsonInclude.Include.NON_DEFAULT) val alternatives: Boolean = false,
    /** Top-level result fields the published check ignores, such as server-generated ids. */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val ignore: List<String> = emptyList(),
    /** RFC 6901 pointers, within each result, of arrays the server builds in no defined order. */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val unorderedArrays: List<String> = emptyList(),
    /** The oldest server release that runs the example, such as `7.0.0`. */
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val minimumServer: String? = null,
) {
    /** The page and title, for messages. */
    val label: String
        get() = "$page \"$title\""
}

/**
 * Replaces the value at [pointer], an RFC 6901 pointer into the written scenario
 * (`/collections/inventory/0/qty`, `/action/pipeline/1`, `/expected/printed/0`), with [value] in
 * extended JSON. [fromSha256] is the digest of the extracted value it replaces
 * ([Overrides.digest]), so a page fixed upstream fails extraction instead of being overwritten.
 */
data class ScenarioPatch(val pointer: String, val fromSha256: String, val value: JsonNode)

/**
 * `audits/catalog/example-overrides.json`: every reviewed override, and the sample datasets an
 * example may query instead of seeding its own data, by collection name.
 */
data class ExampleOverrides(
    val schemaVersion: Int = 1,
    val sampleDatasets: Map<String, String> = emptyMap(),
    val overrides: List<ExampleOverride> = emptyList(),
)

object Overrides {
    const val PATH = "audits/catalog/example-overrides.json"

    /** The rule names core's `FormRules.Rule` defines; a test checks the two lists agree. */
    val RULES =
        setOf(
            "COND_ARRAY",
            "EQ_SHORTHAND",
            "REGEX_FORM",
            "FIELD_OPERATORS_AND",
            "SINGLE_ARGUMENT_ARRAY",
            "PROJECT_ID_FIRST",
            "PROJECTION_FLAGS",
            "UNWIND_PATH",
            "REGEX_OPTIONS",
            "NAMED_ARGUMENTS",
            "GEO_COORDINATES",
        )

    private val SKIPS = setOf("serialization", "behavior", "published")
    private val canonical = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build()

    /** The identity digest of an example's source blocks: their roles and digests, in order. */
    fun identity(blocks: List<SourceBlock>): String =
        Documented.sha256(
            blocks.joinToString("\n") { "${it.role}:${it.sha256}" }.toByteArray(Charsets.UTF_8)
        )

    /** The digest a patch's `fromSha256` names: the value's canonical extended JSON. */
    fun digest(value: BsonValue): String =
        Documented.sha256(BsonDocument("v", value).toJson(canonical).toByteArray(Charsets.UTF_8))

    /**
     * The problems with [overrides] on their own: a duplicate identity, a missing reason, an
     * exclusion that also edits, or an unknown skip, order or rule.
     */
    fun problems(overrides: ExampleOverrides): List<String> {
        val problems = mutableListOf<String>()
        overrides.overrides
            .groupingBy { Triple(it.page, it.title, it.blocksSha256) }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { problems += "${it.first} \"${it.second}\" is overridden more than once" }
        overrides.overrides.forEach { override ->
            if (override.reason.isBlank()) problems += "${override.label} has no reason"
            if (
                override.exclude &&
                    override !=
                        ExampleOverride(
                            override.page,
                            override.title,
                            override.blocksSha256,
                            override.reason,
                            exclude = true,
                        )
            ) {
                problems += "${override.label} is excluded, so it can't also change its scenario"
            }
            (override.skipped.keys - SKIPS).forEach {
                problems += "${override.label} skips an unknown check $it"
            }
            if (override.skipped.values.any { it.isBlank() }) {
                problems += "${override.label} skips a check without a reason"
            }
            if ("behavior" in override.skipped && "published" !in override.skipped) {
                problems += "${override.label} skips behavior, so it must skip published too"
            }
            if (override.order != null && override.order != "ORDERED") {
                problems +=
                    "${override.label} has order ${override.order}; only ORDERED is an override"
            }
            (override.rules - RULES).forEach {
                problems += "${override.label} names an unknown rule $it"
            }
            override.unorderedArrays
                .filterNot { it.startsWith("/") }
                .forEach { problems += "${override.label}: unordered array $it is not a pointer" }
            override.minimumServer
                ?.takeUnless { Regex("\\d+\\.\\d+\\.\\d+").matches(it) }
                ?.let { problems += "${override.label}: minimum server $it is not x.y.z" }
        }
        return problems
    }

    /**
     * Applies [override]'s patches to [scenario] in place.
     *
     * @throws IllegalArgumentException when a pointer names no value, or a value has changed since
     *   the patch was reviewed
     */
    fun patch(scenario: BsonDocument, override: ExampleOverride) {
        override.patches.forEach { patch ->
            val tokens =
                patch.pointer.split('/').drop(1).map { it.replace("~1", "/").replace("~0", "~") }
            require(patch.pointer.startsWith("/") && tokens.isNotEmpty()) {
                "${override.label}: ${patch.pointer} is not a pointer"
            }
            var parent: BsonValue = scenario
            tokens.dropLast(1).forEach { token -> parent = child(parent, token, override, patch) }
            val last = tokens.last()
            val current = child(parent, last, override, patch)
            require(digest(current) == patch.fromSha256) {
                "${override.label}: the value at ${patch.pointer} has changed since the patch was reviewed"
            }
            val value = BsonDocument.parse("{\"v\": ${patch.value}}")["v"]!!
            when (parent) {
                is BsonDocument -> (parent as BsonDocument)[last] = value
                is BsonArray -> (parent as BsonArray)[last.toInt()] = value
                else -> throw IllegalStateException("unreachable")
            }
        }
    }

    private fun child(
        value: BsonValue,
        token: String,
        override: ExampleOverride,
        patch: ScenarioPatch,
    ): BsonValue =
        when (value) {
            is BsonDocument -> value[token]
            is BsonArray -> token.toIntOrNull()?.takeIf { it in value.indices }?.let { value[it] }
            else -> null
        } ?: throw IllegalArgumentException("${override.label}: ${patch.pointer} names no value")
}
