package org.mozilla.javascript

/**
 * Narrow adapter to Rhino 1.9.1's package-private lexer. This development tool runs on the
 * classpath; no JavaScript is compiled/evaluated. Keep the split package isolated here and test
 * every used lexical boundary when updating Rhino. AST lowering remains the authority on allowed
 * values.
 */
internal class AuditShellLexer(source: String) {
    private val parser =
        Parser(
            CompilerEnvirons().apply {
                languageVersion = Context.VERSION_ECMASCRIPT
                isXmlAvailable = false
                isRecordingComments = true
                setRecoverFromErrors(false)
                setIdeMode(false)
            }
        )
    private val stream = TokenStream(parser, null, source, 1)

    init {
        parser.setSourceURI("printed-documents")
        parser.currentPos = stream
    }

    // getToken's structural cases do not look ahead: sourceCursor is just past the physical
    // UTF-16 punctuation, including when the character came from unget. Logical tokenBeg/cursor
    // can undercount supplementary characters in comments; NAME ends also include lookahead.
    fun next(): AuditShellLexeme {
        val kind = stream.getToken()
        val start =
            when (kind) {
                Token.LC,
                Token.RC,
                Token.LB,
                Token.RB,
                Token.LP,
                Token.RP,
                Token.COMMA -> stream.sourceCursor - 1
                else -> -1
            }
        return AuditShellLexeme(
            kind,
            start,
            if (kind == Token.COMMENT) stream.commentType else null,
            if (kind == Token.COMMENT) stream.sourceCursor else -1,
        )
    }
}

internal data class AuditShellLexeme(
    val kind: Int,
    val start: Int,
    val commentType: Token.CommentType?,
    val commentEnd: Int,
)
