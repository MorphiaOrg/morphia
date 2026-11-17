# Ordered scenario normalization

The previous regex/brace-count extraction cannot reliably distinguish setup, actions and
published output, preserve variable dependencies or determine which collection receives an
operation. The replacement's first program milestone accepts explicitly declared source blocks
and lowers a closed AST grammar into an ordered command model. It does not execute documentation
JavaScript, run a server or claim behavior coverage. Automatic grouping of native documentation
nodes remains the next layer; guessing a language or a neighboring output block here would hide
extraction uncertainty.

## Input and output contract

`ShellProgramRequest` schema 1 specifies a shell dialect, result order and ordered source blocks.
Each block has a stable caller-provided identity, raw source, role and declared format. These
identities are source references, not generated example names; persisted descriptive names still
come from the naming registry. Command selectors use a block and local command offset only to
associate operations within the same request.

Roles are `SETUP`, `ACTION`, `EXPECTED` and `EXPECTED_ERROR`. Formats are `SHELL`, expected-only `SHELL_RESULTS`,
`LEGACY_DOCUMENTS` and error-only `TEXT`. A legacy setup/action fragment needs its explicit logical
namespace. Null database means the future runner's isolated default database, not a real database
name. Shell blocks derive namespaces from their AST; conflicting external namespace metadata is
invalid. The request retains every declared field in source provenance.

A normalized result contains ordered commands, captured binding/dependency records, expectations,
raw source hashes and a semantic hash. Typed operation classes distinguish inserts (including
one-document versus batch intent), index definitions, finds and ordered cursor modifiers,
aggregation pipelines, replacement documents and updates. BSON snapshots preserve exact numeric
types, document key order and pipeline order. Snapshots own their encoded data and return fresh
in-process trees. Their representation is a bounded BSON document containing a `value` field;
base64 is transport, not canonical Extended JSON.

The result's semantic hash includes dialect, operation order, namespaces, typed BSON values and
expectation ownership/order. It excludes block names, binding names, comments, source locations
and whitespace. A future grouped scenario fingerprint must also include source-selected setup
and other dependencies before passing it to name reconciliation. This hash alone is not an
accepted identity registry or proof that two language variants are equivalent.

An unsupported program returns no commands, expectations or semantic hash. All validated source
blocks remain in the report, including blocks after the failure. Binding metadata may describe
work completed before rejection, but cannot be executed. Invalid requests can fail validation
before source provenance is populated. An unexpected implementation failure remains visible;
it is not silently downgraded to an unsupported documentation example.

## Closed shell grammar

The maintained Rhino 1.9.1 parser supplies the AST with XML, recovery and IDE mode disabled.
No JavaScript evaluation, host-object bridge, dynamically compiled program or database connection
exists. Plain immutable values share the lowerer and dialect rules documented in the
[shell literal decision](operator-audits-shell-literals.md).

Supported top-level forms are pure `var`/`let`/`const` declarations, direct reassignment to existing
mutable bindings, and explicit collection commands. Each binding event captures its initializer
AST and earlier binding environment. It is re-lowered in the context of its use: initializer
primitives and BSON constructor arguments retain JavaScript primitive semantics, while stored
values follow the selected shell dialect. A cached serialized Int32 cannot accidentally replace
a primitive number passed to `Double` or another constructor. Reassignment cannot change a prior
captured initializer. Namespace aliases have their own binding kinds and dependencies.

Destructuring, loops, functions, arbitrary calls, forward references, object/property mutation,
query-result bindings, computed values and builtin shadowing are unsupported. These restrictions
make captured pure initializer semantics meaningful; this is not a general JavaScript interpreter.
A shared work/depth budget limits repeated binding expansion as well as direct AST traversal.

Collections can be referenced through `db.name`, constant bracket access, `getCollection`,
`getSiblingDB` and captured database/collection aliases. Names use the pinned mongosh predicates:
database names reject slash, backslash, dot, space, double quote and NUL; collection names reject
dollar and NUL. The normalizer also rejects blank and overlong names. Database methods and internal/prototype
properties cannot masquerade as collections; `getCollection` is explicit for a reserved name.
The database-member guard and cursor names were checked against pinned official mongosh source:

