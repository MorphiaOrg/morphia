package dev.morphia.audits.generate

import dev.morphia.audits.repairs.ActionShape
import dev.morphia.audits.repairs.BlockRole
import dev.morphia.audits.repairs.FixtureMigration
import dev.morphia.audits.repairs.FormEquivalence
import dev.morphia.audits.repairs.FormMigration
import dev.morphia.audits.repairs.FormOverride
import dev.morphia.audits.repairs.FormRule
import dev.morphia.audits.repairs.GENERATED
import dev.morphia.audits.repairs.ManifestCase
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import org.bson.BsonBoolean
import org.bson.BsonDocument
import org.bson.BsonRegularExpression
import org.bson.BsonString
import org.bson.BsonValue

/** Why an example of a generated family has no generated case. */
enum class GapKind {
    /** The example has no owner, but its title is generic or shared, so it has no name. */
    UNNAMED,
    AMBIGUOUS,
    /** The documented source does not lower to a supported action. */
    SOURCE_UNSUPPORTED,
    /** A node of the operation has no binding; the detail names the first one. */
    UNBOUND,
    /**
     * The operator's class in the generated namespace is protected, so generation cannot add to it.
     */
    PROTECTED_CLASS,
    /** A reviewer excluded the case; the detail is the reason. */
    EXCLUDED,
}

data class GenerationGap(
    val context: String,
    val operator: String,
    val title: String,
    val kind: GapKind,
    val detail: String,
)

/** The gaps of one generation, as `candidate` writes them to `gaps.json`. */
data class GenerationGaps(val schemaVersion: Int = 1, val gaps: List<GenerationGap>)

/** A reviewed exclusion of a generated case, by its qualified name. */
data class Exclusion(val name: String, val reason: String)

/**
 * The reviewed scope of generation: the families (legacy packages) whose unowned examples are
 * generated, and the cases excluded from them, each with its reason. [sampleDatasets] maps a
 * collection of MongoDB's sample datasets to its dataset: an example that queries one without
 * seeding data is generated with that dataset, and only its serialization is checked.
 */
data class GenerationReview(
    val schemaVersion: Int = 1,
    val families: List<String>,
    val excluded: List<Exclusion> = emptyList(),
    val sampleDatasets: Map<String, String> = emptyMap(),
)

/** A generated case: its manifest entry, its scenario's text, and its test method's source. */
data class GeneratedCase(val manifest: ManifestCase, val scenario: String, val method: String)

data class Generation(
    /** Repository path to contents: every scenario and every operator's test class. */
    val files: Map<String, String>,
    val cases: List<GeneratedCase>,
    val gaps: List<GenerationGap>,
)

/**
 * Generates a test and a `scenario.json` for every unowned example of the reviewed families, from
 * the recorded source text alone. Every node of an operation must have a binding; otherwise the
 * example is a gap naming the first unbound node. Nothing is guessed: a value is emitted only in
 * the form its parameter type takes.
 */
object CaseGenerator {
    const val REVIEW = "audits/catalog/generation-review.json"
    private const val RESOURCES = "core/src/test/resources"
    private const val SOURCES = "core/src/test/java"
    private const val PACKAGE = "dev.morphia.test.generated"
    private const val OBJECT = "java.lang.Object"
    private const val STRING = "java.lang.String"
    private const val BOOLEAN = "java.lang.Boolean"
    private const val ITERABLE = "java.lang.Iterable"
    /** The java.util.regex flag of each regular expression option a query binding takes. */
    private val JAVA_FLAGS =
        mapOf(
            'i' to Pattern.CASE_INSENSITIVE,
            'm' to Pattern.MULTILINE,
            's' to Pattern.DOTALL,
            'x' to Pattern.COMMENTS,
        )

    /** A static call marked by [Emitter.static]: its owner and method. */
    private val STATIC = Regex("\u0001([^#\u0002]+)#([^\u0002]+)\u0002")
    private const val EXPRESSION = "dev.morphia.aggregation.expressions.impls.Expression"
    private const val FIND_OPTIONS = "dev.morphia.query.FindOptions"
    /** The `$meta` keywords Morphia's `Meta` has a factory for, each named as its keyword. */
    private val META = setOf("indexKey", "searchHighlights", "searchScore", "textScore")
    private const val LIST = "java.util.List"
    private const val NUMBER = "java.lang.Number"
    private const val SORTS = "dev.morphia.query.Sort[]"
    /** `$setWindowFields`' outputs, each `output(name).operator(op)` with an optional window. */
    private const val OUTPUTS = "dev.morphia.aggregation.stages.SetWindowFields.Output[]"
    private const val OUTPUT = "dev.morphia.aggregation.stages.SetWindowFields.Output"
    private const val TIME_UNIT = "dev.morphia.aggregation.expressions.TimeUnit"
    /** The binding key of an array literal in an expression. */
    const val ARRAY = "[]"
    /** The catalog entry binding a document literal (`Expressions.document`). */
    const val DOCUMENT = "{}"

