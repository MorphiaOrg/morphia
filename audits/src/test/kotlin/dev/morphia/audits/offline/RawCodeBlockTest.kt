package dev.morphia.audits.offline

import dev.morphia.audits.model.CodeBlock
import dev.morphia.audits.model.Operator
import dev.morphia.audits.model.OperatorType.FILTER
import java.nio.file.Path
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RawCodeBlockTest {
    private fun fixture(name: String): String = javaClass.getResource("/parser/$name")!!.readText()

    @Test
    fun preservesStringsCommentsAndWhitespace() {
        val blocks = CodeBlock.findBlocks(fixture("quoted-strings.rst").lines())
        assertEquals(1, blocks.size)
        assertEquals(
            fixture("quoted-strings.txt").trimEnd(),
            blocks.single().code().joinToString("\n"),
        )
    }

    @Test
    fun preservesCodeWithUnevenIndentationAndStopsAtFollowingProse() {
        val blocks = CodeBlock.findBlocks(fixture("uneven-indentation.rst").lines())
        assertEquals(1, blocks.size)
        assertEquals(listOf("   nestedCall(", "value", ")"), blocks.single().code())
    }

    @Test
    fun optionsCannotConsumeColonPrefixedCodeAfterTheBlankLine() {
        val blocks =
            CodeBlock.findBlocks(
                listOf(
                    ".. code-block:: text",
                    "   :caption: Example",
                    "",
                    "   :keepThisCode",
                    "   value",
                )
            )
        assertEquals(listOf(":keepThisCode", "value"), blocks.single().code())
    }

    @Test
    fun preservesDecimalConstructorsAndOperatorNames() {
        val blocks = CodeBlock.findBlocks(fixture("typed-values.rst").lines())
        assertEquals(
            fixture("typed-values.txt").trimEnd(),
            blocks.single().code().joinToString("\n"),
        )
    }

    @Test
    fun emptyCodeBlockHasAnExplicitDiagnostic() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                CodeBlock.findBlocks(fixture("empty-block.rst").lines())
            }
        assertTrue(error.message!!.contains("Empty code block"))
    }

    @Test
    fun externalInputIsNotSilentlyDropped() {
        val error =
            assertThrows(UnsupportedOperationException::class.java) {
                CodeBlock.findBlocks(fixture("external-io.rst").lines())
            }
        assertTrue(error.message!!.contains("/code-examples/median.js"))
    }

    @Test
    fun literalIncludeRequiresSourceResolution() {
        assertThrows(UnsupportedOperationException::class.java) {
            CodeBlock.findBlocks(listOf(".. literalinclude:: /code-examples/median.js"))
        }
    }

    @Test
    fun unresolvedIncludeCannotDisappearFromAnOperator(@TempDir root: Path) {
        val source = root.resolve("missing.txt")
        source.writeText("Examples\n--------\n\n.. include:: /includes/missing.rst\n")
        val error =
            assertThrows(UnsupportedOperationException::class.java) {
                Operator(FILTER, source.toFile()).examples
            }
        assertTrue(error.message!!.contains("/includes/missing.rst"))
        assertEquals(listOf("missing.txt"), root.toFile().list()!!.toList())
    }

    @Test
    fun readsInlineInputAndOutputSeparately() {
        val blocks = CodeBlock.findBlocks(fixture("inline-io.rst").lines())
        assertEquals(2, blocks.size)
        assertEquals("db.items.find({ n: 1 })", blocks[0].code().single())
        assertEquals("{ n: 1 }", blocks[1].code().single())
    }

    @Test
    fun readingAnOperatorDoesNotGenerateFiles(@TempDir root: Path) {
        val file = root.resolve("eq.txt")
        val input = fixture("operator.rst")
        file.writeText(input)

        val examples = Operator(FILTER, file.toFile()).examples

        assertEquals(1, examples.size)
        assertEquals("Read Documents", examples.single().name)
        assertNotNull(examples.single().actionBlock)
        assertEquals(input, file.toFile().readText())
        assertEquals(listOf("eq.txt"), root.toFile().list()!!.toList())
    }
}
