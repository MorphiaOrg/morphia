package dev.morphia.audits.generate

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.repairs.CaseManifest
import dev.morphia.audits.repairs.CaseManifests
import dev.morphia.audits.repairs.FixtureMigration
import dev.morphia.audits.repairs.FixtureMigrationLedger
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

enum class CandidateAction {
    /** The candidate writes this file. */
    WRITE,
    /** The candidate removes this file, which the current manifest generated and no longer does. */
    DELETE,
    /** A file the namespace owns but generation never writes; its hash must not change. */
    PROTECT,
}

data class CandidateFile(val path: String, val sha256: String, val action: CandidateAction)

/**
 * A regenerated namespace, described without touching the checkout: the base it was generated from
 * (revision and manifest digest), the generator and schema versions, and the SHA-256 of every file
 * it writes, deletes or protects. Its written files sit beside it under `files/`.
 */
data class Candidate(
    val schemaVersion: Int = 1,
    val baseRevision: String,
    val manifestSha256: String,
    val generator: String,
    val scenarioSchema: Int,
    val files: List<CandidateFile>,
)

/** What an integration ran: the candidate it binds, its base, and each mapper's outcome. */
data class Attestation(
    val schemaVersion: Int = 1,
    val candidateSha256: String,
    val baseRevision: String,
    val tests: List<String>,
    /** Each mapper's test exit status; zero passed. */
    val results: Map<String, Int>,
) {
    /** A function, not a property, so it is never written into the attestation it summarizes. */
    fun passed(): Boolean = results.isNotEmpty() && results.values.all { it == 0 }
}

/** Runs a command in a directory and returns its exit status; tests substitute their own. */
fun interface CommandRunner {
    fun run(command: List<String>, directory: Path): Int
}

/** [ProcessBuilder], appending the command's output to [log]. */
class LoggingRunner(private val log: Path) : CommandRunner {
    override fun run(command: List<String>, directory: Path): Int {
        val process =
            ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()))
                .start()
        return process.waitFor()
    }
}

/**
 * Candidate generation, integration and promotion for the generated operator-case namespace. A
 * candidate is generated into its own directory and never into the checkout; integration stages it
 * in a temporary worktree at its base and runs the owning tests under each mapper; promotion
 * applies it only to a checkout that still matches its base, in one step with rollback.
 */
object Candidates {
    /** Identifies the generator; a change to what it writes changes this. */
    const val GENERATOR = "fixture-migration/1"
    const val SCENARIO_SCHEMA = 1
    private const val LEDGER = "audits/catalog/fixture-migration.json"
    private const val DESCRIPTOR = "candidate.json"
    private const val GAPS = "gaps.json"
    val MAPPERS = listOf("critter", "reflection")

    /**
     * Generates the namespace of [repository]'s ledger, and the cases [CaseGenerator] generates
     * from its documented examples, into [out], which must be empty or new. Returns the candidate
     * and the generation's gaps, which `candidate` also writes beside it as `gaps.json`.
     */
    fun candidate(repository: Path, out: Path, revision: String): Candidate {
        val root = repository.toRealPath()
        require(!out.exists() || Files.list(out).use { it.findAny().isEmpty }) {
            "$out is not empty"
        }
        val ledger = AuditJson.fixtureMigration(Files.readString(root.resolve(LEDGER)))
        val contents = sortedMapOf<String, ByteArray>()
        ledger.migrations.forEach {
            contents[it.scenario] =
                FixtureMigration.scenario(it.override, it.ordered, it.options).toByteArray()
        }
        val current = currentManifest(root)
        val generation = generation(root, current)
        generation.files.forEach { (path, text) ->
            require(path !in contents) { "$path is both migrated and generated" }
            contents[path] = text.toByteArray()
        }
        val manifest =
            CaseManifests.capture(
                root,
                ledger,
                contents,
                generation.cases.map { it.manifest },
                generation.files.keys.toList(),
            )
        contents[CaseManifests.PATH] = AuditJson.write(manifest).toByteArray()
        val protected = manifest.files.filter { it.owner == "PROTECTED" }.map { "core/${it.path}" }
        val stale =
            current.files
                .filter { it.owner == "GENERATED" && "core/${it.path}" !in contents }
                .map { CandidateFile("core/${it.path}", it.sha256, CandidateAction.DELETE) }
        val candidate =
            Candidate(
                baseRevision = revision,
                manifestSha256 =
                    CaseManifests.sha256(Files.readAllBytes(root.resolve(CaseManifests.PATH))),
                generator = GENERATOR,
                scenarioSchema = SCENARIO_SCHEMA,
                files =
                    (contents.map { (path, bytes) ->
                            CandidateFile(path, CaseManifests.sha256(bytes), CandidateAction.WRITE)
                        } +
                            protected.map {
                                CandidateFile(
                                    it,
                                    CaseManifests.sha256(Files.readAllBytes(root.resolve(it))),
                                    CandidateAction.PROTECT,
                                )
                            } +
                            stale)
                        .sortedBy { it.path },
            )
        contents.forEach { (path, bytes) ->
            val target = out.resolve("files").resolve(path)
            Files.createDirectories(target.parent)
            Files.write(target, bytes)
        }
        Files.writeString(out.resolve(DESCRIPTOR), AuditJson.write(candidate))
        Files.writeString(
            out.resolve(GAPS),
            AuditJson.write(GenerationGaps(gaps = generation.gaps)),
        )
        return candidate
    }

