package dev.morphia.audits.model

import dev.morphia.audits.RstAuditor
import dev.morphia.audits.findIndent
import dev.morphia.audits.model.OperatorType.EXPRESSION
import dev.morphia.audits.model.OperatorType.STAGE
import dev.morphia.audits.notControl
import dev.morphia.audits.removeWhile
import dev.morphia.audits.sections
import java.io.File

class Operator(var source: File) {
    var versionAdded: String?
    var name = source.nameWithoutExtension
    var resourceFolder: File
    val created: Boolean
    val operator = "\$${name.substringBefore("-")}"
    val type: OperatorType
    val url: String = "https://www.mongodb.com/docs/manual/reference/operator/aggregation/$name/"
    val examples: List<Example>

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
        created = resourceFolder.exists()
        var prior: Example? = null
        examples =
            extractCodeBlocks(source).map {
                val example = Example(this, it.key, it.value, prior)
                prior = example
                example
            }
    }

    fun output() {
        if (!File(resourceFolder, "ignored").exists()) {
            examples
                .filterNot { it.isEmpty() }
                .forEachIndexed { index, it ->
                    it.output(File(resourceFolder, "example${index + 1}"))
                }
        }
    }

    private fun subpath() = if (type == EXPRESSION) "expressions" else "stages"

    private fun extractCodeBlocks(file: File): Map<String, List<CodeBlock>> {
        val lines =
            file
                .readLines()
                .dropWhile { line -> line.trim() !in listOf("Example", "Examples") }
                .drop(3)
                .flatMap { line ->
                    if (line.trim().startsWith(".. include:: ")) {
                        val include = File(RstAuditor.includesRoot, line.substringAfter(":: "))
                        if (include.exists()) {
                            include.readLines()
                        } else listOf(line)
                    } else listOf(line)
                }
                .toMutableList()

        return extractCodeBlocks(lines)
    }

    private fun extractCodeBlocks(lines: MutableList<String>): Map<String, List<CodeBlock>> {
        val sections = lines.sections()
        var blocks = mutableMapOf<String, MutableList<CodeBlock>>()

        sections.forEach { name, data ->
            var current = mutableListOf<CodeBlock>()
            blocks[name] = current
            var line = ""
            while (data.isNotEmpty()) {
                line = data.removeFirst().trim()
                if (line.trim().startsWith(".. code-block:: ")) {
                    current += readBlock(data)
                }
            }
        }

        return blocks
    }

    private fun readBlock(lines: MutableList<String>): CodeBlock {
        lines.removeWhile { !notControl(it) || it.isBlank() }.toMutableList()
        var block = CodeBlock()
        block.indent = findIndent(lines.first())
        while (
            lines.isNotEmpty() &&
                (findIndent(lines.first()) >= block.indent || lines.first().isBlank())
        ) {
            block += lines.removeFirst()
        }
        return block
    }

    override fun toString(): String {
        return "Operator($name -> ${source.relativeTo(RstAuditor.auditRoot)})"
    }
}
