package dev.morphia.audits.discovery

import dev.morphia.audits.coverage.ContractValidation
import dev.morphia.audits.documents.DocumentDiagnostic
import dev.morphia.audits.documents.DocumentPage
import dev.morphia.audits.sources.UnresolvedSource
import dev.morphia.audits.sources.VerifiedSourceBundle
import java.nio.charset.CharacterCodingException
import java.nio.file.Path

/** Includes that fail before yielding nodes still belong to the page that attempted them. */
internal class DiscoveryDependencies(
    private val bundle: VerifiedSourceBundle,
    private val prefix: String,
    private val facts: Map<String, List<LocatedNode>>,
    pages: List<DocumentPage>,
) {
    val resources =
        facts.mapValues { (_, nodes) ->
            nodes
                .flatMap { listOfNotNull(it.node.origin.path) + it.node.origin.includeChain }
                .toMutableSet()
        }
    private val literals = facts.mapValues { mutableSetOf<String>() }
    private val literalDirective =
        Regex("^\\s*\\.\\. (?:[A-Za-z-]+:)?(?:literalinclude|input|output)::\\s+(.+?)\\s*$")

    init {
        facts.forEach { (page, nodes) ->
            nodes.forEach { located ->
                val node = located.node
                if (
                    node.kind == "directive" &&
                        node.name in setOf("include", "literalinclude", "input", "output")
                ) {
                    val owner = node.origin.includeChain.lastOrNull() ?: node.origin.path
                    val argument = node.arguments.joinToString("") { DocumentFacts.text(it) }.trim()
                    if (owner != null && argument.isNotBlank())
                        paths(owner, argument).forEach { path ->
                            resources.getValue(page) += path
                            if (node.name != "include") literals.getValue(page) += path
                        }
                }
            }
        }
        pages
            .filter { it.id in resources }
            .forEach { page ->
                page.dependencies.forEach { dependency ->
                    dependency.origin.path?.let { resources.getValue(page.id) += it }
                }
            }
    }

    fun users(diagnostic: DocumentDiagnostic): Set<String> =
        resources.filterValues { diagnostic.path in it }.keys

    fun rawParagraph(diagnostic: DocumentDiagnostic): String? {
        val number = diagnostic.line ?: return null
        val source =
            try {
                bundle.read(diagnostic.path)
            } catch (failure: UnresolvedSource) {
                if (failure.problem.code == "SOURCE_DRIFT") throw failure
                return null
            }
        return try {
            source.bytes
                .decodeToString(throwOnInvalidSequence = true)
                .lineSequence()
                .drop(number - 1)
                .takeWhile { it.isNotBlank() }
                .joinToString("\n")
        } catch (_: CharacterCodingException) {
            null
        }
    }

    fun rawLine(diagnostic: DocumentDiagnostic): String? {
        val number = diagnostic.line ?: return null
        val source =
            try {
                bundle.read(diagnostic.path)
            } catch (failure: UnresolvedSource) {
                if (failure.problem.code == "SOURCE_DRIFT") throw failure
                return null
            }
        return try {
            source.bytes
                .decodeToString(throwOnInvalidSequence = true)
                .lineSequence()
                .drop(number - 1)
                .firstOrNull()
        } catch (_: CharacterCodingException) {
            null
        }
    }

    fun literal(diagnostic: DocumentDiagnostic, users: Set<String>): Boolean {
        val number = diagnostic.line ?: return false
        val source =
            try {
                bundle.read(diagnostic.path)
            } catch (failure: UnresolvedSource) {
                if (failure.problem.code == "SOURCE_DRIFT") throw failure
                return false
            }
        val line =
            try {
                source.bytes
                    .decodeToString(throwOnInvalidSequence = true)
                    .lineSequence()
                    .drop(number - 1)
                    .firstOrNull()
            } catch (_: CharacterCodingException) {
                return false
            }
        val argument =
            line?.let { literalDirective.matchEntire(it)?.groupValues?.get(1) } ?: return false
        users.forEach { page ->
            val owners =
                facts
                    .getValue(page)
                    .filter { it.node.origin.path == diagnostic.path }
                    .mapNotNull { it.node.origin.includeChain.lastOrNull() }
                    .ifEmpty { listOf(diagnostic.path) }
            owners.forEach { owner -> literals.getValue(page) += paths(owner, argument) }
        }
        return true
    }

    fun onlyFailedLiterals(page: DocumentPage): Boolean {
        val failed = page.dependencies.filter { it.origin.path == null || it.parserDigest == null }
        return failed.isNotEmpty() &&
            failed.all { dependency ->
                val requested = normalize(prefix + dependency.requestedPath)
                dependency.origin.path in literals.getValue(page.id) ||
                    requested in literals.getValue(page.id)
            }
    }

    private fun normalize(value: String): String? =
        Path.of(value)
            .normalize()
            .joinToString("/") { it.toString() }
            .takeIf { ContractValidation.relative(it) }

    private fun paths(owner: String, argument: String): Set<String> {
        if (argument.contains("://")) return emptySet()
        // This mirrors Snooty FileId's lexical RST-reference normalization; Git link targets
        // remain governed by VerifiedSourceBundle and are never rewritten here.
        val candidate =
            if (argument.startsWith('/')) prefix + argument.trimStart('/')
            else (Path.of(owner).parent ?: Path.of("")).resolve(argument).toString()
        val requested = normalize(candidate) ?: return emptySet()
        val resolved =
            try {
                bundle.read(requested).canonicalPath
            } catch (failure: UnresolvedSource) {
                if (failure.problem.code == "SOURCE_DRIFT") throw failure
                requested
            }
        return setOf(requested, resolved)
    }
}
