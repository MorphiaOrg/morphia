package dev.morphia.audits.publish

import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration

/** The kinds of finding the publisher files, each with its own labels. */
enum class FindingType {
    /** A documented operator with no tagged Morphia factory. */
    MISSING_API,
    /** A documented example that appeared upstream since the committed inventory, with no test. */
    NEW_EXAMPLE,
    /** An operator test failing against a new docs commit's scenarios. */
    FAILING_AUDIT,
}

/**
 * One publishable finding. [key] is stable across runs (its type and subject), and [body] is the
 * publisher-owned text for its issue.
 */
data class Finding(val key: String, val type: FindingType, val title: String, val body: String)

/** Which labels each finding type gets; all must exist. */
data class PublicationPolicy(val schemaVersion: Int = 1, val labels: Map<FindingType, List<String>>)

enum class JournalState {
    /** The create was about to be sent; no outcome was recorded. */
    INTENDED,
    /** The create may have reached GitHub, but its issue has not been found. */
    POSSIBLY_CREATED,
    /** The issue is known. */
    PUBLISHED,
}

data class JournalEntry(val key: String, val state: JournalState, val issue: Int? = null)

/** Durable publication state: what was sent, and which issue each key owns. */
data class PublicationJournal(
    val schemaVersion: Int = 1,
    val entries: List<JournalEntry> = emptyList(),
)

enum class PublicationAction {
    CREATED,
    UPDATED,
    UNCHANGED,
    /** A closed issue is a disposition: it is left closed and unchanged. */
    CLOSED,
    /** A create may have happened; the issue has not appeared, so nothing is sent again. */
    UNRESOLVED,
    /** Would be created or updated, in report-only mode. */
    PLANNED,
}

data class Publication(
    val key: String,
    val action: PublicationAction,
    val issue: Int? = null,
    val problems: List<String> = emptyList(),
)

/**
 * Files findings as issues without duplicating them. Each issue carries its finding's key in a
 * marker; the publisher owns only the text between its markers, adds required labels without
 * removing others, and sets a milestone only where none is set, so manual edits survive. A closed
 * issue is never reopened or edited. Before sending a create, the intent is written to the
 * [journal]; a lost or failed response leaves it `POSSIBLY_CREATED`, and the key is then reconciled
 * against the all-state listing by its marker, never created again until a reviewer resolves it.
 *
 * Issues join the open [milestone] with that title. When it does not exist and there is something
 * to publish, it is created first; a closed one stops publication, since its version has shipped.
 */
