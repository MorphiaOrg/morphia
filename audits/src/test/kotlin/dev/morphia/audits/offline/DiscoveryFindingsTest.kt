package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.generate.Attestation
import dev.morphia.audits.generate.DocumentedExample
import dev.morphia.audits.generate.DocumentedPage
import dev.morphia.audits.generate.ExampleInventory
import dev.morphia.audits.generate.ExampleStatus
import dev.morphia.audits.generate.GapKind
import dev.morphia.audits.generate.GenerationGap
import dev.morphia.audits.generate.GenerationGaps
import dev.morphia.audits.publish.DiscoveryFindings
import dev.morphia.audits.publish.FindingType
import dev.morphia.audits.repairs.FormSource
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiscoveryFindingsTest {
    private val commit = "d".repeat(40)

    private fun inventory(vararg examples: DocumentedExample) =
        ExampleInventory(
            sourceRepository = "mongodb/docs",
            commit = commit,
            pages =
                listOf(
                    DocumentedPage(
                        "aggregation/expressions",
                        "bitOr",
                        FormSource(
                            commit,
                            "content/manual/manual/source/reference/operator/aggregation/bitOr.txt",
                        ),
                        "blob",
                        emptyList(),
                        examples.toList(),
                    )
                ),
            skipped = emptyList(),
            summary = emptyMap(),
        )

    private val owned =
        DocumentedExample(
            "Two Integers",
            ExampleStatus.OWNED,
            owner = "aggregation/expressions/bitOr/x",
        )
    private val old = DocumentedExample("Old Unowned", ExampleStatus.UNOWNED, "oldUnowned")
    private val added = DocumentedExample("Added Upstream ", ExampleStatus.UNOWNED, "addedUpstream")

    private fun gap(title: String, detail: String) =
        GenerationGap("aggregation/expressions", "bitOr", title, GapKind.UNBOUND, detail)

    @Test
    fun findingsNameMissingBindingsNewExamplesAndFailedIntegration() {
        val gaps =
            listOf(
                gap("Old Unowned", "/pipeline/0: stage \$match has no binding"),
                gap("Added Upstream ", "/pipeline/1: stage \$match has no binding"),
            )
        val findings =
            DiscoveryFindings.findings(
                inventory(owned, old),
                inventory(owned, old, added),
                gaps,
                Attestation(
                    candidateSha256 = "0",
                    baseRevision = "b",
                    tests = emptyList(),
                    results = mapOf("critter" to 1, "reflection" to 0),
                ),
                "[ERROR] dev.morphia.test.generated.expressions.TestAdd.addThings -- Time elapsed: 0.6 s <<< FAILURE!\n",
            )
        assertEquals(
            listOf(
                "INTEGRATION_FAILURE",
                "MISSING_BINDING:stage \$match",
                "NEW_EXAMPLE:aggregation/expressions/bitOr/Added Upstream",
            ),
            findings.map { it.key },
        )
        val binding = findings.single { it.type == FindingType.MISSING_BINDING }
        // One finding for the node, listing every example it blocks.
        assertTrue("Old Unowned" in binding.body && "Added Upstream" in binding.body, binding.body)
        val example = findings.single { it.type == FindingType.NEW_EXAMPLE }
        assertTrue("UNBOUND" in example.body && "bitOr.txt" in example.body, example.body)
        val integration = findings.single { it.type == FindingType.INTEGRATION_FAILURE }
        assertTrue(
            "`dev.morphia.test.generated.expressions.TestAdd.addThings`" in integration.body,
            integration.body,
        )
    }

    @Test
    fun aQuietRunHasNoFindings() {
        val findings =
            DiscoveryFindings.findings(
                inventory(owned, old),
                inventory(owned, old),
                emptyList(),
                Attestation(
                    candidateSha256 = "0",
                    baseRevision = "b",
                    tests = emptyList(),
                    results = mapOf("critter" to 0, "reflection" to 0),
                ),
                null,
            )
        assertEquals(emptyList<String>(), findings.map { it.key })
    }

    @Test
    fun theCommittedPolicyLabelsEveryFindingType() {
        val root =
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isDirectory(it.resolve("audits/catalog")) }
        val policy =
            AuditJson.publicationPolicy(
                Files.readString(root.resolve("audits/catalog/publication-policy.json"))
            )
        assertEquals(FindingType.entries.toSet(), policy.labels.keys)
        assertEquals(
            GenerationGaps(gaps = listOf(gap("t", "d"))),
            AuditJson.generationGaps(AuditJson.write(GenerationGaps(gaps = listOf(gap("t", "d"))))),
        )
    }
}
