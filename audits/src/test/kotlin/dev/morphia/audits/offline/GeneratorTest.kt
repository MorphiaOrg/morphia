package dev.morphia.audits.offline

import dev.morphia.audits.api.MorphiaApiInventory
import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.generate.BindingForm
import dev.morphia.audits.generate.BindingPosition
import dev.morphia.audits.generate.CaseGenerator
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
import dev.morphia.audits.generate.ReviewedExample
import dev.morphia.audits.generate.ReviewedExamples
import dev.morphia.audits.generate.ScenarioPlans
import dev.morphia.audits.repairs.BlockRole
import dev.morphia.audits.repairs.FixtureMigrationLedger
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.bson.BsonDocument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GeneratorTest {
    @TempDir lateinit var directory: Path

    private fun write(path: Path, text: String) {
        path.parent.createDirectories()
        path.writeText(text)
    }

    private lateinit var pages: ParsedPages

    @BeforeEach
    fun pages() {
        pages = ParsedPages(directory)
    }

    /** The inventory of [pages] under the plan drafted from them. */
    private fun capture(root: Path): ExampleInventory {
        val (bundle, document) = pages.build()
        return DocumentedExamples.capture(
            root,
            bundle,
            document,
            ScenarioPlans.draft(document, "a".repeat(64)),
        )
    }

    private val data = "db.switches.insertMany( [ { _id: 0, a: Int32(3) } ] )"

    private fun aggregate(expression: String) =
        "db.switches.aggregate( [ { \$project: { result: $expression } } ] )"

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
        pages.operator("bitOr", "expression") {
            listOf(
                section(
                    "Examples",
                    code(data),
                    section(
                        "Bitwise ``OR`` with Two Integers",
                        code(aggregate("{ \$bitOr: [ \"\$a\", \"\$a\" ] }")),
                    ),
                    section(
                        "Renamed Since the Fixture",
                        code(aggregate("{ \$bitOr: [ \"\$a\", 7 ] }")),
                    ),
                    section(
                        "Bitwise ``OR`` with a Long and Integer",
                        code(aggregate("{ \$bitOr: [ \"\$a\", Long(\"63\") ] }")),
                        code("[ { _id: 0, result: Long(\"63\") } ]"),
                    ),
                )
            )
        }
        pages.operator("bitAnd", "expression") {
            listOf(section("Example", code(data), code(aggregate("{ \$bitAnd: [ \"\$a\", 1 ] }"))))
        }
        pages.operator("bitXor", "expression") {
            listOf(
                section(
                    "Examples",
                    code(data),
                    section("Use Two", code(aggregate("{ \$bitXor: [ \"\$a\", 1 ] }"))),
                    section("Use two", code(aggregate("{ \$bitXor: [ \"\$a\", 2 ] }"))),
                )
            )
        }
        pages.page("aggregation", "interface") {
            node(
                "root",
                children = listOf(section("Interface", node("text", text = "No directive here."))),
            )
        }
        return capture(root)
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

    /** Query and update pages generate find and update cases with the committed bindings. */
    @Test
    fun findsAndUpdatesAreGenerated() {
        val root = directory.resolve("repository")
        write(
            root.resolve("audits/catalog/fixture-migration.json"),
            AuditJson.write(FixtureMigrationLedger()),
        )
        val seed = "db.inventory.insertMany( [ { _id: 1, qty: Int32(25), tags: [ \"a\" ] } ] )"
        pages.operator("gt", "query", "query") {
            listOf(
                section(
                    "Examples",
                    code(seed),
                    section(
                        "Match Larger Quantities",
                        code("db.inventory.createIndex( { qty: 1 }, { name: \"byQty\" } )"),
                        code("db.inventory.find( { qty: { \$gt: 20 } } )"),
                    ),
                    section(
                        "Project the Quantity",
                        code("db.inventory.find( { qty: { \$gt: 20 } }, { qty: 1 } )"),
                    ),
                )
            )
        }
        pages.operator("inc", "update", "update") {
            listOf(
                section(
                    "Examples",
                    code(seed),
                    section(
                        "Increment and Tag",
                        code(
                            "db.inventory.updateOne( { _id: 1 }, { \$inc: { qty: 2 }, \$set: { tags: [ \"b\" ] } } )"
                        ),
                    ),
                    section(
                        "Increment in a Pipeline",
                        code("db.inventory.updateOne( { _id: 1 }, [ { \$set: { qty: 2 } } ] )"),
                    ),
                )
            )
        }
        val inventory = capture(root)
        val repository =
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isDirectory(it.resolve("audits/catalog")) }
        val generation =
            CaseGenerator.generate(
                inventory,
                AuditJson.dslBindings(Files.readString(repository.resolve(DslBindings.PATH))),
                GenerationReview(families = listOf("query/filters", "query/updates")),
                emptySet(),
            )
        assertEquals(
            listOf("query/filters/gt/matchLargerQuantities", "query/updates/inc/incrementAndTag"),
            generation.cases.map { it.manifest.name },
        )
        val find =
            generation.files.getValue(
                "core/src/test/java/dev/morphia/test/generated/filters/TestGt.java"
            )
        assertTrue("(query) -> query.filter(gt(\"qty\", 20)));" in find, find)
        // The page's index travels with the case; the runner creates it before the action.
        val found = generation.cases.first().manifest
        assertEquals(
            BsonDocument.parse(
                "{collection: 'inventory', keys: {qty: 1}, options: {name: 'byQty'}}"
            ),
            BsonDocument.parse(
                    generation.files.getValue("core/src/test/resources${found.scenario}")
                )
                .getArray("indexes")
                .single(),
        )
        val update =
            generation.files.getValue(
                "core/src/test/java/dev/morphia/test/generated/updates/TestInc.java"
            )
        val call =
            "(query) -> query.filter(eq(\"_id\", 1)),\n                inc(\"qty\", 2),\n" +
                "                set(\"tags\", List.of(\"b\")));"
        assertTrue("runner.update(" in update && call in update, update)
        assertEquals(
            mapOf(
                "Project the Quantity" to "a documented projection is not generated yet",
                "Increment in a Pipeline" to "/update: a pipeline update has no binding",
            ),
            generation.gaps.associate { it.title to it.detail },
        )
    }

    /**
     * A fixture named for an example owns it even when its title and action text have changed since
     * the fixture was written.
     */
    @Test
    fun aFixtureWithTheExamplesNameOwnsIt() {
        write(
            directory.resolve(
                "repository/core/src/test/resources/dev/morphia/test/aggregation/expressions/bitOr/" +
                    "bitwiseOrWithALongAndInteger/name"
            ),
            "Bitwise OR with a Long\n",
        )
        val example =
            inventory()
                .pages
                .single { it.operator == "bitOr" }
                .examples
                .single { it.title == "Bitwise ``OR`` with a Long and Integer" }
        assertEquals(
            Triple(
                ExampleStatus.OWNED,
                "aggregation/expressions/bitOr/bitwiseOrWithALongAndInteger",
                "matched by name",
            ),
            Triple(example.status, example.owner, example.detail),
        )
    }

    /** A page that repeats an example shows it once; the same action on other data is another. */
    @Test
    fun onlyIdenticalExamplesCollapse() {
        val root = directory.resolve("repository")
        write(
            root.resolve("audits/catalog/fixture-migration.json"),
            AuditJson.write(FixtureMigrationLedger()),
        )
        val query = aggregate("{ \$bitOr: [ \"\$a\", 1 ] }")
        pages.operator("bitOr", "expression") {
            listOf(
                section(
                    "Examples",
                    section("Same Name", code(data), code(query)),
                    section("Same Name", code(data), code(query)),
                    section(
                        "Same Name",
                        code("db.switches.insertMany( [ { _id: 1, a: Int32(5) } ] )"),
                        code(query),
                    ),
                )
            )
        }
        assertEquals(2, capture(root).pages.single().examples.size)
    }

    /** A drafted plan reads each operator page's examples section, tabs and highlighting. */
    @Test
    fun theDraftedPlanFollowsEachPage() {
        pages.operator("acos", "expression") {
            listOf(
                section(
                    "Examples",
                    code(data),
                    section(
                        "Inverse Cosine",
                        node(
                            "directive",
                            name = "tab",
                            options = mapOf("tabid" to "\"degrees\""),
                            children = listOf(code(aggregate("{ \$acos: \"\$a\" }"), "bash")),
                        ),
                    ),
                )
            )
        }
        pages.operator("abs", "expression") {
            listOf(section("Example", code(data), code(aggregate("{ \$abs: \"\$a\" }"))))
        }
        pages.operator("cos", "expression") { listOf(section("Behavior", code(data))) }
        val (_, document) = pages.build()
        val plan = ScenarioPlans.draft(document, "a".repeat(64))
        val rules = plan.pages.associateBy { it.id }
        assertEquals(
            setOf("aggregation/expressions/acos", "aggregation/expressions/abs"),
            rules.keys,
        )
        val acos = rules.getValue("aggregation/expressions/acos")
        assertEquals("\$acos", acos.scope.capability.key.token)
        assertEquals(listOf("examples"), acos.sections)
        val policy = acos.sectionAssembly!!
        // The examples section's own code is shared setup only when subsections follow it.
        assertEquals(listOf("examples"), policy.sharedSetupSections)
        assertEquals(listOf("degrees"), policy.scenarioTabs)
        assertEquals(listOf("bash"), policy.additionalShellLanguages)
        val abs = rules.getValue("aggregation/expressions/abs")
        assertEquals(listOf("example"), abs.sections)
        assertTrue(abs.sectionAssembly!!.sharedSetupSections.isEmpty())
    }

    /**
     * Examples that share their section's title take reviewed names; the title then names no owner,
     * so a fixture with that title owns neither of them.
     */
    @Test
    fun siblingsSharingATitleTakeReviewedNames() {
        val root = directory.resolve("repository")
        write(
            root.resolve("audits/catalog/fixture-migration.json"),
            AuditJson.write(FixtureMigrationLedger()),
        )
        write(
            root.resolve(
                "core/src/test/resources/dev/morphia/test/aggregation/expressions/bitOr/twoWays/name"
            ),
            "Two Ways\n",
        )
        pages.operator("bitOr", "expression") {
            listOf(
                section(
                    "Examples",
                    code(data),
                    section(
                        "Two Ways",
                        code(aggregate("{ \$bitOr: [ \"\$a\", 1 ] }")),
                        code(aggregate("{ \$bitOr: [ \"\$a\", 2 ] }")),
                    ),
                )
            )
        }
        val shared = capture(root).pages.single().examples
        assertEquals(
            listOf(ExampleStatus.AMBIGUOUS, ExampleStatus.AMBIGUOUS),
            shared.map { it.status },
        )
        write(
            directory.resolve("repository/${DocumentedExamples.REVIEWED}"),
            AuditJson.write(
                ReviewedExamples(
                    examples =
                        shared.mapIndexed { index, example ->
                            ReviewedExample(
                                "aggregation/expressions/bitOr",
                                "Two Ways",
                                example.blocks.single { it.role == BlockRole.ACTION }.sha256,
                                name = listOf("OR with One", "OR with Two")[index],
                                reason = "the section shows two examples",
                            )
                        }
                )
            ),
        )
        val named = capture(root).pages.single().examples
        assertEquals(
            listOf(
                Triple(ExampleStatus.UNOWNED, "orWithOne", null),
                Triple(ExampleStatus.UNOWNED, "orWithTwo", null),
            ),
            named.map { Triple(it.status, it.stem, it.owner) },
            named.toString(),
        )
    }

    /** A reviewed name stands in for a generic title; a stale review is counted, not applied. */
    @Test
    fun reviewedNamesResolveGenericTitles() {
        val first = inventory()
        val main =
            first.pages.single { it.operator == "bitAnd" }.examples.single { it.title == "main" }
        val action = main.blocks.single { it.role == BlockRole.ACTION }
        fun review(vararg examples: ReviewedExample) =
            write(
                directory.resolve("repository/${DocumentedExamples.REVIEWED}"),
                AuditJson.write(ReviewedExamples(examples = examples.toList())),
            )
        review(
            ReviewedExample(
                "aggregation/expressions/bitAnd",
                "main",
                action.sha256,
                name = "AND a Field with One",
                reason = "untitled",
            ),
            ReviewedExample(
                "aggregation/expressions/bitAnd",
                "main",
                "0".repeat(64),
                name = "Changed Since",
                reason = "its example changed",
            ),
        )
        val named = inventory()
        val example =
            named.pages.single { it.operator == "bitAnd" }.examples.single { it.title == "main" }
        assertEquals(
            Triple(ExampleStatus.UNOWNED, "andAFieldWithOne", "AND a Field with One"),
            Triple(example.status, example.stem, example.name),
        )
        assertEquals(1, named.summary["reviews.stale"])
        val source =
            CaseGenerator.generate(
                    named,
                    bindings.copy(
                        bindings =
                            bindings.bindings +
                                DslBinding(
                                    "\$bitAnd",
                                    BindingPosition.EXPRESSION,
                                    BindingForm.POSITIONAL,
                                    "$expressions.MathExpressions",
                                    "bitAnd",
                                    listOf("java.lang.Object", "java.lang.Object"),
                                )
                    ),
                    GenerationReview(families = listOf("aggregation/expressions")),
                    emptySet(),
                )
                .files
                .getValue(
                    "core/src/test/java/dev/morphia/test/generated/expressions/TestBitAnd.java"
                )
        assertTrue("@DisplayName(\"AND a Field with One\")" in source, source)
        // A review names an example or its owner, never both.
        review(
            ReviewedExample(
                "aggregation/expressions/bitAnd",
                "main",
                action.sha256,
                name = "x",
                owner = "y",
                reason = "both",
            )
        )
        assertThrows(IllegalArgumentException::class.java) { inventory() }
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

    /**
     * Sample-dataset examples, generated with the committed bindings: query filters, a group with a
     * ranked accumulator, and a sort. Each seeds nothing and queries `movies`.
     */
    @Test
    fun sampleDatasetExamplesCheckTheirSerialization() {
        val root = directory.resolve("repository")
        write(
            root.resolve("audits/catalog/fixture-migration.json"),
            AuditJson.write(FixtureMigrationLedger()),
        )
        fun ParsedPages.Page.movies(pipeline: String) = code("db.movies.aggregate( [ $pipeline ] )")
        val topRated =
            "{ \$match: { genres: \"Comedy\", year: { \$gte: 2010, \$lte: 2012 }, " +
                "rated: { \$in: [ \"G\", \"PG\" ] }, \"imdb.rating\": { \$exists: true } } },\n" +
                "  { \$group: { _id: \"\$year\", best: { \$top: { output: [ \"\$title\", " +
                "\"\$imdb.rating\" ], sortBy: { \"imdb.rating\": -1, title: 1 } } } } },\n" +
                "  { \$sort: { _id: 1 } }"
        pages.operator("top", "group") {
            listOf(
                section(
                    "Examples",
                    section(
                        "Top Rated by Year",
                        movies(topRated),
                        code("[ { _id: 2010, best: [ 'A', 9 ] } ]"),
                    ),
                    section(
                        "Grouped by Two Fields",
                        movies(
                            "{ \$group: { _id: { y: \"\$year\" }, best: { \$top: { output: \"\$title\", sortBy: { year: 1 } } } } }"
                        ),
                    ),
                    section(
                        "Without a Rating",
                        movies("{ \$match: { \"imdb.rating\": { \$exists: false } } }"),
                    ),
                    section("Typed Years", movies("{ \$match: { year: { \$type: \"int\" } } }")),
                )
            )
        }
        val inventory = capture(root)
        val repository =
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isDirectory(it.resolve("audits/catalog")) }
        val committed =
            AuditJson.dslBindings(Files.readString(repository.resolve(DslBindings.PATH)))
        val generation =
            CaseGenerator.generate(
                inventory,
                committed,
                GenerationReview(
                    families = listOf("aggregation/expressions"),
                    sampleDatasets = mapOf("movies" to "sample_mflix"),
                ),
                emptySet(),
            )
        val case = generation.cases.single().manifest
        assertEquals("aggregation/expressions/top/topRatedByYear", case.name)
        val source =
            generation.files.getValue(
                "core/src/test/java/dev/morphia/test/generated/expressions/TestTop.java"
            )
        val pipeline =
            "match(eq(\"genres\", \"Comedy\"), gte(\"year\", 2010), lte(\"year\", 2012), " +
                "in(\"rated\", List.of(\"G\", \"PG\")), exists(\"imdb.rating\")),\n" +
                "                        group(id(\"\$year\")).field(\"best\", top(array(\"\$title\", " +
                "\"\$imdb.rating\"), descending(\"imdb.rating\"), ascending(\"title\"))),\n" +
                "                        sort().ascending(\"_id\")"
        assertTrue(pipeline in source, source)
        assertTrue("import java.util.List;" in source, source)
        val scenario =
            BsonDocument.parse(generation.files.getValue("core/src/test/resources${case.scenario}"))
        assertEquals("sample_mflix", scenario.getString("dataset").value)
        assertTrue("sample_mflix" in scenario.getDocument("skipped").getString("results").value)
        assertEquals(
            // $top's named arguments may come in any order.
            listOf("EQ_SHORTHAND", "FIELD_OPERATORS_AND", "NAMED_ARGUMENTS"),
            scenario.getDocument("comparison").getArray("rules").map { it.asString().value },
        )
        assertEquals(
            mapOf(
                "Grouped by Two Fields" to "/pipeline/0/\$group/_id: a compound _id has no binding",
                "Without a Rating" to
                    "/pipeline/0/\$match/imdb.rating/\$exists: \$exists BsonBoolean{value=false} has no binding",
                "Typed Years" to
                    "/pipeline/0/\$match/year/\$type: query operator \$type has no binding",
            ),
            generation.gaps.associate { it.title to it.detail },
        )
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
        assertEquals(
            listOf(
                "aggregation/expressions",
                "aggregation/stages",
                "query/filters",
                "query/updates",
            ),
            review.families,
        )
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
