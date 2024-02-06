package dev.morphia.audits.rst

import dev.morphia.audits.model.CodeBlock
import org.testng.Assert

data class BlockComparator(
    val data: Boolean = false,
    val action: Boolean = false,
    val expected: Boolean = false,
    val index: Boolean = false,
) {
    constructor(global: Boolean) : this(global, global, global, global)

    fun action(first: OperatorExample, second: OperatorExample) {
        isSame("action", action, first.actionBlock, second.actionBlock)
    }

    fun data(first: OperatorExample, second: OperatorExample) {
        isSame("data", data, first.dataBlock, second.dataBlock)
    }

    fun expected(first: OperatorExample, second: OperatorExample) {
        isSame("expected", expected, first.expectedBlock, second.expectedBlock)
    }

    fun index(first: OperatorExample, second: OperatorExample) {
        isSame("index", index, first.indexBlock, second.indexBlock)
    }

    private fun isSame(blockName: String, same: Boolean, first: CodeBlock?, second: CodeBlock?) {
        if (same) {
            Assert.assertSame(first, second, "Should have the same $blockName block")
        } else {
            Assert.assertNotSame(first, second, "Should have unique $blockName blocks")
        }
    }
}
