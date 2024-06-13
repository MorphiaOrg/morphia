package dev.morphia.audits.rst

import dev.morphia.audits.model.CodeBlock
import dev.morphia.audits.model.CodeBlock.Companion.findBlocks
import java.io.File

class OperatorExample(
    val parent: OperatorExample?,
    val operator: String,
    val name: String,
    private val input: List<String>,
) {
    lateinit var folder: File
    var created = false
    var actionBlock: CodeBlock? = null
    var dataBlock = mutableListOf<CodeBlock>()
    var expectedBlock: CodeBlock? = null
    var indexBlock: CodeBlock? = null

    init {
        findBlocks(input).forEachIndexed { index, codeBlock ->
            when {
                codeBlock.isData() -> {
                    if (dataBlock.isNotEmpty()) {
                        codeBlock.supplemental = dataBlock.size + 1
                    }
                    dataBlock += codeBlock
                }
                codeBlock.isAction() && actionBlock == null -> actionBlock = codeBlock
                codeBlock.isIndex() && indexBlock == null -> indexBlock = codeBlock
                index == 0 && codeBlock.isExpected() -> dataBlock += codeBlock
                index != 0 && codeBlock.isExpected() && expectedBlock == null ->
                    expectedBlock = codeBlock
            }
        }
        dataBlock = if (dataBlock.isNotEmpty()) dataBlock else parent?.dataBlock ?: mutableListOf()
        indexBlock = indexBlock ?: parent?.indexBlock
    }

    fun output(folder: File) {
        this.folder = folder
        val lock = File(folder, "lock")
        if (!lock.exists() || System.getProperty("IGNORE_LOCKS") != null) {
            try {
                created = this.folder.mkdirs()
                dataBlock.forEach { it.output(File(folder, "data.json")) }
                indexBlock?.output(File(folder, "index.json"))
                actionBlock?.output(File(folder, "action.json"))
                expectedBlock?.output(File(folder, "expected.json"))
                if (this.folder.exists()) {
                    File(folder, "name").writeText(name)
                }
            } catch (e: Exception) {
                throw RuntimeException("Failed to extract example to ${this.folder}", e)
            }
        } else {
            if (lock.readText().isBlank()) {
                throw RuntimeException("${lock} has no message explaining the need for a lock")
            }
        }
    }

    override fun toString() =
        "OperatorExample(name='$name', actionBlock: ${actionBlock != null}, dataBlock: ${dataBlock.isNotEmpty()}, " +
            "expectedBlock=${expectedBlock != null}, " +
            "indexBlock: ${indexBlock != null})"

    fun valid(): Boolean {
        return actionBlock != null
    }
}
