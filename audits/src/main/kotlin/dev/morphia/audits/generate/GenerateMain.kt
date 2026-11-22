package dev.morphia.audits.generate

import dev.morphia.audits.api.MorphiaApiInventory
import dev.morphia.audits.coverage.AuditJson
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

private const val USAGE =
    """Usage: GenerateMainKt inventory <repository-root> <pages> <current.json>
       GenerateMainKt candidate <repository-root> <out> <core-classpath-file>
       GenerateMainKt integrate <repository-root> <out>
       GenerateMainKt promote <repository-root> <out>"""

/**
 * Checks `audits/catalog/dsl-bindings.json` against core's current source, captured with the
 * dependency [classpath] (`./mvnw dependency:build-classpath -pl :morphia-core`) and core's built
 * classes for its generated sources. A stale binding stops generation.
 */
private fun verifyBindings(root: Path, revision: String, classpath: Path) {
    val entries =
        Files.readString(classpath).trim().split(File.pathSeparatorChar).filter { it.isNotEmpty() }
    val api =
        MorphiaApiInventory.capture(
            root,
            revision,
            listOf("core/src/main/java"),
            entries.map { Path.of(it) } + listOf(root.resolve("core/target/classes")),
        )
    val problems =
        DslBindings.verify(
            AuditJson.dslBindings(Files.readString(root.resolve(DslBindings.PATH))),
            api,
        )
    require(problems.isEmpty()) { "Stale bindings:\n" + problems.joinToString("\n") }
}

/**
 * Inventories the documented examples of fetched pages (`fetch.py current`, then `fetch.py pages`)
 * into `audits/catalog/documented-examples.json`. Checks the DSL bindings, then generates a
 * candidate of the operator-case namespace into `<out>`, integrates it in a temporary worktree
 * (writing `<out>/attestation.json` and `<out>/integration.log`), and promotes an attested
 * candidate into a checkout that still matches its base.
 */
fun main(args: Array<String>) {
    if (args.firstOrNull() == "inventory") {
        require(args.size == 4) { USAGE }
        val root = Path.of(args[1])
        val inventory =
            DocumentedExamples.capture(
                root,
                Path.of(args[2]),
                AuditJson.currentPages(Files.readString(Path.of(args[3]))),
            )
        Files.writeString(root.resolve(DocumentedExamples.PATH), AuditJson.write(inventory))
        return
    }
    require(args.size == if (args.firstOrNull() == "candidate") 4 else 3) { USAGE }
    val root = Path.of(args[1])
    val out = Path.of(args[2])
    when (args[0]) {
        "candidate" -> {
            val revision = Candidates.revision(root)
            verifyBindings(root, revision, Path.of(args[3]))
            Candidates.candidate(root, out, revision)
        }
        "integrate" -> {
            val attestation =
                Candidates.integrate(root, out, LoggingRunner(out.resolve("integration.log")))
            Files.writeString(out.resolve("attestation.json"), AuditJson.write(attestation))
            require(attestation.passed()) { "Integration failed: ${attestation.results}" }
        }
        "promote" ->
            Candidates.promote(
                root,
                out,
                AuditJson.attestation(Files.readString(out.resolve("attestation.json"))),
                Candidates.revision(root),
            )
        else -> throw IllegalArgumentException(USAGE)
    }
}
