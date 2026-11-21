package dev.morphia.audits.offline

import dev.morphia.audits.published.PublishedArray
import dev.morphia.audits.published.PublishedComparison
import dev.morphia.audits.published.PublishedDocument
import dev.morphia.audits.published.PublishedEncoding
import dev.morphia.audits.published.PublishedKind
import dev.morphia.audits.published.PublishedValue
import dev.morphia.audits.semantics.ResultOrder
import java.nio.file.Files
import java.nio.file.Path
import org.bson.BsonDocument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Core's `PrintedResults` runs the same vectors, so the Kotlin comparison and its Java port agree.
 */
class PrintedVectorsTest {
    @Test
    fun goldenVectorsMatchThePublishedComparison() {
        val root =
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isDirectory(it.resolve("core/src/test/resources")) }
        val vectors =
            BsonDocument.parse(
                Files.readString(
                    root.resolve(
                        "core/src/test/resources/dev/morphia/test/generated/vectors/printed.json"
                    )
                )
            )
        val kinds = mutableSetOf<PublishedKind>()
        val verdicts = mutableSetOf<String>()
        vectors
            .getArray("vectors")
            .map { it.asDocument() }
            .forEach { vector ->
                val verdict =
                    try {
                        val constraints =
                            vector.getArray("constraints").map {
                                PublishedEncoding.decode(it) as PublishedDocument
                            }
                        constraints.forEach { collect(it, kinds) }
                        // Encoding is lossless, so scenarios read back what migration wrote.
                        assertEquals(
                            vector.getArray("constraints").values,
                            constraints.map { PublishedEncoding.encode(it) },
                        )
                        PublishedComparison.compareResults(
                                constraints,
                                vector.getArray("actual").map { it.asDocument() },
                                ResultOrder.valueOf(vector.getString("order").value),
                            )
                            .state
                            .name
                    } catch (_: IllegalArgumentException) {
                        "INVALID"
                    }
                verdicts += verdict
                assertEquals(
                    vector.getString("verdict").value,
                    verdict,
                    vector.getString("name").value,
                )
            }
        assertEquals(PublishedKind.entries.toSet(), kinds)
        assertEquals(setOf("EQUAL", "DIFFERENT", "INVALID"), verdicts)
    }

    private fun collect(value: PublishedValue, kinds: MutableSet<PublishedKind>) {
        kinds += value.kind
        when (value) {
            is PublishedDocument -> value.fields.forEach { collect(it.value, kinds) }
            is PublishedArray -> value.items.forEach { collect(it, kinds) }
            else -> Unit
        }
    }
}
