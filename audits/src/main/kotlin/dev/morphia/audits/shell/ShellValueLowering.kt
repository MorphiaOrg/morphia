package dev.morphia.audits.shell

import dev.morphia.audits.shell.ShellLiterals.UnsupportedLiteral
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import org.bson.*
import org.mozilla.javascript.Token
import org.mozilla.javascript.ast.*

/** Evaluation context retains JS primitive semantics before BSON storage conversion. */
internal enum class ShellValueUse {
    STORAGE,
    EXPECTED,
    INITIALIZER,
    ARGUMENT,
}

internal interface ShellValueBindings {
    fun resolve(name: String, use: ShellValueUse, depth: Int, reference: ShellSpan?): BsonValue?

    fun contains(name: String): Boolean = false

    companion object {
        val EMPTY =
            object : ShellValueBindings {
                override fun resolve(
                    name: String,
                    use: ShellValueUse,
                    depth: Int,
                    reference: ShellSpan?,
                ) = null
            }
    }
}

/** Shared across binding expansion so a small source cannot expand without a work bound. */
internal class ShellValueBudget(private val maximumNodes: Int) {
    private var nodes = 0

    fun take(): Boolean = ++nodes <= maximumNodes
}

internal class ShellValueLowering(
    val source: String,
    val dialect: ShellDialect,
    val limits: ShellLiteralLimits,
    val prefix: Int = 1,
    val bindings: ShellValueBindings = ShellValueBindings.EMPTY,
    val budget: ShellValueBudget = ShellValueBudget(limits.maximumNodes),
) {

    fun span(node: AstNode): ShellSpan? {
        // Rhino 1.9.1's UnaryExpression constructor discards operatorPosition;
        // even an in-range hint can omit a sign/comment. Do not publish it.
        if (node is UnaryExpression) return null
        val start = node.absolutePosition - prefix
        val end = start + node.length
        // Rhino can attach a synthetic parenthesis extent to the inner node.
        // Preserve the raw block and omit an unusable hint instead of inventing offsets.
        return if (start in 0..source.length && end in start..source.length) ShellSpan(start, end)
        else null
    }

    fun unsupported(node: AstNode, code: String, message: String): Nothing =
        throw UnsupportedLiteral(code, message, span(node))

    fun value(node: AstNode, depth: Int, use: ShellValueUse = ShellValueUse.STORAGE): BsonValue {
        if (!budget.take() || depth > limits.maximumDepth)
            unsupported(node, "VALUE_LIMIT", "Shell value exceeds AST node/depth limit")
        return when (node) {
            is ParenthesizedExpression -> value(node.expression, depth + 1, use)
            is StringLiteral -> BsonString(string(node.value, node))
            is NumberLiteral -> number(node.number, node, use)
            is KeywordLiteral ->
                when (node.type) {
                    Token.TRUE -> BsonBoolean.TRUE
                    Token.FALSE -> BsonBoolean.FALSE
                    Token.NULL -> BsonNull.VALUE
                    Token.UNDEFINED -> undefined(node, use)
                    else -> unsupported(node, "KEYWORD", "Unsupported value keyword")
                }
            is Name ->
                bindings.resolve(node.identifier, use, depth, span(node))
                    ?: when (node.identifier) {
                        "NaN" -> BsonDouble(Double.NaN)
                        "Infinity" -> BsonDouble(Double.POSITIVE_INFINITY)
                        "undefined" -> undefined(node, use)
                        else ->
                            unsupported(
                                node,
                                "UNRESOLVED_NAME",
                                "No value binding for ${node.identifier}",
                            )
                    }
            is UnaryExpression -> {
                if (node.operator != Token.NEG && node.operator != Token.POS)
                    unsupported(node, "COMPUTED_VALUE", "Only numeric unary signs are supported")
                // A signed constant never evaluates a call or reads an arbitrary binding.
                var syntax = node.operand
                var wrappers = 0
                while (syntax is ParenthesizedExpression) {
                    if (depth + ++wrappers > limits.maximumDepth)
                        unsupported(node, "VALUE_LIMIT", "Numeric value exceeds depth limit")
                    syntax = syntax.expression
                }
                if (syntax !is NumberLiteral && syntax !is UnaryExpression && syntax !is Name)
                    unsupported(
                        node,
                        "COMPUTED_VALUE",
                        "Unary signs require a primitive numeric constant",
                    )
                val operand = value(node.operand, depth + 1, ShellValueUse.ARGUMENT)
                if (!operand.isNumber)
                    unsupported(node, "COMPUTED_VALUE", "Numeric unary operand required")
                val n = operand.asNumber().doubleValue()
                number(if (node.operator == Token.NEG) -n else n, node, use)
            }
            is ArrayLiteral -> {
                if (node.elements.any { it is EmptyExpression })
                    unsupported(node, "ARRAY_HOLE", "Sparse arrays require an explicit disposition")
                BsonArray(node.elements.map { value(it, depth + 1, use) })
            }
            is ObjectLiteral -> {
                val result = BsonDocument()
                for (element in node.elements) {
                    val property =
                        element as? ObjectProperty
                            ?: unsupported(element, "OBJECT_SPREAD", "Object spread is unsupported")
                    if (property.isMethod)
                        unsupported(
                            property,
                            "OBJECT_METHOD",
                            "Object methods/getters/setters are unsupported",
                        )
                    val key =
                        when (val keyNode = property.key) {
                            is Name -> keyNode.identifier
                            is StringLiteral -> string(keyNode.value, keyNode)
                            else ->
                                unsupported(
                                    keyNode,
                                    "OBJECT_KEY",
                                    "Only explicit name/string keys are supported",
                                )
                        }
                    if ('\u0000' in key)
                        unsupported(
                            property,
                            "BSON_KEY_ENCODING",
                            "BSON field names cannot contain NUL",
                        )
                    if (key == "_bsontype")
                        unsupported(
                            property,
                            "BSON_PROTOCOL_KEY",
                            "Shell object would invoke private BSON type dispatch",
                        )
                    if (key == "__proto__")
                        unsupported(
                            property,
                            "PROTOTYPE_KEY",
                            "Prototype-setting shell keys are unsupported",
                        )
                    if (result.containsKey(key))
                        unsupported(property, "DUPLICATE_KEY", "Duplicate BSON key: $key")
                    result[key] = value(property.value, depth + 1, use)
                }
                result
            }
            is RegExpLiteral ->
                ShellRegularExpressions.javascript(node.value, node.flags, span(node))
            is FunctionCall -> {
                if (node.isOptionalCall || node is NewExpression && node.initializer != null)
                    unsupported(
                        node,
                        "CALL_FORM",
                        "Optional calls and constructor initializers are unsupported",
                    )
                if (use == ShellValueUse.ARGUMENT)
                    unsupported(
                        node,
                        "CONSTRUCTOR_ARGUMENT",
                        "Nested constructor calls require an explicit rule",
                    )
                val name =
                    (node.target as? Name)?.identifier
                        ?: unsupported(
                            node,
                            "CALL_TARGET",
                            "Only explicit BSON constructors are supported",
                        )
                if (bindings.contains(name))
                    unsupported(node, "SHADOWED_CONSTRUCTOR", "Constructor name is shadowed: $name")
                ShellLiteralConstructors.construct(
                    name,
                    node is NewExpression,
                    node.arguments.map { value(it, depth + 1, ShellValueUse.ARGUMENT) },
                    span(node),
                    dialect,
                )
            }
            else ->
                unsupported(
                    node,
                    "UNSUPPORTED_AST",
                    "Unsupported value syntax: ${node.javaClass.simpleName}",
                )
        }
    }

    private fun undefined(node: AstNode, use: ShellValueUse): BsonValue =
        if (use == ShellValueUse.EXPECTED)
            unsupported(
                node,
                "EXPECTED_TYPE_AMBIGUOUS",
                "Displayed undefined does not prove a stored BSON type",
            )
        else if (use != ShellValueUse.STORAGE) BsonUndefined()
        else
            when (dialect) {
                ShellDialect.MONGOSH -> BsonNull.VALUE
                ShellDialect.LEGACY_MONGO -> BsonUndefined()
                ShellDialect.UNSPECIFIED ->
                    unsupported(
                        node,
                        "SHELL_DIALECT_REQUIRED",
                        "Undefined BSON storage depends on shell dialect",
                    )
            }

    private fun string(value: String, node: AstNode): String {
        try {
            Charsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(value))
        } catch (failure: CharacterCodingException) {
            unsupported(
                node,
                "BSON_STRING_ENCODING",
                "Unpaired Unicode surrogate cannot be represented as BSON UTF-8",
            )
        }
        return value
    }

    private fun number(value: Double, node: AstNode, use: ShellValueUse): BsonValue {
        if (use == ShellValueUse.INITIALIZER || use == ShellValueUse.ARGUMENT)
            return BsonDouble(value)
        val int32 =
            value.isFinite() &&
                value >= Int.MIN_VALUE &&
                value <= Int.MAX_VALUE &&
                value == kotlin.math.floor(value) &&
                value.toRawBits() != (-0.0).toRawBits()
        if (use == ShellValueUse.EXPECTED) {
            if (int32)
                unsupported(
                    node,
                    "EXPECTED_TYPE_AMBIGUOUS",
                    "Displayed whole numbers can be Int32 or Double; require typed source evidence",
                )
            return BsonDouble(value)
        }
        if (dialect == ShellDialect.UNSPECIFIED && int32)
            unsupported(
                node,
                "SHELL_DIALECT_REQUIRED",
                "Default whole-number BSON type depends on shell dialect",
            )
        return if (dialect == ShellDialect.MONGOSH && int32) BsonInt32(value.toInt())
        else BsonDouble(value)
    }
}
