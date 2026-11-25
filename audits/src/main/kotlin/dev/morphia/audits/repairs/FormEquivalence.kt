package dev.morphia.audits.repairs

import org.bson.BsonArray
import org.bson.BsonBoolean
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonRegularExpression
import org.bson.BsonString
import org.bson.BsonValue

/**
 * A declared equivalence between a documented command form and the form Morphia generates. Each
 * rule maps every spelling in its equivalence class to one canonical spelling and is applied to
 * both sides of a comparison, so neither side is rewritten to look like the other. The description
 * states why the server treats the spellings identically.
 */
enum class FormRule(val description: String) {
    COND_ARRAY(
        "\$cond: {if: a, then: b, else: c} and \$cond: [a, b, c] are the same expression; " +
            "the array form is canonical."
    ),
    EQ_SHORTHAND(
        "In a query predicate {f: {\$eq: v}} matches the same documents as {f: v} unless v is a " +
            "regular expression or a document whose first key is an operator; the short form is " +
            "canonical."
    ),
    REGEX_FORM(
        "In a query predicate f: /p/o, {\$regex: /p/o}, {\$regex: \"p\", \$options: \"o\"} and " +
            "{\$regex: /p/, \$options: \"o\"} are the same match; {\$regex: \"p\", \$options: \"o\"} " +
            "with sorted options is canonical."
    ),
    FIELD_OPERATORS_AND(
        "{f: {\$gt: a, \$lt: b}} is {\$and: [{f: {\$gt: a}}, {f: {\$lt: b}}]}: each operator on a " +
            "field is evaluated independently, so a one-field clause with several comparison " +
            "operators is canonically an \$and of one-operator clauses."
    ),
    SINGLE_ARGUMENT_ARRAY(
        "An expression operator that takes one argument accepts it bare or as a one-element " +
            "argument list: {\$isArray: [\"\$a\"]} is {\$isArray: \"\$a\"}. The bare form is " +
            "canonical; an argument that is itself an array literal is left wrapped."
    ),
    PROJECT_ID_FIRST(
        "The position of _id inside a projection does not change its output; _id first is canonical."
    ),
    PROJECTION_FLAGS(
        "A top-level projection value of true or false is the same as 1 or 0; the number is canonical."
    ),
    UNWIND_PATH("\$unwind: \"\$p\" is \$unwind: {path: \"\$p\"}; the document form is canonical."),
    ALTERNATIVE_COMMANDS(
        "An action block holds several commands of the same kind on one collection and the fixture " +
            "holds one of them. The tool does not prove the alternatives equivalent: each use needs " +
            "a reviewed justification in its declaration."
    ),
    REGEX_OPTIONS(
        "In \$regexFind, \$regexFindAll and \$regexMatch, regex: /p/o, regex: \"p\", options: " +
            "\"o\" and regex: /p/, options: \"o\" are the same match; a string pattern with " +
            "sorted options, and no options key when there are none, is canonical."
    ),
    NAMED_ARGUMENTS(
        "The top-level keys of an operator that takes only named arguments (for example \$filter, " +
            "\$text, " +
            "\$map, \$dateToString, \$bottomN, \$fill, \$graphLookup) may appear in any order; sorted " +
            "keys are canonical. Nested values, including sort specifications, keep their order."
    ),
}

/** How a fixture's action documents are read by the template helpers. */
enum class ActionShape {
    /** One document per pipeline stage. */
    PIPELINE,
    /** The filter, then an optional projection. */
    FIND,
    /** The filter, then the update. */
    UPDATE,
}

/** Applies declared [FormRule]s. Pure: inputs are not modified and fresh trees are returned. */
object FormEquivalence {
    /** Operators whose argument document holds only named parameters. */
    val namedArgumentOperators =
        setOf(
            "\$bottom",
            "\$bottomN",
            "\$dateAdd",
            "\$dateDiff",
            "\$dateFromString",
            "\$dateSubtract",
            "\$dateToString",
            "\$dateTrunc",
            "\$densify",
            "\$fill",
            "\$filter",
            "\$firstN",
            "\$graphLookup",
            "\$lastN",
            "\$let",
            "\$lookup",
            "\$map",
            "\$maxN",
            "\$merge",
            "\$minN",
            "\$reduce",
            "\$setWindowFields",
            "\$regexFind",
            "\$regexFindAll",
            "\$regexMatch",
            "\$sortArray",
            "\$text",
            "\$top",
            "\$topN",
            "\$unwind",
            "\$zip",
        )

    /** Expression operators whose argument takes a pattern in `regex` and flags in `options`. */
    val regexOperators = setOf("\$regexFind", "\$regexFindAll", "\$regexMatch")

