package dev.morphia.audits

import dev.morphia.audits.OperationAudit.Companion.findMethods
import dev.morphia.audits.model.Operator
import dev.morphia.audits.model.OperatorType.EXPRESSION
import dev.morphia.audits.model.OperatorType.STAGE
import dev.morphia.audits.model.Results
import java.io.File

class RstAuditor(val taglet: String) {
    companion object {
        val auditRoot = File("target/mongodb-docs")
        val coreTestRoot = File("../core/src/test/resources")
        val aggRoot = File(auditRoot, "source/reference/operator/aggregation")
    }

    val methods = findMethods(taglet)

    fun aggregations(exclusions: List<String> = listOf()): Results {
        val type =
            when (taglet) {
                "@aggregation.stage" -> STAGE
                else -> EXPRESSION
            }
        val list =
            aggRoot
                .walk()
                .filter { !it.equals(aggRoot) }
                .map { file -> Operator(file.nameWithoutExtension) }
                .toList()
        val operators = list.filter { it.type == type && it.operator !in exclusions }

        val keys = methods.keys
        val notImplemented = operators.filter { it.type == EXPRESSION && it.operator !in keys }
        val newOperators = operators.filter { it.created }
        val created =
            GithubProject.updateGH(
                if (type == EXPRESSION) "aggregation expression" else "aggregation stage",
                notImplemented,
                listOf("enhancement", "aggregation")
            )
        //            0
        val empty =
            operators
                .filter { it.examples.size == 1 }
                .flatMap { it.examples }
                .filterNot { it.folder.exists() }
        return Results(created, empty)
    }
}

fun <String> MutableList<String>.removeWhile(function: (String) -> Boolean): List<String> {
    var removed = mutableListOf<String>()
    while (function(first())) {
        removed += removeFirst()
    }

    return removed
}

fun List<String>.sections(): Map<String, MutableList<String>> {
    var sections = mutableMapOf<String, MutableList<String>>()
    var current = mutableListOf<String>()
    sections.put("main", current)
    forEach {
        if (it.startsWith("~~~")) {
            val name = current.removeLast()
            current = mutableListOf()
            sections.put(name, current)
        } else {
            current.add(it)
        }
    }

    return sections
}

fun notControl(it: String): Boolean {
    return !it.trim().startsWith(":")
}

fun findIndent(line: String) = line.length - line.trimIndent().length