    /**
     * [protected] holds the repository paths that generation must not write: moved handwritten
     * bodies the manifest protects.
     */
    fun generate(
        examples: ExampleInventory,
        catalog: DslBindingCatalog,
        review: GenerationReview,
        protected: Set<String>,
    ): Generation {
        val stages =
            catalog.bindings
                .filter { it.position == BindingPosition.STAGE }
                .associateBy { it.operator }
        val expressions =
            catalog.bindings
                .filter { it.position == BindingPosition.EXPRESSION }
                .associateBy { it.operator }
        val filters =
            catalog.bindings
                .filter { it.position == BindingPosition.FILTER }
                .associateBy { it.operator }
        val updates =
            catalog.bindings
                .filter { it.position == BindingPosition.UPDATE }
                .associateBy { it.operator }
        val excluded = review.excluded.associate { it.name to it.reason }
        val cases = mutableListOf<Pair<GeneratedCase, Emitter>>()
        val gaps = mutableListOf<GenerationGap>()
        val unused = excluded.keys.toMutableSet()
        examples.pages
            .filter { it.context in review.families }
            .forEach { page ->
                val folder = page.context.substringAfter('/')
                // A page name such as `maxN-array-element` becomes `TestMaxNArrayElement`.
                val type =
                    "Test" +
                        page.operator.split('-').joinToString("") { part ->
                            part.replaceFirstChar { it.uppercase() }
                        }
                val source = "$SOURCES/${PACKAGE.replace('.', '/')}/$folder/$type.java"
                page.examples.forEach { example ->
                    fun gap(kind: GapKind, detail: String) {
                        gaps +=
                            GenerationGap(
                                page.context,
                                page.operator,
                                example.name ?: example.title,
                                kind,
                                detail,
                            )
                    }
                    when (example.status) {
                        ExampleStatus.OWNED -> return@forEach
                        ExampleStatus.UNNAMED ->
                            return@forEach gap(GapKind.UNNAMED, example.detail ?: "")
                        ExampleStatus.AMBIGUOUS ->
                            return@forEach gap(GapKind.AMBIGUOUS, example.detail ?: "")
                        ExampleStatus.UNSUPPORTED ->
                            return@forEach gap(GapKind.SOURCE_UNSUPPORTED, example.detail ?: "")
                        ExampleStatus.UNOWNED -> Unit
                    }
                    val stem = example.stem!!
                    val name = "${page.context}/${page.operator}/$stem"
                    excluded[name]?.let {
                        unused -= name
                        return@forEach gap(GapKind.EXCLUDED, it)
                    }
                    if (source in protected) {
                        return@forEach gap(GapKind.PROTECTED_CLASS, "$source is protected")
                    }
                    val documented =
                        try {
                            FormMigration.documented(name, null, example.blocks)
                        } catch (failure: IllegalArgumentException) {
                            return@forEach gap(
                                GapKind.SOURCE_UNSUPPORTED,
                                failure.message!!.removePrefix("$name: "),
                            )
                        }
                    // The runner refuses a stage that reads or writes another database, since the
                    // two behavior runs would share it.
                    if (documented.shape == ActionShape.PIPELINE) {
                        documented.action.firstOrNull(::namesADatabase)?.let { stage ->
                            return@forEach gap(
                                GapKind.SOURCE_UNSUPPORTED,
                                "a stage naming another database: ${stage.asDocument().firstKey}",
                            )
                        }
                    }
                    val emitted = Emitter(stages, expressions, filters, updates)
                    val resource =
                        FixtureMigration.scenarioPath(name, GENERATED).removePrefix(RESOURCES)
                    val indent = "\n                        "
                    val call =
                        try {
                            when (documented.shape) {
                                ActionShape.PIPELINE -> {
                                    val pipeline =
                                        documented.action.mapIndexed { index, stage ->
                                            emitted.stage(stage, "/pipeline/$index")
                                        }
                                    "runner.aggregate(\n                OperatorCase.of(${literal(resource)}),\n" +
                                        "                (aggregation) -> aggregation.pipeline($indent" +
                                        pipeline.joinToString(",$indent") +
                                        "));"
                                }
                                ActionShape.FIND -> {
                                    val options =
                                        documented.action.getOrNull(1)?.let {
                                            emitted.projection(it, "/projection")
                                        }
                                    "runner.find(\n                OperatorCase.of(${literal(resource)}),\n" +
                                        "                ${query(emitted.filters(documented.action[0], "/filter"))}" +
                                        (options?.let { ",\n                $it" } ?: "") +
                                        ");"
                                }
                                ActionShape.UPDATE -> {
                                    val operators = emitted.updates(documented.action[1], "/update")
                                    "runner.update(\n                OperatorCase.of(${literal(resource)}),\n" +
                                        "                ${query(emitted.filters(documented.action[0], "/filter"))}," +
                                        operators.joinToString(",") { "\n                $it" } +
                                        ");"
                                }
                            }
                        } catch (unbound: Unbound) {
                            return@forEach gap(GapKind.UNBOUND, unbound.message!!)
                        }
                    // An example that seeds nothing and queries a sample collection runs against
                    // that dataset, which the tests do not load.
                    val dataset =
                        review.sampleDatasets[documented.collection].takeIf {
                            example.blocks.none { it.role == BlockRole.DATA }
                        }
                    // Results from a collection nothing seeds were printed from data the example
                    // does not show.
                    if (
                        dataset == null &&
                            documented.data.values.all { it.isEmpty() } &&
                            documented.printed?.isNotEmpty() == true
                    ) {
                        return@forEach gap(
                            GapKind.SOURCE_UNSUPPORTED,
                            "the page prints results for ${documented.collection}, which nothing seeds",
                        )
                    }
                    val override =
                        FormOverride(
                            fixture = name,
                            origin = page.source.commit,
                            source = page.source,
                            pageBlob = page.pageBlob,
                            includes = page.includes,
                            unexpandedIncludes = emptyList(),
                            // A reviewed name is the case's title, as its display name is.
                            example = example.name ?: example.title,
                            shape = documented.shape,
                            blocks = example.blocks,
                            rules = emitted.rules.sorted(),
                            retiredLock = null,
                            fixtureFiles = emptyMap(),
                        )
                    val method =
                        """
                        |    @Test
                        |    @DisplayName(${literal(example.name ?: example.title)})
                        |    public void $stem() {
                        |        $call
                        |    }
                        |"""
                            .trimMargin()
                    cases +=
                        GeneratedCase(
                            ManifestCase(name, GENERATED, resource, "$PACKAGE.$folder.$type#$stem"),
                            FixtureMigration.scenario(override, ordered = true, dataset = dataset),
                            method,
                        ) to emitted
                }
            }
        require(unused.isEmpty()) {
            "Exclusions of cases that are not generated: ${unused.sorted()}"
        }
        val files = sortedMapOf<String, String>()
        cases
            .groupBy { (case, _) -> case.manifest.test.substringBefore('#') }
            .forEach { (type, members) ->
                val path = "$SOURCES/${type.replace('.', '/')}.java"
                files[path] = type(type, members.map { it.first }, members.map { it.second })
                members.forEach { (case, _) ->
                    files["$RESOURCES${case.manifest.scenario}"] = case.scenario
                }
            }
        return Generation(
            files,
            cases.map { it.first }.sortedBy { it.manifest.name },
            gaps.sortedWith(compareBy({ it.context }, { it.operator }, { it.title })),
        )
    }

