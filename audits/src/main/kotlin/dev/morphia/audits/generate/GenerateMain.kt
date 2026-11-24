package dev.morphia.audits.generate

import dev.morphia.audits.api.MorphiaApiInventory
import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.repairs.CaseManifests
import dev.morphia.audits.sources.SourceBundle
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

private const val CAPABILITY_CATALOG = "audits/catalog/capability-catalog.json"

private const val USAGE =
    """Usage: GenerateMainKt plan <repository-root> <bundle> <content-sha256> <documents.json>
       GenerateMainKt inventory <repository-root> <bundle> <content-sha256> <documents.json>
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
 * Drafts the scenario plan of parsed operator pages (`DocumentsMainKt` over a source bundle) into
 * `audits/catalog/scenario-plan.json`, or inventories their documented examples under the committed
 * plan into `audits/catalog/documented-examples.json`. Checks the DSL bindings, then generates a
 * candidate of the operator-case namespace into `<out>`, integrates it in a temporary worktree
 * (writing `<out>/attestation.json` and `<out>/integration.log`), and promotes an attested
 * candidate into a checkout that still matches its base.
 */
fun main(args: Array<String>) {
    if (args.firstOrNull() in setOf("plan", "inventory")) {
        require(args.size == 5) { USAGE }
        val root = Path.of(args[1])
        val bundle = SourceBundle.open(Path.of(args[2]), args[3])
        val document = AuditJson.documents(Files.readString(Path.of(args[4])))
        if (args[0] == "plan") {
            val catalog = Files.readAllBytes(root.resolve(CAPABILITY_CATALOG))
            val plan = ScenarioPlans.draft(document, CaseManifests.sha256(catalog))
            Files.writeString(root.resolve(ScenarioPlans.PATH), AuditJson.write(plan))
        } else {
            val plan = AuditJson.scenarioPlan(Files.readString(root.resolve(ScenarioPlans.PATH)))
            val inventory = DocumentedExamples.capture(root, bundle, document, plan)
            Files.writeString(root.resolve(DocumentedExamples.PATH), AuditJson.write(inventory))
        }
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
