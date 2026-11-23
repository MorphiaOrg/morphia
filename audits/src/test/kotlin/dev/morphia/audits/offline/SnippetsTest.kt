package dev.morphia.audits.offline

import dev.morphia.audits.repairs.FormInclude
import dev.morphia.audits.repairs.FormMigration
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Reading pages whose examples live in tested snippets and per-language content. */
class SnippetsTest {
    @TempDir lateinit var tree: Path

    private val source = "content/manual/manual/source"

    @Test
    fun onlyTheShellContentOfALanguageChoiceIsKept() {
        val page =
            """
            .. tabs-drivers::

               .. tab::
                  :tabid: shell

                  .. code-block:: javascript

                     db.c.find()

               .. tab::
                  :tabid: csharp

                  .. sharedinclude:: dbx/csharp/intro.rst

            .. composable-tutorial::

               .. selected-content::
                  :selections: csharp

                  .. sharedinclude:: dbx/csharp/method-intro.rst

               .. selected-content::
                  :selections: shell

                  Shell text.
            """
                .trimIndent()
                .lines()
        assertEquals(
            """
            .. code-block:: javascript

               db.c.find()

            .. composable-tutorial::

               Shell text.

            """
                .trimIndent()
                .lines(),
            FormMigration.shellTabs(page),
        )
    }

    @Test
    fun testedSnippetsAreSplicedInWithTheirBlobs() {
        val snippet =
            tree.resolve("content/code-examples/tested/command-line/mongosh/x/run.snippet.js")
        snippet.parent.createDirectories()
        snippet.writeText("db.c.aggregate([\n\n  { \$count: \"n\" }\n])\n")
        val include = tree.resolve("$source/includes/data.js")
        include.parent.createDirectories()
        include.writeText("db.c.insertOne({ a: 1 })\n")
        val page =
            """
            .. literalinclude:: /includes/data.js
               :language: javascript

            .. io-code-block::

               .. input:: /code-examples/tested/command-line/mongosh/x/run.snippet.js
                  :language: javascript

               .. output:: /code-examples/tested/command-line/mongosh/x/absent-output.sh
            """
                .trimIndent()
                .lines()
        val includes = mutableListOf<FormInclude>()
        assertEquals(
            """
            .. code-block::
               :language: javascript

               db.c.insertOne({ a: 1 })


            .. io-code-block::

               .. input::
                  :language: javascript

                  db.c.aggregate([

                    { ${'$'}count: "n" }
                  ])


               .. output:: /code-examples/tested/command-line/mongosh/x/absent-output.sh
            """
                .trimIndent()
                .lines(),
            FormMigration.snippets(tree, source, page, includes),
        )
        // A target outside the checkout stays for the extractor, which rejects it in an example.
        assertEquals(
            listOf(
                "$source/includes/data.js",
                "content/code-examples/tested/command-line/mongosh/x/run.snippet.js",
            ),
            includes.map { it.path },
        )
        assertEquals(40, includes.first().blob.length)
        assertEquals(
            listOf(".. input:: /code-examples/../secret.js"),
            FormMigration.snippets(
                tree,
                source,
                listOf(".. input:: /code-examples/../secret.js"),
                mutableListOf(),
            ),
        )
        Files.deleteIfExists(snippet)
    }
}
