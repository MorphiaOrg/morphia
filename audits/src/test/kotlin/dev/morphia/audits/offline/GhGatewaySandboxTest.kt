package dev.morphia.audits.offline

import dev.morphia.audits.publish.GhGateway
import dev.morphia.audits.publish.GhMode
import dev.morphia.audits.publish.GhTarget
import dev.morphia.audits.publish.IssuePatch
import dev.morphia.audits.publish.IssueRequest
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

/**
 * Runs the gateway against a real, explicitly designated disposable repository:
 * `-Daudits.sandbox=OWNER/REPO`, which must have an `audit` label and a `sandbox` milestone. It
 * never runs otherwise. The repository is reused across runs, so the test keeps one marked issue
 * and only edits it after the first run.
 */
@EnabledIfSystemProperty(named = "audits.sandbox", matches = "[^/]+/[^/]+")
class GhGatewaySandboxTest {
    private val target =
        System.getProperty("audits.sandbox").split('/').let { GhTarget(it[0], it[1]) }

    @Test
    fun readsCreatesAndEditsThroughGh() {
        val gateway = GhGateway(target, GhMode.PUBLISH)
        gateway.checkVersion()
        assertTrue("audit" in gateway.labels().map { it.name })
        val milestone = gateway.milestones().single { it.title == "sandbox" }

        val marker = "<!-- morphia-audits: gateway-contract -->"
        val existing = gateway.issues().filter { marker in it.body }
        assertTrue(existing.size <= 1, "More than one marked issue: ${existing.map { it.number }}")
        val issue =
            existing.singleOrNull()
                ?: gateway.create(
                    IssueRequest(
                        "Gateway contract",
                        "$marker\nCreated by GhGatewaySandboxTest.",
                        listOf("audit"),
                        milestone.number,
                    )
                )
        assertEquals(listOf("audit"), issue.labels)
        assertEquals(milestone.number, issue.milestone)

        val run = UUID.randomUUID().toString()
        val edited = gateway.update(issue.number, IssuePatch(body = "$marker\nLast run: $run"))
        assertTrue(run in edited.body)
        // The edit changed only the body.
        assertEquals(listOf("audit"), edited.labels)
        assertEquals(milestone.number, edited.milestone)
        assertEquals(edited, gateway.issue(issue.number))
    }
}
