package dev.morphia.audits.publish

import dev.morphia.audits.extraction.DocumentedExample
import dev.morphia.audits.extraction.DocumentedPage
import dev.morphia.audits.extraction.ExampleInventory
import dev.morphia.audits.extraction.ExampleStatus
import dev.morphia.audits.extraction.Extraction
import dev.morphia.audits.extraction.MissingApi
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
object DiscoveryFindings {
    private val failed = Regex("""^\[ERROR] (\S+) -- Time elapsed: .* <<< (FAILURE|ERROR)!$""")

    fun findings(
        committed: ExampleInventory,
        current: ExampleInventory,
        missingApi: List<MissingApi>,
        missingScenarios: List<String>,
        auditLog: String?,
    ): List<Finding> =
        (missingApi.map { api(current, it) } +
                newExamples(committed, current) +
                missingScenarios.map { scenario(current, it) } +
                listOfNotNull(failing(current, auditLog)))
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

    /** An example's key: its page, title and source digest, as overrides name it. */
    private fun key(page: DocumentedPage, example: DocumentedExample) =
        "${page.id}/${example.title.trim()}#${Overrides.identity(example.blocks).take(12)}"

    private fun newExamples(committed: ExampleInventory, current: ExampleInventory): List<Finding> {
        val known = committed.pages.flatMap { page -> page.examples.map { key(page, it) } }.toSet()
        return current.pages
            .filter { it.context in Extraction.FAMILIES }
            .flatMap { page ->
                page.examples
                    .filter { it.status != ExampleStatus.OWNED && key(page, it) !in known }
                    .map { example ->
                        val label = (example.name ?: example.title).trim()
                        Finding(
                            "${FindingType.NEW_EXAMPLE}:${key(page, example)}",
                            FindingType.NEW_EXAMPLE,
                            "Cover the documented ${page.operator} example \"$label\"",
                            "mongodb/docs `${current.commit.take(10)}` documents this ${page.context} " +
                                "example at `${page.source.path}`, and no scenario holds it. It is " +
                                "${example.status}" +
                                (example.detail?.let { ": $it" } ?: "") +
                                ".\n\nRun the extraction (see audits/README.md, the audit) and write " +
                                "its test, or exclude it in `${Overrides.PATH}` with a reason.",
                        )
                    }
            }
    }

    private fun scenario(current: ExampleInventory, line: String): Finding {
        // A line is `<page> "<title>": <state>`, or `<page>: the reader couldn't read the page:
        // ...`;
        // a title may hold ": " itself.
        val subject =
            if ("\": " in line) line.substringBefore("\": ") + "\"" else line.substringBefore(": ")
        return Finding(
            "${FindingType.MISSING_SCENARIO}:$subject",
            FindingType.MISSING_SCENARIO,
            "Extract the documented example $subject",
            "At mongodb/docs `${current.commit.take(10)}` this in-scope example has no scenario " +
                "and no exclusion:\n\n`$line`\n\nTeach the reader the example, or exclude it in " +
                "`${Overrides.PATH}` with a reason.",
        )
    }

    private fun failing(current: ExampleInventory, log: String?): Finding? {
        val tests =
            log.orEmpty()
                .lines()
                .mapNotNull { failed.find(it.trim())?.groupValues?.get(1) }
                .distinct()
                .sorted()
        if (tests.isEmpty()) return null
        return Finding(
            FindingType.FAILING_AUDIT.name,
            FindingType.FAILING_AUDIT,
            "Operator tests fail against the current docs",
            "Scheduled discovery extracted the scenarios of mongodb/docs `${current.commit.take(10)}` " +
                "and ran core's operator tests against them. These failed:\n" +
                tests.joinToString("\n") { "- `$it`" } +
                "\n\nDiagnose from the run's `operator-discovery` artifact: fix Morphia, add a form " +
                "rule with its vector, or record a reviewed override in `${Overrides.PATH}`.",
        )
    }
}
