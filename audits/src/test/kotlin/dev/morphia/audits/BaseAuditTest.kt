package dev.morphia.audits

import dev.morphia.audits.model.Results
import java.io.File
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue

open class BaseAuditTest {
    protected fun validate(results: Results) {
        assertEquals(
            results.created.size,
            0,
            "All existing operators should be represented: " +
                results.created.joinToString("\n\t", prefix = "\n\t") { it.operator }
        )
        assertEquals(
            results.noExamples.size,
            0,
            "All existing operators should have examples: " +
                results.noExamples.joinToString("\n\t", prefix = "\n\t") {
                    "${it.operator}: ${it.name}"
                }
        )
        assertEquals(
            results.noTest.size,
            0,
            "All existing operators should have test cases: " +
                results.noTest.joinToString("\n\t", prefix = "\n\t") {
                    "${it.operator}: ${it.testSource.relativeTo(File("../").absoluteFile)}"
                }
        )
        val noTags = results.noServerRelease.joinToString("\n", "\n")
        assertTrue(
            noTags.trim().isEmpty(),
            "Some operators are missing server release tags: ${noTags}"
        )
    }
}
