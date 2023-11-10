package dev.morphia.model

import dev.morphia.RstAuditor
import dev.morphia.findIndent
import dev.morphia.model.OperatorType.EXPRESSION
import dev.morphia.model.OperatorType.STAGE
import dev.morphia.notControl
import dev.morphia.removeWhile
import dev.morphia.sections
import java.io.File

class Operator(var name: String) {
    var resourceFolder: File
    var source = File(RstAuditor.aggRoot, "$name.txt")
    val created: Boolean
    val operator = "\$${name.substringBefore("-")}"
    val type: OperatorType
    val url: String = "https://www.mongodb.com/docs/manual/reference/operator/aggregation/$name/"
    val examples: List<Example>

    init {
        val rstSource = File(RstAuditor.aggRoot, "$name.txt")
        type = if (rstSource.readText().contains(".. pipeline:: \$")) STAGE else EXPRESSION
        resourceFolder =
            File(
                    RstAuditor.coreTestRoot,
                    "dev/morphia/test/aggregation/${subpath()}/${name.substringBefore("-")}"
                )
                .canonicalFile
        created = resourceFolder.exists()
        var prior: Example? = null
        examples =
            extractCodeBlocks(rstSource).map {
                val example = Example(this, it.key, it.value, prior)
                prior = example
                example
            }
        examples
            .filterNot { it.isEmpty() }
            .forEachIndexed { index, it -> it.output(File(resourceFolder, "example${index + 1}")) }
    }

    private fun subpath() = if (type == EXPRESSION) "expressions" else "stages"

    private fun extractCodeBlocks(file: File): Map<String, List<CodeBlock>> {
        var lines = file.readLines()
        lines = lines.dropWhile { line -> line.trim() !in listOf("Example", "Examples") }.drop(3)

        return extractCodeBlocks(lines.toMutableList())
    }

    private fun extractCodeBlocks(data: MutableList<String>): Map<String, List<CodeBlock>> {
        val sections = data.sections()
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
