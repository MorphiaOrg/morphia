# Operator audits, simplified: extract the examples, write the tests

A war game of a smaller design for the operator audits. The docs reader stays and writes each
documented example as one named `scenario.json`. The test bodies are hand-written, as they are on
master. Generating them, and everything that exists only to generate them, goes away.

## Where we are

| | master | `audits-*` stack (#4553–#4602) |
|---|---|---|
| Audits code (main) | 12 files, 1,696 lines | 85 files, 21,045 lines |
| Audits tests | 224 lines | 17,017 lines |
| Design docs | `mongodb-operator-audits.md` | 6,907 lines over 16 files |
| Example tests | 554 `testExampleN` methods, hand-written | 385 fixture-bound hand-written methods plus 11 manual ones; 152 generated methods |
| Cases in the manifest | n/a | 537: 385 converted, 151 generated, 1 migrated |
| Fixture naming | `exampleN/` with a `name` file | title-based (`regexFlags/`) |
| How a test finds its data | method name → `exampleN/` | hand-written: `ExampleBindings`, then the method name; generated: a path string |

Master already keys fixtures off the method name: `discoverResourceName()` turns `testExample1` into
`example1/`. PR #4530 renamed master's corpus to title-based names (556 ledger entries: 554 methods
and 555 fixture folders), keeping that rule. So the naming half of this design is mostly built.

Most of the stack's size, and nearly all of its churn, comes from generating test bodies. That
means:

- the case generator;
- the reviewed MQL → Morphia binding catalog, and the check of it against Morphia's source;
- per-case comparison rules, ported to Java and kept in step with the Kotlin rules by golden
  vectors;
- the candidate → integrate → promote → attestation pipeline.

Every binding slice touches the catalog, both rule ports, the vectors and a re-attestation.

## Decisions so far

- **One `scenario.json` per example**, the name the stack already uses. Its `collections` is a document with one key per collection,
  each holding that collection's array of seed documents. This replaces `data.json`, `data2.json`
  and the `loadData(collection, index)` special cases; the base class seeds every collection.
- **A test's fixture is found from its method name**, with no path or resource string passed.
- **The untested-examples baseline starts from today's full list**, so nothing fails on day one; only
  examples added after it fail until someone writes their test or records a reason.
- **A missing scenario fails the audits, with no baseline.** Every in-scope docs example must have a
  committed `scenario.json` or a reviewed `exclude` with a reason. That holds from day one, so the
  audits fail today until the current gaps are extracted or excluded (section 5).
- **An example that names no collection gets one.** Where a page's command or seed data names no
  collection, the scenario uses the test class's operator name (`TestHash` → `hash`), or `hash1`,
  `hash2` and so on if that name is already a collection in the scenario.
  - This does not cover the five `db.aggregate([{$documents: …}, …])` examples. MongoDB only runs
    `$documents` in a database-level aggregate, never a collection's, so those need collection-less
    aggregation in both Morphia and the runner first. Until then they are reviewed exclusions.
  - It doesn't cover the three `$merge` "Use Variables Defined…" examples either. They are
    `db.runCommand` calls that name their collection through `db.movieDetails.getName()`, a shell
    form the reader can't lower.
- **"Missing command or result" is split by cause.** A block the reader can't lower fails as that,
  naming the block and the construct it rejected (for example `3*24*60*60000` in `$add`'s date
  example). Only a section that really lacks a command or a printed result keeps the old label.
- **Morphia's API coverage comes from its javadoc tags** (section 5, item 4). This replaces the
  binding catalog, the API check and discovery as the way to find operators Morphia lacks.
- **Issue filing stays**, fed by the new checks instead of the generator (section 5, item 5).
- **The reader runs on the current docs commit.** Extraction always reads the commit the audits
  are pinned to. When the reader learns new syntax, it re-runs over that commit, and the examples it
  can now read get scenarios. Existing scenarios converted during the migration keep the commit
  they were read from, until the next re-extraction brings them to the current one, reviewed as a
  docs bump.
- **`FormRules` becomes position-aware after the migration**, as a separate change. Until then,
  rules stay per case.

## The design

### 1. `scenario.json`

One file per documented example, in a folder named from the example's title:

```
core/src/test/resources/dev/morphia/test/aggregation/stages/lookup/performAConciseCorrelatedSubqueryWithLookup/
    scenario.json
```

It keeps the stack's schema whole, because the runner uses all of it:

```json
{
  "title": "Perform a Concise Correlated Subquery with $lookup",
  "source": { "repository": "mongodb/docs", "commit": "…", "path": "…", "blob": "…" },
  "dataset": null,
  "collections": {
    "orders":      [ { "_id": 1, "item": "filet" } ],
    "restaurants": [ { "_id": 1, "name": "American Steak House" } ]
  },
  "indexes": [],
  "action": { "kind": "aggregate", "collection": "orders", "pipeline": [ … ] },
  "expected": { "printed": [ … ] },
  "comparison": { "order": "UNORDERED", "rules": [ "NAMED_ARGUMENTS" ], "ignore": [], "unorderedArrays": [], "tolerance": null },
  "requirements": { "minimumServer": null, "topology": "STANDALONE" }
}
```

The fields carry meanings a plain "command plus results" schema would lose:

- **`expected`** is either `printed` or `none`.
  - `printed` holds constraints, not exact BSON. For example, `whole` matches an Int32 or a whole
    Double, and `text` matches a string. This covers 499 of today's cases. It has no ellipsis: a page
    that elides output can't be matched, and needs an override.
  - `none` means the page prints nothing to compare (38 cases). Those cases rely on the reference
    run (section 3).
- **`action`** can carry a `read`, the command whose output the page prints after a write (4 cases),
  and `alternatives`, several equivalent commands on one collection (1 case).
- **`comparison.unorderedArrays`** names nested arrays whose order the server doesn't fix, such as
  `$graphLookup`'s `reportingHierarchy` (4 cases). It is separate from the row order.
- **`dataset`** names a sample dataset, such as `sample_mflix`, instead of inline collections.
- **`source.commit`** is per example. Today's cases come from 105 docs commits (section 6).

### 2. A test is its method name plus a lambda

```java
public class TestLookup extends TemplatedTestBase {
    @Test
    @DisplayName("Perform a Concise Correlated Subquery with $lookup")
    public void testPerformAConciseCorrelatedSubqueryWithLookup() {
        testPipeline(aggregation -> aggregation.pipeline(
                lookup("restaurants").localField("restaurant_name").foreignField("name").as("matches")));
    }
}
```

**The lookup contract:**
- The fixture's owner is the JUnit test method running when `testPipeline`, `testQuery` or
  `testUpdate` is called. The base class gets it from JUnit's invocation context, not by walking
  the stack. That gives the right method from helpers and lambdas.
- Only plain `@Test` methods with no parameters can own a fixture. A `@ParameterizedTest`, a
  repeated or factory test, or a method with injected parameters gets a clear failure.
- Its folder is the method name without `test`, lower camel case, under the operator folder that
  `prefix()` derives from the class name.
- `ExampleBindings` goes away: method and folder names round-trip by rule. The 11 manual methods are
  covered in section 5.

**What happens to master's strings and options:**

| On master | Becomes |
|---|---|
| `loadData("inventory", 2)` → `data2.json` | `collections.inventory`; the base seeds every collection |
| `.orderMatters(false)` (108 uses; the default is `true`) | `comparison.order` |
| `.removeIds(true)` (61) | `comparison.ignore: ["_id"]` |
| `.serverVersion(…)` (18) | `requirements.minimumServer` |
| `.skipDataCheck(true)` (56), `.skipActionCheck(true)` (10) | reviewed skips in the overrides file (section 4) |
| `.options(…)`, `.findOptions(…)`, `.updateOptions(…)` | stay in the test: they are part of the Morphia call |

`ActionTestOptions` shrinks to the Morphia-side options. Every fact about the example comes from
the fixture.

### 3. What a test checks: the stack's three checks

1. **Command form.** The MQL Morphia builds equals the fixture's `action`, under the rules in
   `comparison.rules`. What is compared:
   - for an aggregation, the pipeline;
   - for a find, the filter and projection bodies;
   - for an update, the filter and update bodies.

   The stack compares these strictly, through `FormRules` and `StrictBson`. Master compared the same
   bodies leniently, with JSONAssert. Keep the strict check.

   Neither compares the whole wire command or its options (`upsert`, `collation`, `let`, …).
   Capturing those is new work, not part of this design.
2. **Reference behavior.** The runner seeds two identical databases. It runs the documented command
   on one and Morphia's on the other, then compares their results and every collection afterward.
   This is what tests a write's effects. It stays as it is in `OperatorCaseRunner`.

   It doesn't cover everything today:
   - Of the 38 `expected.none` cases, only 15 run it. The other 23 skip execution, so they get only
     the command check.
   - 111 cases skip their result checks, 84 of them because they name a sample dataset (such as
     `sample_mflix`) the test server doesn't load.
3. **Published results.** Morphia's results satisfy the page's printed constraints under
   `comparison`.