class Publisher(
    private val tracker: IssueTracker,
    private val policy: PublicationPolicy,
    private val journal: Path,
    private val publish: Boolean,
    private val milestone: String? = null,
    private val polls: Int = 5,
    private val interval: Duration = Duration.ofSeconds(2),
    private val sleeper: (Duration) -> Unit = { Thread.sleep(it.toMillis()) },
) {
    fun publish(findings: List<Finding>): List<Publication> {
        require(findings.map { it.key }.distinct().size == findings.size) {
            "Duplicate finding keys"
        }
        val labels = tracker.labels().map { it.name }.toSet()
        val missing = policy.labels.values.flatten().toSet() - labels
        require(missing.isEmpty()) { "The policy's labels do not exist: ${missing.sorted()}" }
        val milestone =
            milestone?.let { title ->
                val found = tracker.milestones().filter { it.title == title }
                require(found.all { it.state == "open" }) { "The milestone $title is closed" }
                found.singleOrNull()?.number
                    ?: if (publish && findings.isNotEmpty()) tracker.createMilestone(title).number
                    else null
            }
        // An intent with no recorded outcome may have reached GitHub before a crash.
        var state =
            read().let { current ->
                current.copy(
                    entries =
                        current.entries.map {
                            if (it.state == JournalState.INTENDED)
                                it.copy(state = JournalState.POSSIBLY_CREATED)
                            else it
                        }
                )
            }
        if (publish) write(state)
        val issues = tracker.issues()
        return findings.map { finding ->
            val entry = state.entries.singleOrNull { it.key == finding.key }
            // The listing can lag a recent write, so an issue the journal knows is read directly.
            val listed = issues.filter { marker(finding.key) in it.body }
            val owned =
                if (listed.isEmpty() && entry?.issue != null)
                    listOf(tracker.issue(entry.issue)).filter { marker(finding.key) in it.body }
                else listed
            require(owned.size <= 1) {
                "${finding.key} is marked in more than one issue: ${owned.map { it.number }}"
            }
            val issue = owned.singleOrNull()
            val result =
                when {
                    issue != null -> existing(finding, issue, milestone)
                    entry?.state == JournalState.POSSIBLY_CREATED -> reconcile(finding)
                    entry?.state == JournalState.PUBLISHED ->
                        // A published key whose issue no longer carries its marker was edited by
                        // hand; filing it again would duplicate it.
                        Publication(
                            finding.key,
                            PublicationAction.UNRESOLVED,
                            entry.issue,
                            listOf("issue ${entry.issue} no longer carries the finding's marker"),
                        )
                    else -> create(finding, milestone) { state = it }
                }
            if (publish && result.issue != null && result.action != PublicationAction.UNRESOLVED) {
                state = state.with(JournalEntry(finding.key, JournalState.PUBLISHED, result.issue))
                write(state)
            }
            result
        }
    }

    private fun existing(finding: Finding, issue: GhIssue, milestone: Int?): Publication {
        if (issue.state == "closed")
            return Publication(finding.key, PublicationAction.CLOSED, issue.number)
        val body = owned(issue.body, finding)
        val labels = issue.labels + (labels(finding) - issue.labels.toSet())
        val patch =
            IssuePatch(
                body = body.takeIf { it != issue.body },
                labels = labels.takeIf { it != issue.labels },
                milestone = milestone.takeIf { issue.milestone == null && it != null },
            )
        if (patch == IssuePatch())
            return Publication(finding.key, PublicationAction.UNCHANGED, issue.number)
        if (!publish) return Publication(finding.key, PublicationAction.PLANNED, issue.number)
        val updated = tracker.update(issue.number, patch)
        return Publication(
            finding.key,
            PublicationAction.UPDATED,
            updated.number,
            validate(updated, labels, patch.milestone ?: issue.milestone),
        )
    }

    private fun create(
        finding: Finding,
        milestone: Int?,
        record: (PublicationJournal) -> Unit,
    ): Publication {
        if (!publish) return Publication(finding.key, PublicationAction.PLANNED)
        var state = read().with(JournalEntry(finding.key, JournalState.INTENDED))
        write(state)
        record(state)
        val request = IssueRequest(finding.title, section(finding), labels(finding), milestone)
        val created =
            try {
                tracker.create(request)
            } catch (failure: RuntimeException) {
                // The request may have been received even though no response arrived.
                state = state.with(JournalEntry(finding.key, JournalState.POSSIBLY_CREATED))
                write(state)
                record(state)
                return reconcile(finding, listOf("create failed: ${failure.message}"))
            }
        state = state.with(JournalEntry(finding.key, JournalState.PUBLISHED, created.number))
        write(state)
        record(state)
        val problems = validate(created, request.labels, milestone)
        // A create can succeed without the metadata the caller lacks permission to set; repair it
        // on the same issue rather than creating another.
        val repaired =
            if (problems.isEmpty()) problems
            else
                problems +
                    validate(
                            tracker.update(
                                created.number,
                                IssuePatch(
                                    labels =
                                        created.labels + (request.labels - created.labels.toSet()),
                                    milestone = milestone,
                                ),
                            ),
                            request.labels,
                            milestone,
                        )
                        .map { "after repair: $it" }
        return Publication(finding.key, PublicationAction.CREATED, created.number, repaired)
    }

    /** Polls the all-state listing for the finding's marker; one empty listing proves nothing. */
    private fun reconcile(finding: Finding, problems: List<String> = emptyList()): Publication {
        repeat(polls) { poll ->
            if (poll > 0) sleeper(interval)
            val found = tracker.issues().filter { marker(finding.key) in it.body }
            require(found.size <= 1) { "${finding.key} is marked in more than one issue" }
            found.singleOrNull()?.let {
                return Publication(finding.key, PublicationAction.CREATED, it.number, problems)
            }
        }
        return Publication(
            finding.key,
            PublicationAction.UNRESOLVED,
            problems = problems + "the create may have happened; resolve it before publishing again",
        )
    }

    private fun validate(issue: GhIssue, labels: List<String>, milestone: Int?): List<String> =
        listOfNotNull(
            (labels - issue.labels.toSet())
                .takeIf { it.isNotEmpty() }
                ?.let { "missing labels $it" },
            "milestone is ${issue.milestone}, not $milestone"
                .takeIf { milestone != null && issue.milestone != milestone },
        )

    private fun labels(finding: Finding) = policy.labels[finding.type].orEmpty()

    private fun section(finding: Finding) = "${marker(finding.key)}\n${finding.body}\n$END"

    /** [body] with the publisher's section replaced, and everything around it kept. */
    private fun owned(body: String, finding: Finding): String {
        val start = body.indexOf(marker(finding.key))
        val end = body.indexOf(END, start)
        return if (start < 0 || end < 0) body
        else body.substring(0, start) + section(finding) + body.substring(end + END.length)
    }

    private fun read(): PublicationJournal =
        if (Files.exists(journal)) AuditJson.publicationJournal(Files.readString(journal))
        else PublicationJournal()

    /** Replaces the journal atomically, so a crash leaves the old state or the new one. */
    private fun write(state: PublicationJournal) {
        val temporary = journal.resolveSibling("${journal.fileName}.tmp")
        Files.writeString(temporary, AuditJson.write(state))
        Files.move(
            temporary,
            journal,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    private fun PublicationJournal.with(entry: JournalEntry) =
        copy(entries = (entries.filter { it.key != entry.key } + entry).sortedBy { it.key })

    companion object {
        private const val END = "<!-- morphia-audits:end -->"

        fun marker(key: String) = "<!-- morphia-audits:finding $key -->"
    }
}