- [database](https://github.com/mongodb-js/mongosh/blob/bd8133d2fb80bcb5951542c9b0480608dc5346cf/packages/shell-api/src/database.ts), blob `83815dda295fc692b20cd870afdcd9eff0887370`;
- [cursor](https://github.com/mongodb-js/mongosh/blob/bd8133d2fb80bcb5951542c9b0480608dc5346cf/packages/shell-api/src/cursor.ts), blob `f5398595c25dc1834ab8ccb99a182cd6d74d51b8`;
- [shared cursor](https://github.com/mongodb-js/mongosh/blob/bd8133d2fb80bcb5951542c9b0480608dc5346cf/packages/shell-api/src/aggregate-or-find-cursor.ts), blob `278d674d491cc1d13e0cf3c0410aa84382eef2cd`;
- [abstract cursor](https://github.com/mongodb-js/mongosh/blob/bd8133d2fb80bcb5951542c9b0480608dc5346cf/packages/shell-api/src/abstract-cursor.ts), blob `b842aaaea1bf5a63e7a4b27971ef28e16d2dc45f`.

Supported collection methods are `insertOne`, `insertMany`, legacy `insert`, `createIndex`, `find`,
`aggregate`, `updateOne`, `updateMany`, legacy `update` and `replaceOne`. Finds retain ordered
`projection`, `sort`, `limit`, `skip`, `hint`, `collation`, `batchSize` and `maxTimeMS` modifiers.
`pretty` retains a cursor; `toArray` is terminal. An aggregate `explain` option is unsupported
because explain returns a different result type and needs its own operation model. Unsupported modifiers or further calls on a
materialized array reject the complete program. Query-result display/materialization is not a
separate server operation. Pipelines must be arrays of one-operator documents; unknown operator
tokens remain data, since normalization does not prove server availability or API support.

## Legacy data and expected outcomes

`LegacyDocuments` is an explicit legacy fixture data adapter, using Java BSON 5.14.0's maintained
`JsonReader`. It accepts document streams, comma-separated document lists and a single document
array through two declared framing grammars: original input and a list envelope. No commas are
inserted, braces counted, strings/comments stripped, dates rewritten or examples reshaped. If
both grammars succeed, their ordered BSON meanings must agree. Duplicate fields, unsupported
trailing input, invalid UTF-8/BSON strings and resource-limit failures discard the whole result.
An explicit `[]` represents a known empty result; blank input is not proof of an empty result.

This format differs deliberately from shell literal interpretation. Extended JSON wrappers can
represent BSON typed values in legacy data; an identical shell object can instead contain a
literal `$numberLong` field. The caller must select the format from source provenance or reviewed
migration metadata. Parser acceptance is not a license to change the interpretation of existing
fixtures. The old core fixture reader is still untouched and will be migrated after independent
semantic assessment.

Expected result blocks contain a document/array in SHELL, an explicit
[printed document stream](operator-audits-shell-results.md) in SHELL_RESULTS, or declared typed
legacy documents. Program result schema 2 records modern shell output as explicit
[published read-result constraints](operator-audits-published-expectations.md), independently from
exact typed legacy expectations. Displayed whole numbers never inherit input-storage defaults;
Int32/Double alternatives remain explicit. Unknown display profiles and undefined remain
unsupported. Source grouping must preserve uncertainty, never insert constructors or change formats
just to make an output parse. Expected values can use earlier pure bindings with captured
context/dependencies. A program with exactly one action can omit
`expectedFor`; multiple actions require a selector, including mixed write/read commands. Duplicate
expectations for one action need an upstream grouping decision. The normalizer never chooses the
nearest action or silently combines language tabs. Error blocks require text plus explicit code,
code name and/or message metadata that appears in that source; no error is invented from prose.
Server assertions and compatibility of a particular expectation with a command remain runner work.

## Limits, transport and verification

Defaults bound source blocks (64), binding events (512), commands (512), total source characters
(1 MiB), AST work/depth, individual snapshots (1 MiB) and total stored snapshots (8 MiB). BSON
encoding checks the output budget while writing, validates input and requires a strict lossless
round trip before accepting the snapshot. Node/depth bounds and strict comparison are inherited
from the earlier comparator. Invalid source strings, BSON cstrings and private JavaScript BSON
protocol keys cannot enter a shell value unnoticed.

`ProgramsMainKt <program-request.json>` reads at most 16 MiB of strict UTF-8 request data and
emits candidate JSON. Exit codes are 0 normalized, 3 unsupported, 2 invalid. JSON decoding rejects
unknown fields, duplicate keys, coercion, null primitives and trailing data. Transport requests
require explicit `resultOrder` and a format for every block, including where the Kotlin API has
convenience defaults. Reports have no
encoded-input factory or operation-class polymorphic reader; future consumers regenerate the
model from source requests rather than hydrating arbitrary encoded BSON. The synthetic
`audits/src/test/resources/programs/join-inventory.json` exercises multi-collection setup, an
index, a captured pipeline and explicit expected-result ownership. It is not upstream evidence.

An exploratory replay of the earlier frozen 4,798-input shell probe completed without unexpected
exceptions. With an explicitly assumed MONGOSH dialect and every input treated as one action,
1,043/3,130 documentation blocks and 140/1,660 legacy files normalized; four of eight deliberate
probes normalized. The separate declared legacy-document adapter accepted 1,484/1,660 fixture
files. These are parser measurements, not scenario coverage, language attribution, migrated
fixture approval or expected-result validation. The corpus identity is
`cb074470e038340aeccfc4126a02ec0545bd7b46a1b335884c537bdc4da7570d`; input TSV identity is
`0177bc33e5723cc1fcac603c0865ba4cb3a1a2d88d8705ba5626699e1a15c7b4`, matching the earlier
checked-in shell-probe receipt. Temporary replay helper/results are recorded in the progress log.

Next work must group owned documentation AST nodes using explicit language/tab/admonition roles,
carry setup and include/snippet dependencies, reconcile persisted descriptive identities and
independently assess legacy fixtures before changing core tests. Execution, API binding,
version constraints, curated overrides and generation remain separate stages. `NORMALIZED`
continues to leave serialization/behavior coverage unassessed until the appropriate attestation.

Published pattern roots and typed snapshot bytes now share the 8 MiB stored-value budget
(`maximumStoredValueBytes`); JSON escaping/keys count for published text. See the
[published-expectation contract](operator-audits-published-expectations.md) for per-program versus
whole-report caps.