The three are skipped independently: a case can skip the published check and keep the reference
check. Nine cases do that today.

The migration maps today's skips one to one, each keeping its reason:
- today's combined `skipped.results` becomes `skipped.behavior` plus `skipped.published`;
- a skip for an unloaded dataset becomes the top-level `dataset` requirement, which skips only
  where the dataset is missing.

So every case keeps exactly the checks it runs now.

**Rules stay per case, not global.** Applying every equivalence everywhere is unsafe:
- Update values are partly literal. `{$set: {payload: {$size: [1]}}}` and
  `{$set: {payload: {$size: 1}}}` store different documents, but `FormRules`' update traversal would
  canonicalize them the same.
- Query positions inside stages, such as `$graphLookup`'s `restrictSearchWithMatch`, aren't
  distinguished from expressions yet.

So `comparison.rules` stays in the fixture. The extractor proposes the rules from the page's form;
a reviewer can change them in the overrides file. `FormRules` stays in Java only, with its golden
vectors as Java tests, including the negative ones. The Kotlin port goes. Separating literal, query
and expression positions in `FormRules` comes after the migration, as its own change (see
"Follow-up"). It is the precondition for ever applying rules globally.

**Result order is reviewed policy, not inference.** The default stays `UNORDERED`. "The action
sorts" doesn't prove an order: sort keys can tie, a later stage can regroup, and a write's paired
read is unsorted. The stack tried and rejected that rule. `ORDERED` is set only by review, in the
overrides file.

### 4. Overrides: one reviewed file with stale-source guards

`audits/catalog/example-overrides.json`, each entry with a reason. Entries are keyed by the
example's inventory identity: its page, its section title, and a digest of its blocks. They are not
keyed by fixture path, because an excluded example, or one the reader can't parse, has no fixture.
15 of today's unsupported examples have no name stem at all; this identity still names them. An
entry can be one of:

- **`exclude`:** the page is wrong in a way no patch fixes. For example, it aggregates a collection
  it never seeds, or its results depend on `$$NOW`.
- **A patch to `collections`, `action` or `expected`:** for when the page printed the first stage's
  output, ObjectId `_id`s for integer seeds, and so on.
  - A patch names an RFC 6901 pointer into the written scenario, the digest of the extracted value
    it replaces, and the new value in extended JSON. A number retype is a patch too, so the stack's
    separate retype and correction formats aren't carried over.
  - If the page changes upstream, the digest stops matching and extraction fails. A later docs fix
    can't be silently overwritten.
- **A skip:** `serialization`, `behavior` or `published`, each separately, with its reason. They
  become the scenario's `skipped` entries.
- **`order: "ORDERED"`, or extra `comparison.rules`.**

An override whose identity matches no inventory entry, or whose patch digest no longer matches,
fails extraction. That is the guard `FormCorrection` has today, kept, but checked against the
inventory rather than against fixtures.

### 5. Keeping tests and docs in step

1. **Missing scenarios fail the audits.** Extraction discovers every operator page in the scoped
   manual folders and records every example, including those it can't run. `documented-examples.json`
   (or a slimmed version) stays as that inventory, and page discovery, which lives in
   `ScenarioPlans` today, stays with it.

   The audits fail for every in-scope example that has neither a committed `scenario.json` nor an
   `exclude` override with a reason. There is no baseline for this: an example the reader can't turn
   into a scenario is a failure until the reader learns it, or a reviewer excludes it.

   Today 227 inventory examples have no scenario:
   - 87 the generator couldn't bind to Morphia. Under this design the reader writes their
     scenarios anyway; they then sit in the untested baseline until someone writes their tests.
   - 20 already have reviewed exclusions.
   - 120 are source-conversion gaps: 97 are unsupported at the inventory level, and 23 fail during
     conversion. These are the audit's failures until they are fixed or excluded.

   The generator's 127 source-gap records, which these 120 come from, break down as follows. Seven of
   the earlier-write records belong to examples that already have scenarios, which is why the counts
   differ.
   - about 56 with a missing command or printed result;
   - 10 that depend on an earlier write;
   - 18 with syntax the reader can't lower;
   - 8 with no collection named: five `db.aggregate([{$documents: …}])` and three `$merge`
     `db.runCommand` examples (see "Decisions so far");
   - 10 with setup the reader can't run;
   - 25 others: update options (7) and find modifiers (6) the scenario schema can't express yet,
     unsupported shell forms (5), commands on another database (3), pages that print a collection
     nothing seeds (3), and aggregate options (1).


   Of those 56, about 40 are out-of-scope admin and diagnostic pages, which become exclusions. About
   12 are real examples the reader drops and needs fixing. Seven pages with no examples section are
   recorded as such and are not failures.
