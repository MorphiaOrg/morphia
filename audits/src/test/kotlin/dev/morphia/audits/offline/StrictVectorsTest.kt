package dev.morphia.audits.offline

import dev.morphia.audits.semantics.BsonComparison
import dev.morphia.audits.semantics.BsonComparisonState
import dev.morphia.audits.semantics.ResultOrder
import java.nio.file.Files
import java.nio.file.Path
import org.bson.BsonDocument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Core's `StrictBson` runs the same vectors, so the two strict comparators agree. */
class StrictVectorsTest {
    @Test
    fun goldenVectorsMatchTheStrictComparator() {
        val root =
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isDirectory(it.resolve("core/src/test/resources")) }
        val vectors =
            BsonDocument.parse(
                Files.readString(
                    root.resolve(
                        "core/src/test/resources/dev/morphia/test/generated/vectors/strict.json"
                    )
                )
            )
        vectors
            .getArray("values")
            .map { it.asDocument() }
            .forEach {
                assertEquals(
                    it.getBoolean("equal").value,
                    BsonComparison.compare(it["left"], it["right"]).state ==
                        BsonComparisonState.EQUAL,
                    it.getString("name").value,
                )
            }
        vectors
            .getArray("results")
            .map { it.asDocument() }
            .forEach {
                val order =
                    if (it.getBoolean("ordered").value) ResultOrder.ORDERED
                    else ResultOrder.UNORDERED
                assertEquals(
                    it.getBoolean("equal").value,
                    BsonComparison.compareResults(
                            it.getArray("left").map { row -> row.asDocument() },
                            it.getArray("right").map { row -> row.asDocument() },
                            order,
                        )
                        .state == BsonComparisonState.EQUAL,
                    it.getString("name").value,
                )
            }
    }
}
