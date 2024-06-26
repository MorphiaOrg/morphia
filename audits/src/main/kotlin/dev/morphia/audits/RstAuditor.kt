package dev.morphia.audits

import dev.morphia.audits.OperationAudit.Companion.DOC_ROOT
import dev.morphia.audits.OperationAudit.Companion.findMethods
import dev.morphia.audits.model.Operator
import dev.morphia.audits.model.OperatorType
import dev.morphia.audits.model.OperatorType.EXPRESSION
import dev.morphia.audits.model.OperatorType.FILTER
import dev.morphia.audits.model.OperatorType.STAGE
import dev.morphia.audits.model.OperatorType.UPDATE
import dev.morphia.audits.model.Results
import dev.morphia.audits.model.titleCase
import dev.morphia.audits.rst.OperatorExample
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.TreeMap
import kotlin.collections.Map.Entry
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.Visibility.PUBLIC
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.jboss.forge.roaster.model.source.ParameterSource

class RstAuditor(val type: OperatorType) {
    companion object {
        val gitRoot by lazy {
            var root = File(".").canonicalFile
            while (!File(root, ".git").exists()) root = root.parentFile
            root
        }
        val auditRoot by lazy { File(gitRoot, "audits/target/mongodb-docs") }
        val coreTestRoot = File(gitRoot, "core/src/test/resources").absoluteFile
        val coreTestSourceRoot = File(gitRoot, "core/src/test/java").absoluteFile
        val includesRoot = File(auditRoot, "source")
    }

    var operatorRoot = File(auditRoot, "source/reference/operator/${type.docsRoot()}")

    fun audit(): Results {
        val methods = findMethods(type.taglet())
        val operators = emitExampleData(type)
        val created = updateGH(operators)
        val missingServerRelease = auditServerReleaseTags(operators, methods)

        val noExamples =
            operators.filter { it.type == type }.flatMap { it.examples }.filter { it.created }

        val noTest =
            operators
                .filter { it.examples.isNotEmpty() }
                .filter { it.type == type }
                .filter { !it.testCaseExists }
                .filter { !File(it.resourceFolder, "ignored").exists() }

        operators.forEach { update(it) }
        emitDocs(TreeMap(methods), type)
        return Results(created, noExamples, noTest, missingServerRelease)
    }

    private fun emitExampleData(type: OperatorType): List<Operator> {
        val operators =
            operatorRoot
                .walk()
                .toList()
                .filter { it.isFile }
                .filter { !it.equals(operatorRoot) }
                .map { Operator(type, it) }
                .filter { it.type == type }
                .filter { !it.ignored() }

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
        val tag =
            if (type.root() == "aggregation") {
                "aggregation"
            } else {
                "query"
            }
        return GithubProject.updateGH("${tag} operator", notImplemented, listOf("enhancement", tag))
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
                [%header,cols="1,2,3"]
                |===
                |Operator|Docs|Test Examples
                
                
            """
                .trimIndent()
        )

        methods.forEach {
            val operator = it.key
            val referenceLink =
                "http://docs.mongodb.org/manual/reference/operator/${type.root()}/${operator.substringAfter("$")}"
            docRoot.appendText("| $referenceLink[${operator}]\n")
            docRoot.appendText(docsLinks(it.value, type) + "\n")
            docRoot.appendText("| ${githubLink(operator, type)}\n\n\n")
        }
        docRoot.appendText("|===\n")
    }

    private fun githubLink(operator: String, type: OperatorType): String {
        val name = operator.substringAfter("$").titleCase()
        val ref = "blob/master"
        val location =
            when (type) {
                EXPRESSION -> "aggregation/expressions"
                FILTER -> "query/filters"
                STAGE -> "aggregation/stages"
                UPDATE -> "query/updates"
            }
        val testFileName = "dev/morphia/test/$location/Test${name}"
        return "https://github.com/MorphiaOrg/morphia/$ref/core/src/test/java/$testFileName.java[Test${name}]"
    }

    private fun docsLinks(methods: List<MethodSource<*>>, type: OperatorType): String {
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
            line = "a$line" + lines.joinToString(" +\n")
        }

        return line
    }

    private fun update(operator: Operator) {
        if (operator.examples.isEmpty()) {
            return
        }
        val outputFile: File = operator.testSource
        val source =
            if (!outputFile.exists()) {
                Roaster.create(JavaClassSource::class.java).apply {
                    name = "Test${operator.name.titleCase()}"
                    `package` =
                        when (operator.type) {
                            EXPRESSION -> "dev.morphia.test.aggregation.expressions"
                            FILTER -> "dev.morphia.test.query.filters"
                            STAGE -> "dev.morphia.test.aggregation.stages"
                            UPDATE -> "dev.morphia.test.query.updates"
                        }
                    superType = "dev.morphia.test.TemplatedTestBase"
                }
            } else {
                try {
                    Roaster.parse(outputFile) as JavaClassSource
                } catch (e: Exception) {
                    throw IllegalStateException("Can't parse $outputFile", e)
                }
            }
        source.addTestCases(operator)

        if (!outputFile.parentFile.mkdirs() && !outputFile.parentFile.exists()) {
            throw IOException(
                String.format("Could not create directory: %s", outputFile.parentFile)
            )
        }
        FileWriter(outputFile).use { writer -> writer.write(source.toString()) }
    }

    private fun JavaClassSource.addTestCases(operator: Operator) {
        operator.examples
            .filter { it.actionBlock != null }
            .forEach { example ->
                val testCaseName = "testExample${example.ordinal + 1}"

                if (methods.none { it.name == testCaseName }) {
                    createTestCase(testCaseName, example)
                } else {
                    methods
                        .filter { it.name == testCaseName }
                        .forEach { method ->
                            updateJavadoc(method, example)
                            updateTestAnnotation(method, example)
                        }
                }
            }
    }

    private fun updateJavadoc(method: MethodSource<JavaClassSource>, example: OperatorExample) {
        var text = method.javaDoc.text
        if (!text.startsWith("test data")) {
            text = "test data: ${example.folder.relativeTo(coreTestRoot)}\n" + text
            method.javaDoc.text = text
        }
    }

    private fun updateTestAnnotation(
        method: MethodSource<JavaClassSource>,
        example: OperatorExample,
    ) {
        val annotation = method.getAnnotation("org.testng.annotations.Test")
        if (annotation.getStringValue("testName") == null) {
            annotation.setStringValue("testName", example.name)
        }
    }

    private fun JavaClassSource.createTestCase(testCaseName: String, example: OperatorExample) {
        val method = addMethod().setName(testCaseName).setVisibility(PUBLIC)

        val text = "test data: ${example.folder.relativeTo(coreTestRoot)}\n\n"
        method.javaDoc.text = text + example.actionBlock?.lines?.joinToString("\n")

        method.addAnnotation("org.testng.annotations.Test").setStringValue("testName", example.name)

        if (!example.folder.path.contains("aggregation")) {
            method.setBody(
                """
                        |testQuery((query) -> query.filter(  ));
                        | """
                    .trimMargin()
            )
        } else {
            method.setBody(
                """
                    |testPipeline(aggregation -> aggregation
                    |   .pipeline(
                    |
                    |)); """
                    .trimMargin()
            )
        }
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

fun notControl(it: String): Boolean {
    return !(it.trim().startsWith(":") || it.trim().startsWith(".. "))
}

fun String.findIndent() = length - trimIndent().length
