package dev.morphia.audits

import dev.morphia.audits.model.Results
import java.io.File
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

open class BaseAuditTest {
    protected fun validate(results: Results) {
        Assertions.assertEquals(
            0,
            results.created.size,
            "All existing operators should be represented: " +
                results.created.joinToString("\n\t", prefix = "\n\t") { it.operator },
        )
        Assertions.assertEquals(
            0,
            results.noExamples.size,
            "All existing operators should have examples: " +
                results.noExamples.joinToString("\n\t", prefix = "\n\t") {
                    "${it.operator.name}: ${it.name}"
                },
        )
        Assertions.assertEquals(
            0,
            results.noTest.size,
            "All existing operators should have test cases: " +
                results.noTest.joinToString("\n\t", prefix = "\n\t") {
                    "${it.operator}: ${it.testSource.relativeTo(File("../").absoluteFile)}"
                },
        )
        val noTags = results.noServerRelease.joinToString("\n", "\n")
        Assertions.assertTrue(
            noTags.trim().isEmpty(),
            "Some operators are missing server release tags: ${noTags}",
        )
    }
}