    /** Expression operators that take exactly one argument. */
    val singleArgumentOperators =
        setOf(
            "\$abs",
            "\$ceil",
            "\$exp",
            "\$floor",
            "\$isArray",
            "\$isNumber",
            "\$ln",
            "\$log10",
            "\$size",
            "\$sqrt",
            "\$strLenBytes",
            "\$strLenCP",
            "\$toBool",
            "\$toDate",
            "\$toDecimal",
            "\$toDouble",
            "\$toInt",
            "\$toLong",
            "\$toLower",
            "\$toObjectId",
            "\$toString",
            "\$toUpper",
            "\$year",
        )

    private val comparisonOperators =
        setOf(
            "\$all",
            "\$eq",
            "\$exists",
            "\$gt",
            "\$gte",
            "\$in",
            "\$lt",
            "\$lte",
            "\$mod",
            "\$ne",
            "\$nin",
            "\$size",
            "\$type",
        )

    fun canonical(
        action: List<BsonValue>,
        shape: ActionShape,
        rules: Set<FormRule>,
    ): List<BsonValue> = Canonicalizer(rules).action(action, shape)

    private class Canonicalizer(val rules: Set<FormRule>) {
        fun action(values: List<BsonValue>, shape: ActionShape): List<BsonValue> =
            when (shape) {
                ActionShape.PIPELINE -> values.map { stage(it) }
                ActionShape.FIND ->
                    values.mapIndexed { index, value ->
                        if (index == 0) filter(value) else projection(value)
                    }
                ActionShape.UPDATE ->
                    values.mapIndexed { index, value ->
                        if (index == 0) filter(value) else expression(value)
                    }
            }

        fun pipeline(value: BsonValue): BsonValue =
            if (value is BsonArray) BsonArray(value.values.map { stage(it) }) else expression(value)

        fun stage(value: BsonValue): BsonValue {
            if (value !is BsonDocument) return expression(value)
            val result = BsonDocument()
            value.forEach { (key, argument) ->
                result[key] =
                    when (key) {
                        "\$match" -> filter(argument)
                        "\$project" -> projection(argument)
                        "\$facet" ->
                            if (argument is BsonDocument)
                                BsonDocument().also { facets ->
                                    argument.forEach { (name, stages) ->
                                        facets[name] = pipeline(stages)
                                    }
                                }
                            else expression(argument)
                        "\$unwind" ->
                            named(
                                key,
                                if (FormRule.UNWIND_PATH in rules && argument is BsonString)
                                    BsonDocument("path", argument)
                                else expression(argument),
                            )
                        "\$lookup",
                        "\$unionWith" ->
                            if (argument is BsonDocument)
                                named(
                                    key,
                                    BsonDocument().also { copy ->
                                        argument.forEach { (name, field) ->
                                            copy[name] =
                                                if (name == "pipeline") pipeline(field)
                                                else expression(field)
                                        }
                                    },
                                )
                            else expression(argument)
                        else -> named(key, expression(argument))
                    }
            }
            return result
        }

        fun projection(value: BsonValue): BsonValue {
            if (value !is BsonDocument) return expression(value)
            val entries =
                value.entries.map { (key, field) ->
                    key to
                        if (FormRule.PROJECTION_FLAGS in rules && field is BsonBoolean)
                            BsonInt32(if (field.value) 1 else 0)
                        else expression(field)
                }
            val ordered =
                if (FormRule.PROJECT_ID_FIRST in rules)
                    entries.sortedBy { (key, _) -> if (key == "_id") 0 else 1 }
                else entries
            return BsonDocument().also { result -> ordered.forEach { (k, v) -> result[k] = v } }
        }

        fun filter(value: BsonValue): BsonValue {
            if (value !is BsonDocument) return expression(value)
            if (FormRule.FIELD_OPERATORS_AND in rules && value.size == 1) {
                val (field, predicate) = value.entries.single()
                if (
                    !field.startsWith("$") &&
                        predicate is BsonDocument &&
                        predicate.size > 1 &&
                        predicate.keys.all { it in comparisonOperators }
                ) {
                    return BsonDocument(
                        "\$and",
                        BsonArray(
                            predicate.entries.map { (operator, operand) ->
                                filter(BsonDocument(field, BsonDocument(operator, operand)))
                            }
                        ),
                    )
                }
            }
            val result = BsonDocument()
            value.forEach { (field, predicate) ->
                result[field] =
                    when {
                        field in setOf("\$and", "\$or", "\$nor") && predicate is BsonArray ->
                            BsonArray(predicate.values.map { filter(it) })
                        field.startsWith("$") -> named(field, expression(predicate))
                        else -> predicate(predicate)
                    }
            }
            return result
        }

