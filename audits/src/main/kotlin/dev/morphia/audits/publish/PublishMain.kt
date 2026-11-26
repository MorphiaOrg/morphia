package dev.morphia.audits.publish

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.extraction.ApiCoverage
import dev.morphia.audits.extraction.Coverage
import dev.morphia.audits.extraction.DocumentedExamples
import dev.morphia.audits.extraction.Overrides
import java.nio.file.Files
import java.nio.file.Path

private const val USAGE =
    """Usage: PublishMainKt findings <tree> <committed-inventory.json> <audit.log|-> <extraction.txt|->
       PublishMainKt publish <findings.json> <policy.json> <journal.json> <pom.xml> <OWNER/REPO> [--publish]"""

/**
 * The scheduled discovery's two halves:
 * - `findings` prints the findings of [tree], a checkout extraction has brought to the current docs
 *   commit, against the inventory the branch committed; `audit.log` is core's operator tests run in
 *   that tree, or `-` when they weren't run.
 * - `publish` files them as issues in OWNER/REPO, in the milestone of the release `pom.xml` is
 *   building. The journal names each finding's issue, so a rerun updates rather than refiles.
 *   Without `--publish` it only reports what it would do. `AUDITS_GITHUB_TOKEN`, when set, is the
 *   token `gh` uses.
 */
fun main(args: Array<String>) {
    fun read(path: Path) = Files.readString(path)
    when (args.firstOrNull()) {
        "findings" -> {
            require(args.size == 5) { USAGE }
            val tree = Path.of(args[1]).toRealPath()
            val current = AuditJson.exampleInventory(read(tree.resolve(DocumentedExamples.PATH)))
            val overrides = AuditJson.overrides(read(tree.resolve(Overrides.PATH)))
            val baseline = AuditJson.untested(read(tree.resolve(Coverage.UNTESTED)))
            val findings =
                DiscoveryFindings.findings(
                    AuditJson.exampleInventory(read(Path.of(args[2]))),
                    current,
                    ApiCoverage.missing(current, ApiCoverage.tags(tree), baseline),
                    args[3].takeIf { it != "-" }?.let { read(Path.of(it)) },
                    args[4].takeIf { it != "-" }?.let { read(Path.of(it)) },
                )
            println(
                AuditJson.write(
                    FindingsReport(
                        findings = findings,
                        missingScenarios = Coverage.missingScenarios(tree, current, overrides),
                    )
                )
            )
        }
        "publish" -> {
            require(args.size == 6 || args.size == 7 && args.last() == "--publish") { USAGE }
            val publish = args.size == 7
            val (owner, name) =
                args[5].split('/').also { require(it.size == 2) { USAGE } }.let { it[0] to it[1] }
            val findings = AuditJson.findings(read(Path.of(args[1])))
            val gateway =
                GhGateway(
                    GhTarget(owner, name),
                    if (publish) GhMode.PUBLISH else GhMode.REPORT_ONLY,
                    token = System.getenv("AUDITS_GITHUB_TOKEN"),
                )
            gateway.checkVersion()
            val publications =
                Publisher(
                        gateway,
                        AuditJson.publicationPolicy(read(Path.of(args[2]))),
                        Path.of(args[3]),
                        publish,
                        ProjectMilestone.of(Path.of(args[4])),
                    )
                    .publish(findings)
            println(AuditJson.write(publications))
            // A create that may not have happened, or an issue that differs from its finding, is
            // left for a person; the run says so instead of passing.
            val unsettled =
                publications.filter {
                    it.action == PublicationAction.UNRESOLVED || it.problems.isNotEmpty()
                }
            if (unsettled.isNotEmpty()) {
                System.err.println("Unsettled publications: ${unsettled.joinToString { it.key }}")
                kotlin.system.exitProcess(1)
            }
        }
        else -> throw IllegalArgumentException(USAGE)
    }
}
