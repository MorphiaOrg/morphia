package dev.morphia.audits.generate

import dev.morphia.audits.repairs.ActionShape
import dev.morphia.audits.repairs.FixtureMigration
import dev.morphia.audits.repairs.FormMigration
import dev.morphia.audits.repairs.FormOverride
import dev.morphia.audits.repairs.GENERATED
import dev.morphia.audits.repairs.ManifestCase
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
 * generated, and the cases excluded from them, each with its reason.
 */
data class GenerationReview(
    val schemaVersion: Int = 1,
    val families: List<String>,
    val excluded: List<Exclusion> = emptyList(),
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
        val excluded = review.excluded.associate { it.name to it.reason }
        val cases = mutableListOf<Pair<GeneratedCase, Emitter>>()
        val gaps = mutableListOf<GenerationGap>()
        val unused = excluded.keys.toMutableSet()
        examples.pages
            .filter { it.context in review.families }
            .forEach { page ->
                val folder = page.context.substringAfter('/')
                val type = "Test" + page.operator.replaceFirstChar { it.uppercase() }
                val source = "$SOURCES/${PACKAGE.replace('.', '/')}/$folder/$type.java"
                page.examples.forEach { example ->
                    fun gap(kind: GapKind, detail: String) {
                        gaps +=
                            GenerationGap(page.context, page.operator, example.title, kind, detail)
                    }
                    when (example.status) {
                        ExampleStatus.OWNED -> return@forEach
                        ExampleStatus.UNNAMED ->
                            return@forEach gap(GapKind.UNNAMED, example.detail ?: "")
                        ExampleStatus.AMBIGUOUS ->
                            return@forEach gap(GapKind.AMBIGUOUS, example.detail ?: "")
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
                    if (documented.shape != ActionShape.PIPELINE) {
                        return@forEach gap(
                            GapKind.SOURCE_UNSUPPORTED,
                            "a ${documented.shape} action is not generated yet",
                        )
                    }
                    val emitted = Emitter(stages, expressions)
                    val pipeline =
                        try {
                            documented.action.mapIndexed { index, stage ->
                                emitted.stage(stage, "/pipeline/$index")
                            }
                        } catch (unbound: Unbound) {
                            return@forEach gap(GapKind.UNBOUND, unbound.message!!)
                        }
                    val scenario = FixtureMigration.scenarioPath(name, GENERATED)
                    val override =
                        FormOverride(
                            fixture = name,
                            origin = page.source.commit,
                            source = page.source,
                            pageBlob = page.pageBlob,
                            includes = page.includes,
                            unexpandedIncludes = emptyList(),
                            example = example.title,
                            shape = documented.shape,
                            blocks = example.blocks,
                            rules = emptyList(),
                            retiredLock = null,
                            fixtureFiles = emptyMap(),
                        )
                    val resource = scenario.removePrefix(RESOURCES)
                    val method =
                        """
                        |    @Test
                        |    @DisplayName(${literal(example.title)})
                        |    public void $stem() {
                        |        runner.aggregate(
                        |                OperatorCase.of(${literal(resource)}),
                        |                (aggregation) -> aggregation.pipeline(
                        |                        ${pipeline.joinToString(",\n                        ")}));
                        |    }
                        |"""
                            .trimMargin()
                    cases +=
                        GeneratedCase(
                            ManifestCase(name, GENERATED, resource, "$PACKAGE.$folder.$type#$stem"),
                            FixtureMigration.scenario(override, ordered = true),
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
    ) {
        /** Owner and method of every factory called. */
        val calls = mutableSetOf<Pair<String, String>>()
        /** Types named in the source, such as an enum argument's. */
        val types = mutableSetOf<String>()

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
                    if (!argument.isDocument)
                        throw Unbound("$path: ${binding.operator} takes a document")
                    val document = argument.asDocument()
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
                                ".${option.method}(${value(each, option.parameters.single(), "$path/$key")})"
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
                BindingForm.PROJECTION -> {
                    if (!argument.isDocument)
                        throw Unbound("$path: ${binding.operator} takes a document")
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

        /** [value] as an argument of [type]; anything its type cannot take is unbound. */
        private fun value(value: BsonValue, type: String, path: String): String {
            operator(value, path)?.let { (operator, argument) ->
                val binding =
                    expressions[operator] ?: throw Unbound("$path: $operator has no binding")
                return call(binding, argument, "$path/$operator")
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
                STRING ->
                    if (value.isString) literal(value.asString().value)
                    else throw Unbound("$path: ${value.bsonType} where $type is required")
                else ->
                    // Any other parameter type is an enum whose constants are the upper-cased MQL
                    // values; integration's compile step proves the constant exists.
                    if (value.isString && type.startsWith("dev.morphia.")) {
                        types += type
                        "${type.substringAfterLast('.')}.${value.asString().value.uppercase()}"
                    } else throw Unbound("$path: ${value.bsonType} where $type is required")
            }
        }

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
