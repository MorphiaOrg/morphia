package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.repairs.FixtureFacts
import dev.morphia.audits.repairs.FormCorrection
import dev.morphia.audits.repairs.FormDeclaration
import dev.morphia.audits.repairs.FormGap
import dev.morphia.audits.repairs.FormMigration
import dev.morphia.audits.repairs.FormMigration.Candidate
import dev.morphia.audits.repairs.FormRule
import dev.morphia.audits.repairs.FormSource
import dev.morphia.audits.repairs.FormUnresolved
import dev.morphia.audits.repairs.NumericRetype
import dev.morphia.audits.repairs.NumericType
import dev.morphia.audits.repairs.RepairDisposition
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import org.bson.BsonInt32
import org.bson.BsonString
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

    /** What the repair inventory would record for the synthetic fixtures. */
    private fun facts(
        collections: Map<String, String> = emptyMap(),
        categories: List<String> = listOf("MORPHIA_FORM"),
        helper: String = "testPipeline",
    ) =
        mapOf(
            fixture to FixtureFacts(categories, collections, listOf(helper)),
            "query/filters/type/byType" to
                FixtureFacts(listOf("MORPHIA_FORM"), helpers = listOf("testQuery")),
        )

    private fun candidates(revision: String = commit) =
        listOf(listOf(Candidate(fixture, origin, FormSource(revision, page))))

    @Test
    fun retiresALockOnlyWhenTheDeclaredRulesReproduceTheFixture() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()

        val review = FormMigration.draft(repository, pages, "mongodb/docs", candidates(), facts())
        val declaration = review.declarations.single()
        assertEquals(listOf(FormRule.COND_ARRAY), declaration.rules)
        assertEquals(lockText, declaration.lock)
        assertEquals(review, AuditJson.formReview(AuditJson.write(review)))

        val report = FormMigration.capture(repository, "head", pages, review, facts())
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
            FormMigration.verify(repository, review, report, facts())
        }
        FormMigration.apply(repository, report)
        assertFalse(lock.exists())
        // Verification needs only the recorded source text, not the pages.
        pages.toFile().deleteRecursively()
        FormMigration.verify(repository, review, report, facts())
    }

    @Test
    fun declaredRulesMustBeExactlyTheNecessaryOnes() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        val review = FormMigration.draft(repository, pages, "mongodb/docs", candidates(), facts())
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
                    facts(),
                )
            }
        assertTrue("page or include blobs differ" in moved.message!!, moved.message)
        listOf(emptyList(), listOf(FormRule.COND_ARRAY, FormRule.NAMED_ARGUMENTS)).forEach {
            val failure =
                assertThrows(IllegalArgumentException::class.java) {
                    FormMigration.capture(repository, "head", pages, declare(it), facts())
                }
            assertTrue("needs rules [COND_ARRAY]" in failure.message!!, failure.message)
        }
    }

    @Test
    fun extraCollectionsAreComparedWithTheDataFilesTheTestLoads() {
        page(commit, documented)
        include(
            commit,
            "/includes/sample-inventory.rst",
            inventory +
                """
                |
                |
                |.. code-block:: javascript
                |
                |   db.prices.insertMany( [ { "_id" : 1, "price" : 5 } ] )
                """
                    .trimMargin(),
        )
        curated()
        fixture("useCond", "data2.json", """{ "_id" : 1, "price" : 5 }""")
        val bound = facts(mapOf("data2.json" to "prices"))

        fun gap(collections: Map<String, FixtureFacts>) =
            FormMigration.draft(repository, pages, "mongodb/docs", candidates(), collections)
                .unresolved
                .single()
                .let { it.gap to it.detail }
        assertEquals(FormGap.FIXTURE_UNSUPPORTED to "files [data2.json]", gap(facts()))
        assertEquals(
            FormGap.FIXTURE_UNSUPPORTED to "no data file for collection prices",
            gap(facts(mapOf("data2.json" to "costs"))),
        )
        assertEquals(
            FormGap.FIXTURE_UNSUPPORTED to "a data file targets the action collection",
            gap(facts(mapOf("data2.json" to "inventory"))),
        )

        val review = FormMigration.draft(repository, pages, "mongodb/docs", candidates(), bound)
        assertEquals(listOf(FormRule.COND_ARRAY), review.declarations.single().rules)
        val report = FormMigration.capture(repository, "head", pages, review, bound)
        assertEquals(bound.getValue(fixture).collections, report.overrides.single().collections)
        FormMigration.apply(repository, report)
        FormMigration.verify(repository, review, report, bound)
        val changed =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.verify(repository, review, report, facts())
            }
        assertTrue("test collections changed" in changed.message!!, changed.message)

        // The extra collection's data must match exactly, like data.json.
        fixture("useCond", "lock", lockText)
        fixture("useCond", "data2.json", """{ "_id" : 1, "price" : 5.0 }""")
        assertEquals(FormGap.RESIDUAL_DIFFERENCE to "data2.json TYPE at /0/price", gap(bound))
    }

    @Test
    fun dataThatNamesNoCollectionOrAnotherDatabaseIsAGap() {
        page(commit, documented)
        curated()
        fun gap(data: String) =
            include(commit, "/includes/sample-inventory.rst", data).let {
                FormMigration.draft(repository, pages, "mongodb/docs", candidates(), facts())
                    .unresolved
                    .single()
                    .let { it.gap to it.detail }
            }
        // A listing names no collection, so it cannot be told apart beside several collections.
        assertEquals(
            FormGap.SOURCE_UNSUPPORTED to "document listing beside several collections",
            gap(
                """
                |.. code-block:: javascript
                |
                |   { "_id" : 1, "item" : "abc1", "qty" : 300 }
                |
                |.. code-block:: javascript
                |
                |   db.prices.insertMany( [ { "_id" : 1, "price" : 5 } ] )
                """
                    .trimMargin()
            ),
        )
        assertEquals(
            FormGap.SOURCE_UNSUPPORTED to "command outside the current database",
            gap(inventory.replace("db.inventory.", "db.getSiblingDB(\"other\").inventory.")),
        )
    }

    @Test
    fun numberTypeChangesNeedANumericTypeLock() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        fixture(
            "useCond",
            "data.json",
            """
            |{ "_id" : 1, "item" : "abc1", "qty" : 300.0 }
            |{ "_id" : 2, "item" : "abc2", "qty" : 200.0 }
            """
                .trimMargin(),
        )
        fun draft(vararg categories: String) =
            FormMigration.draft(
                repository,
                pages,
                "mongodb/docs",
                candidates(),
                facts(categories = categories.toList()),
            )
        // A form lock may not change number types, nor a numeric lock change form.
        assertEquals("data.json TYPE at /0/qty", draft("MORPHIA_FORM").unresolved.single().detail)
        assertEquals(
            "action TYPE at /0/${'$'}project/discount/${'$'}cond",
            draft("NUMERIC_TYPE").unresolved.single().detail,
        )

        val review = draft("MORPHIA_FORM", "NUMERIC_TYPE")
        val declaration = review.declarations.single()
        assertEquals(listOf(FormRule.COND_ARRAY), declaration.rules)
        // The two qty values share no deeper pointer than the whole file, which would also
        // change the integral _id values; each keeps its own retype.
        assertEquals(
            listOf(
                NumericRetype("data.json", "/0/qty", NumericType.INT32, NumericType.DOUBLE),
                NumericRetype("data.json", "/1/qty", NumericType.INT32, NumericType.DOUBLE),
            ),
            declaration.retypes,
        )
        val both = facts(categories = listOf("MORPHIA_FORM", "NUMERIC_TYPE"))
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.capture(
                    repository,
                    "head",
                    pages,
                    review.copy(declarations = listOf(declaration.copy(retypes = emptyList()))),
                    both,
                )
            }
        assertTrue("needs retypes" in failure.message!!, failure.message)
        val report = FormMigration.capture(repository, "head", pages, review, both)
        assertEquals(1, report.summary["overrides.retyped"])
        FormMigration.apply(repository, report)
        FormMigration.verify(repository, review, report, both)
        val rejected =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.verify(repository, review, report, facts())
            }
        assertTrue("RESIDUAL_DIFFERENCE data.json TYPE" in rejected.message!!, rejected.message)
    }

    @Test
    fun valueCorrectionsNeedADocsErrorOrCurationLock() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        fixture(
            "useCond",
            "expected.json",
            """
            |{ "_id" : 1, "item" : "abc1", "note" : "sale", "discount" : 30 }
            |{ "_id" : 2, "item" : "abc2", "discount" : 25 }
            """
                .trimMargin(),
        )
        fun draft(vararg categories: String) =
            FormMigration.draft(
                repository,
                pages,
                "mongodb/docs",
                candidates(),
                facts(categories = categories.toList()),
            )
        assertEquals(
            "expected.json EXTRA_FIELD at /0/note",
            draft("MORPHIA_FORM").unresolved.single().detail,
        )
        val review = draft("MORPHIA_FORM", "DOCS_ERROR")
        val declaration = review.declarations.single()
        // The form difference keeps its rule; only the wrong value is corrected.
        assertEquals(listOf(FormRule.COND_ARRAY), declaration.rules)
        // Each field is corrected on its own; nothing else in the row is swept along.
        assertEquals(
            listOf(
                FormCorrection.of("expected.json", "/0/note", null, BsonString("sale"), 2),
                FormCorrection.of("expected.json", "/1/discount", BsonInt32(20), BsonInt32(25)),
            ),
            declaration.corrections,
        )
        val both = facts(categories = listOf("MORPHIA_FORM", "DOCS_ERROR"))
        val report = FormMigration.capture(repository, "head", pages, review, both)
        assertEquals(1, report.summary["overrides.corrected"])

        // A page that now documents another value no longer matches the reviewed correction.
        page(commit, documented.replace("\"discount\" : 20", "\"discount\" : 15"))
        val stale =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.capture(repository, "head", pages, review, both)
            }
        assertTrue("needs corrections" in stale.message!!, stale.message)
    }

    @Test
    fun aReviewerCanRejectADraftedDeclaration() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        fixture(
            "useCond",
            "expected.json",
            """
            |{ "_id" : 1, "item" : "abc1", "discount" : 30 }
            |{ "_id" : 2, "item" : "abc2", "discount" : 25 }
            """
                .trimMargin(),
        )
        val both = facts(categories = listOf("MORPHIA_FORM", "DOCS_ERROR"))
        val drafted = FormMigration.draft(repository, pages, "mongodb/docs", candidates(), both)
        val declaration = drafted.declarations.single()
        fun rejected(reason: String) =
            drafted.copy(
                declarations = emptyList(),
                unresolved =
                    listOf(
                        FormUnresolved(
                            declaration.fixture,
                            declaration.origin,
                            declaration.source,
                            FormGap.REJECTED,
                            reason,
                        )
                    ),
            )
        val review = rejected("the lock does not describe the discount change")
        val report = FormMigration.capture(repository, "head", pages, review, both)
        assertEquals(1, report.summary["gap.REJECTED"])
        FormMigration.apply(repository, report)
        assertTrue(
            repository.resolve("core/src/test/resources/dev/morphia/test/$fixture/lock").exists()
        )
        FormMigration.verify(repository, review, report, both)

        val blank =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.capture(repository, "head", pages, rejected(" "), both)
            }
        assertTrue("reviewer's reason" in blank.message!!, blank.message)

        // A rejection is only for a fixture the tool reproduces; any other gap must be reviewed.
        fixture("useCond", "data.json", """{ "_id" : 1, "item" : "abc1", "qty" : 300.0 }""")
        val stale =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.capture(repository, "head", pages, review, both)
            }
        assertTrue("reviewed REJECTED" in stale.message!!, stale.message)
    }

    @Test
    fun generatedExtractsAreAssembledFromTheirYaml() {
        // Giza builds /includes/extracts/<ref>.rst from extracts*.yaml; the page is not tracked.
        page(
            commit,
            documented.replace("include:: /includes/sample", "include::  /includes/extracts/sample"),
        )
        include(
            commit,
            "/includes/extracts-inventory.yaml",
            """
            |ref: _inventory
            |content: |
            |  .. code-block:: javascript
            |
            |     db.{{collection}}.insertMany( [
            |        { "_id" : 1, "item" : "abc1", "qty" : 300 },
            |        { "_id" : 2, "item" : "abc2", "qty" : 200 }
            |     ] )
            |replacement:
            |  collection: "orders"
            |---
            |ref: sample-inventory
            |inherit:
            |  ref: _inventory
            |  file: extracts-inventory.yaml
            |replacement:
            |  collection: "inventory"
            |...
            """
                .trimMargin(),
        )
        curated()
        val review = FormMigration.draft(repository, pages, "mongodb/docs", candidates(), facts())
        val declaration = review.declarations.single()
        assertEquals(listOf(FormRule.COND_ARRAY), declaration.rules)
        assertEquals(
            listOf("source/includes/extracts-inventory.yaml"),
            declaration.includes.map { it.path }.distinct(),
        )
        FormMigration.capture(repository, "head", pages, review, facts())
    }

    @Test
    fun fixtureFilesMustReadOneDocumentPerLine() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        // The same documents, laid out as the page prints them: the tests' line reader fails.
        fixture(
            "useCond",
            "data.json",
            """
            |[
            |  { "_id" : 1, "item" : "abc1", "qty" : 300 },
            |  { "_id" : 2, "item" : "abc2", "qty" : 200 }
            |]
            """
                .trimMargin(),
        )
        val gap = FormMigration.draft(repository, pages, "mongodb/docs", candidates(), facts())
        assertEquals(
            FormGap.FIXTURE_UNSUPPORTED to "data.json is not one document per line",
            gap.unresolved.single().let { it.gap to it.detail },
        )
    }

    @Test
    fun theFixtureIsReadAsTheKindOfActionItsTestRuns() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        fun gap(facts: Map<String, FixtureFacts>) =
            FormMigration.draft(repository, pages, "mongodb/docs", candidates(), facts)
                .unresolved
                .single()
                .let { it.gap to it.detail }
        // A documented pipeline is never compared with a fixture the test runs as an update.
        assertEquals(
            FormGap.RESIDUAL_DIFFERENCE to "action is PIPELINE, the test runs UPDATE",
            gap(facts(helper = "testUpdate")),
        )
        assertEquals(FormGap.FIXTURE_UNSUPPORTED to "not one test helper", gap(facts(helper = "")))
    }

    @Test
    fun printedResultsAreComparedByWhatTheTextProves() {
        // mongosh output the document reader cannot read: a Decimal128 constructor and a comment.
        page(
            commit,
            documented
                .replace(
                    """{ "_id" : 1, "item" : "abc1", "discount" : 30 }""",
                    """{ _id: 1, item: 'abc1', discount: Decimal128("30") } // first""",
                )
                .replace(
                    """{ "_id" : 2, "item" : "abc2", "discount" : 20 }""",
                    """{ _id: 2, item: 'abc2', discount: Decimal128("20") }""",
                ),
        )
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        fun expected(text: String) = fixture("useCond", "expected.json", text.trimMargin())
        fun draft() = FormMigration.draft(repository, pages, "mongodb/docs", candidates(), facts())
        // A printed 2 may be an Int32 or a Double; a Decimal128 keeps its printed text.
        expected(
            """
            |{ "_id" : 1, "item" : "abc1", "discount" : NumberDecimal("30") }
            |{ "_id" : 2.0, "item" : "abc2", "discount" : NumberDecimal("20") }
            """
        )
        val review = draft()
        assertEquals(listOf(FormRule.COND_ARRAY), review.declarations.single().rules)
        FormMigration.capture(repository, "head", pages, review, facts())

        // Another Decimal128 scale is another value, and no retype or correction applies.
        expected(
            """
            |{ "_id" : 1, "item" : "abc1", "discount" : NumberDecimal("30.0") }
            |{ "_id" : 2, "item" : "abc2", "discount" : NumberDecimal("20") }
            """
        )
        assertEquals(
            FormGap.RESIDUAL_DIFFERENCE to "printed expected VALUE at /0/discount",
            draft().unresolved.single().let { it.gap to it.detail },
        )
        // Printed numbers cannot show the type a NUMERIC_TYPE lock changed, so a match is a gap.
        expected(
            """
            |{ "_id" : 1, "item" : "abc1", "discount" : NumberDecimal("30") }
            |{ "_id" : 2.0, "item" : "abc2", "discount" : NumberDecimal("20") }
            """
        )
        val numeric = facts(categories = listOf("NUMERIC_TYPE"))
        assertEquals(
            FormGap.SOURCE_UNSUPPORTED to "printed results cannot prove number types",
            FormMigration.draft(repository, pages, "mongodb/docs", candidates(), numeric)
                .unresolved
                .single()
                .let { it.gap to it.detail },
        )
    }

    @Test
    fun anyOtherEditKeepsTheLock() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        // An undeclared data edit is not a form difference.
        fixture("useCond", "data.json", """{ "_id" : 1, "item" : "abc1", "qty" : 300.0 }""")

        val review = FormMigration.draft(repository, pages, "mongodb/docs", candidates(), facts())
        assertEquals(emptyList<FormDeclaration>(), review.declarations)
        val gap = review.unresolved.single()
        assertEquals(FormGap.RESIDUAL_DIFFERENCE, gap.gap)
        assertEquals("data.json ARRAY_LENGTH at ", gap.detail)

        val report = FormMigration.capture(repository, "head", pages, review, facts())
        FormMigration.apply(repository, report)
        assertTrue(
            repository.resolve("core/src/test/resources/dev/morphia/test/$fixture/lock").exists()
        )
        FormMigration.verify(repository, review, report, facts())
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
                facts(),
            )
        assertEquals(newer, review.declarations.single().source.commit)
        val only =
            FormMigration.draft(repository, pages, "mongodb/docs", candidates(commit), facts())
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
        val review = FormMigration.draft(repository, pages, "mongodb/docs", candidates(), facts())
        val report = FormMigration.capture(repository, "head", pages, review, facts())
        FormMigration.apply(repository, report)
        val action =
            repository.resolve("core/src/test/resources/dev/morphia/test/$fixture/action.json")
        val original = Files.readString(action)

        fun rejects(message: String, mutate: () -> Unit, restore: () -> Unit) {
            mutate()
            val failure =
                assertThrows(IllegalArgumentException::class.java) {
                    FormMigration.verify(repository, review, report, facts())
                }
            assertTrue(message in failure.message!!, failure.message)
            restore()
            FormMigration.verify(repository, review, report, facts())
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
                FormMigration.verify(repository, redeclared, report, facts())
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
                FormMigration.verify(repository, review, tampered, facts())
            }
        assertTrue("source text digest mismatch" in failure.message!!, failure.message)
    }

    @Test
    fun applyChecksEveryLockBeforeDeletingAny() {
        page(commit, documented)
        include(commit, "/includes/sample-inventory.rst", inventory)
        curated()
        val review = FormMigration.draft(repository, pages, "mongodb/docs", candidates(), facts())
        val report = FormMigration.capture(repository, "head", pages, review, facts())
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
                facts(),
            )
        assertEquals(listOf(FormRule.ALTERNATIVE_COMMANDS), review.declarations.single().rules)
        // The tool cannot prove the alternatives equivalent, so a reviewer must say why.
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.capture(repository, "head", pages, review, facts())
            }
        assertTrue("a justification is required" in failure.message!!, failure.message)
        val justified =
            review.copy(
                declarations =
                    listOf(review.declarations.single().copy(justification = "2 is \"string\""))
            )
        FormMigration.capture(repository, "head", pages, justified, facts())
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
                    facts(),
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
        val inventory = AuditJson.curatedRepairs(read("audits/catalog/curated-repairs.json"))
        FormMigration.verify(root, review, report, FormMigration.facts(inventory))
        // Pages are not committed (separate licence); the review pins their blob ids instead.
        report.overrides.forEach { assertEquals(40, it.pageBlob.length) }

        // Every locked example is either retired or kept with its gap, except the untestable
        // change-stream fixtures, which have no test to compare against.
        val formOnly =
            inventory.examples.filter {
                it.categories.isNotEmpty() && "UNTESTABLE" !in it.categories
            }
        // Converted cases keep their legacy test, so the inventory still holds them; migrated
        // cases left it with theirs.
        val known = inventory.examples.map { it.fixture }.toSet()
        val converted = review.migrated.map { it.fixture }.filter { it in known }
        assertEquals(
            formOnly.map { it.fixture }.toSet(),
            (review.declarations.map { it.fixture } +
                    review.unresolved.map { it.fixture } +
                    converted)
                .toSet(),
        )
        assertEquals(
            (review.declarations.map { it.fixture } + converted).toSet(),
            inventory.examples
                .filter { it.disposition == RepairDisposition.OVERRIDDEN }
                .map { it.fixture }
                .toSet(),
        )
        assertEquals(135, report.overrides.size)
        assertEquals(96, report.unresolved.size)
    }
}
