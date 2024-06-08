package dev.morphia.audits.model

import dev.morphia.audits.RstAuditor
import dev.morphia.audits.model.OperatorType.EXPRESSION
import dev.morphia.audits.model.OperatorType.FILTER
import dev.morphia.audits.model.OperatorType.STAGE
import dev.morphia.audits.rst.OperatorExample
import dev.morphia.audits.rst.RstDocument
import java.io.File

class Operator(var type: OperatorType, var source: File) {
    var versionAdded: String?
    var name = source.nameWithoutExtension
    var resourceFolder: File
    var testSource: File
    val implemented: Boolean
    val testCaseExists: Boolean
    val operator = "\$${name.substringBefore("-")}"

    //    val type: OperatorType
    val url: String = "https://www.mongodb.com/docs/manual/reference/operator/${type.root()}/$name/"
    val examples: List<OperatorExample>

    init {
        if (type == STAGE || type == EXPRESSION) {
            type = if (source.readText().contains(".. pipeline:: \$")) STAGE else EXPRESSION
        }
        versionAdded =
            source
                .readLines()
                .filter { it.contains(".. versionadded:: ") }
                .firstOrNull()
                ?.substringAfterLast(":")
                ?.trim()

        resourceFolder =
            File(
                    RstAuditor.coreTestRoot,
                    "dev/morphia/test/${type.root()}/${subpath()}/${name.substringBefore("-")}"
                )
                .canonicalFile
        testSource =
            File(
                    RstAuditor.coreTestSourceRoot,
                    "dev/morphia/test/${type.root()}/${subpath()}/Test${name.substringBefore("-").titleCase()}.java"
                )
                .canonicalFile

        implemented = resourceFolder.exists()
        testCaseExists = testSource.exists()
        examples = RstDocument.read(operator, source).examples
    }

    fun ignored() = File(resourceFolder, "ignored").exists()

    fun output() {
        if (!ignored()) {
            examples
                .filter { it.actionBlock?.isAction() == true }
                .forEachIndexed { index, it ->
                    it.output(File(resourceFolder, "example${index + 1}"))
                }
        }
    }

    private fun subpath() =
        when (type) {
            EXPRESSION -> "expressions"
            STAGE -> "stages"
            FILTER -> "filters"
            else -> ""
        }

    override fun toString(): String {
        return "Operator($name -> ${source.relativeTo(RstAuditor.auditRoot)})"
    }
}

private fun String.titleCase(): String {
    return first().uppercase() + substring(1)
}
