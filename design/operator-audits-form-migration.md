# MORPHIA_FORM lock migration

The second slice of step 4 of [the operator audit plan](mongodb-operator-audits.md), following the
[curated repair inventory](operator-audits-curated-repairs.md). It replaces the locks whose only
category is `MORPHIA_FORM` with declared equivalences, and retires a lock only when its documented
source, under those rules, is BSON-identical to the preserved curated fixture. A later slice
applies the same machinery to `NUMERIC_TYPE` locks through declared number retypes (see
[Numeric retypes](#numeric-retypes)), and another to `DOCS_ERROR` and `FIXTURE_CURATION` locks
through reviewed value corrections (see [Reviewed corrections](#reviewed-corrections)).

Of the 77 `MORPHIA_FORM` examples, 66 carry no other category. 46 of those locks are retired here;
20 stay, each with a recorded gap. The 11 mixed examples wait for their other families. Of the 26
`NUMERIC_TYPE` examples, 24 carry no category besides `MORPHIA_FORM`; 16 of those are retired and
8 stay with a gap. Of the 52 locked examples filed as `DOCS_ERROR` or `FIXTURE_CURATION` with no
family outside these four, 29 are retired and 23 stay with a gap. In all, 91 locks are retired and
51 are kept.

## Which documentation is the source

The pinned manual (`a5d960b9`) is not where most fixtures came from: its `$cond` page now uses a
different dataset and even shows the array form itself. Morphia's own history does not keep the
pre-curation extraction either, because each fixture and its lock were committed together. The
source is therefore the operator page in `mongodb/docs` as of the day the fixture was written:

- `first`: the last docs commit touching the page at or before the Morphia commit that created the
  fixture (its parent, when that commit removed the page as the manual moved; the commit that first
  added the page, when the fixture predates it);
- `last`: the same, at the latest Morphia commit that added or modified a fixture file (pure renames
  excluded), for fixtures that a later audit run regenerated from newer pages.

`first` wins when it reproduces the fixture; `last` is tried otherwise. Four declarations come from
`last` (three `$filter` examples regenerated in June 2024 and one `$lastN`). Every page is recorded
with its exact commit and git blob id, as is every `.. include::` it splices in, so the source can be
re-fetched and checked against GitHub. `fetch.py` refuses any page or include, fetched or already
on disk, whose git blob id differs from the one GitHub reports for that path and commit. The reviewed
declarations pin each page and include blob id, and `capture` fails if the pages it reads differ.
Include targets that are not tracked files (generated extracts) are recorded per override as
`unexpandedIncludes`.

The pages themselves are never committed (a maintainer decision): `mongodb/docs` is licensed
CC BY-NC-SA 3.0, which does not fit this Apache-2.0 repository. Offline checks therefore start from the recorded source text; the
networked recheck is `fetch.py pages` followed by `FormMainKt capture`, which must reproduce the
committed report byte for byte.

## Rules

Each rule maps every spelling in an equivalence class to one canonical spelling and is applied to
both sides; neither side is rewritten to look like the other. The documented reason the server
treats the spellings alike is part of the rule (`FormRule.description`).

| Rule | Uses | Equivalence | Not applied to |
| --- | --- | --- | --- |
| `COND_ARRAY` | 14 | `$cond: {if, then, else}` ≡ `$cond: [if, then, else]` | any other `$cond` document |
| `PROJECT_ID_FIRST` | 11 | position of `_id` in a projection | other projected fields, whose order is output order |
| `NAMED_ARGUMENTS` | 12 | top-level key order of an allowlisted named-argument operator (`$filter`, `$map`, `$dateToString`, `$bottomN`, `$fill`, `$graphLookup`, ...) | nested values such as `sortBy`, and every operator outside the allowlist |
| `EQ_SHORTHAND` | 4 | `{f: {$eq: v}}` ≡ `{f: v}` in a query predicate | a regex `v`, an operator-document `v`, the aggregation `$eq` expression |
| `SINGLE_ARGUMENT_ARRAY` | 2 | `{$isArray: ["$a"]}` ≡ `{$isArray: "$a"}` for single-argument expression operators | an argument that is itself an array literal (`[[1]]`); query predicates, where `$size` is the query operator |
| `REGEX_FORM` | 1 | `f: /p/o`, `{$regex: /p/o}`, `{$regex: "p", $options: "o"}`, `{$regex: /p/, $options: "o"}` | options given in both places |
| `FIELD_OPERATORS_AND` | 1 | `{f: {$gt: a, $lt: b}}` ≡ `{$and: [{f: {$gt: a}}, {f: {$lt: b}}]}` | `$elemMatch` and non-comparison operators |
| `PROJECTION_FLAGS` | 1 | top-level projection `true`/`false` ≡ `1`/`0` | booleans inside expressions |
| `UNWIND_PATH` | 1 | `$unwind: "$p"` ≡ `$unwind: {path: "$p"}` | |
| `ALTERNATIVE_COMMANDS` | 1 | one action block shows several commands of one kind on one collection; the fixture holds one. Not proven by the tool: each use carries a reviewed `justification` (here `$type: [2, 1]` ≡ `["string", "double"]`) | blocks that mix kinds or collections; a declaration without a justification |

Three retired locks need no rule at all: the documented example already equals the fixture.

No rule looks inside `$literal`, whose content is data. Predicate rules apply only at query positions (a find filter, a `$match` body and the branches of
`$and`/`$or`/`$nor`), and projection rules only to `$project` and find projections. Unit tests pin
each rule and the cases it must not touch. `EXISTS_BOOLEAN` was drafted for `$facet` and dropped:
that example is unresolved for another reason, and an unexercised rule is not kept.

## Numeric retypes

A `NUMERIC_TYPE` lock records that a curated fixture spells a documented number in another BSON
type, usually an integral double where mongosh sends or prints an integer. That is not an
equivalence (an `int32` and a `double` are different commands and results), so it is declared as
an override instead: a `NumericRetype` names a file, an RFC 6901 pointer, a source type and a target
type, and turns every source-type number under that pointer into the target type at exactly the same
value. `int32`, `int64` and `double` are supported; a value with no exact counterpart (2.5 as an
integer, `-0.0`, 2^53+1 as a double) fails the override. `Decimal128` is not supported, since a
decimal's scale makes "the same value" ambiguous.

Retypes are discovered from the comparison: each numeric `TYPE` difference whose two values are equal
becomes a leaf retype. Leaves of one file and type change are then merged into the deepest pointer
that covers them, but only when the merged retype still reproduces the fixture exactly (two `qty`
values do not merge into the whole file when that would also change integral `_id` values). Every
retype must change at least one number and be necessary. For `action.json`, pointers address the
canonical form under the declared rules, so retypes are re-derived for every rule set tried while
the rules are minimized. `overrides.retyped` counts overrides with retypes; a retired lock that
needs neither rules nor retypes counts as `overrides.exact`.

The lock's categories decide what an override may declare: form rules only for a `MORPHIA_FORM` lock
and retypes only for a `NUMERIC_TYPE` lock, so neither stands in for an edit the lock did not record.
The categories come from the repair inventory.

Of the 16 retired `NUMERIC_TYPE` locks, one (`$exp`) needs nothing: its page already shows doubles.
The others retype geometry and `$mod`/`$nearSphere` arguments in the action, or `$linearFill` and
`$percentile` results in the expected output, from `int32` to `double`.

## Reviewed corrections

A `DOCS_ERROR` lock records that the documentation is wrong (a wrong printed result, a misspelled
field, stale BSON sizes); a `FIXTURE_CURATION` lock records that Morphia's fixture deliberately
differs (a collection name the test harness uses, predictable ids). Neither is an equivalence, so
the plan keeps them as reviewed corrections pinned to their source. A `FormCorrection` names a file
and an RFC 6901 pointer and records, as extended JSON, the documented value it replaces or removes
and the curated value it puts there (an added field also records its key position). It applies
only if the page still documents exactly that value (BSON-typed) and, for an added field, does not
document the field; a changed page makes it stale instead of applying it to something else.

Corrections are discovered from the comparison after rules and retypes, one field at a time: a value
difference replaces that value, a documented field the fixture lacks is removed, and a field the
fixture adds is added in place (a renamed field is a removal and an addition). Discovery then goes
on, so every other difference in the same document needs its own correction. A correction never
replaces a whole file or a top-level document (a row, or a pipeline stage), a key-order difference
is never corrected (that is form), and neither is a type change at the same value (that is a
retype): those stay gaps. Corrections apply in order, after retypes, and each must be necessary.
Rules are preferred to corrections: minimizing the rules never drops one if that needs more
corrections. Only `DOCS_ERROR` and `FIXTURE_CURATION` locks may declare corrections.

Every correction was read against its lock reason. Eleven of the 29 retired locks need none at all:
the fixture equals the pinned page. Those pages already showed the corrected values before the
fixtures were written, so each lock's deviation was against some other, unidentified source (probably a
version branch or a stale checkout the legacy extractor read), not against these pages. The other 18
correct wrong documented results (`$asinh`, `$bsonSize`, `$rank`, `$denseRank`, `$type`,
`$setIsSubset`, `$set`), a misspelled field (`$binarySize`, `$rename`), Decimal128 rounding
(`$degreesToRadians`, `$radiansToDegrees`), a field the server does not return (`$hour`), a
simpler `onNull` (`$dateFromString`), ids the fixture drops (`$toUUID`), and the `$graphLookup`
collection name the harness loads data into. One correction goes beyond its lock's wording:
`$degreesToRadians` also removes the documented `ObjectId` `_id` from its expected row, which the
lock ("the angle_a's were different") does not mention; it is accepted as a reviewed decision.

`$toUUID`'s fixture predates every page at its path, so its source is the commit that first added
the page, recorded as `relation: AFTER_ORIGIN`. That source pins the values the correction changes;
it is not where the fixture came from. Every other source is `AT_ORIGIN`.

One fixture spells its lock `lockd`; the declaration records the marker's name.

## What is compared

For each fixture, the legacy extractor reads the recorded page (with tracked includes spliced in as
Sphinx does) and selects the example by the fixture's `name`. Then:

- the action is lowered by the shell normalizer, and data blocks by the shell normalizer (insert
  programs) or the legacy document reader (document listings); nothing is executed;
- documented data is grouped by collection. The action's collection is compared with `data.json`;
  every other one with the data file the test loads into a collection of that name
  (`loadData("airports", 2)` → `data2.json`, as the repair inventory records it). A documented
  collection without such a file, a data file bound to the action's own collection, or a document
  listing beside several collections (it names none) is a `FIXTURE_UNSUPPORTED` or
  `SOURCE_UNSUPPORTED` gap. Each override records its bindings, and verification fails if the
  inventory's differ;
- action, data, expected results and index are compared with the strict BSON comparator, which keeps
  key order and BSON types. Form rules apply only to the action, and declared retypes and
  corrections to any file; everything else must match exactly, because a lock does not say which
  files were edited;
- the rules must be sufficient, and each must be necessary: dropping any one breaks equality. The
  declaration must list exactly that set.

## Inputs and outputs

- `audits/probes/form-sources/fetch.py` is the only networked step. `candidates` proposes sources from
  Git history and `gh`; `pages` fetches pages and their includes at exact commits into
  `<pages>/<commit>/<path>`. It never edits the repository.
- `FormMainKt draft` proposes `audits/catalog/form-review.json` from the candidate files in priority
  order. The review is the reviewed declaration: source, origin commit, exact lock text and rules per
  retired lock, and source plus gap for each one kept.
- `FormMainKt capture` re-derives every declaration from the pages and fails if the rules, lock text
  or gap differ, writing `audits/catalog/form-migration.json`. It records the extracted source text
  and its digest, so the claim can be rechecked offline.
- `FormMainKt apply` checks every retired lock's digest before deleting any of them.

Offline, `FormMigration.verify` recomputes each override from the recorded source text and the
current fixture, under exactly its declared rules, checks the report's blob ids against the review's
pins, and requires retired locks to be gone and kept
locks to remain. The curated repair inventory reports retired examples as `OVERRIDDEN`, with the
categories of their retired lock. The name-backport check accepts a lock as absent only when the
migration retired it with the digest the legacy corpus recorded.

## Gaps kept with their locks

`audits/catalog/form-review.json` lists every kept lock with its gap; by kind:

- `SOURCE_UNSUPPORTED` (20): constructors the strict shell normalizer rejects (mostly `Date` without
  an explicit time, and typed-value constructors), JavaScript function bodies, an expression
  example without a collection, unparseable printed results or shell text, and a data block in
  another database.
- `RESIDUAL_DIFFERENCE` (16): edits no declaration covers for the lock's category. The four
  `$regex` fixtures lost the documented runs of spaces in their data;
  `$allElementsTrue`/`$anyElementTrue` insert BSON `undefined`; `$dateTrunc` changes `startOfWeek`
  case and `$densify` reorders `range` under form locks; `$indexOfBytes`/`$indexOfCP` (documented
  as strings) and `$mul` (Decimal128) are numeric locks that are really other edits; and the rest are
  data or expected files with rows the page does not document, which a correction may not replace
  wholesale.
- `PAGE_UNSUPPORTED` (13): a generated `/includes/extracts/` file inside the example section.
- `FIXTURE_UNSUPPORTED` (2): a data file the legacy reader cannot parse, and an action in the legacy
  `$near` coordinate form it cannot read.

## Next

Test bodies are not compared: a bound test that writes data directly (`insert`, `getDs().save`,
driver calls) is outside this check. None of the 91 retired fixtures' tests does today; recording
such writes in the repair inventory is a separate rule.

The remaining families follow the same pattern. `EXTRACTION` and `SHELL_SYNTAX` need the native
extractor; `NUMERIC_TYPE` needs typed overrides; `DOCS_ERROR` and `FIXTURE_CURATION` stay as reviewed
corrections pinned to a source digest; those corrections are now in place. The largest remaining
gaps are source constructors the strict normalizer rejects and generated include extracts, both of
which the native extractor (step 2) handles.