2. **Orphans fail the build.** A core test checks every fixture-owning method:
   - a method whose fixture is missing fails;
   - a fixture whose `title` differs from its method's `@DisplayName` fails.

   The 11 manual methods are on a short reviewed list of non-fixture tests. Examples: `$bucket`'s
   validation and options tests, and hand-written `$lookup`/`$unwind` tests.
3. **Untested examples are a baseline**, taken from today's full list.
   `audits/catalog/untested-examples.json` lists every scenario with no test. Each entry has a
   reason from a closed set:
   - `NOT_WRITTEN`: nobody has written the test yet.
   - `NO_MORPHIA_API`: the example uses an operator or option Morphia has no API for. The entry
     names it, and item 4 checks the claim.
   - `OUT_OF_SCOPE`: admin and diagnostic stages, `$function`, change streams and similar.

   A new entry that isn't in the baseline fails until someone writes the test, or adds the entry
   with a reason. The baseline is then also Morphia's feature-gap list: filter it by
   `NO_MORPHIA_API`.

   **Every audit run highlights every untested scenario**, not just the new ones. The baseline
   only stops existing entries from failing the run; it never hides them. Each run prints a summary
   (counts by reason, and the change since the last run). It also writes
   `audits/target/untested-scenarios.json`, one entry per untested scenario:
   - the scenario path, title and operator;
   - the reason;
   - the test class and method name the test must have (`TestRegexFind#testRegexFlags`), derived by
     the naming rule.

   That file is a work queue. A person or an agent can take an entry and write its test without
   other context: the scenario holds the data, the command and the expected results. The class's
   existing tests show the style, and the orphan and title checks confirm the result is wired
   correctly. `NOT_WRITTEN` entries are the queue. `NO_MORPHIA_API` entries wait for Morphia, and
   `OUT_OF_SCOPE` entries are reported but not queued.
4. **Morphia's API coverage, from its javadoc tags.** Morphia tags each operator factory:
   `@aggregation.expression $x` (169 distinct expressions today), `@aggregation.stage` (70 tags),
   `@query.filter` (57) and `@update.operator` (31). A check of about 100 lines:
   - reads those tags from core's sources;
   - reads every documented operator, by context, from the inventory.
     - That includes operators whose pages have no examples (`$or`, `$not`, `$size`, `$max`,
       `$geometry`). Today the inventory records those pages only as skipped, so it must keep their
       operator and context.
     - It uses each page's operator token, not its file name. `ScenarioPlans` already tells them
       apart (`maxN-array-element.txt` documents `$maxN`).
   - reports each documented operator with no tagged factory as missing from Morphia;
   - checks `NO_MORPHIA_API` entries, which come in two kinds:
     - **Operator gaps** name an operator. The check fails when that operator now has a tag: the
       test is writable, so the entry must become `NOT_WRITTEN`, or the test gets written.
     - **Option gaps** name an operator and an option, e.g. `$filter`'s `arrayIndexAs`. A tag says
       nothing about options, so the check can't clear these. They stay until a test proves the
       option works.

   A tag is a claim, not a proof: a tagged factory might not support every option the docs show.
   Option-level gaps surface when someone writes the test and the command check fails. The test
   author then records an option gap.
5. **Issue filing.** The scheduled discovery run keeps filing and updating GitHub issues through the
   existing `publish` package:
   - `GhGateway`, the publication journal, milestones and labels stay as they are.
   - `DiscoveryFindings` is rewritten, because it reads the generator's gaps and attestation.
   - So is `PublishMain`, which loads them. `Publisher` loses its use of the coverage code being
     deleted.
   - `FindingType` and the publication policy's labels gain the two new types.
   - The scheduled run itself (`audits/discover.sh`) runs the audits instead of candidate
     integration. Its test results are the input for `FAILING_AUDIT`.
   - The findings, each with a stable key so a recurring one updates its issue:
     - **`MISSING_API`:** one per documented operator with no tagged factory (item 4), listing the
       examples it blocks.
     - **`NEW_EXAMPLE`:** an example added upstream since the committed inventory, with no test.
     - **`MISSING_SCENARIO`:** an in-scope example with no scenario and no exclusion (item 1).
     - **`FAILING_AUDIT`:** a scenario test failing against the new docs commit.
   - The mutation tests against `evanchooly/morphia-audits-sandbox` stay with it.

A docs bump is one command: pin the new commit, re-extract, then review the fixture diff, the
inventory diff and the baseline diff. Then write tests for the new examples.

