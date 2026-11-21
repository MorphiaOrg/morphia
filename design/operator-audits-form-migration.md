# MORPHIA_FORM lock migration

The second slice of step 4 of [the operator audit plan](mongodb-operator-audits.md), following the
[curated repair inventory](operator-audits-curated-repairs.md). It replaces the locks whose only
category is `MORPHIA_FORM` with declared equivalences, and retires a lock only when its documented
source, under those rules, is BSON-identical to the preserved curated fixture. A later slice
applies the same machinery to `NUMERIC_TYPE` locks through declared number retypes (see
[Numeric retypes](#numeric-retypes)), and another to `DOCS_ERROR` and `FIXTURE_CURATION` locks
through reviewed value corrections (see [Reviewed corrections](#reviewed-corrections)).

Of the 77 `MORPHIA_FORM` examples, 66 carry no other category. 48 of those locks are retired here;
18 stay, each with a recorded gap. The 11 mixed examples wait for their other families. Of the 26
`NUMERIC_TYPE` examples, 24 carry no category besides `MORPHIA_FORM`; 17 of those are retired and
7 stay with a gap. Of the 52 locked examples filed as `DOCS_ERROR` or `FIXTURE_CURATION` with no
family outside these four, 35 are retired and 17 stay with a gap. The `EXTRACTION` and
`SHELL_SYNTAX` locks need nothing declared: 30 of their 92 fixtures equal what the current pipeline
extracts from the pinned page (see [Handled natively](#handled-natively)). In all, 130 locks are
retired and 104 are kept with a gap; only the two untestable change-stream fixtures are outside the
review. Seven of those retirements depend on [generated extracts](#generated-extracts), which the
page reader now assembles from their YAML sources, and five on [shell dates](#shell-dates) whose
omitted time or offset the shell defines.

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
Generated `/includes/extracts/` targets are not tracked files; they are assembled from the YAML
they are built from (see [Generated extracts](#generated-extracts)), and each YAML file used is
pinned like any other include. A target that is neither tracked nor resolvable is recorded per
override as `unexpandedIncludes`.

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
| `PROJECT_ID_FIRST` | 12 | position of `_id` in a projection | other projected fields, whose order is output order |
| `NAMED_ARGUMENTS` | 14 | top-level key order of an allowlisted named-argument operator (`$filter`, `$map`, `$dateToString`, `$bottomN`, `$fill`, `$graphLookup`, ...) | nested values such as `sortBy`, and every operator outside the allowlist |
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

Every correction was read against its lock reason. Eleven of the first 29 retired locks need none at all:
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
The three `DOCS_ERROR` locks retired later through [generated extracts](#generated-extracts)
(`$regexMatch`, `$all` with `$elemMatch`, `$elemMatch`) need no correction either, for the same
reason: their pinned pages predate the fixtures and already match them.

`$toUUID`'s fixture predates every page at its path, so its source is the commit that first added
the page, recorded as `relation: AFTER_ORIGIN`. That source pins the values the correction changes;
it is not where the fixture came from. Every other source is `AT_ORIGIN`.

One fixture spells its lock `lockd`; the declaration records the marker's name.

## Handled natively

`EXTRACTION` and `SHELL_SYNTAX` locks record work-arounds for the legacy extractor and the driver's
JSON reader: a wrong example name, the wrong code block taken as expected output, results wrapped
in an array, truncated pipelines, text the old reader could not parse. The plan retires such a lock
once the new pipeline handles the case natively, so these categories allow no declared change: a
lock is retired only when the fixture equals, BSON-identically, what the strict shell normalizer
and document reader extract from the pinned page, with only the declarations its other categories
allow. 28 of 92 do (three of them through [generated extracts](#generated-extracts)). Ten of them were wrong example names: the curated `name` file now selects the
example the current extractor gives that title. Several others only changed layout (unwrapping an
array, one document per line) for the tests' line-based reader; that layout is checked, not
assumed: every data, expected and index file must read, one document per line as
`TemplatedTestBase.loadJson` reads it, as the same documents the comparison sees, or the fixture is
a `FIXTURE_UNSUPPORTED` gap.

## Generated extracts

Many pages include `/includes/extracts/<ref>.rst`, a file the docs build (Giza) generates and Git
does not track. `GizaExtracts` rebuilds it from the `source/includes/extracts*.yaml` documents at
the page's commit: the document whose `ref` matches supplies `content`, an `inherit` (or `source`)
entry supplies the inherited `content` and `replacement` values, the document's own replacements
override inherited ones, and each `{{name}}` is substituted. An extract is left as a directive, and
recorded as unexpanded, when its `ref` is not defined exactly once, its inheritance cannot be
followed, a placeholder stays unreplaced, or an entry in the chain has a field Giza renders beyond
the content (`title`, `style`, `only`, `pre`, `post`, `append`), which the assembler does not
reproduce. Includes are recognized with any whitespace after
`include::`, as Sphinx accepts; two pages spell it with two spaces.

`fetch.py` downloads every `extracts*.yaml` at a page's commit when the page names an untracked
extract, through GraphQL tree listings and batched blob queries, verifying each blob id. Verified
blobs are remembered in `<pages>/.verified.json`, so a rerun checks unchanged files without
spending the REST quota. An absence is remembered only when GitHub answers 404; any other failure stops
the fetch.

With extracts assembled, no fixture is a `PAGE_UNSUPPORTED` gap any more. Seven locks retire: the
three `$regexFind`/`$regexFindAll`/`$regexMatch` "and its options" examples, `$all` with
`$elemMatch`, `$elemMatch`, `$in` on arrays (all exact) and `$near`, whose integer coordinates
become doubles as its `NUMERIC_TYPE` lock says. The other sixteen now report the gap behind the
extract: residual edits (9), an unsupported source construct (1), unsupported fixture files or options (3), no matching example (2) and
one rejection.

### Rejected declarations

`$regexFind`'s email example reproduces with seven corrections, but the page prints its result
twice, after `$addFields` (each email a match document) and after `$set` (each email a string), and
the extractor reads the first. The drafted corrections therefore rewrite every email, which the lock
("the expected doesn't match the comments in data", "tweak the options format") does not describe.
A reviewer records this by moving the fixture to `unresolved` with gap `REJECTED` and the reason.
`capture` accepts a rejection only while the fixture still reproduces and the reason is not blank;
once it stops reproducing, its real gap must be reviewed instead.

## Shell dates

The shell normalizer accepted only dates with an explicit time and UTC offset, deferring date-only
and local-time strings to a reviewed rule. The rule follows what each form means where it runs:

- `ISODate(...)`: mongosh (`shell-bson.ts`) and the legacy shell (`types.js`) read an omitted time
  as midnight and an omitted offset as UTC, and accept an offset without its colon (`-0500`), though only after a time. So
  `ISODate("2020-12-31")` is `2020-12-31T00:00:00Z` and `ISODate("2020-12-25T19:04:00")` is
  `19:04:00Z`.
- `new Date(...)`: ECMAScript reads a date-only string as UTC midnight but a date-time without an
  offset as local time, so only the date-only form is added.

The shells' compact (`20201231`), space-separated, minute-only and hour-only-offset forms are not
reviewed and stay unsupported, as do impossible dates: `$dateDiff`'s "Result Precision" page
inserts `ISODate("2011-06-31")`.

Of the 16 fixtures this blocked, five retire: `$dateAdd`'s future date (the `$merge` target the
harness uses), `$fill` and an uncorrelated `$lookup` (argument order), `$lookup` with an array
(exact) and `$max` on dates (a wrong documented `desc`). The others report the gap behind the date:
results the page does not document (3), JavaScript variable bindings (2), the impossible date, a
`$fill` key order no rule covers, a duplicate key, an unsupported construct, a missing data file,
and an action whose kind differs from the test's (below).

## Printed results

Some pages print results as mongosh prints them (`Decimal128("50")`, `Timestamp({ t: 1, i: 2 })`,
`Long("1")`, comments), which the document reader cannot read. Such a block is now read with the
shell normalizer's `SHELL_RESULTS` format and compared by what the printed text proves
(`PublishedComparison`): a printed `4` is an Int32 or a Double, printed text a string, a printed
`Decimal128` its exact text and scale. The fixture must satisfy that, not equal a guessed BSON value;
printed results take no retype or correction, so any difference is a gap. Because a printed `4` cannot show
whether it was an Int32 or a Double, a fixture whose lock is filed as `NUMERIC_TYPE` never retires
this way: a match is a gap. The results are owned by a
`find` on the action's collection, which certifies mongosh's default read profile for every action
kind (an update's results are the collection read back).

Two locks retire: `$tsSecond` ("updated the Timestamp format", only the spelling differs) and
`$mul` on mixed numeric types ("parser extracts incorrectly", exact). Three printed results differ for
real: `$group`'s `Decimal128` totals became numbers, `$mul` changed a `Decimal128` scale
(`13.7375` to `13.7375000000000000`), and `$sortArray`'s `Decimal128("10.23")` became a double; no
declaration retypes `Decimal128`. The other blocks are not results at all: bit diagrams (3), SQL, a
page missing a comma and a quote, and plan-cache output elided with `...` or built with `+` (2).

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
- the fixture's action is read as the kind of action its test runs (`testPipeline`, `testQuery` or
  `testUpdate`, from the repair inventory), and the documented action must be of that kind. Before,
  the kind was taken from the page, so a page block of `updateOne` calls was compared, filter and
  update document, with `$dateAdd`'s pipeline fixture, and corrections rewrote one into the other.
  A test without exactly one helper is a `FIXTURE_UNSUPPORTED` gap;
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

`audits/catalog/form-review.json` lists every kept lock with its gap. By kind:

- `SOURCE_UNSUPPORTED` (43): expected blocks that are not results (7, see
  [Printed results](#printed-results)), JavaScript
  variable bindings (9), constructors the strict shell normalizer rejects (4: an impossible date,
  two typed-value constructors and `Date()` as a function), other shell syntax or constructs, data
  blocks in another database, and expression examples without a collection.
- `RESIDUAL_DIFFERENCE` (47): edits no declaration covers for the lock's categories, such as the
  four `$regex` fixtures that lost documented runs of spaces, BSON `undefined` in two data files,
  form edits under form locks the rules do not cover, string results where the page prints match
  documents, printed `Decimal128` results the fixture changed (3), a documented action of another
  kind than the test runs, and data or expected files
  with rows the page does not document, which a correction may not replace wholesale.
- `FIXTURE_UNSUPPORTED` (9): fixture files the legacy reader cannot parse, unsupported command
  options, extra data files, a data file the test does not load, and a test that runs no single
  helper.
- `NO_MATCHING_EXAMPLE` (4): no example on the page carries the fixture's name.
- `REJECTED` (1): reproducible, but the drafted declaration was rejected in review (see
  [Rejected declarations](#rejected-declarations)).

## Next

Test bodies are not compared: a bound test that writes data directly (`insert`, `getDs().save`,
driver calls) is outside this check. None of the 130 retired fixtures' tests does today; recording
such writes in the repair inventory is a separate rule.

The remaining families follow the same pattern. `EXTRACTION` and `SHELL_SYNTAX` need the native
extractor; `NUMERIC_TYPE` needs typed overrides; `DOCS_ERROR` and `FIXTURE_CURATION` stay as reviewed
corrections pinned to a source digest; those corrections are now in place. Generated include extracts are now
assembled, shell dates read as the shell defines them and printed results compared by what they
prove. The largest remaining source gap is JavaScript variable bindings; each is a separate extension of the extraction or normalizer.
