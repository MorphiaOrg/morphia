package dev.morphia.audits.naming

import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/** Reads accepted registry and explicit request; emits a candidate without changing either. */
fun main(args: Array<String>) {
    val inputs =
        try {
            require(args.size == 2) { "Usage: NamesMainKt <accepted-registry.json> <request.json>" }
            fun read(path: String): String =
                Files.readAllBytes(Path.of(path)).decodeToString(throwOnInvalidSequence = true)
            AuditJson.names(read(args[0])) to AuditJson.namingRequest(read(args[1]))
        } catch (error: Exception) {
            val invalid =
                NameProposal(
                    state = NameProposalState.INVALID,
                    acceptedSha256 = null,
                    requestSha256 = null,
                    registry = ExampleNameRegistry(entries = emptyList()),
                    resolutions = emptyList(),
                    problems =
                        listOf(
                            NamingProblem(
                                "INVALID_INPUT",
                                null,
                                null,
                                error.message ?: error.javaClass.simpleName,
                            )
                        ),
                )
            System.out.write(AuditJson.write(invalid).toByteArray(Charsets.UTF_8))
            System.out.flush()
            exitProcess(2)
        }
    val proposal = NameReconciliation.propose(inputs.first, inputs.second)
    System.out.write(AuditJson.write(proposal).toByteArray(Charsets.UTF_8))
    System.out.flush()
    when (proposal.state) {
        NameProposalState.READY -> Unit
        NameProposalState.UNRESOLVED -> exitProcess(3)
        NameProposalState.INVALID -> exitProcess(2)
    }
}