    /**
     * The manifest of [root]'s namespace as it stands: the ledger's cases and the generated ones.
     */
    fun manifest(root: Path, ledger: FixtureMigrationLedger): CaseManifest {
        val generation = generation(root, currentManifest(root))
        return CaseManifests.capture(
            root,
            ledger,
            generated = generation.cases.map { it.manifest },
            generatedFiles = generation.files.keys.toList(),
        )
    }

    /**
     * The generated cases of [root]'s documented examples under its bindings and review; none when
     * the repository has no inventory yet. A file the [current] manifest protects is never written.
     */
    fun generation(root: Path, current: CaseManifest): Generation {
        val inventory = root.resolve(DocumentedExamples.PATH)
        if (!Files.exists(inventory)) return Generation(emptyMap(), emptyList(), emptyList())
        return CaseGenerator.generate(
            AuditJson.exampleInventory(Files.readString(inventory)),
            AuditJson.dslBindings(Files.readString(root.resolve(DslBindings.PATH))),
            AuditJson.generationReview(Files.readString(root.resolve(CaseGenerator.REVIEW))),
            current.files.filter { it.owner == "PROTECTED" }.map { "core/${it.path}" }.toSet(),
        )
    }

    fun read(out: Path): Candidate = AuditJson.candidate(Files.readString(out.resolve(DESCRIPTOR)))

    /**
     * The candidate's digest: its descriptor and, in path order, each written file. Fails if a
     * written file no longer has the hash the descriptor records.
     */
    fun digest(out: Path): String {
        val candidate = read(out)
        val parts = mutableListOf(Files.readAllBytes(out.resolve(DESCRIPTOR)))
        candidate.files
            .filter { it.action == CandidateAction.WRITE }
            .forEach {
                val bytes = Files.readAllBytes(out.resolve("files").resolve(it.path))
                require(CaseManifests.sha256(bytes) == it.sha256) {
                    "${it.path} changed since generation"
                }
                parts += it.path.toByteArray()
                parts += bytes
            }
        return CaseManifests.sha256(parts.fold(ByteArray(0)) { all, part -> all + part })
    }

    /**
     * Stages [out] in a temporary worktree of [repository] at the candidate's base, builds core,
     * and runs the tests that own the candidate's cases under each mapper, Critter first. It never
     * edits [repository]'s checkout.
     */
    fun integrate(repository: Path, out: Path, runner: CommandRunner): Attestation {
        val root = repository.toRealPath()
        val candidate = read(out)
        val digest = digest(out)
        val worktree = Files.createTempDirectory("audits-candidate")
        Files.delete(worktree)
        check(
            runner.run(
                listOf(
                    "git",
                    "worktree",
                    "add",
                    "--detach",
                    worktree.toString(),
                    candidate.baseRevision,
                ),
                root,
            ) == 0
        ) {
            "Cannot create a worktree at ${candidate.baseRevision}"
        }
        try {
            stage(worktree, out, candidate)
            val manifest =
                AuditJson.caseManifest(
                    Files.readString(out.resolve("files").resolve(CaseManifests.PATH))
                )
            val tests = manifest.cases.map { it.test.substringBefore('#') }.distinct().sorted()
            val build =
                runner.run(
                    listOf(
                        "./mvnw",
                        "-q",
                        "install",
                        "-pl",
                        ":morphia-core",
                        "-DskipTests",
                        "-Ddeploy.skip=true",
                        "-Dinvoker.skip=true",
                    ),
                    worktree,
                )
            val results =
                MAPPERS.associateWith { mapper ->
                    if (build != 0) build
                    else
                        runner.run(
                            listOf(
                                "./mvnw",
                                "test",
                                "-pl",
                                ":morphia-core",
                                "-Ddeploy.skip=true",
                                "-Dmorphia.mapper=$mapper",
                                "-Dtest=${(tests + "GeneratedCasesTest").joinToString(",")}",
                                "-Dsurefire.failIfNoSpecifiedTests=false",
                            ),
                            worktree,
                        )
                }
            return Attestation(
                candidateSha256 = digest,
                baseRevision = candidate.baseRevision,
                tests = tests,
                results = results,
            )
        } finally {
            runner.run(listOf("git", "worktree", "remove", "--force", worktree.toString()), root)
        }
    }

