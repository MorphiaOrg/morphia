package dev.morphia.audits.model

import java.io.File

class Example(
    val operator: Operator,
    val name: String,
    codeBlocks: List<CodeBlock>,
    prior: Example? = null
) {
    var folder: File = File("/bad%path!!")
    var expectedResults: CodeBlock
    var pipeline: CodeBlock
    var inputData: CodeBlock

    init {
        var (documents, pipe) =
            codeBlocks.partition {
                it.contains("insertMany") ||
                    it.contains("insertOne") ||
                    it.startsWith("{") ||
                    it.startsWith("[")
            }
        pipeline = pipe.firstOrNull { it.contains(".aggregate(") } ?: CodeBlock()
        if (documents.size == 1) {
            if (prior == null) {
                inputData = documents.first()
                expectedResults = CodeBlock()
            } else {
                inputData = prior.inputData
                expectedResults = documents.first()
            }
        } else {
            try {
                inputData = documents.firstOrNull() ?: CodeBlock()
                expectedResults = documents.lastOrNull() ?: CodeBlock()
            } catch (e: NoSuchElementException) {
                throw IllegalStateException("$name has no documents")
            }
        }
    }

    fun isEmpty(): Boolean {
        return !inputData.hasData() && !pipeline.hasData() && !expectedResults.hasData()
    }

    fun size(): Int =
        listOf(inputData, pipeline, expectedResults).map { if (it.hasData()) 1 else 0 }.sum()

    fun output(folder: File) {
        this.folder = folder
        val lock = File(folder, "lock")
        if (!lock.exists() || System.getProperty("IGNORE_LOCKS") != null) {
            writeInputData(folder)
            writePipeline(folder)
            writeExpectedData(folder)
            if (this.folder.exists()) {
                File(folder, "name").writeText(name)
            }
        } else {
            if (lock.readText().isBlank()) {
                throw RuntimeException("${lock} has no message explaining the need for a lock")
            }
        }
    }

    fun writeInputData(folder: File) {
        if (inputData.hasData()) {
            var output = File(folder, "data.json")
            output.parentFile.mkdirs()

            output.writeText(inputData.sanitizeData().joinToString("\n"))
        }
    }

    private fun writeExpectedData(folder: File) {
        if (expectedResults.hasData()) {
            var output = File(folder, "expected.json")
            output.parentFile.mkdirs()
            output.writeText(expectedResults.sanitizeData().joinToString("\n"))
        }
    }

    private fun writePipeline(folder: File) {
        if (pipeline.hasData()) {
            var output = File(folder, "pipeline.json")
            output.parentFile.mkdirs()
            var lines = pipeline.code().toMutableList()
            val first = lines.first()
            if (first.contains("[")) {
                lines[0] = first.substringAfterLast("(")
            } else {
                lines.removeFirst()
            }
            val last = lines.removeLast()
            if (last.contains("]")) {
                lines += last.substringBefore(")")
            }
            output.writeText(lines.joinToString("\n"))
        }
    }

    override fun toString(): String {
        return "Example(operator='${operator.name}', name='$name', size='${size()}')"
    }
}
