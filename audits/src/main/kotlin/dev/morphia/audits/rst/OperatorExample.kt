package dev.morphia.audits.rst

import dev.morphia.audits.model.CodeBlock
import dev.morphia.audits.model.CodeBlock.Companion.findBlocks
import dev.morphia.audits.model.CodeBlock.Type.DATA
import dev.morphia.audits.model.Operator
import java.io.File

class OperatorExample(
    parent: OperatorExample?,
    val operator: Operator,
    val name: String,
    input: List<String>,
    var ordinal: Int,
) {
    val folder by lazy { File(operator.resourceFolder, "example${ordinal + 1}") }
    var created = false
    var actionBlock: CodeBlock? = null
    var dataBlock = mutableListOf<CodeBlock>()
    var expectedBlock: CodeBlock? = null
    var indexBlock: CodeBlock? = null

    init {
        val blocks = findBlocks(input)
        indexBlock = blocks.removeWhile { it.isIndex() }.firstOrNull()
        dataBlock += blocks.removeWhile { !it.isAction() && !it.isIndex() }
        dataBlock.forEachIndexed { index, block ->
            block.type = DATA
            if (index != 0) {
                block.supplemental = index
            }
        }
        actionBlock = blocks.firstOrNull { it.isAction() }
        expectedBlock = blocks.firstOrNull { it.isExpected() }

        if (dataBlock.isEmpty()) {
            dataBlock += (parent?.dataBlock ?: emptyList())
        }
        if (indexBlock == null) {
            indexBlock = parent?.indexBlock
        }
    }

    fun output(folder: File) {
        created = folder.mkdirs()
        val lock = File(folder, "lock")
        if (!lock.exists() || System.getProperty("IGNORE_LOCKS") != null) {
            try {
                dataBlock.forEach { it.output(File(folder, "data.json")) }
                indexBlock?.output(File(folder, "index.json"))
                actionBlock?.output(File(folder, "action.json"))
                expectedBlock?.output(File(folder, "expected.json"))
                if (this.folder.exists()) {
                    File(folder, "name").writeText(name)
                }
            } catch (e: Exception) {
                throw RuntimeException("Failed to extract example to ${folder}", e)
            }
        } else {
            if (lock.readText().isBlank()) {
                throw RuntimeException("$lock has no message explaining the need for a lock")
            }
        }
    }

    override fun toString() =
        "OperatorExample(name='$name', actionBlock: ${actionBlock != null}, dataBlock: ${dataBlock.isNotEmpty()}, " +
            "expectedBlock=${expectedBlock != null}, " +
            "indexBlock: ${indexBlock != null})"
}
