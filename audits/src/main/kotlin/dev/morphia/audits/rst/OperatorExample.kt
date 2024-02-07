package dev.morphia.audits.rst

import dev.morphia.audits.model.CodeBlock
import dev.morphia.audits.model.CodeBlock.Companion.findBlocks
import java.io.File

class OperatorExample(
    val operator: String,
    val name: String,
    private val input: List<String>,
) {
    lateinit var folder: File
    var created = false
    var actionBlock: CodeBlock? = null
    var dataBlock: CodeBlock? = null
    var expectedBlock: CodeBlock? = null
    var indexBlock: CodeBlock? = null

    init {
        findBlocks(input).forEachIndexed { index, codeBlock ->
            when {
                codeBlock.isData() && dataBlock == null -> dataBlock = codeBlock
                codeBlock.isAction() && actionBlock == null -> actionBlock = codeBlock
                codeBlock.isIndex() && indexBlock == null -> indexBlock = codeBlock
                index == 0 && codeBlock.isExpected() -> dataBlock = codeBlock
                index != 0 && codeBlock.isExpected() && expectedBlock == null ->
                    expectedBlock = codeBlock
            }
        }
    }

    fun output(folder: File) {
        this.folder = folder
        val lock = File(folder, "lock")
        if (!lock.exists() || System.getProperty("IGNORE_LOCKS") != null) {
            try {
                created = this.folder.mkdirs()
                dataBlock?.output(File(folder, "data.json"))
                indexBlock?.output(File(folder, "index.json"))
                actionBlock?.output(File(folder, "pipeline.json"), false)
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
        "OperatorExample(name='$name', actionBlock: ${actionBlock != null}, dataBlock: ${dataBlock != null}, " +
            "expectedBlock=${expectedBlock != null}, " +
            "indexBlock: ${indexBlock != null})"

    fun valid(): Boolean {
        return dataBlock != null && actionBlock != null && expectedBlock != null
    }
}
