# Printed shell document results

Native legacy sections do not always render results as one JavaScript value. The pinned `$abs`
example contains an insert command, an aggregate command and four printed documents separated by
newlines. Treating that output as a JavaScript program or a typed legacy JSON fixture either fails
syntax or invents one BSON type for plain whole numbers. `SHELL_RESULTS` is an explicit expected-only
format for this presentation grammar. The original block, its hash, order and all text remain intact.
It is never selected by retrying a failed parser or used to infer a dataset/namespace. Native IO
parents already establish output ownership, so their reference outputs select SHELL_RESULTS
explicitly. The extraction normalizer advances to native-io:2; grouping identities remain independent
of this program-format choice. Standalone legacy sections still require their ownership layer.

The grammar accepts one document array or a stream of document roots separated by whitespace,
comments or single commas. Streams reject leading/trailing/repeated separators and adjacent roots without a
separator. Arrays mixed with streams, statements and non-document roots are rejected. The single-array
case follows the closed JavaScript-literal grammar. An explicit empty array is
a known empty result; blank/comment-only text is not. Every complete, untouched fragment is parsed
through the existing strict Rhino AST lowerer into published constraints. Explicit constructors keep
their types; plain integers retain their Int32/Double alternatives. There is no inserted comma,
brace-text counting, string/comment rewriting, or document code execution.

## Lexer adapter and position contract

Rhino 1.9.1 exposes its maintained AST parser but keeps `TokenStream` package private. A small Kotlin
adapter under `org.mozilla.javascript` isolates the split package; audits runs on the classpath and
this adapter never enters core artifacts. This avoids a second JavaScript grammar or runtime just
for framing. The dependency remains pinned and compilation/tests gate any upgrade. JPMS/module-path
support is not claimed. Reassess the adapter if Rhino exposes a public tokenizer or another parser
becomes preferable; do not add reflective access to private internals.

The lexer handles quoted delimiters and explicit line/block/JSDoc comments. HTML-like comments
and hashbangs are rejected at every depth because they can hide printed rows or fields. Outside
roots, every character must be accounted for as whitespace, an optional comma or a whitelisted
lexer-owned comment. Invisible format characters outside those comments cannot become separators. A bounded stack matches only structural tokens;
the AST parser remains the authority on valid expressions. Bare regex/slash and template syntax are
rejected before lexer state could confuse boundaries; explicit BSONRegExp constructors still work.
Token and AST work share the program budget; nesting and stored-value/artifact limits remain active
across every fragment. A later malformed root discards all commands/expectations, retaining sources.

The pinned lexer's logical cursor can undercount supplementary characters in line comments, while
literal/block-comment fast paths reset it. NAME token ends can include lookahead. Structural tokens
have no lookahead: the adapter uses the physical UTF-16 `sourceCursor - 1`, and framing verifies the
actual punctuation at that position. Arbitrary token spans are never published. AST hints for new
fragments containing surrogate code units are conservatively omitted; the full source remains available, and verified boundaries are used only for parsing. For other fragments, offsets map back into the original full block. Existing
literal/source formats retain their diagnostic contract. Tests cover Unicode comments/identifiers,
CRLF/BOM, nested data, constructors, delimiter strings and malformed tails.

The ordered-program parser identity advances to `ordered-program:3`. The meaning-model and output
schema remain version 2: streams and arrays expressing the same result have the same semantic hash.
Source provenance records the new explicit format. Read-result profile requirements and strict typed
reference-versus-DSL comparison remain independent and unchanged.

## Pinned experiment and next layer

The checked-in request fixture copies three owned `$abs` code blocks exactly. Its selection record
assigns reviewed roles by code digest and records parser-reported physical provenance. The replay
first verifies the bundle/owned report through native extraction, matches every selected code block
to the fixture without an ordinal, then normalizes that request twice and retains a receipt. It also
retains ordinary SHELL's syntax rejection for the same output, establishing the format difference.
This is a framing/normalization experiment. It does not automatically assemble legacy sections,
reconcile a generic `main` title, migrate tests/data, attest a server runtime or claim behavior coverage.

Source extraction still reports the standalone blocks as unassigned until the next section-assembly
layer supplies ownership and roles. That layer must derive setup from ancestors or explicit reviewed
selectors, preserve ordering/variables and foreign variants, and avoid borrowing the first sibling's
data/index. Descriptive naming then uses recovered headings/context and persisted reconciliation.
The source-format work here is a prerequisite; it does not waive those ownership requirements.

The structural/trivia position guards deliberately abort a batch on lexer-contract drift rather than
blaming documentation syntax. They are upgrade incompatibilities, not ordinary unsupported snippets.
Ordinary SHELL AST hints around supplementary line comments/identifiers remain a tracked follow-up;
hints are never identity or rewrite authority. The new printed-fragment path omits uncertain hints.
