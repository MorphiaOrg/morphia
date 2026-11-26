package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.publish.Finding
import dev.morphia.audits.publish.FindingType
import dev.morphia.audits.publish.GhException
import dev.morphia.audits.publish.GhFailure
import dev.morphia.audits.publish.GhIssue
import dev.morphia.audits.publish.GhLabel
import dev.morphia.audits.publish.GhMilestone
import dev.morphia.audits.publish.IssuePatch
import dev.morphia.audits.publish.IssueRequest
import dev.morphia.audits.publish.IssueTracker
import dev.morphia.audits.publish.JournalEntry
import dev.morphia.audits.publish.JournalState
import dev.morphia.audits.publish.ProjectMilestone
import dev.morphia.audits.publish.PublicationAction
import dev.morphia.audits.publish.PublicationJournal
import dev.morphia.audits.publish.PublicationPolicy
import dev.morphia.audits.publish.Publisher
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PublisherTest {
    @TempDir lateinit var directory: Path

    private val journal: Path
        get() = directory.resolve("journal.json")

    private val policy =
        PublicationPolicy(
            labels =
                mapOf(
                    FindingType.MISSING_API to listOf("enhancement"),
                    FindingType.FAILING_AUDIT to listOf("bug"),
                )
        )

    private val finding =
        Finding(
            "MISSING_API:docs/STAGE/\$vectorSearch",
            FindingType.MISSING_API,
            "Add \$vectorSearch",
            "Gap.",
        )

    /** An in-memory repository; a scripted create can fail or omit metadata. */
    private class Fake : IssueTracker {
        val issues = mutableListOf<GhIssue>()
        var creates = 0
        var updates = 0
        /** Replaces a create's outcome; null creates normally. */
        var onCreate: ((IssueRequest) -> GhIssue)? = null

        override fun issues() = issues.toList()

        override fun issue(number: Int) = issues.single { it.number == number }

        override fun labels() = listOf("enhancement", "bug", "manual").map { GhLabel(it) }

        // The milestone's number differs from any id it might have.
        val milestones = mutableListOf(GhMilestone(7, "3.x", "open"))

        override fun milestones() = milestones.toList()

        override fun createMilestone(title: String) =
            GhMilestone(milestones.maxOf { it.number } + 1, title, "open").also { milestones += it }

        override fun create(request: IssueRequest): GhIssue {
            creates++
            onCreate?.let {
                return it(request)
            }
            return add(request)
        }

        fun add(
            request: IssueRequest,
            labels: List<String> = request.labels,
            milestone: Int? = request.milestone,
        ): GhIssue {
            val issue =
                GhIssue(
                    issues.size + 1,
                    request.title,
                    "open",
                    request.body,
                    labels,
                    milestone,
                    "u",
                )
            issues += issue
            return issue
        }

        override fun update(number: Int, patch: IssuePatch): GhIssue {
            updates++
            val index = issues.indexOfFirst { it.number == number }
            val current = issues[index]
            val updated =
                current.copy(
                    body = patch.body ?: current.body,
                    labels = patch.labels ?: current.labels,
                    milestone = patch.milestone ?: current.milestone,
                )
            issues[index] = updated
            return updated
        }
    }

    private fun publisher(fake: Fake, publish: Boolean = true, polls: Int = 3) =
        Publisher(fake, policy, journal, publish, "3.x", polls = polls, sleeper = {})

    @Test
    fun aFindingIsFiledOnceAndRerunsChangeNothing() {
        val fake = Fake()
        val first = publisher(fake).publish(listOf(finding)).single()
        assertEquals(PublicationAction.CREATED, first.action)
        assertEquals(listOf("enhancement"), fake.issues.single().labels)
        assertEquals(7, fake.issues.single().milestone)
        assertEquals(
            PublicationAction.UNCHANGED,
            publisher(fake).publish(listOf(finding)).single().action,
        )
        assertEquals(1, fake.creates)
        assertEquals(0, fake.updates)
        assertEquals(
            JournalEntry(finding.key, JournalState.PUBLISHED, 1),
            AuditJson.publicationJournal(Files.readString(journal)).entries.single(),
        )
    }

    @Test
    fun anIssueTheListingLagsIsReadByItsNumber() {
        val fake = Fake()
        publisher(fake).publish(listOf(finding))
        // Right after a write, GitHub's listing may not show the issue yet.
        val lagging =
            object : IssueTracker by fake {
                override fun issues() = emptyList<GhIssue>()
            }
        val result =
            Publisher(lagging, policy, journal, true, "3.x", sleeper = {})
                .publish(listOf(finding))
                .single()
        assertEquals(PublicationAction.UNCHANGED, result.action)
        assertEquals(1, fake.creates)
    }

    @Test
    fun manualEditsSurviveAnUpdate() {
        val fake = Fake()
        publisher(fake).publish(listOf(finding))
        val issue = fake.issues.single()
        fake.issues[0] =
            issue.copy(
                body = "Triage notes.\n" + issue.body + "\nMore notes.",
                labels = listOf("manual"),
                milestone = 3,
            )
        val result = publisher(fake).publish(listOf(finding.copy(body = "Gap, restated."))).single()
        assertEquals(PublicationAction.UPDATED, result.action)
        val updated = fake.issues.single()
        assertTrue(updated.body.startsWith("Triage notes.\n"))
        assertTrue(updated.body.endsWith("\nMore notes."))
        assertTrue(
            "Gap, restated." in updated.body &&
                "Gap." !in updated.body.replace("Gap, restated.", "")
        )
        // Required labels are added; manual ones and a manual milestone stay.
        assertEquals(listOf("manual", "enhancement"), updated.labels)
        assertEquals(3, updated.milestone)
    }

    @Test
    fun aClosedIssueIsADisposition() {
        val fake = Fake()
        publisher(fake).publish(listOf(finding))
        fake.issues[0] = fake.issues[0].copy(state = "closed")
        val result = publisher(fake).publish(listOf(finding.copy(body = "Changed."))).single()
        assertEquals(PublicationAction.CLOSED, result.action)
        assertEquals(0, fake.updates)
        assertEquals(1, fake.creates)
    }

    @Test
    fun aLostCreateIsReconciledNotReplayed() {
        val fake = Fake()
        var reads = 0
        // The create times out, but GitHub received it: the issue appears on the second poll.
        fake.onCreate = { request ->
            throw GhException(GhFailure.TIMEOUT, "timed out").also { pending = request }
        }
        val tracker =
            object : IssueTracker by fake {
                override fun issues(): List<GhIssue> {
                    reads++
                    if (reads == 3) pending?.let { fake.add(it) }
                    return fake.issues()
                }
            }
        val result =
            Publisher(tracker, policy, journal, true, "3.x", polls = 3, sleeper = {})
                .publish(listOf(finding))
                .single()
        assertEquals(PublicationAction.CREATED, result.action)
        assertEquals(1, result.issue)
        assertEquals(1, fake.creates)
    }

    private var pending: IssueRequest? = null

    @Test
    fun anUnfoundCreateStaysUnresolvedAcrossRuns() {
        val fake = Fake()
        fake.onCreate = { throw GhException(GhFailure.TIMEOUT, "timed out") }
        assertEquals(
            PublicationAction.UNRESOLVED,
            publisher(fake).publish(listOf(finding)).single().action,
        )
        fake.onCreate = null
        // A later run does not create it again while the journal says it may exist.
        assertEquals(
            PublicationAction.UNRESOLVED,
            publisher(fake).publish(listOf(finding)).single().action,
        )
        assertEquals(1, fake.creates)
        // Once the issue appears it is adopted.
        fake.add(
            IssueRequest(
                "t",
                Publisher.marker(finding.key) + "\nGap.\n<!-- morphia-audits:end -->",
                listOf("enhancement"),
                7,
            )
        )
        assertEquals(
            PublicationAction.UNCHANGED,
            publisher(fake).publish(listOf(finding)).single().action,
        )
    }

    @Test
    fun anIntentWithoutAnOutcomeIsRecoveredAsPossiblyCreated() {
        Files.writeString(
            journal,
            AuditJson.write(
                PublicationJournal(
                    entries = listOf(JournalEntry(finding.key, JournalState.INTENDED))
                )
            ),
        )
        val fake = Fake()
        assertEquals(
            PublicationAction.UNRESOLVED,
            publisher(fake).publish(listOf(finding)).single().action,
        )
        assertEquals(0, fake.creates)
    }

    @Test
    fun metadataACreateOmitsIsRepairedOnTheSameIssue() {
        val fake = Fake()
        fake.onCreate = { request -> fake.add(request, labels = emptyList(), milestone = null) }
        val result = publisher(fake).publish(listOf(finding)).single()
        assertEquals(PublicationAction.CREATED, result.action)
        assertTrue(result.problems.any { "missing labels" in it }, result.problems.toString())
        assertEquals(1, fake.issues.size)
        assertEquals(listOf("enhancement"), fake.issues.single().labels)
        assertEquals(7, fake.issues.single().milestone)
    }

    @Test
    fun aMissingMilestoneIsCreatedOnlyWhenThereIsSomethingToPublish() {
        val fake = Fake()
        fun publisher(publish: Boolean) =
            Publisher(fake, policy, journal, publish, "3.0.1", sleeper = {})
        publisher(true).publish(emptyList())
        publisher(false).publish(listOf(finding))
        assertEquals(listOf("3.x"), fake.milestones.map { it.title })
        publisher(true).publish(listOf(finding))
        assertEquals(GhMilestone(8, "3.0.1", "open"), fake.milestones.last())
        assertEquals(8, fake.issues.single().milestone)
        // The next run finds it rather than creating another.
        publisher(true).publish(listOf(finding))
        assertEquals(2, fake.milestones.size)
    }

    @Test
    fun aClosedMilestoneStopsPublication() {
        val fake = Fake()
        fake.milestones += GhMilestone(8, "2.5.3", "closed")
        assertThrows(IllegalArgumentException::class.java) {
            Publisher(fake, policy, journal, true, "2.5.3").publish(listOf(finding))
        }
        assertEquals(0, fake.creates)
    }

    @Test
    fun theMilestoneIsTheReleaseThePomBuilds() {
        fun pom(version: String) =
            directory.resolve("pom.xml").also {
                Files.writeString(
                    it,
                    "<project><parent><version>73</version></parent>$version<dependencies>" +
                        "<dependency><version>9.9.9</version></dependency></dependencies></project>",
                )
            }
        assertEquals("3.0.1", ProjectMilestone.of(pom("<version>3.0.1-SNAPSHOT</version>")))
        assertEquals("3.1.0", ProjectMilestone.of(pom("<version>3.1.0</version>")))
        assertThrows(IllegalArgumentException::class.java) { ProjectMilestone.of(pom("")) }
        assertThrows(IllegalArgumentException::class.java) {
            ProjectMilestone.of(pom("<version>\${revision}</version>"))
        }
        val root =
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isDirectory(it.resolve("audits/catalog")) }
        assertTrue(Regex("""\d+\.\d+\.\d+""").matches(ProjectMilestone.of(root.resolve("pom.xml"))))
    }

    @Test
    fun reportOnlyChangesNothing() {
        val fake = Fake()
        assertEquals(
            PublicationAction.PLANNED,
            publisher(fake, publish = false).publish(listOf(finding)).single().action,
        )
        assertEquals(0, fake.creates + fake.updates)
        assertTrue(!Files.exists(journal))
    }

    @Test
    fun missingLabelsAndDuplicateMarkersStopPublication() {
        val fake = Fake()
        assertThrows(IllegalArgumentException::class.java) {
            Publisher(
                    fake,
                    policy.copy(labels = mapOf(FindingType.MISSING_API to listOf("absent"))),
                    journal,
                    true,
                )
                .publish(listOf(finding))
        }
        repeat(2) { fake.add(IssueRequest("t", Publisher.marker(finding.key), emptyList(), null)) }
        assertThrows(IllegalArgumentException::class.java) {
            publisher(fake).publish(listOf(finding))
        }
        assertEquals(0, fake.creates)
    }
}
