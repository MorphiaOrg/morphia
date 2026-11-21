package dev.morphia.audits.offline

import dev.morphia.audits.repairs.GizaExtracts
import java.nio.file.Path
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GizaExtractsTest {
    @TempDir lateinit var includes: Path

    private fun yaml(name: String, text: String) =
        includes.resolve(name).writeText(text.trimMargin())

    @Test
    fun inheritedContentTakesTheChildsReplacements() {
        yaml(
            "extracts-parent.yaml",
            """
            |ref: _base
            |content: "{{role}} bypasses validation."
            |replacement:
            |  role: "the base"
            """,
        )
        yaml(
            "extracts-child.yaml",
            """
            |ref: findAndModify
            |inherit:
            |  ref: _base
            |  file: extracts-parent.yaml
            |replacement:
            |  role: ":dbcommand:`findAndModify`"
            |---
            |ref: base
            |inherit:
            |  ref: _base
            |  file: extracts-parent.yaml
            """,
        )
        val extract = GizaExtracts(includes).resolve("findAndModify")!!
        assertEquals(":dbcommand:`findAndModify` bypasses validation.", extract.content)
        assertEquals(
            listOf(
                includes.resolve("extracts-child.yaml"),
                includes.resolve("extracts-parent.yaml"),
            ),
            extract.files,
        )
        assertEquals(
            "the base bypasses validation.",
            GizaExtracts(includes).resolve("base")!!.content,
        )
    }

    @Test
    fun unresolvableExtractsAreLeftAlone() {
        yaml(
            "extracts-a.yaml",
            "|ref: twice\n|content: one\n|---\n|ref: open\n|content: \"{{missing}}\"",
        )
        yaml(
            "extracts-b.yaml",
            "|ref: twice\n|content: two\n|---\n|ref: orphan\n|inherit:\n|  ref: gone\n|  file: extracts-b.yaml",
        )
        yaml(
            "extracts-c.yaml",
            "|ref: titled\n|title: Heading\n|content: text\n|---\n" +
                "|ref: child\n|inherit:\n|  ref: titled\n|  file: extracts-c.yaml\n|---\n" +
                "|ref: doubled\n|content: one\n|---\n|ref: doubled\n|content: two",
        )
        // Not extracts YAML, so never read.
        yaml("steps-a.yaml", "|ref: steps\n|content: step")
        val extracts = GizaExtracts(includes)
        assertNull(extracts.resolve("twice"))
        assertNull(extracts.resolve("open"))
        assertNull(extracts.resolve("orphan"))
        assertNull(extracts.resolve("steps"))
        // Rendered fields beyond the content, inherited or not, and refs repeated in one file.
        assertNull(extracts.resolve("titled"))
        assertNull(extracts.resolve("child"))
        assertNull(extracts.resolve("doubled"))
        assertNull(GizaExtracts(includes.resolve("absent")).resolve("twice"))
    }

    @Test
    fun inheritanceCyclesAreRejected() {
        yaml(
            "extracts-cycle.yaml",
            "|ref: a\n|inherit:\n|  ref: b\n|  file: extracts-cycle.yaml\n|---\n" +
                "|ref: b\n|inherit:\n|  ref: a\n|  file: extracts-cycle.yaml",
        )
        assertThrows(IllegalArgumentException::class.java) { GizaExtracts(includes).resolve("a") }
    }
}
