package dev.morphia.audits.model

import dev.morphia.audits.RstAuditor
import dev.morphia.audits.model.OperatorType.EXPRESSION
import dev.morphia.audits.model.OperatorType.FILTER
import dev.morphia.audits.model.OperatorType.STAGE
import dev.morphia.audits.rst.RstDocument
import java.io.File

class Operator private constructor(var type: OperatorType) {
    val versionAdded by lazy {
        source
            .readLines()
            .filter { it.contains(".. versionadded:: ") }
            .firstOrNull()
            ?.substringAfterLast(":")
            ?.trim()
    }
    val resourceFolder by lazy {
        File(
                RstAuditor.coreTestRoot,
                "dev/morphia/test/${type.root()}/${subpath()}/${name.substringBefore("-")}"
            )
            .canonicalFile
    }
    val testSource by lazy {
        File(
                RstAuditor.coreTestSourceRoot,
                "dev/morphia/test/${type.root()}/${subpath()}/Test${name.substringBefore("-").titleCase()}.java"
            )
            .canonicalFile
    }
    val implemented by lazy { resourceFolder.exists() }
    val testCaseExists by lazy { testSource.exists() }
    val rstAuditor = RstAuditor(type)
    val operator by lazy { "\$${name.substringBefore("-")}" }
    val url: String by lazy {
        "https://www.mongodb.com/docs/manual/reference/operator/${type.root()}/$name/"
    }
    val examples by lazy { RstDocument.read(operator, source).examples }
    lateinit var source: File
    lateinit var name: String

    constructor(type: OperatorType, file: File) : this(type) {
        source = file
        name = file.nameWithoutExtension
        updateType()
    }

    constructor(type: OperatorType, name: String) : this(type) {
        this.name = name
        source = File("${rstAuditor.operatorRoot}/$name.txt")
        updateType()
    }

    private fun updateType() {
        if (type == STAGE || type == EXPRESSION) {
            type = if (source.readText().contains(".. pipeline:: \$")) STAGE else EXPRESSION
        }
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
