package dev.morphia.audits.repairs

import dev.morphia.audits.semantics.BsonComparison
import dev.morphia.audits.semantics.BsonComparisonState
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonValue
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings

/**
 * Reviewed correction of one documented value at [pointer] (RFC 6901) of [file]. [documented] and
 * [curated] are canonical extended JSON of a `{"v": value}` document:
 * - both set: the documented value is replaced by the curated one;
 * - only [documented]: the documented field is removed;
 * - only [curated]: the field is added to its document at key position [index].
 *
 * Allowed only for DOCS_ERROR and FIXTURE_CURATION locks. A correction pins the documented value it
 * replaces or removes, so a changed page makes it stale instead of applying it to something else.
 * It never reaches a whole file or a top-level document (a row or pipeline stage), so every other
 * difference in that row needs its own correction. Corrections apply in order, after retypes; for
 * `action.json` the pointer addresses the canonical form under the declaration's rules.
 */
data class FormCorrection(
    val file: String,
    val pointer: String,
    val documented: String?,
    val curated: String?,
    val index: Int? = null,
) {
    init {
        require(pointer.count { it == '/' } >= 2) {
            "A correction must be inside a top-level document: $this"
        }
        require(documented != null || curated != null) { "A correction must change something" }
        require((index != null) == (documented == null)) { "Only an added field has a position" }
    }

    companion object {
        private val settings = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build()

        fun of(
            file: String,
            pointer: String,
            documented: BsonValue?,
            curated: BsonValue?,
            index: Int? = null,
        ) = FormCorrection(file, pointer, documented?.let(::json), curated?.let(::json), index)

        fun json(value: BsonValue): String = BsonDocument("v", value).toJson(settings)

        fun value(json: String): BsonValue = BsonDocument.parse(json)["v"]!!
    }
}

object FormCorrections {
    /**
     * Applies [corrections] (all for one file) to a copy of [values]; fails with the pointer of the
     * first correction whose documented value is not what the page now has.
     */
    fun apply(values: List<BsonValue>, corrections: List<FormCorrection>): Result {
        val root = BsonDocument("v", BsonArray(values)).clone().getArray("v")
        corrections.forEach { correction ->
            val stale = Result.Stale(correction.pointer)
            val key = correction.pointer.substringAfterLast('/').unescape()
            if (correction.documented == null) {
                val parent =
                    NumericRetypes.resolve(root, correction.pointer.substringBeforeLast('/'))?.first
                        as? BsonDocument ?: return stale
                val entries = parent.entries.map { it.key to it.value }.toMutableList()
                if (key in parent || correction.index!! > entries.size) return stale
                entries.add(correction.index, key to FormCorrection.value(correction.curated!!))
                parent.clear()
                entries.forEach { (name, value) -> parent[name] = value }
                return@forEach
            }
            val (current, replace) =
                NumericRetypes.resolve(root, correction.pointer) ?: return stale
            val pinned = FormCorrection.value(correction.documented)
            if (
                BsonComparison.compare(BsonArray(listOf(current)), BsonArray(listOf(pinned)))
                    .state != BsonComparisonState.EQUAL
            ) {
                return stale
            }
            if (correction.curated != null) replace(FormCorrection.value(correction.curated))
            else {
                val parent =
                    NumericRetypes.resolve(root, correction.pointer.substringBeforeLast('/'))?.first
                        as? BsonDocument ?: return stale
                parent.remove(key)
            }
        }
        return Result.Applied(root.values)
    }

    private fun String.unescape() = replace("~1", "/").replace("~0", "~")

    sealed interface Result {
        data class Applied(val values: List<BsonValue>) : Result

        data class Stale(val pointer: String) : Result
    }
}
