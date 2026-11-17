package dev.morphia.audits.shell

import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.HexFormat
import org.bson.*
import org.mozilla.javascript.CompilerEnvirons
import org.mozilla.javascript.Context
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.Parser
import org.mozilla.javascript.Token
import org.mozilla.javascript.ast.*

/** Parses a single shell value without compiling, evaluating or executing JavaScript programs. */
object ShellLiterals {
    const val PARSER = "rhino:1.9.1/shell-literal:1"

    fun normalize(
        source: String,
        dialect: ShellDialect,
        limits: ShellLiteralLimits = ShellLiteralLimits(),
    ): ShellLiteralResult {
        var hash: String? = null
        fun failed(state: ShellLiteralState, problem: ShellLiteralProblem) =
            ShellLiteralResult(state, source, hash, PARSER, dialect, null, listOf(problem))
        if (source.length > limits.maximumCharacters)
            return failed(
                ShellLiteralState.INVALID,
                ShellLiteralProblem(
                    "SOURCE_LIMIT",
                    "Shell value exceeds source character limit",
                    null,
                ),
            )
        try {
            val bytes =
                Charsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(source))
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(bytes)
            hash = HexFormat.of().formatHex(digest.digest())
        } catch (failure: CharacterCodingException) {
            return failed(
                ShellLiteralState.INVALID,
                ShellLiteralProblem(
                    "SOURCE_ENCODING",
                    "Source contains an unpaired Unicode surrogate",
                    null,
                ),
            )
        }
        return try {
            val environment =
                CompilerEnvirons().apply {
                    languageVersion = Context.VERSION_ECMASCRIPT
                    isXmlAvailable = false
                    setRecoverFromErrors(false)
                    // IDE mode can return an incomplete tree after a parser recursion error.
                    setIdeMode(false)
                }
            val root = Parser(environment).parse("(" + source + "\n)", "audit-value", 1)
            val statement =
                root.statements.singleOrNull() as? ExpressionStatement
                    ?: throw UnsupportedLiteral(
                        "VALUE_REQUIRED",
                        "Expected exactly one value",
                        null,
                    )
            val wrapper =
                statement.expression as? ParenthesizedExpression
                    ?: throw UnsupportedLiteral(
                        "VALUE_REQUIRED",
                        "Expected a value expression",
                        null,
                    )
            val value = Lowering(source, dialect, limits).value(wrapper.expression, 0)
            ShellLiteralResult(
                ShellLiteralState.NORMALIZED,
                source,
                hash,
                PARSER,
                dialect,
                value,
                emptyList(),
            )
        } catch (failure: EvaluatorException) {
            // The parser reports line/column hints rather than original-source offsets. Keep
            // the entire unchanged block as evidence rather than fabricating a precise span.
            failed(
                ShellLiteralState.UNSUPPORTED,
                ShellLiteralProblem(
                    "SYNTAX",
                    failure.message ?: "Invalid shell value syntax",
                    ShellSpan(0, source.length),
                ),
            )
        } catch (failure: UnsupportedLiteral) {
            failed(
                ShellLiteralState.UNSUPPORTED,
                ShellLiteralProblem(failure.code, failure.message ?: failure.code, failure.span),
            )
        }
    }

    internal class UnsupportedLiteral(val code: String, message: String, val span: ShellSpan?) :
        RuntimeException(message)

    private class Lowering(
        val source: String,
        val dialect: ShellDialect,
        val limits: ShellLiteralLimits,
    ) {
        var nodes = 0

        fun span(node: AstNode): ShellSpan? {
            // Rhino 1.9.1's UnaryExpression constructor discards operatorPosition;
            // even an in-range hint can omit a sign/comment. Do not publish it.
            if (node is UnaryExpression) return null
            val start = node.absolutePosition - 1
            val end = start + node.length
            // Rhino can attach a synthetic parenthesis extent to the inner node.
            // Preserve the raw block and omit an unusable hint instead of inventing offsets.
            return if (start in 0..source.length && end in start..source.length)
                ShellSpan(start, end)
            else null
        }

        fun unsupported(node: AstNode, code: String, message: String): Nothing =
            throw UnsupportedLiteral(code, message, span(node))

        fun value(node: AstNode, depth: Int, argument: Boolean = false): BsonValue {
            if (++nodes > limits.maximumNodes || depth > limits.maximumDepth)
                unsupported(node, "VALUE_LIMIT", "Shell value exceeds AST node/depth limit")
            return when (node) {
                is ParenthesizedExpression -> value(node.expression, depth + 1, argument)
                is StringLiteral -> BsonString(string(node.value, node))
                is NumberLiteral -> number(node.number, node, argument)
                is KeywordLiteral ->
                    when (node.type) {
                        Token.TRUE -> BsonBoolean.TRUE
                        Token.FALSE -> BsonBoolean.FALSE
                        Token.NULL -> BsonNull.VALUE
                        Token.UNDEFINED -> undefined(node)
                        else -> unsupported(node, "KEYWORD", "Unsupported value keyword")
                    }
                is Name ->
                    when (node.identifier) {
                        "NaN" -> BsonDouble(Double.NaN)
                        "Infinity" -> BsonDouble(Double.POSITIVE_INFINITY)
                        "undefined" -> undefined(node)
                        else ->
                            unsupported(
                                node,
                                "UNRESOLVED_NAME",
                                "No value binding for ${node.identifier}",
                            )
                    }
                is UnaryExpression -> {
                    if (node.operator != Token.NEG && node.operator != Token.POS)
                        unsupported(
                            node,
                            "COMPUTED_VALUE",
                            "Only numeric unary signs are supported",
                        )
                    // A signed constant never evaluates a call or reads an arbitrary binding.
                    var syntax = node.operand
                    var wrappers = 0
                    while (syntax is ParenthesizedExpression) {
                        if (depth + ++wrappers > limits.maximumDepth)
                            unsupported(node, "VALUE_LIMIT", "Numeric value exceeds depth limit")
                        syntax = syntax.expression
                    }
                    if (
                        syntax !is NumberLiteral &&
                            syntax !is UnaryExpression &&
                            !(syntax is Name && syntax.identifier in setOf("NaN", "Infinity"))
                    )
                        unsupported(
                            node,
                            "COMPUTED_VALUE",
                            "Unary signs require a primitive numeric constant",
                        )
                    val operand = value(node.operand, depth + 1, true)
                    if (!operand.isNumber)
                        unsupported(node, "COMPUTED_VALUE", "Numeric unary operand required")
                    val n = operand.asNumber().doubleValue()
                    number(if (node.operator == Token.NEG) -n else n, node, argument)
                }
                is ArrayLiteral -> {
                    if (node.elements.any { it is EmptyExpression })
                        unsupported(
                            node,
                            "ARRAY_HOLE",
                            "Sparse arrays require an explicit disposition",
                        )
                    BsonArray(node.elements.map { value(it, depth + 1, argument) })
                }
                is ObjectLiteral -> {
                    val result = BsonDocument()
                    for (element in node.elements) {
                        val property =
                            element as? ObjectProperty
                                ?: unsupported(
                                    element,
                                    "OBJECT_SPREAD",
                                    "Object spread is unsupported",
                                )
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
                        if (key == "__proto__")
                            unsupported(
                                property,
                                "PROTOTYPE_KEY",
                                "Prototype-setting shell keys are unsupported",
                            )
                        if (result.containsKey(key))
                            unsupported(property, "DUPLICATE_KEY", "Duplicate BSON key: $key")
                        result[key] = value(property.value, depth + 1, argument)
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
                    if (argument)
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
                    ShellLiteralConstructors.construct(
                        name,
                        node is NewExpression,
                        node.arguments.map { value(it, depth + 1, true) },
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

        private fun undefined(node: AstNode): BsonValue =
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

        private fun number(value: Double, node: AstNode, argument: Boolean): BsonValue {
            if (argument) return BsonDouble(value)
            val int32 =
                value.isFinite() &&
                    value >= Int.MIN_VALUE &&
                    value <= Int.MAX_VALUE &&
                    value == kotlin.math.floor(value) &&
                    value.toRawBits() != (-0.0).toRawBits()
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
}
