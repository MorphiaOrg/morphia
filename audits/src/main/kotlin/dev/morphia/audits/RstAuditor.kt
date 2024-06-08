package dev.morphia.audits

import dev.morphia.audits.OperationAudit.Companion.DOC_ROOT
import dev.morphia.audits.OperationAudit.Companion.findMethods
import dev.morphia.audits.model.Operator
import dev.morphia.audits.model.OperatorType
import dev.morphia.audits.model.Results
import java.io.File
import java.util.TreeMap
import kotlin.collections.Map.Entry
import org.jboss.forge.roaster.model.source.MethodSource
import org.jboss.forge.roaster.model.source.ParameterSource

object RstAuditor {
    val auditRoot by lazy {
        var gitRoot = File(".").canonicalFile
        while (!File(gitRoot, ".git").exists()) gitRoot = gitRoot.parentFile
        File(gitRoot, "audits/target/mongodb-docs")
    }
    val coreTestRoot = File("../core/src/test/resources").absoluteFile
    val coreTestSourceRoot = File("../core/src/test/java").absoluteFile
    lateinit var operatorRoot: File
    val includesRoot = File(auditRoot, "source")

    fun audit(type: OperatorType): Results {
        operatorRoot = File(auditRoot, "source/reference/operator/${type.root()}")

        val methods = findMethods(type.taglet())
        val operators = emitExampleData(type)
        val created = updateGH(operators)
        val missingServerRelease = auditServerReleaseTags(operators, methods)

        val noExamples =
            operators.filter { it.type == type }.flatMap { it.examples }.filter { it.created }

        val noTest =
            operators
                .filter { it.type == type }
                .filter { !it.testCaseExists }
                .filter { !File(it.resourceFolder, "ignored").exists() }

        emitDocs(TreeMap(methods), type)
        return Results(created, noExamples, noTest, missingServerRelease)
    }

    private fun emitExampleData(type: OperatorType): List<Operator> {
        val toList =
            operatorRoot
                .walk()
                .filter { it.isFile }
                .filter { !it.equals(operatorRoot) }
                .map { Operator(type, it) }
                .toList()
        val operators = toList.filter { it.type == type }

        operators.forEach { it.output() }
        return operators
    }

    private fun auditServerReleaseTags(
        operators: List<Operator>,
        methods: Map<String, List<MethodSource<*>>>,
    ): List<String> {
        val mapped =
            operators
                .map { it to methods[it.operator] }
                .filter { it.first.versionAdded != null && !hasReleaseTag(it) }
                .toMap()
        val missingServerRelease =
            mapped
                .map { it: Entry<Operator, List<MethodSource<*>>?> ->
                    val version = it.key.versionAdded
                    it.value?.map { method ->
                        "${method.getOrigin().getQualifiedName()}#${method.name} needs a release tag with version $version"
                    }
                }
                .flatMap { it as? List<String?> ?: emptyList() }
                .filterNotNull()
        return missingServerRelease
    }

    private fun updateGH(operators: List<Operator>): List<Operator> {
        val notImplemented = operators.filter { !it.implemented }
        return GithubProject.updateGH(
            "aggregation operator",
            notImplemented,
            listOf("enhancement", "aggregation")
        )
    }

    private fun hasReleaseTag(it: Pair<Operator, List<MethodSource<*>>?>): Boolean {
        return it.second?.all { method ->
            return method.javaDoc.tags.any() { it.name == "@mongodb.server.release" }
        } == true
    }

    private fun emitDocs(methods: Map<String, List<MethodSource<*>>>, type: OperatorType) {
        val docRoot = File(DOC_ROOT, "modules/ROOT/pages/${type.docsName()}.adoc")
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
            val referenceLink =
                "http://docs.mongodb.org/manual/reference/operator/${type.root()}/${operator.substringAfter("$")}"
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
    val name = if (anchor) type.getQualifiedName() else type.getSimpleName()
    val varargs = isVarArgs()
    val qualified = type.isQualified()
    return if (varargs) "$name${ellipsis}" else name
}

fun List<String>.sections(): Map<String, MutableList<String>> {
    val sections = mutableMapOf<String, MutableList<String>>()
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
    return !(it.trim().startsWith(":") || it.trim().startsWith(".. "))
}

fun String.findIndent() = length - trimIndent().length
