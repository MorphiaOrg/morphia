package dev.morphia.audits.scenarios

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.documents.*
import dev.morphia.audits.sources.UnresolvedSource
import dev.morphia.audits.sources.VerifiedSourceBundle
import java.util.Locale

/** Source failures retain their diagnostic; only verified reference/foreign owners are isolated. */
internal object ScenarioSourceHealth {
    private data class Owner(val node: DocumentNode, val foreign: Boolean)

    fun inspect(
        bundle: VerifiedSourceBundle,
        page: DocumentPage,
        rule: ScenarioPageRule,
        diagnostics: List<DocumentDiagnostic>,
        resources: Set<String>,
        visit: () -> Unit,
    ): List<ScenarioSourceDiagnostic> {
        val owners = mutableMapOf<Pair<String, Int>, MutableList<Owner>>()
        val todo = ArrayDeque<Pair<DocumentNode, Boolean>>()
        listOfNotNull(page.raw, page.expanded).forEach { todo.add(it to false) }
        while (todo.isNotEmpty()) {
            visit()
            val (node, inheritedForeign) = todo.removeLast()
            val tab =
                if (
                    node.kind == "directive" &&
                        node.name == "tab" &&
                        node.domain in setOf(null, "", "mongodb", "std")
                )
                    node.options["tabid"]?.let {
                        try {
                            AuditJson.textValue(it)
                        } catch (_: Exception) {
                            null
                        }
                    }
                else null
            val foreign = inheritedForeign || tab in rule.foreignTabs
            val path = node.origin.path
            val line = node.origin.reportedLine
            if (path != null && line != null)
                owners.getOrPut(path to line) { mutableListOf() } += Owner(node, foreign)
            (node.children + node.arguments).forEach { todo.add(it to foreign) }
        }
        val cachedLines = mutableMapOf<String, List<String>>()
        fun paragraph(diagnostic: DocumentDiagnostic): String? {
            val line = diagnostic.line ?: return null
            val lines =
                cachedLines[diagnostic.path]
                    ?: run {
                        val resource =
                            try {
                                bundle.read(diagnostic.path)
                            } catch (failure: UnresolvedSource) {
                                if (failure.problem.code == "SOURCE_DRIFT") throw failure
                                return null
                            }
                        resource.bytes.decodeToString(throwOnInvalidSequence = true).lines().also {
                            cachedLines[diagnostic.path] = it
                        }
                    }
            val result = mutableListOf<String>()
            var index = line - 1
            while (index in lines.indices && lines[index].isNotBlank()) {
                visit()
                result += lines[index++]
            }
            return result.joinToString("\n")
        }
        return diagnostics
            .filter { it.path in resources }
            .map { diagnostic ->
                val located = owners[diagnostic.path to diagnostic.line].orEmpty()
                val reference =
                    diagnostic.code in setOf("TargetNotFound", "UnnamedPage") ||
                        (diagnostic.code == "CannotOpenFile" &&
                            located.any {
                                it.node.kind == "ref_role" &&
                                    it.node.domain == "std" &&
                                    it.node.name == "doc"
                            } &&
                            paragraph(diagnostic)?.let { ":doc:`" in it || ":std:doc:`" in it } ==
                                true)
                val foreign = located.isNotEmpty() && located.all { it.foreign }
                val error =
                    diagnostic.severity.uppercase(Locale.ROOT) in setOf("ERROR", "SEVERE", "FATAL")
                val reason =
                    when {
                        reference -> "CROSS_REFERENCE"
                        foreign -> "DECLARED_FOREIGN_VARIANT"
                        !error -> "NON_ERROR_DIAGNOSTIC"
                        else -> "SOURCE_GROUPING_ERROR"
                    }
                ScenarioSourceDiagnostic(diagnostic, error && !reference && !foreign, reason)
            }
    }
}
