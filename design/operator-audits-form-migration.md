# MORPHIA_FORM lock migration

The second slice of step 4 of [the operator audit plan](mongodb-operator-audits.md), following the
[curated repair inventory](operator-audits-curated-repairs.md). It replaces the locks whose only
category is `MORPHIA_FORM` with declared equivalences, and retires a lock only when its documented
source, under those rules, is BSON-identical to the preserved curated fixture.

Of the 77 `MORPHIA_FORM` examples, 66 carry no other category. 43 of those locks are retired here;
23 stay, each with a recorded gap. The 11 mixed examples wait for their other families.

## Which documentation is the source

The pinned manual (`a5d960b9`) is not where most fixtures came from: its `$cond` page now uses a
different dataset and even shows the array form itself. Morphia's own history does not keep the
pre-curation extraction either, because each fixture and its lock were committed together. The
source is therefore the operator page in `mongodb/docs` as of the day the fixture was written:

- `first`: the last docs commit touching the page at or before the Morphia commit that created the
  fixture;
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
| `PROJECT_ID_FIRST` | 10 | position of `_id` in a projection | other projected fields, whose order is output order |
| `NAMED_ARGUMENTS` | 9 | top-level key order of an allowlisted named-argument operator (`$filter`, `$map`, `$dateToString`, `$bottomN`, `$fill`, `$graphLookup`, ...) | nested values such as `sortBy`, and every operator outside the allowlist |
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

## What is compared

For each fixture, the legacy extractor reads the recorded page (with tracked includes spliced in as
Sphinx does) and selects the example by the fixture's `name`. Then:

- the action is lowered by the shell normalizer, and data blocks by the shell normalizer (insert
  programs) or the legacy document reader (document listings); nothing is executed;
- action, data, expected results and index are compared with the strict BSON comparator, which keeps
  key order and BSON types. Only the action takes rules. Data, expected and index must match exactly,
  because a lock does not say which files were edited;
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

- `RESIDUAL_DIFFERENCE` (8): real, undeclared edits. The four `$regex` fixtures lost the documented
  runs of spaces in their data; `$allElementsTrue`/`$anyElementTrue` insert BSON `undefined` where
  the documented insert does not; `$dateTrunc` changes `startOfWeek` case, which the page does not
  call case-insensitive; `$densify` reorders the nested `range` document.
- `SOURCE_UNSUPPORTED` (9): constructors the strict shell normalizer rejects (`Date` without an
  explicit time, and one typed-value constructor), a JavaScript function body, an expression example
  without a collection, and an unparseable printed result.
- `FIXTURE_UNSUPPORTED` (4): data for several collections (`data2.json`) and one data file the legacy
  reader cannot parse.
- `PAGE_UNSUPPORTED` (2): a generated `/includes/extracts/` file inside the example section.

## Next

The remaining families follow the same pattern. `EXTRACTION` and `SHELL_SYNTAX` need the native
extractor; `NUMERIC_TYPE` needs typed overrides; `DOCS_ERROR` and `FIXTURE_CURATION` stay as reviewed
corrections pinned to a source digest. Multi-collection data is the most common remaining
`MORPHIA_FORM` gap and should come before further rules.
