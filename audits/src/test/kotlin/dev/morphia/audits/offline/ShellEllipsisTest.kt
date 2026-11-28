package dev.morphia.audits.offline

import dev.morphia.audits.extraction.BlockRole
import dev.morphia.audits.extraction.Documented
import dev.morphia.audits.extraction.DocumentedPage
import dev.morphia.audits.extraction.FormSource
import dev.morphia.audits.extraction.ScenarioInput
import dev.morphia.audits.extraction.ScenarioWriter
import dev.morphia.audits.extraction.SourceBlock
import dev.morphia.audits.shell.*
import org.bson.BsonDocument
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShellEllipsisTest {
    /** The expectation a find's printed [results] make, which must read. */
    private fun expectation(results: String): ProgramExpectation {
        val result =
            ShellPrograms.normalize(
                ShellProgramRequest(
                    dialect = ShellDialect.MONGOSH,
                    blocks =
                        listOf(
                            ProgramBlock("action", "db.items.find({});", ProgramRole.ACTION),
                            ProgramBlock(
                                "result",
                                results,
                                ProgramRole.EXPECTED,
                                ProgramFormat.SHELL_RESULTS,
                            ),
                        ),
                )
            )
        assertEquals(ShellLiteralState.NORMALIZED, result.state, "$results: ${result.problems}")
        return result.expectations.single()
    }

    @Test
    fun theDocsEllipsesElide() {
        for (source in
            listOf(
                // A bare ellipsis the parser rejects: an omitted value, items or results.
                "[ { _id: ..., title: 'Baseball' } ]",
                "[ { matchedCount: ..., modifiedCount: 1 } ]",
                "[ { a: 1 },\n  ...\n]",
                "[ { a: 1 } ]\n...\n",
                "// a caption\r[ { v: ... } ]",
                "// a caption\u2028[ { v: ... } ]",
                "[ { _id: ObjectId('...') } ]",
                // Text the parser reads as any value, or as truncated.
                "[ { matchedCount: '...' } ]",
                "[ { imdb: { '...': '...' } } ]",
                "[ { plot: \"It begins with a sci-fi motif ...\" } ]",
                "[ { plot: 'cut short... ' } ]",
                "[ { plot: 'cut...\\n' } ]",
                "[ { plot: ('cut...') } ]",
                "[ { plot: \"cut\\u002e\\u002e\\u002e\" } ]",
                "[ { tags: [ 'a', 'b...' ] } ]",
            )) {
            val expectation = expectation(source)
            assertTrue(expectation.elided, source)
            assertNull(expectation.published, source)
        }
    }

    @Test
    fun ellipsesTheParserReadsAsValuesDoNot() {
        for (source in
            listOf(
                "[ { title: '...And God Created Woman' } ]",
                "[ { title: 'He said \\'...\\' then left' } ]",
                "[ { a: 1 } ] // more follow...",
                "[ { a: 1 } ] /* ... */",
                "[ { a: 1.5 } ]",
                "[ { v: BSONRegExp('a...', 'i') } ]",
                "[ { v: Decimal128('1'), w: BSONRegExp(\"x...\", \"\") } ]",
            )) {
            val expectation = expectation(source)
            assertFalse(expectation.elided, source)
            assertNotNull(expectation.published, source)
        }
    }

    @Test
    fun theScanOnlyReadsBlocksTheParserRejects() {
        // A regular-expression literal holding a quote hides what follows: the block stays
        // unreadable rather than eliding, so nothing passes silently.
        assertFalse(ShellEllipsis.scan("[ { re: /'/, v: ... } ]"))
        assertTrue(ShellEllipsis.scan("[ { v: ... } ]"))
        assertFalse(ShellEllipsis.scan("[ { v: BSONRegExp('a...') } ]"))
        assertTrue(ShellEllipsis.scan("[ { v: ObjectId('...') } ]"))
    }

    @Test
    fun anElidedResultIsAnExpectationWithNothingToCheck() {
        val result =
            ShellPrograms.normalize(
                ShellProgramRequest(
                    dialect = ShellDialect.MONGOSH,
                    blocks =
                        listOf(
                            ProgramBlock("action", "db.items.find({});", ProgramRole.ACTION),
                            ProgramBlock(
                                "result",
                                "[ { _id: ..., a: 1 } ]\n...\n",
                                ProgramRole.EXPECTED,
                                ProgramFormat.SHELL_RESULTS,
                            ),
                        ),
                )
            )
        assertEquals(ShellLiteralState.NORMALIZED, result.state, result.problems.toString())
        val expectation = result.expectations.single()
        assertTrue(expectation.elided)
        assertNull(expectation.published)
        assertNull(expectation.documents)
        assertNull(expectation.publishedProfile)
    }

    @Test
    fun anElidedPageDocumentsItsCommandAndNoPrintedResults() {
        fun block(role: BlockRole, text: String) =
            SourceBlock(role, text, Documented.sha256(text.toByteArray()))
        val documented =
            Documented.documented(
                "elided",
                null,
                listOf(
                    block(BlockRole.DATA, "db.items.insertMany([ { a: 1 }, { a: 2 } ])"),
                    block(BlockRole.ACTION, "db.items.find({ a: { \$gt: 0 } })"),
                    block(BlockRole.EXPECTED, "[ { _id: ..., a: 1 },\n  ...\n]"),
                ),
            )
        assertTrue(documented.elided)
        assertNull(documented.printed)
        assertEquals("items", documented.collection)
        // Its scenario checks Morphia's command and behavior, and skips the published check.
        val page =
            DocumentedPage(
                "query/filters",
                "gt",
                FormSource(
                    "a".repeat(40),
                    "content/manual/manual/source/reference/operator/query/gt.txt",
                ),
                "b".repeat(40),
                emptyList(),
                emptyList(),
            )
        val scenario =
            BsonDocument.parse(
                ScenarioWriter.render(ScenarioInput(page, "elided", "Elided", documented))
            )
        assertEquals(
            ScenarioWriter.ELIDED,
            scenario.getDocument("expected").getString("none").value,
        )
        assertEquals(setOf("published"), scenario.getDocument("skipped").keys)
    }
}
