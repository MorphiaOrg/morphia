package dev.morphia.audits.offline

import dev.morphia.audits.api.MorphiaApiInventory
import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.generate.BindingForm
import dev.morphia.audits.generate.BindingPosition
import dev.morphia.audits.generate.CaseGenerator
import dev.morphia.audits.generate.CurrentPage
import dev.morphia.audits.generate.CurrentPages
import dev.morphia.audits.generate.DocumentedExamples
import dev.morphia.audits.generate.DslBinding
import dev.morphia.audits.generate.DslBindingCatalog
import dev.morphia.audits.generate.DslBindings
import dev.morphia.audits.generate.ExampleInventory
import dev.morphia.audits.generate.ExampleStatus
import dev.morphia.audits.generate.Exclusion
import dev.morphia.audits.generate.GapKind
import dev.morphia.audits.generate.GenerationReview
import dev.morphia.audits.generate.MethodRef
import dev.morphia.audits.repairs.FixtureMigrationLedger
import dev.morphia.audits.repairs.FormSource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GeneratorTest {
    @TempDir lateinit var directory: Path

    private val commit = "c".repeat(40)
    private val folder = "content/manual/manual/source/reference/operator/aggregation"

    private fun write(path: Path, text: String) {
        path.parent.createDirectories()
        path.writeText(text)
    }

    private fun page(operator: String, directive: String, examples: String) {
        write(
            directory.resolve("pages/$commit/$folder/$operator.txt"),
            """
            |=======
            |$operator
            |=======
            |
            |.. $directive:: ${'$'}$operator
            |
            |$examples
            |"""
                .trimMargin(),
        )
    }

    private fun block(code: String) =
        ".. code-block:: javascript\n\n" + code.trimIndent().prependIndent("   ") + "\n"

    private val data = block("db.switches.insertMany( [ { _id: 0, a: Int32(3) } ] )")

    private fun aggregate(expression: String) =
        block("db.switches.aggregate( [ { \$project: { result: $expression } } ] )")

    /**
     * A repository with two legacy `$bitOr` fixtures, one owning its example by title and one by
     * action text, and three pages: `$bitOr`'s third example is unowned, `$bitAnd`'s only example
     * has a generic title, and `$bitXor`'s two examples share a name.
     */
    private fun inventory(): ExampleInventory {
        val root = directory.resolve("repository")
        write(
            root.resolve("audits/catalog/fixture-migration.json"),
            AuditJson.write(FixtureMigrationLedger()),
        )
        val legacy =
            root.resolve("core/src/test/resources/dev/morphia/test/aggregation/expressions/bitOr")
        write(legacy.resolve("titled/name"), "Bitwise ``OR`` with Two Integers\n")
        write(
            legacy.resolve("moved/action.json"),
            "{ \$project: {\n result: { \$bitOr: [ \"\$a\", 7 ] } } }",
        )
        page(
            "bitOr",
            "expression",
            """
            |Examples
            |--------
            |
            |$data
            |Bitwise ``OR`` with Two Integers
            |~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            |
            |${aggregate("{ \$bitOr: [ \"\$a\", \"\$a\" ] }")}
            |Renamed Since the Fixture
            |~~~~~~~~~~~~~~~~~~~~~~~~~
            |
            |${aggregate("{ \$bitOr: [ \"\$a\", 7 ] }")}
            |Bitwise ``OR`` with a Long and Integer
            |~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            |
            |${aggregate("{ \$bitOr: [ \"\$a\", Long(\"63\") ] }")}
            |${block("[ { _id: 0, result: Long(\"63\") } ]")}"""
                .trimMargin(),
        )
        page(
            "bitAnd",
            "expression",
            "Example\n-------\n\n$data\n${aggregate("{ \$bitAnd: [ \"\$a\", 1 ] }")}",
        )
        page(
            "bitXor",
            "expression",
            """
            |Examples
            |--------
            |
            |$data
            |Use Two
            |~~~~~~~
            |
            |${aggregate("{ \$bitXor: [ \"\$a\", 1 ] }")}
            |Use two
            |~~~~~~~
            |
            |${aggregate("{ \$bitXor: [ \"\$a\", 2 ] }")}"""
                .trimMargin(),
        )
        write(directory.resolve("pages/$commit/$folder/interface.txt"), "No directive here.\n")
        val sources =
            CurrentPages(
                commit = commit,
                pages =
                    listOf("bitAnd", "bitOr", "bitXor", "interface").map {
                        CurrentPage(FormSource(commit, "$folder/$it.txt"))
                    },
            )
        return DocumentedExamples.capture(root, directory.resolve("pages"), sources)
    }

    @Test
    fun examplesAreMatchedToTheirOwnersByTitleOrByActionText() {
        val inventory = inventory()
        val examples =
            inventory.pages
                .flatMap { page -> page.examples.map { "${page.operator}/${it.title}" to it } }
                .toMap()
        assertEquals(listOf("bitAnd", "bitOr", "bitXor"), inventory.pages.map { it.operator })
        assertEquals(
            "aggregation/expressions/bitOr/titled",
            examples.getValue("bitOr/Bitwise ``OR`` with Two Integers").owner,
        )
        val moved = examples.getValue("bitOr/Renamed Since the Fixture")
        assertEquals(
            "aggregation/expressions/bitOr/moved" to "matched by action text",
            moved.owner to moved.detail,
        )
        val unowned = examples.getValue("bitOr/Bitwise ``OR`` with a Long and Integer")
        assertEquals(
            ExampleStatus.UNOWNED to "bitwiseOrWithALongAndInteger",
            unowned.status to unowned.stem,
        )
        assertTrue(unowned.blocks.isNotEmpty())
        assertEquals(ExampleStatus.UNNAMED, examples.getValue("bitAnd/main").status)
        assertEquals(
            listOf(ExampleStatus.AMBIGUOUS, ExampleStatus.AMBIGUOUS),
            listOf("bitXor/Use Two", "bitXor/Use two").map { examples.getValue(it).status },
        )
        // An owned example keeps no source text: only unowned ones are generated from it.
        assertTrue(
            examples.values.filter { it.status == ExampleStatus.OWNED }.all { it.blocks.isEmpty() }
        )
        assertEquals(listOf("no operator directive"), inventory.skipped.map { it.detail })
        assertEquals(inventory, AuditJson.exampleInventory(AuditJson.write(inventory)))
    }

    private val expressions = "dev.morphia.aggregation.expressions"
    private val bindings =
        DslBindingCatalog(
            bindings =
                listOf(
                    DslBinding(
                        "\$project",
                        BindingPosition.STAGE,
                        BindingForm.PROJECTION,
                        "dev.morphia.aggregation.stages.Projection",
                        "project",
                        emptyList(),
                        fields =
                            mapOf(
                                "include" to MethodRef("include", listOf("java.lang.String")),
                                "exclude" to MethodRef("exclude", listOf("java.lang.String")),
                                "suppressId" to MethodRef("suppressId", emptyList()),
                                "value" to
                                    MethodRef(
                                        "include",
                                        listOf("java.lang.String", "java.lang.Object"),
                                    ),
                            ),
                    ),
                    DslBinding(
                        "\$bitOr",
                        BindingPosition.EXPRESSION,
                        BindingForm.POSITIONAL,
                        "$expressions.MathExpressions",
                        "bitOr",
                        listOf("java.lang.Object", "java.lang.Object"),
                    ),
                )
        )

    private val name = "aggregation/expressions/bitOr/bitwiseOrWithALongAndInteger"

    @Test
    fun generatesABoundExampleAndNamesEveryGap() {
        val inventory = inventory()
        val review = GenerationReview(families = listOf("aggregation/expressions"))
        val generation = CaseGenerator.generate(inventory, bindings, review, emptySet())
        assertEquals(listOf(name), generation.cases.map { it.manifest.name })
        val case = generation.cases.single().manifest
        assertEquals("GENERATED", case.kind)
        assertEquals(
            "dev.morphia.test.generated.expressions.TestBitOr#bitwiseOrWithALongAndInteger",
            case.test,
        )
        val source =
            generation.files.getValue(
                "core/src/test/java/dev/morphia/test/generated/expressions/TestBitOr.java"
            )
        assertTrue("project().include(\"result\", bitOr(\"\$a\", 63L))" in source, source)
        assertTrue("@DisplayName(\"Bitwise ``OR`` with a Long and Integer\")" in source, source)
        assertTrue(source.startsWith("// spotless:off\n") && source.endsWith("// spotless:on\n"))
        val scenario = generation.files.getValue("core/src/test/resources${case.scenario}")
        assertTrue("\"\$numberLong\": \"63\"" in scenario, scenario)
        assertEquals(
            listOf(GapKind.UNNAMED, GapKind.AMBIGUOUS, GapKind.AMBIGUOUS),
            generation.gaps.map { it.kind },
        )
        // A second run writes the same bytes.
        assertEquals(generation, CaseGenerator.generate(inventory, bindings, review, emptySet()))
    }

    @Test
    fun anUnboundNodeAnExclusionOrAProtectedClassIsAGap() {
        val inventory = inventory()
        val review = GenerationReview(families = listOf("aggregation/expressions"))
        fun gap(
            catalog: DslBindingCatalog = bindings,
            reviewed: GenerationReview = review,
            protected: Set<String> = emptySet(),
        ) =
            CaseGenerator.generate(inventory, catalog, reviewed, protected).let { generation ->
                assertTrue(generation.cases.isEmpty())
                generation.gaps.single { it.title.startsWith("Bitwise") }
            }
        val unbound = gap(bindings.copy(bindings = bindings.bindings.dropLast(1)))
        assertEquals(
            GapKind.UNBOUND to "/pipeline/0/\$project/result: \$bitOr has no binding",
            unbound.kind to unbound.detail,
        )
        assertEquals(
            GapKind.EXCLUDED to "the page prints another operator's results",
            gap(
                    reviewed =
                        review.copy(
                            excluded =
                                listOf(
                                    Exclusion(name, "the page prints another operator's results")
                                )
                        )
                )
                .let { it.kind to it.detail },
        )
        assertEquals(
            GapKind.PROTECTED_CLASS,
            gap(
                    protected =
                        setOf(
                            "core/src/test/java/dev/morphia/test/generated/expressions/TestBitOr.java"
                        )
                )
                .kind,
        )
        // An exclusion that names no generated case is stale.
        assertThrows(IllegalArgumentException::class.java) {
            CaseGenerator.generate(
                inventory,
                bindings,
                review.copy(
                    excluded = listOf(Exclusion("aggregation/expressions/bitOr/gone", "stale"))
                ),
                emptySet(),
            )
        }
        // Families outside the review are not generated at all.
        val none =
            CaseGenerator.generate(
                inventory,
                bindings,
                GenerationReview(families = emptyList()),
                emptySet(),
            )
        assertTrue(none.cases.isEmpty() && none.gaps.isEmpty())
    }

    @Test
    fun bindingsAreCheckedAgainstTheCurrentSource() {
        val root = directory.resolve("api")
        fun java(name: String, text: String) =
            write(root.resolve("src/p/$name.java"), "package p;\n\n$text\n")
        java("Expr", "public class Expr {}")
        java("Base", "public class Base<T> { public T as(String name) { return null; } }")
        java(
            "Filter",
            "public class Filter extends Base<Filter> { public Filter limit(Object limit) { return this; } }",
        )
        java(
            "Exprs",
            """
            public class Exprs {
                /** @aggregation.expression ${'$'}add */
                public static Expr add(Object first, Object... more) { return null; }
                public Expr plain(Object value) { return null; }
                public static Filter filter(Object input, Object cond) { return null; }
            }
            """
                .trimIndent(),
        )
        val api = MorphiaApiInventory.capture(root, "revision", listOf("src"))
        val add =
            DslBinding(
                "\$add",
                BindingPosition.EXPRESSION,
                BindingForm.POSITIONAL,
                "p.Exprs",
                "add",
                listOf("java.lang.Object", "java.lang.Object[]"),
                varargs = true,
            )
        val filter =
            DslBinding(
                "\$filter",
                BindingPosition.EXPRESSION,
                BindingForm.NAMED,
                "p.Exprs",
                "filter",
                listOf("java.lang.Object", "java.lang.Object"),
                arguments = listOf("input", "cond"),
                // `as` is inherited from the generic supertype.
                options =
                    mapOf(
                        "as" to MethodRef("as", listOf("java.lang.String")),
                        "limit" to MethodRef("limit", listOf("java.lang.Object")),
                    ),
            )
        fun problems(vararg binding: DslBinding) =
            DslBindings.verify(DslBindingCatalog(bindings = binding.toList()), api)
        assertEquals(emptyList<String>(), problems(add, filter))
        assertEquals(
            1,
            problems(add.copy(parameters = listOf("java.lang.Object", "java.lang.String[]"))).size,
        )
        assertEquals(1, problems(add.copy(varargs = false)).size)
        assertEquals(
            1,
            problems(
                    add.copy(
                        method = "plain",
                        parameters = listOf("java.lang.Object"),
                        varargs = false,
                    )
                )
                .size,
        )
        assertEquals(
            1,
            problems(
                    filter.copy(
                        options = mapOf("cond" to MethodRef("missing", listOf("java.lang.Object")))
                    )
                )
                .size,
        )
        assertEquals(1, problems(filter.copy(arguments = listOf("input"))).size)
        assertEquals(1, problems(add, add).size)
    }

    @Test
    fun theCommittedBindingsAndReviewRead() {
        val root =
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isDirectory(it.resolve("audits/catalog")) }
        val catalog = AuditJson.dslBindings(Files.readString(root.resolve(DslBindings.PATH)))
        assertTrue(catalog.bindings.isNotEmpty())
        val review =
            AuditJson.generationReview(Files.readString(root.resolve(CaseGenerator.REVIEW)))
        assertEquals(listOf("aggregation/expressions"), review.families)
        // The inventory's recorded source text is what generation reads; its digests must hold.
        val inventory =
            AuditJson.exampleInventory(Files.readString(root.resolve(DocumentedExamples.PATH)))
        inventory.pages
            .flatMap { it.examples }
            .flatMap { it.blocks }
            .forEach {
                assertEquals(
                    dev.morphia.audits.repairs.CaseManifests.sha256(it.text.toByteArray()),
                    it.sha256,
                )
            }
    }
}
