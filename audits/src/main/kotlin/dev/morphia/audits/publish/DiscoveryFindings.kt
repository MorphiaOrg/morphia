package dev.morphia.audits.publish

import dev.morphia.audits.generate.Attestation
import dev.morphia.audits.generate.ExampleInventory
import dev.morphia.audits.generate.ExampleStatus
import dev.morphia.audits.generate.GapKind
import dev.morphia.audits.generate.GenerationGap

/**
 * The publishable findings of one discovery run (`audits/discover.sh`): each binding the generated
 * cases lack, each example that appeared upstream with no test since [committed] was accepted, and
 * a failing integration. Keys are stable across runs, so a recurring finding updates its issue.
 */
object DiscoveryFindings {
    private val unbound = Regex("""^\S*: (.+) has no binding$""")
    private val failed = Regex("""^\[ERROR] (\S+) -- Time elapsed: .* <<< (FAILURE|ERROR)!$""")

    fun findings(
        committed: ExampleInventory,
        current: ExampleInventory,
        gaps: List<GenerationGap>,
        attestation: Attestation?,
        integrationLog: String?,
    ): List<Finding> =
        (bindings(current, gaps) +
                examples(committed, current, gaps) +
                listOfNotNull(integration(current, attestation, integrationLog)))
            .sortedBy { it.key }

    /** One finding per unbound node, listing the examples it blocks. */
    private fun bindings(current: ExampleInventory, gaps: List<GenerationGap>): List<Finding> =
        gaps
            .filter { it.kind == GapKind.UNBOUND }
            .groupBy { unbound.find(it.detail)?.groupValues?.get(1) ?: it.detail }
            .map { (node, blocked) ->
                Finding(
                    "${FindingType.MISSING_BINDING}:$node",
                    FindingType.MISSING_BINDING,
                    "Bind $node for generated operator tests",
                    "Generated operator tests cannot be written for these documented examples " +
                        "(mongodb/docs `${current.commit.take(10)}`) because $node has no entry in " +
                        "`audits/catalog/dsl-bindings.json`. If Morphia has an API for it, add the " +
                        "binding; if not, the API comes first.\n\n" +
                        blocked.joinToString("\n") {
                            "- ${it.context} `${it.operator}`: ${it.title.trim()} (`${it.detail}`)"
                        },
                )
            }

    /** Examples without a handwritten owner that the committed inventory did not have. */
    private fun examples(
        committed: ExampleInventory,
        current: ExampleInventory,
        gaps: List<GenerationGap>,
    ): List<Finding> {
        fun key(context: String, operator: String, title: String) =
            "$context/$operator/${title.trim()}"
        val known =
            committed.pages
                .flatMap { page ->
                    page.examples.map { key(page.context, page.operator, it.title) }
                }
                .toSet()
        val gapOf = gaps.associateBy { key(it.context, it.operator, it.title) }
        return current.pages.flatMap { page ->
            page.examples
                .filter { it.status != ExampleStatus.OWNED }
                .filter { key(page.context, page.operator, it.title) !in known }
                .map { example ->
                    val key = key(page.context, page.operator, example.title)
                    val state =
                        gapOf[key]?.let { "Generation reports ${it.kind}: ${it.detail}" }
                            ?: if (example.status == ExampleStatus.UNOWNED)
                                "It is generated when its family is in `audits/catalog/generation-review.json`."
                            else "It is ${example.status}: ${example.detail}"
                    Finding(
                        "${FindingType.NEW_EXAMPLE}:$key",
                        FindingType.NEW_EXAMPLE,
                        "Cover the documented ${page.operator} example \"${example.title.trim()}\"",
                        "mongodb/docs `${current.commit.take(10)}` documents this ${page.context} example " +
                            "at `${page.source.path}`, and no test covers it.\n\n$state\n\n" +
                            "Accept the new inventory (see audits/README.md, Weekly discovery) and " +
                            "regenerate, or cover it by hand.",
                    )
                }
        }
    }

    private fun integration(
        current: ExampleInventory,
        attestation: Attestation?,
        log: String?,
    ): Finding? {
        if (attestation == null || attestation.passed()) return null
        val tests =
            log.orEmpty()
                .lines()
                .mapNotNull { failed.find(it.trim())?.groupValues?.get(1) }
                .distinct()
                .sorted()
        return Finding(
            FindingType.INTEGRATION_FAILURE.name,
            FindingType.INTEGRATION_FAILURE,
            "Generated operator cases fail integration",
            "Scheduled discovery at mongodb/docs `${current.commit.take(10)}` integrated the regenerated " +
                "candidate and it failed: " +
                attestation.results.entries.joinToString { "${it.key} exited ${it.value}" } +
                ".\n\n" +
                (if (tests.isEmpty()) "No failing test was named; see the run's `integration.log`."
                else "Failing tests:\n" + tests.joinToString("\n") { "- `$it`" }) +
                "\n\nDiagnose from the run's `operator-discovery` artifact; a case the page or server makes " +
                "unverifiable is excluded in `audits/catalog/generation-review.json` with its evidence.",
        )
    }
}
