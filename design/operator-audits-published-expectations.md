# Published read-result expectations

Documentation output is an independent oracle, but printed shell text does not always identify a
single BSON value. Previously, a plain whole number blocked an otherwise owned scenario because
choosing Int32 or Double would fabricate evidence. Ordered-program schema 2 instead retains a
closed constraint tree alongside the original source. Typed legacy document fragments continue to
produce immutable BSON snapshots. Errors remain source-backed error assertions. Exactly one of
these expectation forms is present. No serialized result/pattern input is hydrated; regenerate from
source requests. The semantic hash includes the meaning-model version and published profile; schema/parser versions
are explicit compatibility boundaries for prior program artifacts.

## Display profile

The profile is restricted to modern mongosh find/aggregate read results. Its source basis is
[mongosh bd8133d2](https://github.com/mongodb-js/mongosh/blob/bd8133d2fb80bcb5951542c9b0480608dc5346cf/packages/service-provider-node-driver/src/node-driver-service-provider.ts)
(`promoteLongs: false`, default value promotion) and the
[BSON decoder 7348ff59](https://github.com/mongodb/js-bson/blob/7348ff59c70c4cbe39d39ba6dc3f2291569e6848/src/parser/deserializer.ts).
These pins document the modeled defaults; they do not assert which shell originally authored an
upstream example. A plan explicitly selecting MONGOSH opts into this profile. Server execution and
its exact runtime/profile attestation remain a later stage. Legacy/unspecified shell display,
write-result objects and aggregate options overriding BSON decoding remain unsupported. DBRef-shaped
published documents (both `$ref` and `$id` keys), including nested ones, are conservatively unsupported
until an explicit DBRef rule models decoder promotion/display. Input documents retain their existing
storage interpretation.
Find's projection argument remains a projection, including fields named like decoder options.

- Plain finite integral numbers in the Int32 range allow Int32 or the same Double. Long remains
  wrapped under these defaults. Other plain numbers require Double. Negative zero excludes Int32
  and positive zero; no numeric tolerance or cross-type coercion is inferred.
- Explicit Int32, Double and Long constructors retain typed assertions. Plain text allows BSON
  string or legacy BSON symbol, both promoted to JavaScript strings by the decoder.
- Printed Double NaN does not prove its payload bits. It asserts Double NaN; the typed comparator
  independently retains raw-bit parity. Decimal128 asserts its canonical printed text, including
  scale and signed zero, rather than inventing unprinted BID/NaN payload bits.
- Bare JavaScript regex literals/RegExp constructors cannot establish raw BSON regex pattern/options
  after decoder/JavaScript normalization and are unsupported. Explicit BSONRegExp assertions retain
  their typed pattern/options. Undefined and unknown values require explicit future handling.

## Structure and matching

Document fields and nested arrays are explicit nodes; missing/extra fields and array order remain
observable. JavaScript own-property enumeration sorts canonical array-index keys before ordinary
string keys. Input object lowering now follows that rule too. Published matching projects only that
loss of BSON order; ordinary string-key relative order is preserved. See
[ECMAScript OrdinaryOwnPropertyKeys](https://tc39.es/ecma262/multipage/ordinary-and-exotic-objects-behaviours.html#sec-ordinaryownpropertykeys).
Typed reference-versus-DSL comparison still preserves every BSON type, key order and numeric bit.
The two comparisons must both pass when execution is implemented; a published constraint alone
cannot attest that the generated DSL uses the correct BSON type.

Ordered results compare row by row. Explicit unordered results form a multiset: bipartite matching
handles overlapping constraints without consuming the only match of a more specific row. Duplicate
counts matter. There is no inferred nested-array sorting, field omission, epsilon or output generated
from the implementation under test.

All pattern and BSON trees are validated before returning any mismatch, including hidden rows and
subtrees. Shared depth/work/result/pair bounds reject cycles, Java nulls, raw/lazy BSON containers,
unpaired surrogates, NUL keys and duplicate pattern keys. Repeated captured-binding expansion has a
shared 16 Mi-character budget before UTF-8 encoding. Concrete snapshot bytes and serialized
published pattern roots share `maximumStoredValueBytes` (8 MiB default); published costs include
JSON-escaped controls, Unicode, keys and type metadata, measured once per root without allocating
serialized output. Exact leaves conservatively pay both retained snapshot and serialized-pattern
costs. Exhaustion yields `STORED_VALUE_LIMIT` and preserves source without commands. The budget
bounds rendering expansion within each program. Program artifacts have a 64 MiB default cap
(maximum 128 MiB), checked before hashing/output. A per-program oversized meaning/result becomes an
unsupported program with an
`ARTIFACT_LIMIT` problem, so extraction preserves that source group and continues. The final output
check remains a backstop: if even the failure artifact exceeds a caller-supplied cap, the operation
rejects output rather than emitting a truncated report. The independent whole-report byte cap
remains a batch-size boundary: many legitimate bounded programs or large raw-source provenance can
still require a smaller extraction batch. Per-program failure isolation does not promise an
unbounded aggregate artifact.
Callers must not concurrently mutate in-process lists/BSON values. No patterns or snapshots are
accepted as an executable artifact or a behavioral coverage result.

## Validation and remaining work

The published probe reuses the prior extraction plan, owned document artifact and source bundle.
Its receipt records repeatability, reference/foreign group counts, descriptive-name stability and
program-normalization changes. This remains a source/normalization experiment, not an execution
attestation. Dataset/setup/constraint closure, reviewed legacy section grouping, the real descriptive
name backport, curations, generation, execution, monitoring and issue publication remain separate
work. No core tests/data, locks or accepted registry/catalog are changed by this slice.
