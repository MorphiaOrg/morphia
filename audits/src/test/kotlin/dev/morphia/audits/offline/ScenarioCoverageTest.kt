package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.extraction.Coverage
import dev.morphia.audits.extraction.DocumentedExamples
import dev.morphia.audits.extraction.Overrides
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The audit's completeness checks, against the committed inventory, overrides, scenarios and tests.
 * They need no docs checkout: extraction recorded what the pages hold.
 */
class ScenarioCoverageTest {
    private val root: Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("core/src/test/resources")) }

    private val overrides = AuditJson.overrides(Files.readString(root.resolve(Overrides.PATH)))

    @Test
    fun overridesAreReviewable() {
        assertEquals(emptyList<String>(), Overrides.problems(overrides))
    }

    @Test
    fun overrideRulesAreCoresRules() {
        val source =
            Files.readString(root.resolve("core/src/test/java/dev/morphia/test/FormRules.java"))
        val rules =
            Regex("""enum Rule \{([^}]*)\}""")
                .find(source)!!
                .groupValues[1]
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        assertEquals(rules, Overrides.RULES)
    }

    /** Every in-scope documented example has a scenario, or a reviewed exclusion. */
    @Test
    fun everyExampleHasAScenario() {
        val inventory =
            AuditJson.exampleInventory(Files.readString(root.resolve(DocumentedExamples.PATH)))
        val missing = Coverage.missingScenarios(root, inventory, overrides)
        assertEquals(
            emptyList<String>(),
            missing,
            "${missing.size} documented examples have no scenario. Fix the reader, or exclude the " +
                "example in ${Overrides.PATH} with a reason.",
        )
    }

    /**
     * Every scenario has its test or a baseline entry. Every untested scenario is listed on every
     * run, in `audits/target/untested-scenarios.json` and the printed summary, not only the new
     * ones.
     */
    @Test
    fun everyScenarioHasItsTestOrABaselineEntry() {
        val baseline = AuditJson.untested(Files.readString(root.resolve(Coverage.UNTESTED)))
        val (untested, problems) = Coverage.baseline(Coverage.untested(root), baseline)
        val queue = root.resolve("audits/target/untested-scenarios.json")
        Files.createDirectories(queue.parent)
        Files.writeString(queue, AuditJson.write(untested) + "\n")
        println(Coverage.summary(untested) + "Work queue: $queue")
        assertEquals(emptyList<String>(), problems)
    }
}
