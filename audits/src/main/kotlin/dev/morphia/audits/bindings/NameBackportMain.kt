package dev.morphia.audits.bindings

import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path

/**
 * One-time descriptive-name backport. `plan` writes the ledger only; `apply` also renames methods
 * and fixture directories, renames the policy entries and regenerates the core descriptor, then
 * verifies that every preserved fixture byte survived. Run `apply` once, on a clean tree.
 */
fun main(args: Array<String>) {
    require(args.size == 3 && args[0] in setOf("plan", "apply")) {
        "Usage: NameBackportMainKt plan|apply <repository-root> <revision>"
    }
    val root = Path.of(args[1]).toRealPath()
    fun read(path: String) =
        Files.readAllBytes(root.resolve(path)).decodeToString(throwOnInvalidSequence = true)
    val legacy = AuditJson.legacyBindings(read("audits/catalog/legacy-bindings.json"))
    val corpus = AuditJson.corpus(read("audits/catalog/legacy-corpus.json"))
    val review = AuditJson.nameBackportReview(read("audits/catalog/name-backport-review.json"))
    val ledger = NameBackport.plan(legacy, corpus, review, NameBackport.inheritedMethods(root))
    Files.writeString(root.resolve("audits/catalog/name-backport.json"), AuditJson.write(ledger))
    if (args[0] == "plan") return

    NameBackport.apply(root, ledger)
    val policyPath = "audits/catalog/binding-policy.json"
    val policy = NameBackport.project(AuditJson.bindingPolicy(read(policyPath)), ledger)
    Files.writeString(root.resolve(policyPath), AuditJson.write(policy))
    val catalog = LegacyBindings.capture(root, args[2], policy)
    Files.writeString(
        root.resolve("core/src/test/resources/dev/morphia/test/example-bindings.json"),
        AuditJson.write(catalog.descriptor()),
    )
    check(catalog == NameBackport.project(legacy, ledger, args[2])) {
        "Renamed bindings differ from the legacy import mapped through the ledger"
    }
    NameBackport.verify(root, corpus, ledger)
}
