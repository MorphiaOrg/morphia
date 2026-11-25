package dev.morphia.audits.publish

import dev.morphia.audits.extraction.Coverage
import dev.morphia.audits.extraction.ExampleInventory
import dev.morphia.audits.extraction.ExampleStatus
import dev.morphia.audits.extraction.Extraction
import dev.morphia.audits.extraction.MissingApi
import dev.morphia.audits.extraction.MissingScenario
import dev.morphia.audits.extraction.Overrides

/**
 * The publishable findings of one scheduled discovery run (`audits/discover.sh`), each keyed so a
 * recurring finding updates its issue:
 * - `MISSING_API`: a documented operator with no tagged Morphia factory;
 * - `NEW_EXAMPLE`: an example the current docs have and the committed inventory doesn't, with no
 *   scenario;
 * - `MISSING_SCENARIO`: an in-scope example with neither a scenario nor an exclusion;
 * - `FAILING_AUDIT`: operator tests that fail against the current docs' scenarios.
 */
/** What `PublishMainKt findings` writes and `publish` reads. */
data class FindingsReport(val schemaVersion: Int = 1, val findings: List<Finding>)

object DiscoveryFindings {
    private val failed = Regex("""^\[ERROR] (\S+) -- Time elapsed: .* <<< (FAILURE|ERROR)!$""")
    /**
     * Extraction's own failures that no other finding carries: stale overrides, failed refreshes.
     */
    private val unextracted = Regex("""^NOT EXTRACTED (\S+) "(.*)": (.*)$""")
    private val ownFailures =
        mapOf(
            "the override matches no example" to "stale-override",
            "the override can't apply" to "unapplied-override",
            "its source changed" to "refresh",
        )

    fun findings(
        committed: ExampleInventory,
        current: ExampleInventory,
        missingApi: List<MissingApi>,
        missingScenarios: List<MissingScenario>,
        auditLog: String?,
        extraction: String? = null,
    ): List<Finding> =
        (missingApi.map { api(current, it) } +
                newExamples(committed, current) +
                missingScenarios.map { scenario(current, it) } +
                failing(current, auditLog) +
                overrides(current, extraction))
            .sortedBy { it.key }

    private fun api(current: ExampleInventory, missing: MissingApi): Finding =
        Finding(
            "${FindingType.MISSING_API}:${missing.context}/${missing.operator}",
            FindingType.MISSING_API,
            "Add support for ${missing.operator} (${missing.context})",
            "mongodb/docs `${current.commit.take(10)}` documents `${missing.operator}` in the " +
                "${missing.context} family (`${missing.page}`), and no Morphia factory is tagged " +
                "`${missing.operator}`. If a factory exists, tag it; otherwise the API comes first." +
                if (missing.blocks.isEmpty()) ""
                else
                    "\n\nIts untested scenarios:\n" +
                        missing.blocks.joinToString("\n") { "- `$it`" },
        )

    /**
     * Every example the committed inventory doesn't have, including one extraction just wrote a
     * scenario for: it still needs its test.
     */
    private fun newExamples(committed: ExampleInventory, current: ExampleInventory): List<Finding> {
        val known = committed.pages.flatMap(Coverage::exampleKeys).toSet()
        return current.pages
            .filter { it.context in Extraction.FAMILIES }
            .flatMap { page ->
                page.examples
                    .zip(Coverage.exampleKeys(page))
                    .filter { (_, key) -> key !in known }
                    .map { (example, key) ->
                        val label = (example.name ?: example.title).trim()
                        val state =
                            if (example.status == ExampleStatus.OWNED)
                                "Extraction wrote its scenario, `${example.owner}`; write its test, or add it to " +
                                    "`${Coverage.UNTESTED}` with a reason."
                            else
                                "No scenario holds it: it is ${example.status}" +
                                    (example.detail?.let { ": $it" } ?: "") +
                                    ". Teach the reader the example, or exclude it in `${Overrides.PATH}` with a reason."
                        Finding(
                            "${FindingType.NEW_EXAMPLE}:$key",
                            FindingType.NEW_EXAMPLE,
                            "Cover the documented ${page.operator} example \"$label\"",
                            "mongodb/docs `${current.commit.take(10)}` documents this new ${page.context} " +
                                "example at `${page.source.path}`.\n\n$state",
                        )
                    }
            }
    }

    private fun scenario(current: ExampleInventory, missing: MissingScenario): Finding =
        Finding(
            "${FindingType.MISSING_SCENARIO}:${missing.key}",
            FindingType.MISSING_SCENARIO,
            "Extract the documented example ${missing.key}",
            "At mongodb/docs `${current.commit.take(10)}` this in-scope example has no scenario " +
                "and no exclusion:\n\n`${missing.line}`\n\nTeach the reader the example, or exclude it in " +
                "`${Overrides.PATH}` with a reason.",
        )

    /** One finding per failing test, so closing one doesn't hide another; and one if none ran. */
    private fun failing(current: ExampleInventory, log: String?): List<Finding> {
        if (log == null) return emptyList()
        val tests =
            log.lines()
                .mapNotNull { failed.find(it.trim())?.groupValues?.get(1) }
                .distinct()
                .sorted()
        val findings =
            tests.map { test ->
                Finding(
                    "${FindingType.FAILING_AUDIT}:$test",
                    FindingType.FAILING_AUDIT,
                    "$test fails against the current docs",
                    "Scheduled discovery extracted the scenarios of mongodb/docs `${current.commit.take(10)}` " +
                        "and ran core's operator tests against them; `$test` failed.\n\nDiagnose from the " +
                        "run's `operator-discovery` artifact (`audit.log`, `scenarios.diff`): fix Morphia, add a " +
                        "form rule with its vector, or record a reviewed override in `${Overrides.PATH}`.",
                )
            }
        // A build that fails before any test runs names no test, and would otherwise file nothing.
        // Maven runs quietly, so discover.sh marks a run that exited non-zero itself.
        if (tests.isEmpty() && ("[DISCOVERY]" in log || "BUILD FAILURE" in log)) {
            return listOf(
                Finding(
                    "${FindingType.FAILING_AUDIT}:build",
                    FindingType.FAILING_AUDIT,
                    "Operator tests didn't run against the current docs",
                    "Scheduled discovery at mongodb/docs `${current.commit.take(10)}` couldn't run core's " +
                        "operator tests: the build failed before any test ran. See the run's `audit.log`.",
                )
            )
        }
        return findings
    }

    private fun overrides(current: ExampleInventory, extraction: String?): List<Finding> =
        extraction
            .orEmpty()
            .lines()
            .mapNotNull { unextracted.find(it.trim()) }
            .mapNotNull { match ->
                ownFailures.entries
                    .firstOrNull { match.groupValues[3].startsWith(it.key) }
                    ?.let { match to it.value }
            }
            // One example can fail more than one way; each way is its own finding, once.
            .distinctBy { (match, kind) ->
                Triple(match.groupValues[1], match.groupValues[2], kind)
            }
            .map { (match, kind) ->
                val subject = "${match.groupValues[1]} \"${match.groupValues[2]}\""
                Finding(
                    "${FindingType.FAILING_AUDIT}:extraction:$kind:$subject",
                    FindingType.FAILING_AUDIT,
                    "Extraction fails for $subject",
                    "At mongodb/docs `${current.commit.take(10)}`, extraction reports for $subject: " +
                        "${match.groupValues[3]}.\n\nReview its override in `${Overrides.PATH}`.",
                )
            }
}