    /**
     * Whether [value] has a `db` key at any depth, as `OperatorCaseRunner` decides a stage names
     * another database.
     */
    private fun namesADatabase(value: BsonValue): Boolean =
        when {
            value.isDocument ->
                "db" in value.asDocument() || value.asDocument().values.any(::namesADatabase)
            value.isArray -> value.asArray().values.any(::namesADatabase)
            else -> false
        }

    /** The query function of a find or an update: its filters, or the query itself for none. */
    private fun query(filters: List<String>): String =
        if (filters.isEmpty()) "(query) -> query"
        else "(query) -> query.filter(${filters.joinToString(", ")})"

    private fun type(
        qualified: String,
        cases: List<GeneratedCase>,
        emitted: List<Emitter>,
    ): String {
        val calls = emitted.flatMap { it.calls }.toSet()
        // A method name two owners share is called through its class.
        val shared = calls.groupBy { it.second }.filterValues { it.size > 1 }.keys
        val classes =
            (emitted.flatMap { it.types } + calls.filter { it.second in shared }.map { it.first })
                .toSortedSet()
        val statics =
            calls.filter { it.second !in shared }.map { "${it.first}.${it.second}" }.sorted()
        val imports =
            (classes +
                    listOf(
                        "dev.morphia.test.OperatorCase",
                        "dev.morphia.test.OperatorCaseRunner",
                        "dev.morphia.test.TestBase",
                        "org.junit.jupiter.api.DisplayName",
                        "org.junit.jupiter.api.Test",
                    ))
                .toSortedSet()
        val groups =
            listOf("java.", "javax.", "com.", "dev.", "edu.", "org.", "net.").map { prefix ->
                imports.filter { it.startsWith(prefix) }
            } +
                listOf(
                    imports.filter { name ->
                        listOf("java.", "javax.", "com.", "dev.", "edu.", "org.", "net.").none {
                            name.startsWith(it)
                        }
                    }
                )
        val header =
            (groups
                    .filter { it.isNotEmpty() }
                    .map { group -> group.joinToString("\n") { "import $it;" } } +
                    listOfNotNull(
                        statics
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString("\n") { "import static $it;" }
                    ))
                .joinToString("\n\n")
        val methods =
            STATIC.replace(cases.sortedBy { it.manifest.name }.joinToString("\n") { it.method }) {
                val (owner, method) = it.destructured
                if (method in shared) "${owner.substringAfterLast('.')}.$method" else method
            }
        val simple = qualified.substringAfterLast('.')
        // Generated code keeps its own layout: the formatter would rewrap it, and the manifest
        // pins each file's bytes.
        return """
            |// spotless:off
            |package ${qualified.substringBeforeLast('.')};
            |
            |$header
            |
            |/** Generated by GenerateMainKt from audits/catalog/documented-examples.json; do not edit. */
            |public class $simple extends TestBase {
            |    private final OperatorCaseRunner runner = new OperatorCaseRunner(this);
            |
            |$methods}
            |// spotless:on
            |"""
            .trimMargin()
    }

    private class Unbound(message: String) : RuntimeException(message)