### 6. Migration from the stack

1. **Keep #4530's renames.** Methods and folders are already title-based. Keep its reviewed naming
   catalogs too: `example-names.json` (the 93 reviewed names for generic or shared titles) and
   `name-backport-review.json` (reviewed stems for collisions).
2. **Keep the reader and what it depends on.**
   - The reader: `sources`, `documents`, `shell`, `scenarios`, `naming` (7,705 lines).
   - Its dependencies, which v1 of this design missed:
     - `published` (528): printed-result constraints. It is not issue publishing.
     - `semantics` (541): BSON types and comparators.
     - The contracts and JSON parts of `coverage` (about 1,200 of its 1,813 lines).
   - Relocated from packages being deleted:
     - from `repairs`:
       - the fixture writer (`FixtureMigration`, 802);
       - what it calls in `FormMigration`: `documented()` and the source and block contracts;
       - `NumericRetype` (151), which the override guard needs;
       - the override guard itself (`FormCorrection`, 104), rewritten to check by digest.
       - The fixture writer also calls the Kotlin `FormEquivalence.canonical()` before it applies
         corrections and retypes (`FixtureMigration.pinned()`). Patches must apply to the raw
         extracted value instead, and existing patch pointers written against the canonical form
         must be migrated. Only then can the Kotlin port be deleted.
     - from `generate`: page discovery and the inventory (`ScenarioPlans`, `DocumentedExamples`: 746).

   Rewiring needed:
   - `FixtureMigration` stops reading `LegacyBindingCatalog`: a fixture's owner is now its test
     method.
   - `DocumentedExamples` stops matching the migration ledger, legacy `name` files and `action.json`.
     It decides which examples have scenarios by reading the committed `scenario.json` files.
   - Its source contracts and hashing helper move out of `repairs`.
   - That is roughly 12,500 lines kept, against about 21,000 now. This is an estimate until the
     packages are split.
3. **Don't re-extract everything at one pin during the migration.** Today's fixtures cite 105 docs
   commits. Some come from historical sources, because the current manual no longer reproduces them.
   - Convert the existing fixtures as they are, keeping each one's `source.commit`.
   - Moving them to the current pin is a separate docs bump, reviewed like any other. Version
     changes are kept apart from repairs.
4. **Freeze the generated tests into the existing classes.** The 152 generated methods are correct,
   and pass under both mappers. They become ordinary hand-written tests:
   - Merge each into the existing class of the same name (`generated/expressions/TestAdd` into
     `aggregation/expressions/TestAdd`), with imports merged. Create the class where none exists.
     Moving files or renaming classes would break `prefix()`.
   - Switch from `TestBase` plus `OperatorCaseRunner` with a path string to `TemplatedTestBase`'s
     `testPipeline`/`testQuery`/`testUpdate`. Name the method `test<Stem>`.
   - Move each fixture from `generated/fixtures/…/scenario.json` to the operator folder's
     `<stem>/scenario.json`.
   - Then delete the generator.
5. **Convert master-era fixtures.** Each folder's `action.json`, `data*.json`, `expected.json`,
   `index.json` and `name` become one `scenario.json`. The 385 converted cases already have one,
   so for them this is mostly a move.
6. **Delete:**
   - from `generate`: the generator and candidates;
   - `bindings`, `api`, `discovery`;
   - from `publish`, only its generator inputs: `DiscoveryFindings`, `PublishMain` and
     `Publisher` are adapted as described in section 5, item 5, and the rest stays;
   - the rest of `coverage` and `repairs`;
   - the Kotlin rules port;
   - the manifest and the attestation pipeline;
   - the per-slice design docs, which this document replaces.

What remains:
- about 12,500 lines of reader code;
- `publish`, about 980 lines, with `DiscoveryFindings` rewritten;
- the javadoc-tag API check, about 100 lines;
- one fixture format;
- `FormRules` and its vectors, in Java;
- one overrides file;
- one inventory, one baseline (with reasons) and one orphan test.

## What we give up

- **Automatic coverage of new examples.** A new docs example needs someone to write its Morphia
  call. The baseline makes that visible, but nothing fills it.
- **Per-option proof of Morphia's API.** The binding catalog proved each documented operator
  form had a reviewed factory and option call. The javadoc-tag check proves only that an operator
  has a factory. Missing options surface when a test is written.
- **Generated gap reports at the node level.** When Morphia's MQL differs from a page, the test's command check fails,
  and its author decides what to do:
  - fix Morphia;
  - add an equivalence rule, with a vector;
  - record a reviewed skip.

