# BSON comparison contract

This is the first deliverable of step 3 in `mongodb-operator-audits.md`. It supplies an
independent oracle for scenario normalization and later migration of curated expectations.
The implementation lives in `dev.morphia.audits.semantics`; it accepts driver `BsonValue`
trees directly, using plain materialized `BsonDocument`/`BsonArray` containers. It does not
execute shell code, access a server, infer expected output, or change existing core assertions. The BSON dependency uses the repository's driver version;
JSONAssert is a test-only dependency for the compatibility probes below.

## Default comparison

`BsonComparison.compare(expected, actual)` checks the complete BSON type and value tree.
Missing and null differ. Extra fields fail. Every array and pipeline retains its order and
duplicate counts. Document key order is preserved at every level, including sort specifications,
compound index keys, and embedded documents used in equality comparisons. Regex pattern/options,
binary subtype/bytes, ObjectIds, timestamp components, dates, JavaScript source and scope,
and deprecated BSON scalar types retain their distinctions. JavaScript is compared as data.

Double values compare by raw IEEE bits, including signed zero and NaN payloads. Decimal128
values compare both encoded words, retaining exponent/scale and signed zero. These deliberately
conservative defaults make representation changes visible; later reviewed scenario policy can
relax finite numeric representation without collapsing BSON types. Values already normalized
by the driver's constructors (for example regex option ordering) cannot be distinguished by
this API. Raw/lazy documents (including `RawBsonDocument` and `BsonDocumentWrapper`) and container
subclasses are INVALID. They can preserve duplicate keys, decode lazily or perform unbounded
lookups; this comparator never silently materializes them. The future shell/transport layer must
reject duplicate keys before creating plain `BsonDocument` containers.

The result is EQUAL, DIFFERENT (with the first mismatch's pointer and category), or INVALID
(with an input/resource diagnostic). The whole input is validated before comparison, so an early
mismatch cannot conceal a cyclic or excessively deep subtree. Shared acyclic subtrees are valid.
Java null BSON values are INVALID; BSON null uses `BsonNull`.
Inputs and policy collections must not be mutated concurrently with comparison.

## Explicit policies

Policy paths use RFC 6901 pointers: the empty string is the root, `/` is an empty field name,
`/a~1b` names field `a/b`, and `/~0` names field `~`. Array indices are decimal path segments.
There are no wildcards, recursive relaxations, or implicit omission rules.

- `unorderedDocuments` ignores key order at exactly the named document. Its fields and nested
  values still have to match; child documents retain their own ordering policy.
- `numericTolerances` accepts finite values of the same BSON numeric type at exactly the named
  path when `abs(a-b) <= absolute + relative * max(abs(a), abs(b))`. Decimal arithmetic is used;
  doubles use their canonical decimal round-trip value. Absolute and relative bounds are
  nonnegative and precision/scale bounded. NaN and infinity only match identical encodings.
  A zero tolerance explicitly allows equal finite numeric values with different representations
  (for example Decimal128 `1.0`/`1.00` or signed zero). It never permits INT32/INT64/DOUBLE/DECIMAL128
  interchange. Policies on absent or inapplicable paths have no effect.
- JavaScript-with-scope uses the synthetic `/$scope` child for its scope document; source text
  always matches exactly.

`compareResults` accepts lists of result documents. ORDERED checks row position. UNORDERED
compares only the top-level result multiset, preserving duplicates and all nested array order.
Policy paths are relative to each row, independent of row position; reported ordered mismatches
include their row index. Unordered mismatch pointers identify the expected row at which matching fails.

Tolerance is not transitive. Greedy removal is therefore unsound: expected values `[1,0]` and
actual values `[0,2]`, with absolute tolerance 1, require assigning 1 to 2 and 0 to 0. The
comparator uses deterministic bipartite matching with augmenting paths. Input traversal,
comparison and matching share a work budget. Depth, result count and pair count also have
explicit limits. Exhausting a limit returns INVALID; it never returns equality or a missing
operator claim. Large cases must select reviewed bounded limits or split into smaller scenarios.

## Compatibility probes and migration gate

`BsonComparisonTest` runs the old JSONAssert modes and the new comparator on the same controlled
counterexamples. These reproduce the comparison modes in `core/.../TestBase.java` without a
MongoDB instance or the legacy helper's database/codec dependency.

| Probe | Old comparison | New default | Required disposition |
| --- | --- | --- | --- |
| INT32 1 vs DOUBLE 1.0 or INT64 1, relaxed JSON | strict accepts | TYPE difference | Preserve BSON type; any intentional compatibility needs explicit future policy. |
| Reversed compound sort keys | strict accepts | KEY_ORDER difference | Keep order; changing it changes sort priority. |
| Unexpected result field | lenient accepts | EXTRA_FIELD difference | Reject; expected output must describe the complete intended result. |
| Reordered nested array | lenient accepts | VALUE difference | Reject; top-level unordered result policy cannot relax nested arrays. |
| Reordered pipeline stages | lenient accepts | difference | Reject; stage order is semantic. |
| Reordered result rows | strict rejects; lenient accepts | ORDERED rejects; UNORDERED accepts | Choose result ordering from the scenario's sort/expectation evidence. |
| Decimal scale; double signed zero | strict rejects decimal scale; accepts double signed zero | difference | Preserve by default; reviewed zero tolerance may permit finite equality. |

These are regression probes, not a completed assessment of the 556 curated cases. Actual
runtime outputs are not available in the frozen inventory and must not be invented. Before
removing a lock or migrating its expectation, the scenario runner must record both old and
new outcomes against the same actual output and classify differences as an explicit compatible
policy, an incorrect expectation, or a Morphia defect. Keep the old case until that disposition
and both mapper runs pass. A permissive old result is never the new oracle.

## Remaining step 3 work

Select and pin a maintained shell AST parser using the frozen corpus; retain source spans,
restrict constructors and pure constant expressions, model ordered setup/actions/results,
reject unsupported code without execution, then connect those scenarios to the persisted
name registry. Accepted coverage expectations must be reviewed independently of each candidate
catalog. Descriptive method/folder migration, curated overrides and executable generation remain
separate deliverables. This comparator does not mark any capability as behavior-tested.
