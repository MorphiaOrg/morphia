package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.extraction.ApiCoverage
import dev.morphia.audits.extraction.Coverage
import dev.morphia.audits.extraction.DocumentedExamples
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Morphia's API coverage from its javadoc tags. Every run reports the documented operators with no
 * tagged factory, in `audits/target/missing-api.json`; the baseline's operator gaps must still
 * hold.
 */
class ApiCoverageTest {
    private val root: Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("core/src/test/resources")) }

    private val tags = ApiCoverage.tags(root)
    private val baseline = AuditJson.untested(Files.readString(root.resolve(Coverage.UNTESTED)))

    @Test
    fun theTagsAreRead() {
        // About 270 distinct tagged operators today; an empty read would let every check pass.
        assertTrue(tags.values.sumOf { it.size } > 200, "${tags.mapValues { it.value.size }}")
    }

    @Test
    fun missingApiIsReported() {
        val inventory =
            AuditJson.exampleInventory(Files.readString(root.resolve(DocumentedExamples.PATH)))
        assertTrue(inventory.operators.isNotEmpty(), "the inventory records no operators")
        val missing = ApiCoverage.missing(inventory, tags, baseline)
        val report = root.resolve("audits/target/missing-api.json")
        Files.createDirectories(report.parent)
        Files.writeString(report, AuditJson.write(missing) + "\n")
        println(
            "Documented operators with no tagged Morphia factory: ${missing.size}\n" +
                missing.joinToString("\n") { "  ${it.context} ${it.operator}" } +
                "\nReport: $report"
        )
    }

    @Test
    fun operatorGapsStillHold() {
        assertEquals(emptyList<String>(), ApiCoverage.staleGaps(baseline, tags))
    }
}