    /**
     * Applies [out] to [repository] when [attestation] passed for exactly this candidate and the
     * checkout still matches its base: the revision, the manifest digest and every protected file,
     * uncommitted edits included. A stale file is deleted only while it still has the hash the
     * previous manifest recorded, and a written path must be one the previous manifest generated or
     * a new one. Every change is applied together; on any failure the originals are restored.
     */
    fun promote(repository: Path, out: Path, attestation: Attestation, revision: String) {
        val root = repository.toRealPath()
        val candidate = read(out)
        val problems = mutableListOf<String>()
        if (!attestation.passed())
            problems += "the attestation did not pass: ${attestation.results}"
        if (attestation.candidateSha256 != digest(out))
            problems += "the attestation is for another candidate"
        if (attestation.baseRevision != candidate.baseRevision)
            problems += "the attestation ran on another base"
        if (revision != candidate.baseRevision)
            problems += "the checkout is at $revision, not ${candidate.baseRevision}"
        val manifest = Files.readAllBytes(root.resolve(CaseManifests.PATH))
        if (CaseManifests.sha256(manifest) != candidate.manifestSha256)
            problems += "the manifest changed since generation"
        val generated =
            currentManifest(root)
                .files
                .filter { it.owner == "GENERATED" }
                .associate { "core/${it.path}" to it.sha256 }
        candidate.files.forEach { file ->
            val path = root.resolve(file.path)
            val hash = if (path.exists()) CaseManifests.sha256(Files.readAllBytes(path)) else null
            when (file.action) {
                CandidateAction.PROTECT ->
                    if (hash != file.sha256) problems += "${file.path}: a protected file changed"
                CandidateAction.DELETE ->
                    if (hash != file.sha256 || generated[file.path] != file.sha256)
                        problems += "${file.path}: not the stale file the manifest generated"
                CandidateAction.WRITE ->
                    if (
                        hash != null &&
                            file.path != CaseManifests.PATH &&
                            generated[file.path] != hash
                    )
                        problems +=
                            "${file.path}: exists and is not a generated file; it is a conflict, not something to overwrite"
            }
        }
        require(problems.isEmpty()) { "Refusing to promote:\n" + problems.joinToString("\n") }
        val backups = mutableMapOf<Path, ByteArray?>()
        try {
            candidate.files
                .filter { it.action != CandidateAction.PROTECT }
                .forEach { file ->
                    val path = root.resolve(file.path)
                    backups[path] = if (path.exists()) Files.readAllBytes(path) else null
                    when (file.action) {
                        CandidateAction.WRITE -> {
                            Files.createDirectories(path.parent)
                            Files.write(
                                path,
                                Files.readAllBytes(out.resolve("files").resolve(file.path)),
                            )
                        }
                        else -> Files.delete(path)
                    }
                }
        } catch (failure: Exception) {
            backups.forEach { (path, bytes) ->
                if (bytes == null) Files.deleteIfExists(path) else Files.write(path, bytes)
            }
            throw failure
        }
    }

    private fun stage(worktree: Path, out: Path, candidate: Candidate) {
        candidate.files.forEach { file ->
            val path = worktree.resolve(file.path)
            when (file.action) {
                CandidateAction.WRITE -> {
                    Files.createDirectories(path.parent)
                    Files.copy(
                        out.resolve("files").resolve(file.path),
                        path,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    )
                }
                CandidateAction.DELETE -> Files.deleteIfExists(path)
                CandidateAction.PROTECT -> Unit
            }
        }
    }

    private fun currentManifest(root: Path) =
        AuditJson.caseManifest(Files.readString(root.resolve(CaseManifests.PATH)))

    /** The checkout's revision, from `git`. */
    fun revision(repository: Path): String {
        val process =
            ProcessBuilder("git", "rev-parse", "HEAD").directory(repository.toFile()).start()
        val revision = process.inputStream.bufferedReader().readText().trim()
        check(process.waitFor() == 0 && revision.matches(Regex("[0-9a-f]{40}"))) {
            "Cannot read the revision of $repository"
        }
        return revision
    }
}