        private fun predicate(value: BsonValue): BsonValue {
            var result = expression(value, query = true)
            if (FormRule.EQ_SHORTHAND in rules && result is BsonDocument && result.size == 1) {
                val operand = result["\$eq"]
                if (
                    operand != null &&
                        operand !is BsonRegularExpression &&
                        !(operand is BsonDocument &&
                            operand.keys.firstOrNull()?.startsWith("$") == true)
                ) {
                    result = operand
                }
            }
            if (FormRule.REGEX_FORM in rules) result = regex(result)
            return result
        }

        private fun regex(value: BsonValue): BsonValue {
            val (pattern, options) =
                when {
                    value is BsonRegularExpression -> value.pattern to value.options
                    value is BsonDocument &&
                        "\$regex" in value &&
                        value.keys.all { it == "\$regex" || it == "\$options" } -> {
                        val separate =
                            when (val options = value["\$options"]) {
                                null -> ""
                                is BsonString -> options.value
                                else -> return value
                            }
                        when (val expression = value["\$regex"]) {
                            is BsonString -> expression.value to separate
                            is BsonRegularExpression ->
                                if (expression.options.isNotEmpty() && separate.isNotEmpty())
                                    return value
                                else expression.pattern to expression.options + separate
                            else -> return value
                        }
                    }
                    else -> return value
                }
            return BsonDocument("\$regex", BsonString(pattern)).also {
                val sorted = options.toCharArray().sorted().joinToString("")
                if (sorted.isNotEmpty()) it["\$options"] = BsonString(sorted)
            }
        }

        /**
         * [query] marks query-operator positions (inside a predicate), where `$size` and friends
         * are query operators rather than single-argument expressions. `$literal` content is data
         * and is never canonicalized.
         */
        fun expression(value: BsonValue, query: Boolean = false): BsonValue =
            when (value) {
                is BsonDocument ->
                    BsonDocument().also { result ->
                        value.forEach { (key, field) ->
                            if (key == "\$literal") {
                                result[key] = field
                                return@forEach
                            }
                            var converted = expression(field, query)
                            if (key == "\$cond" && FormRule.COND_ARRAY in rules) {
                                converted = condition(converted)
                            }
                            if (
                                !query &&
                                    FormRule.SINGLE_ARGUMENT_ARRAY in rules &&
                                    key in singleArgumentOperators &&
                                    converted is BsonArray &&
                                    converted.size == 1 &&
                                    converted[0] !is BsonArray
                            ) {
                                converted = converted[0]
                            }
                            if (FormRule.REGEX_OPTIONS in rules && key in regexOperators) {
                                converted = regexOptions(converted)
                            }
                            result[key] = named(key, converted)
                        }
                    }
                is BsonArray -> BsonArray(value.values.map { expression(it, query) })
                else -> value
            }

        /**
         * A regex expression's argument with its pattern as a string and its flags, from the
         * pattern and the `options` key, sorted in `options`. Flags in both places are left alone,
         * since the server rejects them.
         */
        private fun regexOptions(value: BsonValue): BsonValue {
            if (value !is BsonDocument) return value
            val (pattern, inline) =
                when (val regex = value["regex"]) {
                    is BsonRegularExpression -> regex.pattern to regex.options
                    // A `$` string is a field path the server evaluates, not a pattern.
                    is BsonString ->
                        if (regex.value.startsWith("$")) return value else regex.value to ""
                    else -> return value
                }
            val separate =
                when (val options = value["options"]) {
                    null -> ""
                    is BsonString ->
                        if (options.value.startsWith("$")) return value else options.value
                    else -> return value
                }
            if (inline.isNotEmpty() && separate.isNotEmpty()) return value
            val options = (inline + separate).toCharArray().sorted().joinToString("")
            return BsonDocument().also { result ->
                value.forEach { (name, field) ->
                    when (name) {
                        "regex" -> result[name] = BsonString(pattern)
                        "options" -> if (options.isNotEmpty()) result[name] = BsonString(options)
                        else -> result[name] = field
                    }
                }
                if (options.isNotEmpty() && "options" !in result)
                    result["options"] = BsonString(options)
            }
        }

        private fun condition(value: BsonValue): BsonValue =
            if (value is BsonDocument && value.keys == setOf("if", "then", "else"))
                BsonArray(listOf(value["if"]!!, value["then"]!!, value["else"]!!))
            else value

        private fun named(key: String, value: BsonValue): BsonValue =
            if (
                FormRule.NAMED_ARGUMENTS in rules &&
                    key in namedArgumentOperators &&
                    value is BsonDocument
            )
                BsonDocument().also { sorted ->
                    value.keys.sorted().forEach { sorted[it] = value[it] }
                }
            else value
    }
}
