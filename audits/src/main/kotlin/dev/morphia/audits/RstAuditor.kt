package dev.morphia.audits

import dev.morphia.audits.OperationAudit.Companion.DOC_ROOT
import dev.morphia.audits.OperationAudit.Companion.findMethods
import dev.morphia.audits.model.Operator
import dev.morphia.audits.model.OperatorType
import dev.morphia.audits.model.OperatorType.EXPRESSION
import dev.morphia.audits.model.OperatorType.STAGE
import dev.morphia.audits.model.Results
import java.io.File
import java.util.TreeMap
import kotlin.collections.Map.Entry
import org.jboss.forge.roaster.model.source.MethodSource
import org.jboss.forge.roaster.model.source.ParameterSource

object RstAuditor {
    val auditRoot = File("target/mongodb-docs")
    val coreTestRoot = File("../core/src/test/resources")
    val aggRoot = File(auditRoot, "source/reference/operator/aggregation")
    val includesRoot = File(auditRoot, "source")

    fun aggregations(type: OperatorType): Results {
        val methods =
            when (type) {
                EXPRESSION -> findMethods("@aggregation.expression")
                STAGE -> findMethods("@aggregation.stage")
            }
        val operators =
            aggRoot
                .walk()
                .filter { it.isFile }
                .filter { !it.equals(aggRoot) }
                .map { Operator(it) }
                .filter { it.type == type }
                .toList()

        val keys = methods.keys
        val notImplemented = operators.filter { it.operator !in keys }
        val created =
            GithubProject.updateGH(
                "aggregation operator",
                notImplemented,
                listOf("enhancement", "aggregation")
            )
        var mapped =
            operators
                .map { it to methods[it.operator] }
                .filter { it.first.versionAdded != null && !hasReleaseTag(it) }
                .toMap()

        var missingServerRelease =
            mapped
                .map { it: Entry<Operator, List<MethodSource<*>>?> ->
                    val version = it.key.versionAdded
                    it.value?.map { method ->
                        "${method.getOrigin().getQualifiedName()}#${method.name} needs a release tag with version $version"
                    }
                }
                .flatMap { it as List<String?> }
                .filterNotNull()
        operators.forEach { it.output() }
        val empty =
            operators
                .filter { it.examples.size == 1 }
                .flatMap { it.examples }
                .filterNot { it.folder.exists() }

        emitDocs(TreeMap(methods), type)
        return Results(
            created,
            empty,
            if (type == EXPRESSION) missingServerRelease else emptyList()
        )
    }

    private fun hasReleaseTag(it: Pair<Operator, List<MethodSource<*>>?>): Boolean {
        return it.second?.all { method ->
            val tag =
                method.javaDoc.tags
                    .filter { it.name == "@mongodb.server.release" }
                    .map { it.value }
                    .firstOrNull()

            return tag == it.first.versionAdded
        } == true
    }

    private fun emitDocs(methods: Map<String, List<MethodSource<*>>>, type: OperatorType) {
        var docRoot = File(DOC_ROOT, "modules/ROOT/pages/${type.docsName()}.adoc")
        docRoot.writeText(
            """
                [%header,cols="1,2"]
                |===
                |Operator|Docs
                
                
            """
                .trimIndent()
        )

        methods.forEach {
            val methods = it.value
            val docs = docsLinks(methods)
            val operator = it.key
            var referenceLink =
                "http://docs.mongodb.org/manual/reference/operator/aggregation/${operator.substringAfter("$")}"
            docRoot.appendText("| $referenceLink[${operator}]\n")
            docRoot.appendText(docs + "\n")
        }
        docRoot.appendText("|===\n")
    }

    private fun docsLinks(methods: List<MethodSource<*>>): String {
        val lines =
            methods.map { method ->
                val className = method.getOrigin().getName()
                val packageName = method.getOrigin().getPackage().replace('.', '/')
                val anchor =
                    method.getName() +
                        method.getParameters().joinToString(",", "(", ")") { it.anchorLink() }
                val signature =
                    method.getName() +
                        method.getParameters().joinToString(",", "(", ")") { it.anchorLink(false) }
                "link:javadoc/${packageName}/${className}.html#$anchor[${className}#$signature]"
            }

        var line = "| "
        if (lines.size == 1) {
            line += lines[0]
        } else {
            line = "a$line" + lines.joinToString("\n * ", "\n\n * ", "\n")
        }

        return line + "\n"
    }
}

private fun ParameterSource<*>.anchorLink(anchor: Boolean = true): String {
    val ellipsis = if (anchor) "%2E%2E%2E" else "..."
    val type = getType()
    var name = if (anchor) type.getQualifiedName() else type.getSimpleName()
    val varargs = isVarArgs()
    val qualified = type.isQualified()
    return if (varargs) "$name${ellipsis}" else name
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
