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
import org.bson.BsonBoolean
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
    private const val LIST = "java.util.List"
    private const val NUMBER = "java.lang.Number"
    private const val SORTS = "dev.morphia.query.Sort[]"
    /** The binding key of an array literal in an expression. */
    const val ARRAY = "[]"

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
                    if (documented.shape == ActionShape.FIND && documented.action.size != 1) {
                        return@forEach gap(
                            GapKind.SOURCE_UNSUPPORTED,
                            "a documented projection is not generated yet",
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
                                ActionShape.FIND ->
                                    "runner.find(\n                OperatorCase.of(${literal(resource)}),\n" +
                                        "                ${query(emitted.filters(documented.action[0], "/filter"))});"
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
        val methods = cases.sortedBy { it.manifest.name }.joinToString("\n") { it.method }
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
            calls += binding.owner to binding.method
            val factory = "${binding.method}(%s)"
            return when (binding.form) {
                BindingForm.POSITIONAL -> {
                    val values =
                        if (argument.isArray) argument.asArray().values else listOf(argument)
                    val fixed =
                        if (binding.varargs) binding.parameters.size - 1
                        else binding.parameters.size
                    if (values.size < fixed || (!binding.varargs && values.size != fixed)) {
                        throw Unbound(
                            "$path: ${values.size} arguments for ${binding.method}${binding.parameters}"
                        )
                    }
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
                    val document = argument.asDocument()
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
                    if (key.isDocument && operator(key, "$path/_id") == null)
                        throw Unbound("$path/_id: a compound _id has no binding")
                    calls += binding.owner to id.method
                    factory.format("${id.method}(${value(key, OBJECT, "$path/_id")})") +
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
                calls += binding.owner to binding.method
                fields.asDocument().entries.map { (field, value) ->
                    // A field-only factory (`$unset`'s `unset(String, String...)`) ignores the
                    // value.
                    if (binding.parameters.size == 1 || binding.varargs)
                        "${binding.method}(${literal(field)})"
                    else
                        "${binding.method}(${literal(field)}, " +
                            "${value(value, binding.parameters[1], "$path/$operator/$field", plain = true)})"
                }
            }
        }

        /** The filters of one field's predicate: one per operator, or `$eq` for a plain value. */
        private fun predicates(field: String, predicate: BsonValue, path: String): List<String> {
            if (field.startsWith("$")) throw Unbound("$path: query operator $field has no binding")
            val operators =
                predicate
                    .takeIf { it.isDocument }
                    ?.asDocument()
                    ?.takeIf { it.isNotEmpty() && it.keys.all { key -> key.startsWith("$") } }
            if (operators == null) {
                rules += FormRule.EQ_SHORTHAND
                return listOf(filter("\$eq", field, predicate, path))
            }
            if (operators.size > 1) rules += FormRule.FIELD_OPERATORS_AND
            return operators.entries.map { (operator, value) ->
                filter(operator, field, value, "$path/$operator")
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
            calls += binding.owner to binding.method
            if (binding.parameters.size == 1) {
                if (value != BsonBoolean.TRUE)
                    throw Unbound("$path: $operator $value has no binding")
                return "${binding.method}(${literal(field)})"
            }
            return "${binding.method}(${literal(field)}, ${value(value, binding.parameters[1], path, plain = true)})"
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
            if (value.isDocument && type == SORTS) {
                // `{f: -1, g: 1}` as `Sort` varargs, in order.
                return value.asDocument().entries.joinToString(", ") { (field, each) ->
                    val direction =
                        when {
                            each.isNumber && each.asNumber().doubleValue() == 1.0 -> "ascending"
                            each.isNumber && each.asNumber().doubleValue() == -1.0 -> "descending"
                            else -> throw Unbound("$path/$field: a sort by $each has no binding")
                        }
                    calls += SORTS.removeSuffix("[]") to direction
                    "$direction(${literal(field)})"
                }
            }
            if (value.isArray && type == OBJECT) {
                // An array argument, such as `$top`'s output of several fields, is an expression
                // in Morphia; its binding is the catalog's `[]` entry.
                val binding =
                    expressions[ARRAY] ?: throw Unbound("$path: an array literal has no binding")
                return call(binding, value, "$path/$ARRAY")
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
                    if (value.isString && enums.any { type.startsWith(it) }) {
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
