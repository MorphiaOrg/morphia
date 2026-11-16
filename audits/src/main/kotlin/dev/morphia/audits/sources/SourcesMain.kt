package dev.morphia.audits.sources

import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/** Explicit fresh outputs; ordinary Maven builds never call this command. */
fun main(args: Array<String>) {
    val report =
        try {
            require(args.size == 4 && args[2] in setOf("--repository", "--fetch-to")) {
                "Usage: SourcesMainKt <request.json> <new-bundle-dir> (--repository <existing-git-dir> | --fetch-to <new-git-dir>)"
            }
            val request =
                AuditJson.sourceRequest(
                    Files.readAllBytes(Path.of(args[0]))
                        .decodeToString(throwOnInvalidSequence = true)
                )
            val destination = Path.of(args[1]).toAbsolutePath().normalize()
            GitSnapshot.validate(request)
            require(
                !Files.exists(destination, java.nio.file.LinkOption.NOFOLLOW_LINKS) &&
                    Files.isDirectory(destination.parent)
            ) {
                "Bundle destination must be fresh with an existing parent"
            }
            val repository = Path.of(args[3])
            if (args[2] == "--fetch-to") GitAcquisition.fetch(request, repository)
            val manifest = SourceBundle.write(repository, request, destination)
            SourceAcquisitionReport(
                state =
                    if (manifest.state == SourceState.READY) AcquisitionState.READY
                    else AcquisitionState.PARTIAL,
                manifest = manifest,
                problems = manifest.problems,
            )
        } catch (failure: Exception) {
            SourceAcquisitionReport(
                state = AcquisitionState.INVALID,
                manifest = null,
                problems =
                    listOf(
                        SourceProblem(
                            "ACQUISITION_FAILED",
                            "request",
                            emptyList(),
                            failure.message ?: failure.javaClass.simpleName,
                        )
                    ),
            )
        }
    System.out.write(AuditJson.write(report).toByteArray(Charsets.UTF_8))
    System.out.flush()
    when (report.state) {
        AcquisitionState.READY -> Unit
        AcquisitionState.PARTIAL -> exitProcess(3)
        AcquisitionState.INVALID -> exitProcess(2)
    }
}
