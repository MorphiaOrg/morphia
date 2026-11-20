package dev.morphia.audits.rst

import dev.morphia.audits.model.CodeBlock
import dev.morphia.audits.model.CodeBlock.Companion.findBlocks
import dev.morphia.audits.model.CodeBlock.Type.DATA
import dev.morphia.audits.model.Operator

/** Raw legacy parsing result. This model cannot generate fixtures or edit test sources. */
class OperatorExample(
    parent: OperatorExample?,
    val operator: Operator,
    val name: String,
    input: List<String>,
) {
    val actionBlock: CodeBlock?
    val dataBlock: List<CodeBlock>
    val expectedBlock: CodeBlock?
    val indexBlock: CodeBlock?

    init {
        val blocks = findBlocks(input)
        indexBlock = blocks.removeWhile { it.isIndex() }.firstOrNull() ?: parent?.indexBlock
        val data = blocks.removeWhile { !it.isAction() && !it.isIndex() }
        data.forEach { it.type = DATA }
        dataBlock = data.ifEmpty { parent?.dataBlock ?: emptyList() }
        actionBlock = blocks.firstOrNull { it.isAction() }
        expectedBlock = blocks.firstOrNull { it.isExpected() }
    }
}
