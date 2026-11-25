package dev.morphia.audits.offline

import dev.morphia.audits.publish.Finding
import dev.morphia.audits.publish.FindingType
import dev.morphia.audits.publish.GhGateway
import dev.morphia.audits.publish.GhMode
import dev.morphia.audits.publish.GhTarget
import dev.morphia.audits.publish.IssuePatch
import dev.morphia.audits.publish.PublicationAction
import dev.morphia.audits.publish.PublicationPolicy
import dev.morphia.audits.publish.Publisher
import java.nio.file.Path
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.api.io.TempDir

/**
 * Publishes through `gh` to the disposable repository named by `-Daudits.sandbox=OWNER/REPO` (an
 * `audit` label and a `sandbox` milestone). The repository is reused across runs: the findings'
 * markers find the issues earlier runs filed, so each run starts with an empty journal and still
 * creates nothing twice.
 */
@EnabledIfSystemProperty(named = "audits.sandbox", matches = "[^/]+/[^/]+")
class PublisherSandboxTest {
    @TempDir lateinit var directory: Path

    private val target =
        System.getProperty("audits.sandbox").split('/').let { GhTarget(it[0], it[1]) }

    private val policy =
        PublicationPolicy(
            labels =
                mapOf(
                    FindingType.MISSING_API to listOf("audit"),
                    FindingType.MISSING_SCENARIO to listOf("audit"),
                )
        )

    private val findings =
        listOf(
            Finding(
                "MISSING_API:sandbox/PIPELINE_STAGE/\$example",
                FindingType.MISSING_API,
                "Sandbox: missing API",
                "A gap.",
            ),
            Finding(
                "MISSING_SCENARIO:sandbox/AGGREGATION_EXPRESSION/\$example",
                FindingType.MISSING_SCENARIO,
                "Sandbox: version annotation",
                "An annotation.",
            ),
        )

    @Test
    fun publishingTwiceFilesEachFindingOnceAndKeepsManualEdits() {
        val gateway = GhGateway(target, GhMode.PUBLISH)
        // One journal across runs, as a real publisher keeps; issues are checked by number, since
        // the listing can lag a write.
        val journal = directory.resolve("journal.json")
        fun publisher() = Publisher(gateway, policy, journal, true, "sandbox")

        val first = publisher().publish(findings)
        assertTrue(
            first.all { it.action != PublicationAction.UNRESOLVED && it.problems.isEmpty() },
            first.toString(),
        )
        val numbers = first.map { it.issue!! }
        assertEquals(numbers.size, numbers.distinct().size)
        numbers.zip(findings).forEach { (number, finding) ->
            val issue = gateway.issue(number)
            assertTrue(Publisher.marker(finding.key) in issue.body)
            assertEquals(listOf("audit"), issue.labels)
        }

        val second = publisher().publish(findings)
        assertEquals(
            listOf(PublicationAction.UNCHANGED, PublicationAction.UNCHANGED),
            second.map { it.action },
        )
        assertEquals(numbers, second.map { it.issue })

        // A note added by hand outside the publisher's section survives a changed finding.
        val issue = gateway.issue(numbers[0])
        val note = "Triage note ${UUID.randomUUID()}"
        gateway.update(issue.number, IssuePatch(body = "$note\n${issue.body}"))
        val restated = findings[0].copy(body = "A gap, restated ${UUID.randomUUID()}.")
        val third = publisher().publish(listOf(restated))
        assertEquals(PublicationAction.UPDATED, third.single().action)
        val updated = gateway.issue(numbers[0])
        assertTrue(updated.body.startsWith(note), updated.body)
        assertTrue(restated.body in updated.body, updated.body)
        assertEquals(listOf("audit"), updated.labels)
    }
}
