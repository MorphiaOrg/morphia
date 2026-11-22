package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.generate.Attestation
import dev.morphia.audits.generate.Candidate
import dev.morphia.audits.generate.CandidateAction
import dev.morphia.audits.generate.CandidateFile
import dev.morphia.audits.generate.Candidates
import dev.morphia.audits.generate.CommandRunner
import dev.morphia.audits.repairs.CaseManifest
import dev.morphia.audits.repairs.CaseManifests
import dev.morphia.audits.repairs.OwnedFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CandidatesTest {
    @TempDir lateinit var directory: Path

    private val root =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("core/src/test/java/dev/morphia/test")) }

    private val base = "a".repeat(40)

    @Test
    fun aCandidateOfTheCommittedTreeIsDeterministicAndMatchesIt() {
        val first = directory.resolve("first")
        val second = directory.resolve("second")
        val candidate = Candidates.candidate(root, first, base)
        Candidates.candidate(root, second, base)
        assertEquals(Candidates.digest(first), Candidates.digest(second))
        // The committed tree is in sync with its ledger, so every written file equals the
        // checkout's
        // and nothing is stale; generation wrote only into its own directory.
        candidate.files.forEach {
            val committed = CaseManifests.sha256(Files.readAllBytes(root.resolve(it.path)))
            assertEquals(committed, it.sha256, it.path)
        }
        assertTrue(candidate.files.none { it.action == CandidateAction.DELETE })
        assertEquals(
            Files.readString(root.resolve(CaseManifests.PATH)),
            first.resolve("files").resolve(CaseManifests.PATH).readText(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            Candidates.candidate(root, first, base)
        }
    }

    /** A checkout with one generated file, one stale generated file and one protected file. */
    private fun checkout(): Path {
        val checkout = directory.resolve("checkout")
        fun file(path: String, text: String) =
            checkout.resolve(path).also {
                it.parent.createDirectories()
                it.writeText(text)
            }
        file("core/src/test/resources/g/kept.json", "old kept")
        file("core/src/test/resources/g/stale.json", "stale")
        file("core/src/test/java/P.java", "protected")
        val manifest =
            CaseManifest(
                cases = emptyList(),
                files =
                    listOf(
                        OwnedFile("src/test/resources/g/kept.json", sha("old kept"), "GENERATED"),
                        OwnedFile("src/test/resources/g/stale.json", sha("stale"), "GENERATED"),
                        OwnedFile("src/test/java/P.java", sha("protected"), "PROTECTED"),
                    ),
            )
        file(CaseManifests.PATH, AuditJson.write(manifest))
        return checkout
    }

    private fun sha(text: String) = CaseManifests.sha256(text.toByteArray())

    /** A candidate that rewrites kept.json, adds new.json, drops stale.json and protects P.java. */
    private fun candidate(checkout: Path, extra: List<CandidateFile> = emptyList()): Path {
        val out = directory.resolve("candidate")
        val written =
            mapOf(
                "core/src/test/resources/g/kept.json" to "new kept",
                "core/src/test/resources/g/new.json" to "added",
            )
        written.forEach { (path, text) ->
            out.resolve("files").resolve(path).also {
                it.parent.createDirectories()
                it.writeText(text)
            }
        }
        val candidate =
            Candidate(
                baseRevision = base,
                manifestSha256 =
                    CaseManifests.sha256(Files.readAllBytes(checkout.resolve(CaseManifests.PATH))),
                generator = Candidates.GENERATOR,
                scenarioSchema = Candidates.SCENARIO_SCHEMA,
                files =
                    written.map { (path, text) ->
                        CandidateFile(path, sha(text), CandidateAction.WRITE)
                    } +
                        CandidateFile(
                            "core/src/test/resources/g/stale.json",
                            sha("stale"),
                            CandidateAction.DELETE,
                        ) +
                        CandidateFile(
                            "core/src/test/java/P.java",
                            sha("protected"),
                            CandidateAction.PROTECT,
                        ) +
                        extra,
            )
        out.resolve("candidate.json").writeText(AuditJson.write(candidate))
        return out
    }

    private fun attestation(
        out: Path,
        results: Map<String, Int> = mapOf("critter" to 0, "reflection" to 0),
    ) =
        Attestation(
            candidateSha256 = Candidates.digest(out),
            baseRevision = base,
            tests = emptyList(),
            results = results,
        )

    @Test
    fun anAttestedCandidatePromotesInOneStep() {
        val checkout = checkout()
        val out = candidate(checkout)
        Candidates.promote(checkout, out, attestation(out), base)
        assertEquals("new kept", checkout.resolve("core/src/test/resources/g/kept.json").readText())
        assertEquals("added", checkout.resolve("core/src/test/resources/g/new.json").readText())
        assertFalse(checkout.resolve("core/src/test/resources/g/stale.json").exists())
        assertEquals("protected", checkout.resolve("core/src/test/java/P.java").readText())
    }

    @Test
    fun aCheckoutThatMovedOnIsRefused() {
        val checkout = checkout()
        val out = candidate(checkout)
        val good = attestation(out)
        fun refused(revision: String = base, attestation: Attestation = good) =
            assertThrows(IllegalArgumentException::class.java) {
                Candidates.promote(checkout, out, attestation, revision)
            }
        refused(revision = "b".repeat(40))
        refused(attestation = good.copy(results = mapOf("critter" to 0, "reflection" to 1)))
        refused(attestation = good.copy(candidateSha256 = "0".repeat(64)))
        // An uncommitted edit to a protected file.
        checkout.resolve("core/src/test/java/P.java").writeText("edited")
        refused()
        checkout.resolve("core/src/test/java/P.java").writeText("protected")
        // A stale file edited since the manifest recorded it is not deleted.
        checkout.resolve("core/src/test/resources/g/stale.json").writeText("edited")
        refused()
        checkout.resolve("core/src/test/resources/g/stale.json").writeText("stale")
        // A file the manifest does not own at a path the candidate writes is a conflict.
        checkout.resolve("core/src/test/resources/g/new.json").writeText("hand written")
        val failure = refused()
        assertTrue("conflict" in failure.message!!, failure.message)
        assertEquals("old kept", checkout.resolve("core/src/test/resources/g/kept.json").readText())
    }

    @Test
    fun aFailedPromotionRestoresTheOriginals() {
        val checkout = checkout()
        // The last write cannot happen: its target's parent is a file.
        checkout.resolve("core/src/test/resources/z").writeText("blocks a directory")
        val blocked = "core/src/test/resources/z/blocked.json"
        val out =
            candidate(checkout, listOf(CandidateFile(blocked, sha("x"), CandidateAction.WRITE)))
        out.resolve("files").resolve(blocked).also {
            it.parent.createDirectories()
            it.writeText("x")
        }
        assertThrows(Exception::class.java) {
            Candidates.promote(checkout, out, attestation(out), base)
        }
        assertEquals("old kept", checkout.resolve("core/src/test/resources/g/kept.json").readText())
        assertFalse(checkout.resolve("core/src/test/resources/g/new.json").exists())
        assertEquals("stale", checkout.resolve("core/src/test/resources/g/stale.json").readText())
    }

    @Test
    fun integrationRunsInATemporaryWorktreeAndAlwaysRemovesIt() {
        val out = directory.resolve("generated")
        Candidates.candidate(root, out, base)
        val commands = mutableListOf<List<String>>()
        var worktree: Path? = null
        val runner = CommandRunner { command, _ ->
            commands += command
            when {
                command.take(3) == listOf("git", "worktree", "add") -> {
                    worktree = Path.of(command[4]).createDirectories()
                    0
                }
                command.take(3) == listOf("git", "worktree", "remove") -> {
                    worktree!!.toFile().deleteRecursively()
                    0
                }
                "-Dmorphia.mapper=reflection" in command -> 1
                else -> 0
            }
        }
        val attestation = Candidates.integrate(root, out, runner)
        assertEquals(base, commands.first().last())
        assertEquals(mapOf("critter" to 0, "reflection" to 1), attestation.results)
        assertFalse(attestation.passed())
        assertEquals(attestation, AuditJson.attestation(AuditJson.write(attestation)))
        // Critter runs first, then reflection, and the worktree goes away.
        val mappers = commands.flatMap { it.filter { arg -> arg.startsWith("-Dmorphia.mapper=") } }
        assertEquals(listOf("-Dmorphia.mapper=critter", "-Dmorphia.mapper=reflection"), mappers)
        assertEquals(listOf("git", "worktree", "remove"), commands.last().take(3))
        assertFalse(worktree!!.exists())
        assertEquals(Candidates.digest(out), attestation.candidateSha256)
    }
}