## Follow-up after the migration

- **Position-aware `FormRules`.** Give the canonicalizer a map of each command's shape:
  - which stage arguments are queries (`$match`, `restrictSearchWithMatch`, `$geoNear`'s `query`),
    expressions or subpipelines;
  - which update values are literals and which are expressions (pipeline updates).

  Then each rule fires only where it's valid, with negative vectors for each position.
- **Drop `comparison.rules`** once every rule is safe everywhere: all rules then apply to every
  test. This is a separate, verified change. The migration itself keeps every case's current checks
  (section 3), so it changes no test outcomes.

## Agent-written tests

The design makes untested scenarios a queue that an agent can work through:

1. Take a `NOT_WRITTEN` entry from `untested-scenarios.json`.
2. Read its `scenario.json`, and the existing tests in its class for style.
3. Write `test<Stem>` with the scenario's title as `@DisplayName`, using Morphia's API for the
   command.
4. Run it under both mappers:
   - If the command check fails because Morphia writes an equivalent form, add the rule to the
     scenario through the overrides file, with a reason a reviewer can check.
   - If Morphia lacks the operator or an option, record `NO_MORPHIA_API` with what's missing.
   - If the page is wrong, record an override with a digest.
5. Open a PR. A person reviews the test, any rule or override, and the reason.

The checks keep an agent honest: the strict command check, the reference run, the published
results, the orphan and title checks, and digest-guarded overrides. Rules and overrides still
need human review, because they are where a wrong test could pass.

## Open questions

None at the moment.

## Appendix: `scenario.json` schema

One file per documented example, at
`core/src/test/resources/dev/morphia/test/<family>/<operator>/<stem>/scenario.json`. It is MongoDB
canonical Extended JSON: numbers keep their BSON type (`{"$numberInt": "1"}`,
`{"$numberLong": "1"}`, `{"$numberDouble": "1.5"}`). The extractor writes it; people never edit
it, and corrections go through the overrides file. Every key below is required unless it says
otherwise.

The runner rejects:
- unknown top-level, `action`, `comparison`, `requirements` and `skipped` keys;
- an `order`, `topology`, `context` or `expected` kind outside the listed values;
- a non-null `tolerance`;
- a `name` that isn't the scenario's folder, a non-string `expected.none`, and an alternative with
  keys other than its kind's command keys;
- a missing `title` or a malformed `source`, except on the runner's self-tests.

The runner's
self-test fixtures (`runner/…`) have no `title` and a `null` `source`; they are exempt, as test
inputs for the runner rather than docs examples.

### Top level

| Key | Type | Meaning and values |
|---|---|---|
| `schemaVersion` | Int32 | Format version. `1`. |
| `name` | string | The fixture path below the test resources, `<family>/<operator>/<stem>`, e.g. `aggregation/expressions/regexFind/regexFlags`. It must match the file's folder. |
| `title` | string | The example's display title. Usually the title on the docs page, with its inline markup as written. Where the page's title is generic (`main`) or shared, it's a reviewed name instead: e.g. `main` on `$addToSet`'s page becomes "Collect the Genres of Each Director". Those 93 reviewed names live in `example-names.json`, which stays, so re-extraction and the work queue produce the same names. The owning test's `@DisplayName` must equal it. |
| `context` | string | Where the operator is used. One of `EXPRESSION` (`aggregation/expressions`), `STAGE` (`aggregation/stages`), `FILTER` (`query/filters`) or `UPDATE` (`query/updates`). |
| `source` | document | Where the example was read from. See [source](#source). |
| `dataset` | string or null | A MongoDB sample dataset the example queries instead of inline data, e.g. `sample_mflix`. `null` when the example seeds its own data. When set, the behavior and published checks run only where that dataset is loaded. |
| `collections` | document | The seed data: one key per collection, each holding that collection's array of documents in insertion order, e.g. `{"orders": [ … ], "restaurants": [ … ]}`. An empty document when the example seeds nothing. A collection the page doesn't name gets the class's operator name (`hash`), then `hash1`, `hash2`. |
| `indexes` | array | Indexes created on both databases after seeding. See [indexes](#indexes). Empty when there are none. |
| `action` | document | The documented command. See [action](#action). |
| `expected` | document | What the page says the result is. See [expected](#expected). |
| `comparison` | document | How results and commands are compared. See [comparison](#comparison). |
| `requirements` | document | What the server must provide. See [requirements](#requirements). |
| `skipped` | document | Optional. Checks this example does not run, each with its reason. See [skipped](#skipped). Absent when every check runs. |

### `source`

| Key | Type | Meaning and values |
|---|---|---|
| `repository` | string | `mongodb/docs`. |
| `commit` | string | The 40-character docs commit the example was read from. |
| `path` | string | The page's path in that commit, e.g. `content/manual/manual/source/reference/operator/aggregation/regexFind.txt`. |
| `blob` | string | The page's Git blob id at that commit, so a change to the page is detectable without the full commit. |

### `indexes`

Each item:

| Key | Type | Meaning and values |
|---|---|---|
| `collection` | string | The collection to index. It need not be seeded: creating the index creates it, as for `$merge`'s `onlyInsertNewData` target. |
| `keys` | document | The index keys as the page wrote them, e.g. `{"price": 1}`, `{"location": "2dsphere"}`, `{"title": "text"}`. |
| `options` | document | Optional. Index options as the page wrote them, e.g. `{"unique": true}`. |

### `action`

`kind` decides which other keys are present. Exactly the listed keys appear.

| Key | Type | Meaning and values |
|---|---|---|
| `kind` | string | One of `aggregate`, `find` or `update`. |
| `collection` | string | The collection the command runs on. |
| `pipeline` | array | `aggregate` only. The stages, in order. |
| `filter` | document | `find` and `update` only. The query filter. |
| `projection` | document | `find` only, and optional. The projection, when the page gives one. |
| `update` | document | `update` only. The update document (`{"$set": …}`, `{"$inc": …}`). Pipeline-style updates are not supported yet. |
| `multi` | boolean | `update` only. `true` for `updateMany`, `false` for `updateOne`. |
| `read` | document | Optional, and only on a write (an `update`, or an `aggregate` ending in `$merge` or `$out`). The find the page runs afterwards to show the effect; its results replace the action's own in the behavior and published checks. Keys: `collection` and `filter`, required; `projection` and `sort`, optional. |
| `alternatives` | array | Optional. Other spellings of the same command that the page shows, each in the action's own shape: `{"pipeline": […]}`, `{"filter": …, "projection": …}` or `{"filter": …, "update": …}`. Each needs a reviewed reason in the overrides file, because nothing proves the alternatives equivalent. |

### `expected`

Exactly one key:

| Key | Type | Meaning and values |
|---|---|---|
| `printed` | array | The results the page prints, one item per result document, as constraints (below). Most examples use this. |
| `documents` | array | The results as exact BSON documents. Not written by the extractor: only the runner's own self-test fixtures use it. |
| `none` | string | The page prints no result; the value says why, e.g. `"the page publishes no result"`. Only the serialization and behavior checks apply. |

A `printed` value is a constraint on what the shell output proves, not a guessed BSON value. The
result count must match exactly, and there is no ellipsis. Each value is a one-key document:

| Key | Holds | Matches |
|---|---|---|
| `document` | an array of `{"name": …, "value": …}` fields, in printed order | a document with exactly those fields, no more. The order is the one the shell prints: JavaScript property order, where integer-like keys (`"0"`, `"12"`) come first in numeric order, then the others as stored. Each field's `value` is itself a constraint. Duplicate or NUL-containing names, and DBRef-shaped documents, are rejected. |
| `array` | an array of constraints | an array of exactly that many matching items, in order unless `comparison.unorderedArrays` names it |
| `whole` | an Int32 | a number the shell printed as a whole number: an Int32, or a Double with that value. `whole: 0` doesn't match a Double negative zero. A printed `Long("5")` is `exact` instead. |
| `text` | a string | a String, or a legacy Symbol, with that text |
| `exact` | any scalar BSON value except JavaScript-with-scope | exactly that value and BSON type, e.g. a date, an ObjectId, an Int64, a Double with a fraction, `null` |
| `nan` | `true` | a Double NaN |
| `decimal` | a string of at most 1,024 characters, in Decimal128's own canonical text | a Decimal128 whose text is exactly that string, so its quantum is preserved (`"1.50"` is not `"1.5"`) |

### `comparison`

| Key | Type | Meaning and values |
|---|---|---|
| `order` | string | `UNORDERED` (the default): results compare as a multiset. `ORDERED`: results compare in order. `ORDERED` is set only by review, because a sort doesn't prove an order. |
| `rules` | array of strings | The equivalences applied to both sides before the serialization check. Each is one of the rules below. Empty when the page's form is exactly Morphia's. |
| `ignore` | array of strings | Top-level fields removed from both results before the published check, e.g. `["_id"]` when the page omits generated ids. No dotted paths. |
| `unorderedArrays` | array of strings | Optional. JSON Pointers into each result document whose arrays compare as multisets, e.g. `/reportingHierarchy`. Applies to the behavior and published checks. |
| `tolerance` | null | Reserved for numeric tolerance; always `null`, and any other value fails. |

The `rules` values:

| Rule | Equates |
|---|---|
| `COND_ARRAY` | `$cond: {if: a, then: b, else: c}` and `$cond: [a, b, c]` |
| `EQ_SHORTHAND` | in a query, `{f: {$eq: v}}` and `{f: v}`, unless `v` is a regex or an operator document |
| `REGEX_FORM` | in a query, `f: /p/o`, `{$regex: /p/o}` and `{$regex: "p", $options: "o"}` |
| `REGEX_OPTIONS` | in `$regexFind`, `$regexFindAll` and `$regexMatch`, flags in `regex: /p/o` and in `options: "o"` |
| `FIELD_OPERATORS_AND` | `{f: {$gt: a, $lt: b}}` and `{$and: [{f: {$gt: a}}, {f: {$lt: b}}]}` |
| `SINGLE_ARGUMENT_ARRAY` | `{$op: [x]}` and `{$op: x}`, for operators that take one argument, unless `x` is an array |
| `NAMED_ARGUMENTS` | the keys of a named-argument operator (`$filter`, `$dateToString`, `$lookup`, …) in any order |
| `PROJECT_ID_FIRST` | `_id` anywhere in a projection, and first |
| `PROJECTION_FLAGS` | `true`/`false` and `1`/`0` as top-level projection values |
| `UNWIND_PATH` | `$unwind: "$p"` and `$unwind: {path: "$p"}` |

Rules apply only where they are declared (see section 3).

### `requirements`

| Key | Type | Meaning and values |
|---|---|---|
| `minimumServer` | string or null | The oldest server version the example works on, as major.minor.patch, e.g. `"7.0.0"` (today's values: `6.3.0`, `7.0.0`, `8.0.0`). The test is skipped on an older server. `null` for no minimum. |
| `topology` | string | `STANDALONE`. Replica-set and sharded examples aren't supported yet; any other value fails. |

### `skipped`

Optional. One key per check this example does not run; each value is the reason, a non-empty
string.

| Key | Skips |
|---|---|
| `serialization` | The command check: Morphia's MQL against `action`. |
| `behavior` | The reference run: the documented command and Morphia's, on identical databases. |
| `published` | The published check: results against `expected`. |

On the stack today the behavior and published skips are one key, `results`. The migration splits it
(section 3).

## Implementation plan

The `audits-*` stack (#4512–#4602) is about 90 PRs deep, with generator work threaded through most
of them. Cherry-picking would mean splitting nearly every commit. Instead, the new design starts
from master and copies the final state of what it keeps from the stack's tip, then removes or
reworks what it drops. Each step is a PR stacked on the one before:

1. **Design** (this document).
2. **Reader.** The `audits` module with the packages the reader needs and their tests: `sources`,
   `documents`, `shell`, `scenarios`, `naming`, `published`, `semantics`, and the contracts and
   JSON parts of `coverage`. No generator, bindings, API inventory, discovery, repairs or publish
   yet.
3. **Scenario runner.** In core's tests:
   - `OperatorCaseRunner`'s three checks, `FormRules` with its vectors, `PrintedResults` and
     `StrictBson`;
   - fixture lookup through JUnit's invocation context;
   - the orphan and title checks.
4. **Scenarios and tests.**
   - Master's tests are renamed (#4530's ledger), their fixtures become `<stem>/scenario.json`, and
     `ActionTestOptions` facts move into the scenarios.
   - The generated cases are frozen into the operator classes.
   - The legacy `exampleN` loader goes.
5. **Extraction to scenarios.** The writer, digest-guarded overrides, the inventory, the
   missing-scenario failure, the untested baseline and the work queue.
6. **API check** from javadoc tags.
7. **Issue filing.** `publish`, adapted, and the scheduled job.

**Fixes found by the old stack come with the new one.** No old PR is closed until its fixes are
in the new series:
- **Morphia fixes**, in step 2's PR as their own commit:
  - the ranked-accumulator `sortBy` encoding, with its test;
  - whole-number parameters written as Int32 (`CodecHelper.whole`, across 11 codecs);
  - the `$toUUID` and `$documents` server releases;
  - `AggregationOptions.let`'s server release.
- **Test-body fixes** (wrong DSL calls in master's tests, and #4553's ordered maps for four update
  tests) come with step 4, which takes the stack's test bodies.
- **Unchanged:** the `$merge` `keepExisting` fix (#4591) was never in the stack, and stays its own
  PR.
