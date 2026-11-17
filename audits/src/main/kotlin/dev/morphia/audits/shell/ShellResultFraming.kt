package dev.morphia.audits.shell

import org.mozilla.javascript.AuditShellLexer
import org.mozilla.javascript.Token

/** Token-based framing only; the maintained AST parser still validates every untouched value. */
internal object ShellResultFraming {
    private val opening = setOf(Token.LC, Token.LB, Token.LP)
    private val structural = opening + setOf(Token.RC, Token.RB, Token.RP)
    private val comments =
        setOf(Token.CommentType.LINE, Token.CommentType.BLOCK_COMMENT, Token.CommentType.JSDOC)
    private val templates =
        setOf(
            Token.TEMPLATE_LITERAL,
            Token.TEMPLATE_CHARS,
            Token.TEMPLATE_LITERAL_SUBST,
            Token.TAGGED_TEMPLATE_LITERAL,
        )

    fun frames(
        source: String,
        limits: ShellLiteralLimits,
        budget: ShellValueBudget,
    ): List<ShellSpan> {
        val lexer = AuditShellLexer(source)
        val stack = ArrayDeque<Int>()
        val frames = mutableListOf<ShellSpan>()
        var start = -1
        var array = false
        var comma = false
        var sawComment = false
        var triviaEnd = 0
        fun fail(code: String, message: String): Nothing =
            throw ShellLiterals.UnsupportedLiteral(code, message, null)
        fun whitespace(begin: Int, end: Int) {
            check(begin in 0..end && end <= source.length) {
                "Pinned lexer trivia-position contract changed"
            }
            if ((begin until end).any { !isSpace(source[it]) })
                fail(
                    "DOCUMENT_STREAM_SYNTAX",
                    "Only explicit whitespace/comments may separate result roots",
                )
        }
        fun punctuation(kind: Int): Char =
            when (kind) {
                Token.LC -> '{'
                Token.RC -> '}'
                Token.LB -> '['
                Token.RB -> ']'
                Token.LP -> '('
                Token.RP -> ')'
                Token.COMMA -> ','
                else -> error("Not a structural token")
            }
        fun verify(kind: Int, at: Int) {
            check(at in source.indices && source[at] == punctuation(kind)) {
                "Pinned lexer structural-position contract changed"
            }
        }
        while (true) {
            if (!budget.take()) fail("VALUE_LIMIT", "Printed results exceed token work limit")
            val token = lexer.next()
            when (token.kind) {
                Token.ERROR -> fail("DOCUMENT_STREAM_SYNTAX", "Invalid printed-result token")
                Token.EOF -> {
                    if (stack.isNotEmpty() || comma || frames.isEmpty())
                        fail("DOCUMENT_STREAM_SYNTAX", "Empty or unfinished printed results")
                    whitespace(triviaEnd, source.length)
                    return frames
                }
                Token.EOL -> continue
                Token.COMMENT -> {
                    if (token.commentType !in comments)
                        fail(
                            "DOCUMENT_STREAM_SYNTAX",
                            "HTML-like comments and hashbangs are not printed result syntax",
                        )
                    if (stack.isEmpty()) {
                        // Locate only the comment introducer after validated whitespace. Its body
                        // and physical end are owned by the maintained lexer, never rescanned.
                        var commentStart = triviaEnd
                        while (
                            commentStart < source.length && isSpace(source[commentStart])
                        ) commentStart++
                        check(
                            commentStart <= token.commentEnd && token.commentEnd <= source.length
                        ) {
                            "Pinned lexer comment-position contract changed"
                        }
                        if (
                            !source.startsWith("//", commentStart) &&
                                !source.startsWith("/*", commentStart)
                        )
                            fail("DOCUMENT_STREAM_SYNTAX", "Unexpected text before result comment")
                        triviaEnd = token.commentEnd
                        sawComment = true
                    }
                    continue
                }
                Token.DIV,
                Token.ASSIGN_DIV ->
                    fail(
                        "PUBLISHED_REGEX",
                        "Printed JavaScript regex requires explicit disposition",
                    )
            }
            if (token.kind in templates)
                fail("UNSUPPORTED_AST", "Template syntax is unsupported in printed results")
            if (stack.isEmpty()) {
                if (token.kind == Token.COMMA) {
                    if (frames.isEmpty() || comma || array)
                        fail("DOCUMENT_STREAM_SYNTAX", "Unexpected result separator")
                    verify(token.kind, token.start)
                    whitespace(triviaEnd, token.start)
                    triviaEnd = token.start + 1
                    comma = true
                    continue
                }
                if (token.kind != Token.LC && token.kind != Token.LB)
                    fail(
                        "DOCUMENT_STREAM_SYNTAX",
                        "Printed results require document roots or one document array",
                    )
                whitespace(triviaEnd, token.start)
                if (
                    frames.isNotEmpty() &&
                        (array ||
                            token.kind == Token.LB ||
                            !comma && !sawComment && token.start == triviaEnd)
                )
                    fail(
                        "DOCUMENT_STREAM_SYNTAX",
                        "Result roots require whitespace/comment or comma separation; arrays cannot be mixed with streams",
                    )
                start = token.start
                array = token.kind == Token.LB
                comma = false
                sawComment = false
            }
            if (token.kind in structural) {
                verify(token.kind, token.start)
                if (token.kind in opening) {
                    stack.addLast(token.kind)
                    if (stack.size > limits.maximumDepth)
                        fail("VALUE_LIMIT", "Printed results exceed nesting limit")
                } else {
                    val open =
                        when (token.kind) {
                            Token.RC -> Token.LC
                            Token.RB -> Token.LB
                            else -> Token.LP
                        }
                    if (stack.removeLastOrNull() != open)
                        fail("DOCUMENT_STREAM_SYNTAX", "Unbalanced printed results")
                    if (stack.isEmpty()) {
                        frames += ShellSpan(start, token.start + 1)
                        triviaEnd = token.start + 1
                        sawComment = false
                    }
                }
            }
        }
    }

    private fun isSpace(value: Char) =
        value in "\t\n\u000b\u000c\r\u2028\u2029\ufeff" ||
            Character.getType(value) == Character.SPACE_SEPARATOR.toInt()
}
