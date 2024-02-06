package dev.morphia.audits.rst

import org.testng.Assert

data class ExampleValidator(
    val data: Boolean = true,
    val action: Boolean = true,
    val expected: Boolean = true,
    val index: Boolean = false
) {
    constructor(global: Boolean) : this(global, global, global, global)

    fun action(example: OperatorExample) {
        if (action) {
            Assert.assertNotNull(
                example.actionBlock,
                "Should have an action block for ${example.name}"
            )
        } else {
            Assert.assertNull(
                example.actionBlock,
                "Should not have an action block for ${example.name}"
            )
        }
    }

    fun data(example: OperatorExample) {
        if (data) {
            Assert.assertNotNull(example.dataBlock, "Should have a data block for ${example.name}")
        } else {
            Assert.assertNull(example.dataBlock, "Should not have a data block for ${example.name}")
        }
    }

    fun expected(example: OperatorExample) {
        if (expected) {
            Assert.assertNotNull(
                example.expectedBlock,
                "Should have an expected block for ${example.name}"
            )
        } else {
            Assert.assertNull(
                example.expectedBlock,
                "Should not have an expected block for ${example.name}"
            )
        }
    }

    fun index(example: OperatorExample) {
        if (index) {
            Assert.assertNotNull(
                example.indexBlock,
                "Should have a index block for ${example.name}"
            )
        } else {
            Assert.assertNull(
                example.indexBlock,
                "Should not have a index block for ${example.name}"
            )
        }
    }

    fun name(example: OperatorExample, name: String) {
        Assert.assertEquals(example.name, name, "Should have the correct name")
    }
}
