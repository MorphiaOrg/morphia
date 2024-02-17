package dev.morphia.audits.model

import dev.morphia.audits.RstAuditor
import dev.morphia.audits.model.OperatorType.EXPRESSION
import dev.morphia.audits.model.OperatorType.STAGE
import dev.morphia.audits.rst.OperatorExample
import dev.morphia.audits.rst.RstDocument
import java.io.File

class Operator(var source: File) {
    var versionAdded: String?
    var name = source.nameWithoutExtension
    var resourceFolder: File
    val implemented: Boolean
    val operator = "\$${name.substringBefore("-")}"
    val type: OperatorType
    val url: String = "https://www.mongodb.com/docs/manual/reference/operator/aggregation/$name/"
    val examples: List<OperatorExample>

    init {
        type = if (source.readText().contains(".. pipeline:: \$")) STAGE else EXPRESSION
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
                    "dev/morphia/test/aggregation/${subpath()}/${name.substringBefore("-")}"
                )
                .canonicalFile
        implemented = resourceFolder.exists()
        examples = RstDocument.read(operator, source).examples
    }

    fun ignored() = File(resourceFolder, "ignored").exists()

    fun output() {
        if (!ignored()) {
            examples
                .filter { it.actionBlock?.isPipeline() == true }
                .forEachIndexed { index, it ->
                    it.output(File(resourceFolder, "example${index + 1}"))
                }
        }
    }

    private fun subpath() = if (type == EXPRESSION) "expressions" else "stages"

    override fun toString(): String {
        return "Operator($name -> ${source.relativeTo(RstAuditor.auditRoot)})"
    }
}
