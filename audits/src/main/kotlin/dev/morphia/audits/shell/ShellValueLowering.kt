package dev.morphia.audits.shell

import dev.morphia.audits.published.*
import dev.morphia.audits.semantics.BsonSnapshot
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

    fun published(name: String, depth: Int, reference: ShellSpan?): PublishedValue? = null

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
    private var characters = 0L

    fun text(length: Int): Boolean {
        characters += length
        return characters <= 16_777_216
    }

    fun take(): Boolean = ++nodes <= maximumNodes
}

internal class ShellValueLowering(
    val source: String,
    val dialect: ShellDialect,
    val limits: ShellLiteralLimits,
    val spanOffsetsReliable: Boolean = true,
    val prefix: Int = 1,
    val bindings: ShellValueBindings = ShellValueBindings.EMPTY,
    val budget: ShellValueBudget = ShellValueBudget(limits.maximumNodes),
) {

    fun span(node: AstNode): ShellSpan? {
        // Rhino 1.9.1 String-input cursor positions can undercount supplementary characters
        // in comments/identifiers, while literal fast paths reset them. Omit uncertain hints.
        if (!spanOffsetsReliable) return null
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

    private interface Factory<T> {
        fun scalar(value: BsonValue): T

        fun text(value: String): T

        fun number(value: Double, node: AstNode): T

        fun array(values: List<T>): T

        fun document(values: Map<String, T>): T

        fun resolve(node: Name, depth: Int): T?
    }

    fun value(node: AstNode, depth: Int, use: ShellValueUse = ShellValueUse.STORAGE): BsonValue =
        lower(
            node,
            depth,
            use,
            object : Factory<BsonValue> {
                override fun scalar(value: BsonValue) = value

                override fun text(value: String) = BsonString(value)

                override fun number(value: Double, node: AstNode) = number(value, node, use)

                override fun array(values: List<BsonValue>) = BsonArray(values)

                override fun document(values: Map<String, BsonValue>) =
                    BsonDocument().apply { values.forEach { (k, v) -> this[k] = v } }

                override fun resolve(node: Name, depth: Int) =
                    bindings.resolve(node.identifier, use, depth, span(node))
            },
        )

    fun published(
        node: AstNode,
        depth: Int,
        snapshot: (BsonValue) -> BsonSnapshot,
    ): PublishedValue {
        if (dialect != ShellDialect.MONGOSH)
            unsupported(
                node,
                "PUBLISHED_PROFILE",
                "Published shell results require the pinned mongosh read-result profile",
            )
        return lower(
            node,
            depth,
            ShellValueUse.EXPECTED,
            object : Factory<PublishedValue> {
                override fun scalar(value: BsonValue): PublishedValue =
                    when {
                        value is BsonDouble && value.value.isNaN() -> PublishedDoubleNaN
                        value is BsonDecimal128 -> PublishedDecimal128(value.value.toString())
                        else -> PublishedExact(snapshot(value))
                    }

                override fun text(value: String) = PublishedText(value)

                override fun number(value: Double, node: AstNode): PublishedValue =
                    if (isInt32(value)) PublishedWholeNumber(value.toInt())
                    else scalar(BsonDouble(value))

                override fun array(values: List<PublishedValue>) = PublishedArray(values)

                override fun document(values: Map<String, PublishedValue>) =
                    PublishedDocument(values.map { PublishedField(it.key, it.value) })

                override fun resolve(node: Name, depth: Int) =
                    bindings.published(node.identifier, depth, span(node))
            },
        )
    }

    private fun <T> lower(node: AstNode, depth: Int, use: ShellValueUse, factory: Factory<T>): T {
        if (!budget.take() || depth > limits.maximumDepth)
            unsupported(node, "VALUE_LIMIT", "Shell value exceeds AST node/depth limit")
        return when (node) {
            is ParenthesizedExpression -> lower(node.expression, depth + 1, use, factory)
            is StringLiteral -> factory.text(string(node.value, node))
            is NumberLiteral -> factory.number(node.number, node)
            is KeywordLiteral ->
                when (node.type) {
                    Token.TRUE -> factory.scalar(BsonBoolean.TRUE)
                    Token.FALSE -> factory.scalar(BsonBoolean.FALSE)
                    Token.NULL -> factory.scalar(BsonNull.VALUE)
                    Token.UNDEFINED -> factory.scalar(undefined(node, use))
                    else -> unsupported(node, "KEYWORD", "Unsupported value keyword")
                }
            is Name ->
                factory.resolve(node, depth)
                    ?: when (node.identifier) {
                        "NaN" -> factory.number(Double.NaN, node)
                        "Infinity" -> factory.number(Double.POSITIVE_INFINITY, node)
                        "undefined" -> factory.scalar(undefined(node, use))
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
                factory.number(if (node.operator == Token.NEG) -n else n, node)
            }
            is ArrayLiteral -> {
                if (node.elements.any { it is EmptyExpression })
                    unsupported(node, "ARRAY_HOLE", "Sparse arrays require an explicit disposition")
                factory.array(node.elements.map { lower(it, depth + 1, use, factory) })
            }
            is ObjectLiteral -> {
                val result = linkedMapOf<String, T>()
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
                            is Name -> string(keyNode.identifier, keyNode)
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
                    result[key] = lower(property.value, depth + 1, use, factory)
                }
                if (use == ShellValueUse.EXPECTED && "\$ref" in result && "\$id" in result)
                    unsupported(
                        node,
                        "PUBLISHED_DBREF",
                        "DBRef-shaped published documents need an explicit display rule",
                    )
                factory.document(
                    JavaScriptKeyOrder.order(result.keys.toList()).associateWith {
                        result.getValue(it)
                    }
                )
            }
            is RegExpLiteral -> {
                if (use == ShellValueUse.EXPECTED)
                    unsupported(
                        node,
                        "PUBLISHED_REGEX",
                        "Printed JavaScript regex does not prove the original BSON pattern/options",
                    )
                factory.scalar(
                    ShellRegularExpressions.javascript(node.value, node.flags, span(node))
                )
            }
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
                if (use == ShellValueUse.EXPECTED && name == "RegExp")
                    unsupported(
                        node,
                        "PUBLISHED_REGEX",
                        "Printed JavaScript regex does not prove original BSON pattern/options",
                    )
                if (bindings.contains(name))
                    unsupported(node, "SHADOWED_CONSTRUCTOR", "Constructor name is shadowed: $name")
                factory.scalar(
                    ShellLiteralConstructors.construct(
                        name,
                        node is NewExpression,
                        node.arguments.map { value(it, depth + 1, ShellValueUse.ARGUMENT) },
                        span(node),
                        dialect,
                    )
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
        if (!budget.text(value.length))
            unsupported(node, "VALUE_LIMIT", "Expanded shell text exceeds character budget")
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

    private fun isInt32(value: Double) =
        value.isFinite() &&
            value >= Int.MIN_VALUE &&
            value <= Int.MAX_VALUE &&
            value == kotlin.math.floor(value) &&
            value.toRawBits() != (-0.0).toRawBits()

    private fun number(value: Double, node: AstNode, use: ShellValueUse): BsonValue {
        if (use == ShellValueUse.INITIALIZER || use == ShellValueUse.ARGUMENT)
            return BsonDouble(value)
        val int32 = isInt32(value)
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
