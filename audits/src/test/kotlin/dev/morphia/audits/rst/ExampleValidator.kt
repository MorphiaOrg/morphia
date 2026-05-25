package dev.morphia.audits.rst

import org.junit.jupiter.api.Assertions

data class ExampleValidator(
    val data: Boolean = true,
    val action: Boolean = true,
    val expected: Boolean = true,
    val index: Boolean = false,
) {
    constructor(global: Boolean) : this(global, global, global, global)

    fun action(example: OperatorExample) {
        if (action) {
            Assertions.assertNotNull(
                example.actionBlock,
                "Should have an action block for ${example.name}",
            )
        } else {
            Assertions.assertNull(
                example.actionBlock,
                "Should not have an action block for ${example.name}",
            )
        }
    }

    fun data(example: OperatorExample) {
        if (data) {
            Assertions.assertNotNull(
                example.dataBlock,
                "Should have a data block for ${example.name}",
            )
        } else {
            Assertions.assertNull(
                example.dataBlock,
                "Should not have a data block for ${example.name}",
            )
        }
    }

    fun expected(example: OperatorExample) {
        if (expected) {
            Assertions.assertNotNull(
                example.expectedBlock,
                "Should have an expected block for ${example.name}",
            )
        } else {
            Assertions.assertNull(
                example.expectedBlock,
                "Should not have an expected block for ${example.name}",
            )
        }
    }

    fun index(example: OperatorExample) {
        if (index) {
            Assertions.assertNotNull(
                example.indexBlock,
                "Should have a index block for ${example.name}",
            )
        } else {
            Assertions.assertNull(
                example.indexBlock,
                "Should not have a index block for ${example.name}",
            )
        }
    }

    fun name(example: OperatorExample, name: String) {
        Assertions.assertEquals(name, example.name, "Should have the correct name")
    }
}
