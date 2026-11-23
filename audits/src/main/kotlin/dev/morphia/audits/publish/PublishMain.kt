package dev.morphia.audits.publish

import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path

private const val USAGE =
    """Usage: PublishMainKt <coverage-report.json> <policy.json> <journal.json> <pom.xml> <OWNER/REPO> [--publish]
       PublishMainKt discovery <discovery-out> <committed-inventory.json> <policy.json> <journal.json> <pom.xml> <OWNER/REPO> [--publish]"""

/**
 * Files findings as issues in OWNER/REPO: a validated coverage report's, or, with `discovery`,
 * those of an `audits/discover.sh` output directory against the committed example inventory. Issues
 * join the milestone of the release `pom.xml`'s version is building, created when missing. Without
 * `--publish` it only reports what it would do and never spawns a mutating command.
 * `AUDITS_GITHUB_TOKEN`, when set, is the token `gh` uses (CI maps its job token to it); otherwise
 * `gh` uses its stored credentials. Prints each publication.
 */
fun main(args: Array<String>) {
    fun read(path: Path) = Files.readString(path)
    val discovery = args.firstOrNull() == "discovery"
    val positional = if (discovery) 7 else 5
    require(args.size == positional || args.size == positional + 1 && args.last() == "--publish") {
        USAGE
    }
    val publish = args.size == positional + 1
    val (owner, repository) =
        args[positional - 1]
            .split('/')
            .also { require(it.size == 2) { USAGE } }
            .let { it[0] to it[1] }
    val findings =
        if (discovery) {
            val out = Path.of(args[1])
            val optional = { name: String ->
                out.resolve(name).takeIf { Files.exists(it) }?.let(::read)
            }
            DiscoveryFindings.findings(
                AuditJson.exampleInventory(read(Path.of(args[2]))),
                AuditJson.exampleInventory(read(out.resolve("documented-examples.json"))),
                AuditJson.generationGaps(read(out.resolve("candidate/gaps.json"))).gaps,
                optional("candidate/attestation.json")?.let(AuditJson::attestation),
                optional("candidate/integration.log"),
            )
        } else Publisher.findings(AuditJson.report(read(Path.of(args[0]))))
    val (policy, journal) = if (discovery) args[3] to args[4] else args[1] to args[2]
    val gateway =
        GhGateway(
            GhTarget(owner, repository),
            if (publish) GhMode.PUBLISH else GhMode.REPORT_ONLY,
            token = System.getenv("AUDITS_GITHUB_TOKEN"),
        )
    gateway.checkVersion()
    val publications =
        Publisher(
                gateway,
                AuditJson.publicationPolicy(read(Path.of(policy))),
                Path.of(journal),
                publish,
                ProjectMilestone.of(Path.of(args[positional - 2])),
            )
            .publish(findings)
    println(AuditJson.write(publications))
}
