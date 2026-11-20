package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.repairs.FormDeclaration
import dev.morphia.audits.repairs.FormGap
import dev.morphia.audits.repairs.FormMigration
import dev.morphia.audits.repairs.FormMigration.Candidate
import dev.morphia.audits.repairs.FormRule
import dev.morphia.audits.repairs.FormSource
import dev.morphia.audits.repairs.FormUnresolved
import dev.morphia.audits.repairs.RepairDisposition
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FormMigrationTest {
    @TempDir lateinit var repository: Path
    @TempDir lateinit var pages: Path

    private val origin = "0".repeat(40)
    private val commit = "a".repeat(40)
    private val newer = "b".repeat(40)
    private val page = "source/reference/operator/aggregation/cond.txt"
    private val fixture = "aggregation/expressions/cond/useCond"
    private val lockText = "morphia generates the array form\n"

    private fun page(revision: String, text: String) =
        pages.resolve("$revision/$page").also {
            it.parent.createDirectories()
            it.writeText(text)
        }

    private fun include(revision: String, path: String, text: String) =
        pages.resolve("$revision/source$path").also {
            it.parent.createDirectories()
            it.writeText(text)
        }

    private fun fixture(name: String, file: String, text: String) =
        repository
            .resolve(
                "core/src/test/resources/dev/morphia/test/aggregation/expressions/cond/$name/$file"
            )
            .also {
                it.parent.createDirectories()
                it.writeText(text)
            }

    private val documented =
        """
        Examples
        --------

        .. include:: /includes/sample-inventory.rst

        Use Cond
        ~~~~~~~~

        .. code-block:: javascript

           db.inventory.aggregate( [
              { ${'$'}project: { item: 1, discount: { ${'$'}cond: { if: { ${'$'}gte: [ "${'$'}qty", 250 ] }, then: 30, else: 20 } } } }
           ] )

        The operation returns:

        .. code-block:: javascript

           { "_id" : 1, "item" : "abc1", "discount" : 30 }
           { "_id" : 2, "item" : "abc2", "discount" : 20 }
        """
            .trimIndent()

    private val inventory =
        """
        .. code-block:: javascript

           db.inventory.insertMany( [
              { "_id" : 1, "item" : "abc1", "qty" : 300 },
              { "_id" : 2, "item" : "abc2", "qty" : 200 }
           ] )
        """
            .trimIndent()

    private fun curated(
        action: String =
            """[ { ${'$'}project: { item: 1, discount: { ${'$'}cond: [ { ${'$'}gte: [ "${'$'}qty", 250 ] }, 30, 20 ] } } } ]"""
    ) {
        fixture("useCond", "name", "Use Cond\n")
        fixture("useCond", "lock", lockText)
        fixture("useCond", "action.json", action)
        fixture(
            "useCond",
            "data.json",
            """
            |{ "_id" : 1, "item" : "abc1", "qty" : 300 }
            |{ "_id" : 2, "item" : "abc2", "qty" : 200 }
            """
                .trimMargin(),
        )
        fixture(
            "useCond",
            "expected.json",
            """
            |{ "_id" : 1, "item" : "abc1", "discount" : 30 }
            |{ "_id" : 2, "item" : "abc2", "discount" : 20 }
            """
                .trimMargin(),
        )
    }

    private fun candidates(revision: String = commit) =
        listOf(listOf(Candidate(fixture, origin, FormSource(revision, page))))

    @Test
    fun retiresALockOnlyWhenTheDeclaredRulesReproduceTheFixture() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()

        val review = FormMigration.draft(repository, pages, "mongodb/docs", candidates())
        val declaration = review.declarations.single()
        assertEquals(listOf(FormRule.COND_ARRAY), declaration.rules)
        assertEquals(lockText, declaration.lock)
        assertEquals(review, AuditJson.formReview(AuditJson.write(review)))

        val report = FormMigration.capture(repository, "head", pages, review)
        val override = report.overrides.single()
        assertEquals("Use Cond", override.example)
        assertEquals(
            listOf("source/includes/sample-inventory.rst"),
            override.includes.map { it.path },
        )
        assertEquals(40, override.pageBlob.length)
        assertEquals(1, report.summary["rule.COND_ARRAY"])
        assertEquals(report, AuditJson.formMigration(AuditJson.write(report)))

        val lock = repository.resolve("core/src/test/resources/dev/morphia/test/$fixture/lock")
        assertThrows(IllegalArgumentException::class.java) {
            FormMigration.verify(repository, review, report)
        }
        FormMigration.apply(repository, report)
        assertFalse(lock.exists())
        // Verification needs only the recorded source text, not the pages.
        pages.toFile().deleteRecursively()
        FormMigration.verify(repository, review, report)
    }

    @Test
    fun declaredRulesMustBeExactlyTheNecessaryOnes() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        val review = FormMigration.draft(repository, pages, "mongodb/docs", candidates())
        fun declare(rules: List<FormRule>) =
            review.copy(declarations = listOf(review.declarations.single().copy(rules = rules)))

        val moved =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.capture(
                    repository,
                    "head",
                    pages,
                    review.copy(
                        declarations =
                            listOf(review.declarations.single().copy(pageBlob = "0".repeat(40)))
                    ),
                )
            }
        assertTrue("page or include blobs differ" in moved.message!!, moved.message)
        listOf(emptyList(), listOf(FormRule.COND_ARRAY, FormRule.NAMED_ARGUMENTS)).forEach {
            val failure =
                assertThrows(IllegalArgumentException::class.java) {
                    FormMigration.capture(repository, "head", pages, declare(it))
                }
            assertTrue("needs rules [COND_ARRAY]" in failure.message!!, failure.message)
        }
    }

    @Test
    fun anyOtherEditKeepsTheLock() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        // An undeclared data edit is not a form difference.
        fixture("useCond", "data.json", """{ "_id" : 1, "item" : "abc1", "qty" : 300.0 }""")

        val review = FormMigration.draft(repository, pages, "mongodb/docs", candidates())
        assertEquals(emptyList<FormDeclaration>(), review.declarations)
        val gap = review.unresolved.single()
        assertEquals(FormGap.RESIDUAL_DIFFERENCE, gap.gap)
        assertEquals("data.json ARRAY_LENGTH at ", gap.detail)

        val report = FormMigration.capture(repository, "head", pages, review)
        FormMigration.apply(repository, report)
        assertTrue(
            repository.resolve("core/src/test/resources/dev/morphia/test/$fixture/lock").exists()
        )
        FormMigration.verify(repository, review, report)
    }

    @Test
    fun laterCandidatesAreTriedWhenTheFirstPageDoesNotReproduce() {
        page(
            commit,
            documented
                .replace("Use Cond", "Use Cond Older")
                .replace("~~~~~~~~\n", "~~~~~~~~~~~~~~\n"),
        )
        include(commit, "/includes/sample-inventory.rst", inventory)
        page(newer, documented)
        include(newer, "/includes/sample-inventory.rst", inventory)
        curated()

        val review =
            FormMigration.draft(
                repository,
                pages,
                "mongodb/docs",
                candidates(commit) + candidates(newer),
            )
        assertEquals(newer, review.declarations.single().source.commit)
        val only = FormMigration.draft(repository, pages, "mongodb/docs", candidates(commit))
        assertEquals(
            FormUnresolved(
                fixture,
                origin,
                FormSource(commit, page),
                FormGap.NO_MATCHING_EXAMPLE,
                "Use Cond",
            ),
            only.unresolved.single(),
        )
    }

    @Test
    fun verificationRejectsDriftAfterRetirement() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        val review = FormMigration.draft(repository, pages, "mongodb/docs", candidates())
        val report = FormMigration.capture(repository, "head", pages, review)
        FormMigration.apply(repository, report)
        val action =
            repository.resolve("core/src/test/resources/dev/morphia/test/$fixture/action.json")
        val original = Files.readString(action)

        fun rejects(message: String, mutate: () -> Unit, restore: () -> Unit) {
            mutate()
            val failure =
                assertThrows(IllegalArgumentException::class.java) {
                    FormMigration.verify(repository, review, report)
                }
            assertTrue(message in failure.message!!, failure.message)
            restore()
            FormMigration.verify(repository, review, report)
        }
        rejects(
            "fixture files changed since the override",
            { action.writeText(original.replace("30", "31")) },
            { action.writeText(original) },
        )
        val redeclared =
            review.copy(
                declarations =
                    listOf(review.declarations.single().copy(lock = "a different lock\n"))
            )
        val mismatch =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.verify(repository, redeclared, report)
            }
        assertTrue("override differs from its declaration" in mismatch.message!!, mismatch.message)
        val tampered =
            report.copy(
                overrides =
                    listOf(
                        report.overrides.single().let {
                            it.copy(blocks = it.blocks.map { b -> b.copy(text = b.text + " ") })
                        }
                    )
            )
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.verify(repository, review, tampered)
            }
        assertTrue("source text digest mismatch" in failure.message!!, failure.message)
    }

    @Test
    fun applyChecksEveryLockBeforeDeletingAny() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        val review = FormMigration.draft(repository, pages, "mongodb/docs", candidates())
        val report = FormMigration.capture(repository, "head", pages, review)
        val lock = fixture("useCond", "lock", "edited after capture\n")

        assertThrows(IllegalArgumentException::class.java) {
            FormMigration.apply(repository, report)
        }
        assertTrue(lock.exists())
    }

    @Test
    fun alternativeCommandsNeedTheirOwnRule() {
        val filters = "source/reference/operator/query/type.txt"
        pages.resolve("$commit/$filters").also {
            it.parent.createDirectories()
            it.writeText(
                """
                Examples
                --------

                Querying by Data Type
                ~~~~~~~~~~~~~~~~~~~~~

                .. code-block:: javascript

                   db.addressBook.find( { zipCode : { ${'$'}type : 2 } } );
                   db.addressBook.find( { zipCode : { ${'$'}type : "string" } } );
                """
                    .trimIndent()
            )
        }
        val folder =
            repository.resolve("core/src/test/resources/dev/morphia/test/query/filters/type/byType")
        folder.createDirectories()
        folder.resolve("name").writeText("Querying by Data Type\n")
        folder.resolve("lock").writeText("picked the form morphia uses\n")
        folder
            .resolve("action.json")
            .writeText("""db.addressBook.find( { zipCode : { ${'$'}type : "string" } } )""")

        val review =
            FormMigration.draft(
                repository,
                pages,
                "mongodb/docs",
                listOf(
                    listOf(
                        Candidate("query/filters/type/byType", origin, FormSource(commit, filters))
                    )
                ),
            )
        assertEquals(listOf(FormRule.ALTERNATIVE_COMMANDS), review.declarations.single().rules)
        // The tool cannot prove the alternatives equivalent, so a reviewer must say why.
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.capture(repository, "head", pages, review)
            }
        assertTrue("a justification is required" in failure.message!!, failure.message)
        val justified =
            review.copy(
                declarations =
                    listOf(review.declarations.single().copy(justification = "2 is \"string\""))
            )
        FormMigration.capture(repository, "head", pages, justified)
        val unneeded =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.capture(
                    repository,
                    "head",
                    pages,
                    justified.copy(
                        declarations =
                            listOf(justified.declarations.single().copy(rules = emptyList()))
                    ),
                )
            }
        assertTrue("a justification is required" in unneeded.message!!, unneeded.message)
    }

    /**
     * The committed migration must still hold offline: every override's recorded source reproduces
     * its fixture under exactly its declared rules, retired locks are gone, and every locked
     * example whose only category is MORPHIA_FORM is either retired or listed with its gap.
     */
    @Test
    fun committedMigrationMatchesTheTree() {
        val root =
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isDirectory(it.resolve("core/src/test/java/dev/morphia/test")) }
        fun read(path: String) =
            Files.readAllBytes(root.resolve(path)).decodeToString(throwOnInvalidSequence = true)
        val review = AuditJson.formReview(read("audits/catalog/form-review.json"))
        val report = AuditJson.formMigration(read("audits/catalog/form-migration.json"))
        assertEquals(read("audits/catalog/form-review.json"), AuditJson.write(review))
        FormMigration.verify(root, review, report)
        // Pages are not committed (separate licence); the review pins their blob ids instead.
        report.overrides.forEach { assertEquals(40, it.pageBlob.length) }

        val inventory = AuditJson.curatedRepairs(read("audits/catalog/curated-repairs.json"))
        val formOnly = inventory.examples.filter { it.categories == listOf("MORPHIA_FORM") }
        assertEquals(
            formOnly.map { it.fixture }.toSet(),
            (review.declarations.map { it.fixture } + review.unresolved.map { it.fixture }).toSet(),
        )
        assertEquals(
            review.declarations.map { it.fixture }.toSet(),
            inventory.examples
                .filter { it.disposition == RepairDisposition.OVERRIDDEN }
                .map { it.fixture }
                .toSet(),
        )
        assertEquals(43, report.overrides.size)
        assertEquals(23, report.unresolved.size)
    }
}
