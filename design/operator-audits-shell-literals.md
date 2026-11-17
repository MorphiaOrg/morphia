# Shell parser and BSON literal normalization

This is a step 3 foundation following the strict comparator in #4519. It parses one shell
value into a fresh typed BSON tree. It does not group examples, bind variables, plan database
operations, infer expected output, generate tests or claim behavior coverage. Those tasks use
this boundary when constructing complete scenarios. The original source, UTF-8 digest, selected
shell dialect and diagnostics remain attached to every result.

## Parser selection and measured compatibility

Use the public Java AST from [Rhino 1.9.1](https://github.com/mozilla/rhino/tree/Rhino1_9_1_Release),
pinned in the audits POM. It runs on the existing Java 17 audit runtime and needs no additional
executable/helper installation. The candidate JAR SHA-256 is
`12dbb224cea124a5d32968722e8eb161c967bf15f3c9a795d1b05592b1147975`.
The alternative was [Acorn 8.19.0](https://github.com/acornjs/acorn), an actively maintained
ESTree parser that would require an additional Node runtime and coordinator. Acorn remains a
viable replacement if measured compatibility or API changes warrant it.

The explicit probe in `audits/probes/shell/` checks both candidates without executing snippets.
The input is the owned 350-page report for source bundle `9632a61a...`, plus all 1,660 JSON
files in the hash-verified legacy corpus (including 704 files under cases with lock markers),
plus eight counterexamples: 4,798 inputs. Both program and single-expression syntax modes are
measured separately using Rhino's current `VERSION_ECMASCRIPT` mode and Acorn's ECMAScript
2026 mode (both dependencies pinned). This compares each parser's current supported grammar,
not identical spec conformance; inputs and bytes are never rewritten to fix syntax. The expression mode
adds only a parenthesis envelope and a final newline, retaining the original block intact.

| Input category | Inputs | Rhino: script or expression accepted | Acorn: script or expression accepted |
| --- | ---: | ---: | ---: |
| Exact `javascript` language blocks | 2,696 | 1,980 | 1,976 |
| Legacy JSON fixture files | 1,660 | 1,343 | 1,342 |
| Eight controlled probes | 8 | 7 | 7 |

All other language labels, including malformed labels with directive options embedded in them,
remain separate in the machine receipt. Even C# or shell-command blocks can happen to parse as
JavaScript; parser acceptance does not select a scenario or establish its language.

There are ten differences in acceptance. Rhino additionally accepts mathematical assignment
forms, one command-like block and a PCRE-style regex literal that is invalid JavaScript regex
syntax; Acorn additionally accepts two C# blocks as JavaScript expressions. These are explicit
counterexamples to treating parse success as semantic coverage. The restricted lowering rejects
assignments and unknown calls, and separately validates JavaScript regex patterns with Rhino's
regex compiler. The regex compiler never matches a pattern or executes JavaScript code.

Of the 317 legacy files Rhino rejects in this syntax probe, 235 are expected-result files,
72 are data files, seven are actions and three are indexes. Most result/data failures consist
of consecutive documents outside an enclosing array. 160 of the 704 locked JSON files fail.
They require an explicit document-stream adapter or a reviewed disposition, not brace counting,
regex splitting, textual replacement or unconditional wrapping. This benchmark is neither a
migration-equivalence assessment nor an accepted coverage baseline.

## Supported value contract

`ShellLiterals.normalize(source, dialect, limits)` returns NORMALIZED with one BSON value,
UNSUPPORTED with diagnostics, or INVALID for an input encoding/size violation. A failure never
returns a partially lowered value. Unexpected implementation/fatal errors remain visible.
This is an in-process API; `BsonValue` is not serialized through the audit DTO JSON codec.

- Preserve string contents, object key order, nested array order, duplicates in arrays,
  numeric constructors, dates, timestamps, ObjectIds, regex options and binary subtypes/bytes.
- Reject duplicate decoded object keys, NUL field names/patterns, prototype-setting properties,
  unpaired Unicode surrogates, object methods/accessors/spreads, computed keys, sparse arrays,
  arbitrary calls, functions, unresolved bindings and computed values. Signed primitive numeric
  constants are supported; arithmetic and unary coercion of constructor objects are not.
- A closed constructor allowlist supports explicit numeric, date, ObjectId, timestamp, binary,
  UUID and regex values. Unknown constructors, nested constructor expressions and unsupported
  argument forms remain unresolved. No dynamic defaults such as current-time dates or generated
  ObjectIds are synthesized. Date strings require an explicit offset and millisecond precision;
  local-time/date-only strings and sub-millisecond policies need separate reviewed handling.
- `Date(...)` as a function produces runtime text and is rejected. Explicit `new Date(...)`
  and direct `ISODate(...)` are distinct supported forms. Timestamp defaults/zero timestamps
  with server-generated behavior remain unsupported rather than becoming fixed expectations.
- Regex literals/`RegExp` use the pinned JavaScript regex compiler for syntax validation;
  supported JavaScript flags are `i` and `m`. Compilation has length/nesting guards and never matches.
  `BSONRegExp` is explicit BSON pattern data (including `s` and `x`); its PCRE validity is a server concern.
  JavaScript `s`/`g` flags have different serialized BSON behavior and remain unsupported. String
  RegExp patterns requiring source canonicalization (empty, slash or line terminators) remain
  unsupported; regex literal source text is retained without applying string coercion rules.
- Limits bound source characters, lowered AST depth/nodes, and regex compiler input. Parser
  recovery and IDE partial-tree mode are disabled. Ordinary Maven execution never acquires docs,
  a Node runtime, parser sources or an upstream checkout.

Shell dialect is required because [MongoDB documents different numeric and undefined storage
in mongosh and the legacy shell](https://www.mongodb.com/docs/mongodb-shell/reference/compatibility/).
MONGOSH uses Int32 for fitting whole numbers and Double otherwise, preserving negative zero;
LEGACY_MONGO uses Double for ordinary numeric literals. UNSPECIFIED still permits values that
are Double under both dialects (fractions, large numbers, signed zero, NaN and infinity). Their `undefined` BSON storage is null
and undefined respectively. UNSPECIFIED rejects those ambiguous defaults while allowing common
explicit constructors. Modern constructor aliases require MONGOSH. The normalizer deliberately restricts NumberLong to strings outside the reviewed legacy
profile, even where a shell version accepts deprecated numeric arguments. Numeric `Long(n)`
means low 32 bits with zero high bits, not `fromNumber(n)` or NumberLong(n); this distinction
is preserved for negative/large safe-integer inputs. Double string coercion is unsupported;
Java floating-point grammar must not substitute for JavaScript conversion semantics.

AST spans are optional parser hints: bounded half-open UTF-16 offsets into the original source,
excluding the synthetic envelope. Rhino can attach unusable envelope extents to inner nodes.
Its pinned UnaryExpression implementation also drops operatorPosition, giving in-range extents
that omit a sign/comment. Both kinds of unusable spans are omitted. Downstream evidence must
verify hint text against raw source before asserting an exact span. Syntax errors retain the complete unchanged block rather than fabricating exact
locations. Invalid/oversized source has no digest if UTF-8 identity could not be computed safely.

## Validation and next integration

Offline tests compare normalized results with the strict BSON comparator, exercise constructor
range/type/dialect errors, preserve string whitespace and EOF comments, check spans, test size/
depth/node/regex limits and verify that arbitrary Java/JavaScript calls never create a file.

The next scenario slice must add ordered setup/actions, explicit binding/dependency provenance,
multiple collections and indexes, query/options and update/pipeline operations, result streams
and expected errors. Select source blocks from documentation structure and reviewed language/
dialect policy. Do not infer a dialect from whether a parser accepts the snippet. Attach these
values to source evidence and the persisted descriptive-name registry, then measure semantic
coverage on representative source-backed cases. No existing method/folder, lock or expectation
is migrated by this literal-only foundation.

## Constructor source verification

Review confirmed the Long distinction in the official [js-bson constructor at revision
7348ff59](https://github.com/mongodb/js-bson/blob/7348ff59c70c4cbe39d39ba6dc3f2291569e6848/src/long.ts)
and [mongosh shell wrappers at revision bd8133d2](https://github.com/mongodb-js/mongosh/blob/bd8133d2fb80bcb5951542c9b0480608dc5346cf/packages/shell-bson/src/shell-bson.ts).
For example, `Long(-1)` represents 4294967295, while `Long('-1')` and NumberLong('-1')
represent -1. Numeric Long inputs are restricted to exact safe integers; fractional/unsafe
coercions and unsupported signatures remain unresolved. Representation exceptions are caught
only at explicit library parse calls, allowing unexpected dispatch/implementation errors to surface.

The preserved [benchmark receipt](../audits/probes/shell/receipt.json) was generated by the
checked-in probe after aligning parser modes. It records helper hashes, runtime versions,
owned-document and legacy corpus hashes, every language group and all ten acceptance differences.
Its corpus SHA is `cb074470e038340aeccfc4126a02ec0545bd7b46a1b335884c537bdc4da7570d`;
its TSV input SHA is `0177bc33e5723cc1fcac603c0865ba4cb3a1a2d88d8705ba5626699e1a15c7b4`.
Those input identities and the table counts reproduce the initial exploratory probe. This
receipt documents a compatibility experiment and cannot promote a catalog or prove scenario coverage.

## BSON serialization boundary checks

The pinned [js-bson serializer](https://github.com/mongodb/js-bson/blob/7348ff59c70c4cbe39d39ba6dc3f2291569e6848/src/parser/serializer.ts)
serializes native JavaScript regex source and selected flags differently from explicit BSONRegExp
pattern/options. The normalizer therefore permits only the proven `i`/`m` JavaScript subset and
rejects string patterns whose source would be escaped or synthesized. Explicit BSONRegExp keeps
its supplied BSON pattern/options. Valid JavaScript syntax alone is insufficient to infer BSON.

Integer and decimal constructor strings use ASCII grammars. Finite Decimal128 input is bounded
before library parsing; special values use locale-independent constants. Per the pinned
[js-bson decimal parser](https://github.com/mongodb/js-bson/blob/7348ff59c70c4cbe39d39ba6dc3f2291569e6848/src/decimal128.ts),
NaN spellings/signs yield the canonical positive NaN encoding, while infinity retains its sign.
Java's Unicode-digit acceptance and default-locale case folding must not change normalization.

Printed multi-document results use an isolated adapter to the same pinned lexer before strict AST
lowering. See the [printed-result design](operator-audits-shell-results.md) for its package-visibility
and physical-position contract and the additional upgrade regressions. Literal storage semantics
remain separate from printed result constraints.
