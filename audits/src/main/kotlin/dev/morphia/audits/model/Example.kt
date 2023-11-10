package dev.morphia.audits.model

import java.io.File

class Example(
    val operator: Operator,
    val name: String,
    codeBlocks: List<CodeBlock>,
    val prior: Example? = null
) {
    var folder: File = File("/bad%path!!")
    var feeder = false
    val expectedResults: CodeBlock
    val pipeline: CodeBlock
    var inputData: CodeBlock

    init {
        inputData =
            codeBlocks.firstOrNull { it.contains("insertMany") || it.contains("insertOne") }
                ?: CodeBlock()
        if (!inputData.hasData() && prior != null) {
            prior.feeder = true
            inputData = prior.inputData
        }
        pipeline = codeBlocks.firstOrNull { it.contains(".aggregate(") } ?: CodeBlock()
        expectedResults =
            codeBlocks.firstOrNull { it.startsWith("{") || it.startsWith("[") } ?: CodeBlock()
    }

    fun isEmpty(): Boolean {
        return !inputData.hasData() && !pipeline.hasData() && !expectedResults.hasData()
    }

    fun size(): Int =
        listOf(inputData, pipeline, expectedResults).map { if (it.hasData()) 1 else 0 }.sum()

    fun output(folder: File) {
        this.folder = folder
        writeInputData(folder)
        writePipeline(folder)
        writeExpectedData(folder)
        if (this.folder.exists()) {
            File(folder, "name").writeText(name)
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
