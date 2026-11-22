package dev.morphia.audits.publish

import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path

private const val USAGE =
    "Usage: PublishMainKt <coverage-report.json> <policy.json> <journal.json> <OWNER/REPO> [--publish]"

/**
 * Files a validated coverage report's findings as issues in OWNER/REPO. Without `--publish` it only
 * reports what it would do and never spawns a mutating command. `AUDITS_GITHUB_TOKEN`, when set, is
 * the token `gh` uses; otherwise `gh` uses its stored credentials. Prints each publication.
 */
fun main(args: Array<String>) {
    require(args.size == 4 || args.size == 5 && args[4] == "--publish") { USAGE }
    fun read(path: String) = Files.readString(Path.of(path))
    val (owner, repository) =
        args[3].split('/').also { require(it.size == 2) { USAGE } }.let { it[0] to it[1] }
    val publish = args.size == 5
    val gateway =
        GhGateway(
            GhTarget(owner, repository),
            if (publish) GhMode.PUBLISH else GhMode.REPORT_ONLY,
            token = System.getenv("AUDITS_GITHUB_TOKEN"),
        )
    gateway.checkVersion()
    val publications =
        Publisher(gateway, AuditJson.publicationPolicy(read(args[1])), Path.of(args[2]), publish)
            .publish(Publisher.findings(AuditJson.report(read(args[0]))))
    println(AuditJson.write(publications))
}
