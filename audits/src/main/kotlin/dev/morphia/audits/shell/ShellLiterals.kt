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
import org.mozilla.javascript.ast.*

/** Parses a single shell value without compiling, evaluating or executing JavaScript programs. */
object ShellLiterals {
    const val PARSER = "rhino:1.9.1/shell-literal:2"

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
            val value = ShellValueLowering(source, dialect, limits).value(wrapper.expression, 0)
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
}
