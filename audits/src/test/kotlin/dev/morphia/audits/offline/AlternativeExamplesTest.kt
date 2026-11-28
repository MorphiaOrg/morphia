package dev.morphia.audits.offline

import dev.morphia.audits.extraction.BlockRole
import dev.morphia.audits.extraction.Documented
import dev.morphia.audits.extraction.DocumentedExamples
import dev.morphia.audits.extraction.DocumentedExamples.Owned
import dev.morphia.audits.extraction.DocumentedExamples.Unrunnable
import dev.morphia.audits.extraction.ExampleStatus
import dev.morphia.audits.extraction.ReviewedExample
import dev.morphia.audits.extraction.SourceBlock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AlternativeExamplesTest {
    private val page = "aggregation/expressions/cond"
    private val objectForm =
        "db.movies.aggregate([ { \$project: { p: { \$cond: { if: true, then: 1, else: 2 } } } } ])"
    private val arrayForm =
        "db.movies.aggregate([ { \$project: { p: { \$cond: [ true, 1, 2 ] } } } ])"
    private val printed = "[ { _id: 1, p: 1 } ]"

    private fun block(role: BlockRole, text: String) =
        SourceBlock(role, text, Documented.sha256(text.toByteArray()))

    private fun named(name: String) =
        ReviewedExample(
            page,
            "main",
            block(BlockRole.ACTION, objectForm).sha256,
            name = name,
            reason = "r",
        )

    private val alternative =
        ReviewedExample(
            page,
            "main",
            block(BlockRole.ACTION, arrayForm).sha256,
            alternativeOf = block(BlockRole.ACTION, objectForm).sha256,
            reason = "the same command in the array syntax",
        )

    @Test
    fun anAlternativeFoldsIntoTheExampleItSpells() {
        val used = mutableSetOf<ReviewedExample>()
        val examples =
            DocumentedExamples.examples(
                listOf(
                    "main" to
                        Unrunnable(
                            listOf(
                                block(BlockRole.ACTION, objectForm),
                                block(BlockRole.EXPECTED, printed),
                            ),
                            null,
                        ),
                    "main" to Unrunnable(listOf(block(BlockRole.ACTION, arrayForm)), null),
                ),
                // The legacy scenario kept the generic title; the reviewed name wins over it.
                listOf(Owned("$page/condExpressionOperator", "main")),
                listOf(named("Price by Rating"), alternative),
                used,
            )
        val example = examples.single()
        assertEquals(ExampleStatus.UNOWNED, example.status, example.detail)
        assertEquals("priceByRating", example.stem)
        assertTrue(example.alternatives)
        assertEquals(listOf(BlockRole.ACTION, BlockRole.EXPECTED), example.blocks.map { it.role })
        // Both spellings are the action, in page order, and the digest covers them.
        val action = example.blocks.first()
        assertEquals("$objectForm\n$arrayForm", action.text)
        assertEquals(Documented.sha256(action.text.toByteArray()), action.sha256)
        assertEquals(2, used.size)
    }

    @Test
    fun anAlternativeWithResultsOfItsOwnStaysAnExample() {
        val used = mutableSetOf<ReviewedExample>()
        val examples =
            DocumentedExamples.examples(
                listOf(
                    "main" to Unrunnable(listOf(block(BlockRole.ACTION, objectForm)), null),
                    "main" to
                        Unrunnable(
                            listOf(
                                block(BlockRole.ACTION, arrayForm),
                                block(BlockRole.EXPECTED, printed),
                            ),
                            null,
                        ),
                ),
                emptyList(),
                listOf(named("Price by Rating"), alternative),
                used,
            )
        // Folding it would drop what it prints, so it stays for review, and its review is stale.
        assertEquals(2, examples.size)
        assertFalse(examples.any { it.alternatives })
        assertEquals(ExampleStatus.UNNAMED, examples[1].status)
        assertEquals(setOf(named("Price by Rating")), used)
    }

    @Test
    fun anAlternativeOfNothingOnThePageStaysAnExample() {
        val used = mutableSetOf<ReviewedExample>()
        val examples =
            DocumentedExamples.examples(
                listOf("main" to Unrunnable(listOf(block(BlockRole.ACTION, arrayForm)), null)),
                emptyList(),
                listOf(alternative),
                used,
            )
        assertEquals(ExampleStatus.UNNAMED, examples.single().status)
        assertTrue(used.isEmpty())
    }
}