    /** Emits one operation's Java, recording the static methods and types it uses. */
    private class Emitter(
        val stages: Map<String, DslBinding>,
        val expressions: Map<String, DslBinding>,
        val filters: Map<String, DslBinding>,
        val updates: Map<String, DslBinding>,
    ) {
        /** Owner and method of every factory called. */
        val calls = mutableSetOf<Pair<String, String>>()

        /**
         * A static factory's name, recorded and marked for the class to resolve: a name two owners
         * share is called through its class (`Filters.eq` and `ComparisonExpressions.eq`).
         */
        fun static(owner: String, method: String): String {
            calls += owner to method
            return "\u0001$owner#$method\u0002"
        }

        /** Types named in the source, such as an enum argument's. */
        val types = mutableSetOf<String>()
        /**
         * The form rules the emitted spelling needs: Morphia writes an implicit equality as `$eq`,
         * several operators on one field as one filter each, and named arguments in its own order.
         */
        val rules = mutableSetOf<FormRule>()

        fun stage(value: BsonValue, path: String): String {
            val (operator, argument) =
                operator(value, path) ?: throw Unbound("$path: not a stage document")
            val binding = stages[operator] ?: throw Unbound("$path: stage $operator has no binding")
            return call(binding, argument, "$path/$operator")
        }

        private fun operator(value: BsonValue, path: String): Pair<String, BsonValue>? {
            if (!value.isDocument) return null
            val document = value.asDocument()
            val key = document.keys.singleOrNull() ?: return null
            if (!key.startsWith("$")) return null
            return key to document[key]!!
        }

        private fun call(binding: DslBinding, argument: BsonValue, path: String): String {
            val factory = "${static(binding.owner, binding.method)}(%s)"
            return when (binding.form) {
                BindingForm.POSITIONAL -> {
                    // `$cond: {if, then, else}` is `$cond: [if, then, else]`, which Morphia writes.
                    val condition =
                        argument
                            .takeIf {
                                binding.operator == "\$cond" &&
                                    it.isDocument &&
                                    it.asDocument().keys == setOf("if", "then", "else")
                            }
                            ?.asDocument()
                            ?.let { listOf(it["if"]!!, it["then"]!!, it["else"]!!) }
                    if (condition != null) rules += FormRule.COND_ARRAY
                    // `{$op: [a]}` is `{$op: a}` for an operator of one argument, which Morphia
                    // writes bare; SINGLE_ARGUMENT_ARRAY equates them unless `a` is an array.
                    if (
                        !binding.varargs &&
                            binding.parameters.size == 1 &&
                            argument.isArray &&
                            argument.asArray().size == 1
                    ) {
                        if (
                            binding.operator !in FormEquivalence.singleArgumentOperators ||
                                argument.asArray()[0].isArray
                        )
                            throw Unbound(
                                "$path: Morphia writes ${binding.operator}'s argument without its array"
                            )
                        rules += FormRule.SINGLE_ARGUMENT_ARRAY
                    }
                    // A varargs factory given one argument writes it bare (`sum("$a")` is
                    // `$sum: "$a"`), so a one-item array has no binding; `array` and
                    // `concatArrays` always write an array.
                    if (
                        binding.varargs &&
                            binding.operator !in setOf(ARRAY, "\$concatArrays") &&
                            argument.isArray &&
                            argument.asArray().size == 1
                    )
                        throw Unbound(
                            "$path: Morphia writes ${binding.operator}'s lone argument bare"
                        )
                    // `concatArrays` always writes an array; its accumulator form takes one bare.
                    if (binding.operator == "\$concatArrays" && !argument.isArray)
                        throw Unbound("$path: Morphia writes \$concatArrays' argument as an array")
                    val values =
                        condition
                            ?: if (argument.isArray) argument.asArray().values else listOf(argument)
                    // `condition` builds a `List.of`, which rejects null.
                    if (binding.operator == "\$cond" && values.any { it.isNull })
                        throw Unbound("$path: Morphia's condition() rejects a null operand")
                    val fixed =
                        if (binding.varargs) binding.parameters.size - 1
                        else binding.parameters.size
                    if (values.size < fixed || (!binding.varargs && values.size != fixed)) {
                        throw Unbound(
                            "$path: ${values.size} arguments for ${binding.method}${binding.parameters}"
                        )
                    }
                    // Java reads a lone null as the varargs array itself.
                    if (binding.varargs && values.size == fixed + 1 && values.last().isNull)
                        throw Unbound("$path: Java passes a lone null as the varargs array")
                    val component = binding.parameters.last().removeSuffix("[]")
                    factory.format(
                        values
                            .mapIndexed { index, each ->
                                value(
                                    each,
                                    if (index < fixed) binding.parameters[index] else component,
                                    "$path/$index",
                                )
                            }
                            .joinToString(", ")
                    )
                }
                BindingForm.NAMED -> {
                    if (!argument.isDocument) {
                        if (!binding.shorthand)
                            throw Unbound("$path: ${binding.operator} takes a document")
                        return factory.format(value(argument, binding.parameters.single(), path))
                    }
                    // The server reads named arguments in any order, and the generator cannot know
                    // the order Morphia writes them in.
                    if (binding.operator in FormEquivalence.namedArgumentOperators)
                        rules += FormRule.NAMED_ARGUMENTS
                    val document =
                        if (binding.operator in FormEquivalence.regexOperators)
                            regexArgument(argument.asDocument(), path)
                        else argument.asDocument()
                    // Without options, Morphia writes a shorthand operator in its short form.
                    if (binding.shorthand && document.keys.all { it in binding.arguments }) {
                        if (binding.operator != "\$unwind")
                            throw Unbound(
                                "$path: Morphia writes ${binding.operator} without options as its shorthand"
                            )
                        rules += FormRule.UNWIND_PATH
                    }
                    val missing = binding.arguments.filter { it !in document }
                    if (missing.isNotEmpty())
                        throw Unbound("$path: ${binding.operator} without $missing")
                    val head =
                        factory.format(
                            binding.arguments
                                .mapIndexed { index, key ->
                                    value(document[key]!!, binding.parameters[index], "$path/$key")
                                }
                                .joinToString(", ")
                        )
                    head +
                        document.entries
                            .filter { it.key !in binding.arguments }
                            .joinToString("") { (key, each) ->
                                val option =
                                    binding.options[key]
                                        ?: throw Unbound(
                                            "$path/$key: ${binding.operator} option $key has no binding"
                                        )
                                // `TextSearchFilterCodec` writes these flags only when true.
                                if (binding.operator == "\$text" && each == BsonBoolean.FALSE)
                                    throw Unbound("$path/$key: Morphia omits $key: false")
                                ".${option.method}(${arguments(option, each, "$path/$key")})"
                            }
                }
                BindingForm.FIELDS -> {
                    if (!argument.isDocument)
                        throw Unbound("$path: ${binding.operator} takes a document")
                    val method = binding.fields.getValue("value").method
                    factory.format("") +
                        argument.asDocument().entries.joinToString("") { (field, each) ->
                            ".$method(${literal(field)}, ${value(each, OBJECT, "$path/$field")})"
                        }
                }
                BindingForm.FILTERS -> factory.format(filters(argument, path).joinToString(", "))
                BindingForm.FIELD ->
                    throw Unbound("$path: ${binding.operator} is a query operator, not a value")
                BindingForm.GROUP -> {
                    if (!argument.isDocument || "_id" !in argument.asDocument())
                        throw Unbound("$path: ${binding.operator} takes a document with an _id")
                    val id = binding.fields.getValue("id")
                    val group = argument.asDocument()
                    val key = group["_id"]!!
                    factory.format(
                        "${static(binding.owner, id.method)}(${value(key, OBJECT, "$path/_id")})"
                    ) +
                        group.entries
                            .filter { it.key != "_id" }
                            .joinToString("") { (field, each) ->
                                val method = binding.fields.getValue("value").method
                                ".$method(${literal(field)}, ${value(each, OBJECT, "$path/$field")})"
                            }
                }
                BindingForm.SORT -> {
                    if (!argument.isDocument)
                        throw Unbound("$path: ${binding.operator} takes a document")
                    factory.format("") +
                        argument.asDocument().entries.joinToString("") { (field, each) ->
                            val direction =
                                when {
                                    each.isNumber && each.asNumber().doubleValue() == 1.0 ->
                                        "ascending"
                                    each.isNumber && each.asNumber().doubleValue() == -1.0 ->
                                        "descending"
                                    else ->
                                        throw Unbound(
                                            "$path/$field: a sort by $each has no binding"
                                        )
                                }
                            ".${binding.fields.getValue(direction).method}(${literal(field)})"
                        }
                }
                BindingForm.PROJECTION -> {
                    if (!argument.isDocument)
                        throw Unbound("$path: ${binding.operator} takes a document")
                    // `Projection` rejects exclusions beside inclusions or values, except _id's.
                    val kinds =
                        argument.asDocument().entries.map { (field, each) ->
                            (field == "_id" && flag(each) == false) to flag(each)
                        }
                    if (
                        kinds.any { (id, flag) -> !id && flag == false } &&
                            kinds.any { (_, flag) -> flag != false }
                    )
                        throw Unbound(
                            "$path: Morphia rejects exclusions beside inclusions or values"
                        )
                    // The projection writes a suppressed _id first, wherever the page put it.
                    if (argument.asDocument().keys.indexOf("_id") > 0)
                        rules += FormRule.PROJECT_ID_FIRST
                    factory.format("") +
                        argument.asDocument().entries.joinToString("") { (field, each) ->
                            val flag = flag(each)
                            val key =
                                when {
                                    field == "_id" && flag == false -> "suppressId"
                                    flag == true -> "include"
                                    flag == false -> "exclude"
                                    else -> "value"
                                }
                            val method = binding.fields.getValue(key).method
                            when (key) {
                                "suppressId" -> ".$method()"
                                "value" ->
                                    ".$method(${literal(field)}, ${value(each, OBJECT, "$path/$field")})"
                                else -> ".$method(${literal(field)})"
                            }
                        }
                }
            }
        }

        /**
         * A regex expression's argument with its pattern as a string and any flags of a regular
         * expression in `options`, as Morphia's `pattern(String)` and `options(String)` write them.
         */
        private fun regexArgument(document: BsonDocument, path: String): BsonDocument {
            val regex = document["regex"] ?: return document
            // `pattern(String)` and `options(String)` take literals; a field path or an expression
            // there is evaluated by the server, so it has no binding.
            val (pattern, flags) =
                when {
                    regex.isRegularExpression ->
                        regex.asRegularExpression().let { it.pattern to it.options }
                    regex.isString && !regex.asString().value.startsWith("$") ->
                        regex.asString().value to ""
                    else -> throw Unbound("$path/regex: a computed pattern has no binding")
                }
            document["options"]?.let { options ->
                if (!options.isString || options.asString().value.startsWith("$"))
                    throw Unbound("$path/options: computed options have no binding")
            }
            if (flags.isNotEmpty() && "options" in document)
                throw Unbound("$path/regex: the server rejects flags in both regex and options")
            // Morphia writes the pattern as a regular expression and its flags in `options`.
            if (regex.isString || flags.isNotEmpty()) rules += FormRule.REGEX_OPTIONS
            return BsonDocument().also { copy ->
                document.forEach { (key, each) ->
                    copy[key] = if (key == "regex") BsonString(pattern) else each
                }
                if (flags.isNotEmpty()) copy["options"] = BsonString(flags)
            }
        }

        /**
         * The arguments of an option call: its one parameter's value, or, when its last parameter
         * is an array (`on(String, String...)`), a single value or an array's items in order.
         */
        private fun arguments(option: MethodRef, value: BsonValue, path: String): String {
            val parameters = option.parameters
            if (parameters.size == 1) return value(value, parameters.single(), path)
            if (!parameters.last().endsWith("[]"))
                throw Unbound("$path: ${option.method}$parameters takes no single value")
            val values = if (value.isArray) value.asArray().values else listOf(value)
            // Morphia writes one value alone (`MergeCodec`'s `on: "_id"`), never a one-item array.
            if (value.isArray && values.size == 1)
                throw Unbound("$path: Morphia writes a one-item ${option.method} as its item")
            val fixed = parameters.size - 1
            if (values.size < fixed)
                throw Unbound("$path: ${values.size} arguments for ${option.method}$parameters")
            val component = parameters.last().removeSuffix("[]")
            return values
                .mapIndexed { index, each ->
                    value(each, if (index < fixed) parameters[index] else component, "$path/$index")
                }
                .joinToString(", ")
        }

        /**
         * A find's documented projection as Morphia's `FindOptions`, or null for an empty one,
         * which the runner compares as it is. Morphia writes the inclusions, then the exclusions,
         * each in order, as `1` and `0`.
         */
        fun projection(projection: BsonValue, path: String): String? {
            if (!projection.isDocument) throw Unbound("$path: not a projection document")
            val fields = projection.asDocument()
            if (fields.isEmpty()) return null
            // `{f: {$meta: "textScore"}}` alone is `project(Meta.textScore("f"))`; Morphia rejects
            // a metadata projection beside any other.
            fields.entries.singleOrNull()?.let { (field, value) ->
                val keyword =
                    value
                        .takeIf { it.isDocument && it.asDocument().keys == setOf("\$meta") }
                        ?.asDocument()
                        ?.get("\$meta")
                        ?.takeIf { it.isString }
                        ?.asString()
                        ?.value
                if (keyword != null) {
                    if (keyword !in META)
                        throw Unbound("$path/$field: Meta has no factory for \$meta \"$keyword\"")
                    types += FIND_OPTIONS
                    types += "dev.morphia.query.Meta"
                    return "new FindOptions().projection().project(Meta.$keyword(${literal(field)}))"
                }
            }
            val flags =
                fields.entries.map { (field, value) ->
                    if (value.isBoolean) rules += FormRule.PROJECTION_FLAGS
                    // `Projection.map` writes Int32 flags; only booleans have a rule equating them.
                    if (value.isInt64 || value.isDouble)
                        throw Unbound(
                            "$path/$field: Morphia writes a flag of ${value.bsonType} as an Int32"
                        )
                    field to
                        (flag(value)
                            ?: throw Unbound("$path/$field: a projection of $value has no binding"))
                }
            val includes = flags.filter { it.second }.map { it.first }
            val excludes = flags.filter { !it.second }.map { it.first }
            if (includes.isNotEmpty() && excludes.any { it != "_id" })
                throw Unbound("$path: Morphia rejects inclusions beside exclusions other than _id")
            // Only a lone _id exclusion can follow an inclusion it preceded, and the server ignores
            // where _id sits.
            if (includes + excludes != flags.map { it.first }) rules += FormRule.PROJECT_ID_FIRST
            types += FIND_OPTIONS
            return "new FindOptions()" +
                listOf("include" to includes, "exclude" to excludes)
                    .filter { it.second.isNotEmpty() }
                    .joinToString("") { (method, names) ->
                        ".projection().$method(${names.joinToString(", ") { literal(it) }})"
                    }
        }

        /** The filters of a query document, one per field operator, in order. */
        fun filters(query: BsonValue, path: String): List<String> {
            if (!query.isDocument) throw Unbound("$path: not a query document")
            return query.asDocument().entries.flatMap { (field, predicate) ->
                predicates(field, predicate, "$path/$field")
            }
        }

        /** The update operators of an update document, one call per field, in order. */
        fun updates(update: BsonValue, path: String): List<String> {
            if (!update.isDocument) throw Unbound("$path: a pipeline update has no binding")
            return update.asDocument().entries.flatMap { (operator, fields) ->
                val binding =
                    updates[operator]
                        ?: throw Unbound(
                            "$path/$operator: update operator $operator has no binding"
                        )
                if (!fields.isDocument) throw Unbound("$path/$operator: $operator takes a document")
                val name = static(binding.owner, binding.method)
                fields.asDocument().entries.map { (field, value) ->
                    // A field-only factory (`$unset`'s `unset(String, String...)`) ignores the
                    // value.
                    if (binding.parameters.size == 1 || binding.varargs) "$name(${literal(field)})"
                    else
                        "$name(${literal(field)}, " +
                            "${value(value, binding.parameters[1], "$path/$operator/$field", plain = true)})"
                }
            }
        }

        /** The filters of one field's predicate: one per operator, or `$eq` for a plain value. */
        private fun predicates(field: String, predicate: BsonValue, path: String): List<String> {
            if (field.startsWith("$")) {
                // A top-level operator (`$expr`, `$text`) reads its value as its binding's form.
                val binding =
                    filters[field]?.takeIf { it.form != BindingForm.FIELD }
                        ?: throw Unbound("$path: query operator $field has no binding")
                // `{$expr: [e]}` is an array value, which is truthy, not an argument list.
                if (binding.form == BindingForm.POSITIONAL && predicate.isArray)
                    throw Unbound("$path: $field takes one expression, not an array")
                return listOf(call(binding, predicate, path))
            }
            val operators =
                predicate
                    .takeIf { it.isDocument }
                    ?.asDocument()
                    ?.takeIf { it.isNotEmpty() && it.keys.all { key -> key.startsWith("$") } }
            if (operators == null) {
                rules += FormRule.EQ_SHORTHAND
                return listOf(filter("\$eq", field, predicate, path))
            }
            val entries = regexOptions(operators, path)
            // REGEX_FORM equates a string pattern only in a predicate of `$regex` alone.
            if (entries.size > 1 && entries["\$regex"]?.isString == true)
                throw Unbound("$path/\$regex: a string \$regex beside other operators")
            if (entries.size > 1) rules += FormRule.FIELD_OPERATORS_AND
            return entries.entries.map { (operator, value) ->
                filter(operator, field, value, "$path/$operator")
            }
        }

        /**
         * A predicate's operators with `{$regex: "p", $options: "o"}` or `{$regex: /p/, $options:
         * "o"}` as the one regular expression `/p/o` that Morphia writes.
         */
        private fun regexOptions(operators: BsonDocument, path: String): BsonDocument {
            val options = operators["\$options"] ?: return operators
            // REGEX_FORM equates a predicate of only `$regex` and `$options`.
            if (operators.size > 2)
                throw Unbound("$path/\$options: \$options beside operators other than \$regex")
            val regex = operators["\$regex"]
            val pattern =
                when {
                    regex == null || !options.isString -> null
                    regex.isString -> regex.asString().value
                    // The server rejects flags in both places.
                    regex.isRegularExpression && regex.asRegularExpression().options.isEmpty() ->
                        regex.asRegularExpression().pattern
                    else -> null
                }
                    ?: throw Unbound(
                        "$path/\$options: \$options takes a string, with a \$regex without flags"
                    )
            rules += FormRule.REGEX_FORM
            return BsonDocument().also { copy ->
                operators.forEach { (operator, value) ->
                    when (operator) {
                        "\$regex" ->
                            copy[operator] =
                                BsonRegularExpression(pattern, options.asString().value)
                        "\$options" -> {}
                        else -> copy[operator] = value
                    }
                }
            }
        }

        private fun filter(
            operator: String,
            field: String,
            value: BsonValue,
            path: String,
        ): String {
            val binding =
                filters[operator] ?: throw Unbound("$path: query operator $operator has no binding")
            val name = static(binding.owner, binding.method)
            if (binding.parameters.size == 1) {
                if (value != BsonBoolean.TRUE)
                    throw Unbound("$path: $operator $value has no binding")
                return "$name(${literal(field)})"
            }
            if (binding.options.isNotEmpty()) return regex(binding, field, value, path)
            if (binding.parameters.size > 2) {
                // `{f: {$mod: [d, r]}}` spreads its array over the parameters after the field.
                val values = if (value.isArray) value.asArray().values else emptyList()
                if (values.size != binding.parameters.size - 1)
                    throw Unbound(
                        "$path: ${binding.method}${binding.parameters} takes an array of ${binding.parameters.size - 1}"
                    )
                // `ModFilterCodec` writes a long that fits in an int as an Int32.
                values.forEachIndexed { index, each ->
                    if (each.isInt64)
                        throw Unbound(
                            "$path/$index: Morphia writes ${each.asInt64().value}L as an Int32"
                        )
                }
                return "$name(${literal(field)}, " +
                    values
                        .mapIndexed { index, each ->
                            constant(binding, each, binding.parameters[index + 1], "$path/$index")
                        }
                        .joinToString(", ") +
                    ")"
            }
            if (binding.varargs) {
                // `type(f, Type...)` writes one value alone and several as an array.
                val values = if (value.isArray) value.asArray().values else listOf(value)
                if (value.isArray && values.size < 2)
                    throw Unbound("$path: Morphia writes a ${values.size}-item $operator otherwise")
                val component = binding.parameters[1].removeSuffix("[]")
                return "$name(${literal(field)}, " +
                    values
                        .mapIndexed { index, each ->
                            constant(
                                binding,
                                each,
                                component,
                                if (value.isArray) "$path/$index" else path,
                            )
                        }
                        .joinToString(", ") +
                    ")"
            }
            return "$name(${literal(field)}, ${constant(binding, value, binding.parameters[1], path)})"
        }

        /** [value] as [type], through [binding]'s constants when it has them. */
        private fun constant(
            binding: DslBinding,
            value: BsonValue,
            type: String,
            path: String,
        ): String {
            if (binding.constants.isEmpty()) return value(value, type, path, plain = true)
            val name =
                value.takeIf { it.isString }?.let { binding.constants[it.asString().value] }
                    ?: throw Unbound(
                        "$path: ${type.substringAfterLast('.')} has no constant for ${binding.operator} ${display(value)}"
                    )
            types += type
            return "${type.substringAfterLast('.')}.$name"
        }

        /**
         * A query regular expression: the pattern, then an option call per flag. A string pattern
         * is the same match as a regular expression, which is what Morphia writes.
         */
        private fun regex(
            binding: DslBinding,
            field: String,
            value: BsonValue,
            path: String,
        ): String {
            val (pattern, flags) =
                when {
                    value.isRegularExpression ->
                        value.asRegularExpression().let { it.pattern to it.options }
                    value.isString -> {
                        rules += FormRule.REGEX_FORM
                        value.asString().value to ""
                    }
                    else ->
                        throw Unbound(
                            "$path: ${binding.operator} takes a pattern, not ${value.bsonType}"
                        )
                }
            // Morphia writes each flag once.
            if (flags.toSet().size != flags.length)
                throw Unbound("$path: Morphia writes ${binding.operator}'s repeated flags once")
            val options =
                flags.map { flag ->
                    binding.options[flag.toString()]
                        ?: throw Unbound("$path: ${binding.operator} option $flag has no binding")
                }
            // Morphia compiles the pattern with java.util.regex, whose syntax is not the server's,
            // and the driver writes the compiled pattern's flags, into which Java folds the
            // pattern's own final modifiers (`(?i)abc` is written `/(?i)abc/i`).
            val requested = flags.fold(0) { bits, flag -> bits or (JAVA_FLAGS[flag] ?: 0) }
            // `regex(f, p)` compiles the pattern alone before any option call adds a flag.
            val compiled =
                try {
                    Pattern.compile(pattern)
                    Pattern.compile(pattern, requested)
                } catch (e: PatternSyntaxException) {
                    throw Unbound(
                        "$path: java.util.regex rejects ${literal(pattern)}: ${e.description}"
                    )
                }
            if (compiled.flags() != requested)
                throw Unbound(
                    "$path: java.util.regex folds ${literal(pattern)}'s modifiers into its flags"
                )
            return "${static(binding.owner, binding.method)}(${literal(field)}, ${literal(pattern)})" +
                options.joinToString("") { ".${it.method}()" }
        }

        /**
         * `$setWindowFields`' `output: {name: {$op: v, window: {documents | range, unit}}, …}` as
         * `output(name).operator(op)` calls, each with `.window().documents(l, u)` or
         * `.window().range(l, u[, unit])`, in order. `SetWindowFieldsCodec` writes the operator,
         * then the window's documents, range and unit.
         */
        private fun outputs(outputs: BsonDocument, path: String): String =
            outputs.entries.joinToString(", ") { (name, spec) ->
                val at = "$path/$name"
                if (!spec.isDocument) throw Unbound("$at: an output takes a document")
                val fields = spec.asDocument()
                val operators = fields.keys.filter { it.startsWith("$") }
                val operator =
                    operators.singleOrNull()
                        ?: throw Unbound("$at: an output takes one operator, not $operators")
                fields.keys
                    .firstOrNull { it != operator && it != "window" }
                    ?.let { throw Unbound("$at/$it: an output's $it has no binding") }
                if (fields.keys.first() != operator)
                    throw Unbound("$at: Morphia writes an output's operator before its window")
                val binding =
                    expressions[operator]
                        ?: throw Unbound("$at/$operator: $operator has no binding")
                val call =
                    "${static(OUTPUT, "output")}(${literal(name)})" +
                        ".operator(${call(binding, fields[operator]!!, "$at/$operator")})"
                call + (fields["window"]?.let { window(it, "$at/window") } ?: "")
            }

        private fun window(window: BsonValue, path: String): String {
            if (!window.isDocument) throw Unbound("$path: a window takes a document")
            val fields = window.asDocument()
            val bounds = listOf("documents", "range").filter { it in fields }
            val kind =
                bounds.singleOrNull()
                    ?: throw Unbound("$path: a window takes documents or range, not $bounds")
            val allowed = if (kind == "range") setOf("range", "unit") else setOf("documents")
            fields.keys
                .firstOrNull { it !in allowed }
                ?.let { throw Unbound("$path/$it: a window's $it has no binding") }
            if (fields.keys.toList() != allowed.filter { it in fields })
                throw Unbound("$path: Morphia writes a window's range before its unit")
            val pair = fields[kind]!!
            if (!pair.isArray || pair.asArray().size != 2)
                throw Unbound("$path/$kind: a window's $kind takes [lower, upper]")
            val (lower, upper) =
                pair.asArray().values.mapIndexed { index, each ->
                    if (!(each.isString || each.isInt32))
                        throw Unbound("$path/$kind/$index: a ${each.bsonType} bound has no binding")
                    value(each, OBJECT, "$path/$kind/$index")
                }
            val unit = fields["unit"]?.let { ", ${value(it, TIME_UNIT, "$path/unit")}" } ?: ""
            return ".window().$kind($lower, $upper$unit)"
        }

        /** Whether [value] is an array holding a document at any depth of nested arrays. */
        private fun holdsADocument(value: BsonValue): Boolean =
            value.isArray && value.asArray().any { it.isDocument || holdsADocument(it) }

        private fun display(value: BsonValue): String =
            when {
                value.isString -> "\"${value.asString().value}\""
                value.isInt32 -> "${value.asInt32().value}"
                value.isInt64 -> "${value.asInt64().value}"
                value.isDouble -> "${value.asDouble().value}"
                else -> "${value.bsonType}"
            }

        /**
         * Whether a projection value is an inclusion (`1`, `true`) or an exclusion (`0`, `false`).
         */
        private fun flag(value: BsonValue): Boolean? =
            when {
                value.isBoolean -> value.asBoolean().value
                value.isInt32 || value.isInt64 || value.isDouble ->
                    when (value.asNumber().doubleValue()) {
                        1.0 -> true
                        0.0 -> false
                        else -> null
                    }
                else -> null
            }

        /**
         * [value] as an argument of [type]; anything its type cannot take is unbound. A [plain]
         * value (a query or update operand) is data, so its arrays are lists; elsewhere an array is
         * an expression.
         */
        private fun value(
            value: BsonValue,
            type: String,
            path: String,
            plain: Boolean = false,
        ): String {
            operator(value, path)?.let { (operator, argument) ->
                val binding =
                    expressions[operator] ?: throw Unbound("$path: $operator has no binding")
                return call(binding, argument, "$path/$operator")
            }
            if (value.isArray && (type == ITERABLE || type == LIST || plain && type == OBJECT)) {
                val items = value.asArray().values
                if (items.any { it.isNull }) throw Unbound("$path: List.of cannot hold null")
                types += "java.util.List"
                return items
                    .mapIndexed { index, item -> value(item, OBJECT, "$path/$index", plain) }
                    .joinToString(", ", "List.of(", ")")
            }
            if (value.isDocument && type == OUTPUTS) return outputs(value.asDocument(), path)
            if (value.isDocument && type == SORTS) {
                // `{f: -1, g: 1}` as `Sort` varargs, in order.
                return value.asDocument().entries.joinToString(", ") { (field, each) ->
                    val direction =
                        when {
                            each.isNumber && each.asNumber().doubleValue() == 1.0 -> "ascending"
                            each.isNumber && each.asNumber().doubleValue() == -1.0 -> "descending"
                            else -> throw Unbound("$path/$field: a sort by $each has no binding")
                        }
                    "${static(SORTS.removeSuffix("[]"), direction)}(${literal(field)})"
                }
            }
            if (value.isArray && type == OBJECT) {
                // An array argument, such as `$top`'s output of several fields, is an expression
                // in Morphia; its binding is the catalog's `[]` entry. `ArrayLiteralCodec` fails on
                // a null item.
                if (value.asArray().any { it.isNull })
                    throw Unbound("$path: Morphia cannot write an array literal's null item")
                val binding =
                    expressions[ARRAY] ?: throw Unbound("$path: an array literal has no binding")
                return call(binding, value, "$path/$ARRAY")
            }
            // `document()` is an Expression, which only an Object or Expression parameter takes.
            if (value.isDocument && !plain && (type == OBJECT || type == EXPRESSION)) {
                // A document of expressions is `document().field(k, v)…`, the catalog's `{}` entry.
                // Its codec skips null fields, and a `$` key would be an operator.
                val document = value.asDocument()
                document.keys
                    .firstOrNull { it.startsWith("$") }
                    ?.let {
                        throw Unbound(
                            "$path/$it: a document literal with an operator key has no binding"
                        )
                    }
                document.entries
                    .firstOrNull { it.value.isNull }
                    ?.let {
                        throw Unbound(
                            "$path/${it.key}: Morphia omits a document literal's null field"
                        )
                    }
                // `ArrayLiteralCodec` fails on a document item inside a document literal.
                document.entries
                    .firstOrNull { (_, each) -> holdsADocument(each) }
                    ?.let {
                        throw Unbound(
                            "$path/${it.key}: Morphia cannot write a document in a document literal's array"
                        )
                    }
                val binding =
                    expressions[DOCUMENT]
                        ?: throw Unbound("$path: a document literal has no binding")
                return call(binding, value, "$path/$DOCUMENT")
            }
            if (value.isDocument) throw Unbound("$path: a document literal has no binding")
            if (value.isArray) throw Unbound("$path: an array literal has no binding")
            return when (type) {
                OBJECT ->
                    when {
                        value.isString -> literal(value.asString().value)
                        value.isInt32 -> "${value.asInt32().value}"
                        value.isInt64 -> "${value.asInt64().value}L"
                        value.isDouble -> double(value.asDouble().value)
                        value.isBoolean -> "${value.asBoolean().value}"
                        value.isNull -> "null"
                        else -> throw Unbound("$path: a ${value.bsonType} literal has no binding")
                    }
                BOOLEAN ->
                    if (value.isBoolean) "${value.asBoolean().value}"
                    else throw Unbound("$path: ${value.bsonType} where $type is required")
                STRING ->
                    if (value.isString) literal(value.asString().value)
                    else throw Unbound("$path: ${value.bsonType} where $type is required")
                "long" ->
                    when {
                        value.isInt32 -> "${value.asInt32().value}"
                        value.isInt64 -> "${value.asInt64().value}L"
                        else -> throw Unbound("$path: ${value.bsonType} where $type is required")
                    }
                NUMBER ->
                    when {
                        value.isInt32 -> "${value.asInt32().value}"
                        value.isInt64 -> "${value.asInt64().value}L"
                        value.isDouble -> double(value.asDouble().value)
                        else -> throw Unbound("$path: ${value.bsonType} where $type is required")
                    }
                else ->
                    // Any other parameter type is an enum whose constants are the MQL values in
                    // upper snake case (`keepExisting` is `KEEP_EXISTING`); integration's compile
                    // step proves the constant exists.
                    if (value.isString && type != EXPRESSION && enums.any { type.startsWith(it) }) {
                        types += type
                        "${type.substringAfterLast('.')}.${constant(value.asString().value)}"
                    } else throw Unbound("$path: ${value.bsonType} where $type is required")
            }
        }

        /** Morphia's and the driver's packages, whose enums take MQL values. */
        private val enums = listOf("dev.morphia.", "com.mongodb.")

        private fun constant(value: String): String =
            value.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").uppercase()

        private fun double(value: Double): String =
            when {
                value.isNaN() -> "Double.NaN"
                value == Double.POSITIVE_INFINITY -> "Double.POSITIVE_INFINITY"
                value == Double.NEGATIVE_INFINITY -> "Double.NEGATIVE_INFINITY"
                else -> value.toString()
            }
    }

    /** A Java string literal. */
    private fun literal(text: String): String =
        "\"" +
            text
                .flatMap { char ->
                    when (char) {
                        '\\' -> "\\\\".toList()
                        '"' -> "\\\"".toList()
                        '\n' -> "\\n".toList()
                        '\t' -> "\\t".toList()
                        '\r' -> "\\r".toList()
                        else ->
                            if (char < ' ' || char > '~') "\\u%04x".format(char.code).toList()
                            else listOf(char)
                    }
                }
                .joinToString("") +
            "\""
}
