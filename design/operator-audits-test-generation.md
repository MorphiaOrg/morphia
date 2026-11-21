# Generated operator tests

Step 5 of [the operator audit plan](mongodb-operator-audits.md): run every documented operator
example through a descriptor-driven runner that checks serialization, behavior and the published
result separately, and generate typed Morphia DSL for examples no test covers yet, without erasing
handwritten work. This document fixes the contracts and the order the slices land in. The plan's
section "Generate useful tests without erasing handwritten work" is the requirement; this is the
design that meets it.

## Where step 4 leaves things

- **Legacy cases:** 556 `TemplatedTestBase` cases, each bound to one fixture directory through
  the binding manifest ([binding design](operator-audits-binding-manifest.md)), with descriptive
  names backported ([backport design](operator-audits-name-backport.md)). 138 locks are retired
  through declared, source-pinned equivalences and 96 stay with a recorded gap
  ([form migration](operator-audits-form-migration.md)).
- **Offline normalization:** audits can turn a documented example into normalized BSON: setup
  inserts per collection, one action (find, update or aggregate), expected documents or printed
  constraints, and indexes (`ShellPrograms`, `ScenarioExtraction`, `PublishedComparison`).
- **API evidence:** audits reads Morphia's public API from source (`MorphiaApiInventory`) without
  depending on core's compiled classes.

**How much is unowned.** A rough count over a 2025 `mongodb/docs` checkout finds about 550
documented examples on operator pages, about 521 of them owned by a legacy case. The remaining
35 or so of the 556 legacy cases own examples that page no longer documents under that name (the
form migration already records four such `NO_MATCHING_EXAMPLE` gaps). The ~29 unowned examples
belong to 14 operators, mostly:
- stages that need an environment the tests don't have (sessions, search indexes, sharded data
  distribution, query stats);
- the positional update operators, whose fixtures are named differently.

None is in the math family. So today the work is mostly bringing the owned examples onto the new
runner. Generation pays off as upstream adds examples, which step 6's monitoring reports.

## Boundaries

- **One owner per example.** An example a legacy test owns is never generated a second time. The
  accepted registry admits exactly one test and one fixture per example (see [Identity](#identity)).
- **Handwritten bodies survive.** A legacy body keeps its source file. It joins the runner through a
  compatibility adapter that supplies its descriptor, not by being regenerated. The one exception
  is slice 1's single case, whose body moves verbatim into the new form so that the runner is proven
  before the adapter exists.
- **Independent references.** A case's reference operation is the documented one, never a fixture
  edited to match Morphia (see [References for legacy cases](#references-for-legacy-cases)).
- **No placeholders.** If validated bindings cannot express a case's operation, the case is reported
  as a gap naming the missing binding. No empty, `TODO` or `@Disabled` test is emitted.
- **No core-to-audits dependency.** Audits emits files and core compiles and runs them. Ordinary core
  builds compile the committed generated files like any other test and never run audits; integration
  never relies on the `-Daudits` profile, which skips tests.
- **No self-certified expectations.** Expected output comes from the documentation (exact documents,
  or printed constraints), never from Morphia's own result.

## Contracts

### Identity

A documented example is identified by its qualified descriptive name (context, operator,
descriptive example name) from the accepted naming registry. Three sources map names to owners:

- **Legacy owners:** the binding manifest maps each legacy fixture directory to its test method. The
  name-backport ledger records each fixture's descriptive name and its former `exampleN` name as an
  alias.
- **Generated owners:** the generated manifest lists each generated case by qualified name.
- **Scenarios:** an extracted scenario carries its qualified name from naming reconciliation.

A scenario matches an owner only through these names, never by method name or position. Statuses:

- `AMBIGUOUS`: the scenario matches more than one owner. It gets no test.
- `UNNAMED`: its name is unresolved in the registry. It gets no test.
- `SOURCE_REMOVED`: an owner whose example the current page no longer documents. It stays pinned
  to its recorded source revision (see below), and the report shows it.

A registry check fails the build only if a name has two owners.

### References for legacy cases

Many legacy fixtures were edited to match Morphia's output; that is what their locks recorded. So a
legacy case's reference action and expected results come from its documented source, not from its
fixture files:

- **The 138 retired locks** have an accepted form-migration override: the recorded source text,
  normalized, plus the override's declared rules, retypes and corrections. That documented BSON is
  the reference. The declared rules become the case's `comparison.rules`, and the retypes and
  corrections are applied to the reference as the override pins them.
- **Cases that never had a lock** take their documented scenario from a pinned revision: the page as
  of the Morphia commit that created the fixture, found and verified the way the form migration
  finds its sources (`fetch.py candidates`, with blob pins). Drift between that revision and the
  current page is reported separately, so upstream edits do not turn into Morphia failures.
- **Collection names:** some `FIXTURE_CURATION` corrections only rename a documented collection to
  the legacy harness's `example_test`, for example `$graphLookup`'s `from` and `$merge`'s target.
  The runner seeds real collection names, so emitting a scenario reverts those corrections to the
  documented name. The case's test body must then name the documented collection too, so such cases
  are listed for review rather than converted silently.
- **The 96 cases still locked** have no independent reference. They run on their curated BSON with
  the status `CURATED_REFERENCE`. Their serialization and behavior checks are reported, but not
  counted as parity.

### Scenario file

`scenario.json` is a case's complete input, so the runner never re-reads documentation. Values are
canonical extended JSON (`$numberInt`, `$date`, …), so BSON types survive.

```json
{
  "schemaVersion": 1,
  "name": "aggregation/expressions/abs/absoluteValue",
  "context": "EXPRESSION",
  "source": { "repository": "mongodb/docs", "commit": "…", "path": "…", "blob": "…" },
  "dataset": null,
  "collections": { "ratings": [ { "_id": 1, "start": 5, "end": 8 } ] },
  "indexes": [],
  "action": { "kind": "aggregate", "collection": "ratings", "pipeline": [ … ] },
  "expected": { "documents": [ … ] },
  "comparison": { "order": "ORDERED", "rules": [], "tolerance": null },
  "requirements": { "minimumServer": "7.0", "topology": "STANDALONE" }
}
```

- **`expected`:** either `documents` (exact), `printed` (the published constraints of
  [published expectations](operator-audits-published-expectations.md)), or `none` with a reason.
  `none` covers an example that publishes no output; its case reports serialization and behavior
  only, and the coverage report shows the missing check with that reason.
- **`comparison.rules`:** the declared form rules this case may use, reviewed per case as in the lock
  migration.
- **`comparison.ignore`:** fields removed from both results before comparing, reviewed per case. The
  legacy `removeIds` option becomes `["_id"]` here, for examples whose documented ids are
  generated.
- **`comparison.tolerance`:** an invariant or tolerance policy for nondeterministic, approximate or
  time-dependent operators (`$rand`, `$sample`, `$$NOW`, floating-point trigonometry), never a
  captured result.
- **`dataset`:** names a versioned sample dataset. Until the dataset contract exists, a case that
  needs one is a gap: behavior is unavailable, and no substitute data pretends to reproduce the
  documented output.

### Descriptor and runner

`OperatorCase` is the descriptor: the qualified name, the absolute resource path of its
`scenario.json`, and either the typed DSL lambda (generated cases) or a reference to a handwritten
adapter (legacy cases). Context, requirements and comparison policy come from the scenario, so they
are declared once. The descriptor replaces the legacy name and stack-trace lookup (`prefix()`,
`exampleN`).

```java
@Test
@DisplayName("Absolute value")
void absoluteValue() {
    runner.aggregate(OperatorCase.of("/dev/morphia/test/generated/fixtures/expressions/abs/absoluteValue/scenario.json"),
        aggregation -> aggregation.pipeline(project().include("delta", abs(subtract("$start", "$end")))));
}
```

`OperatorCaseRunner` is built by composition on `TestBase`, for its datastore and mapper setup, not
on `TemplatedTestBase`, so none of the legacy resource lookup or name validation leaks in. It checks
three things separately and reports each:

1. **Serialization:** the BSON Morphia emits for the operation equals the scenario's action BSON
   strictly (types and key order), under only the case's declared rules.
2. **Behavior:** the reference operation (the scenario's BSON, sent through the driver) and the
   Morphia operation run against two databases, each seeded with the scenario's real collection
   names. Results, errors and the final contents of every collection the scenario touches are
   compared strictly. Separate databases keep `$lookup`, `$unionWith`, `$out` and `$merge` from
   crossing between the two runs.
3. **Published expectation:** both results satisfy `expected`.

It runs under both mappers through the existing test setup.

### Comparators in two languages

The strict comparator, the form rules and the published-constraint comparator are Kotlin in audits;
the runner needs them in Java in core. To keep the two from drifting, each comparator has one set of
golden vectors: input pairs and the expected verdict, committed under
`core/src/test/resources/dev/morphia/test/generated/vectors/`. The audits tests and the core tests
both run every vector. Slice 1 needs only strict equality (`BsonDocument.equals` with key order and
types); form rules and printed constraints are ported later, each with its vectors.

### DSL bindings

The generator never guesses a factory. `audits/catalog/dsl-bindings.json` is a reviewed table that
maps an MQL shape to Morphia factory calls, for example:

| MQL | Binding |
| --- | --- |
| `{$abs: <expr>}` | `dev.morphia.aggregation.expressions.MathExpressions.abs(Object)` |
| `{$add: [<expr>, <expr>, …]}` | `MathExpressions.add(Object, Object...)` |
| `{$project: {<field>: <expr>, …}}` | `Projection.project()`, then `.include(String, Object)` per field |
| `"$<path>"` | the string itself |

The `"$<path>"` binding holds only for the `Document`-typed operations the runner uses, where Morphia
passes the string through; it is not a mapped entity field reference.

Every binding is checked against `MorphiaApiInventory`: the owner, method, arity and parameter types
must exist in the current source, or the binding is stale and generation stops. An operation is
generated only if every node of its BSON tree has a binding. Otherwise the case is a gap that names
the first unbound node, which is the adapter task to propose. The signature check is not a compile
check: `integrate`'s compile step is the generator's contract test against the current API.

### Generated namespace, manifest and completeness

Generated files live only under:

- `core/src/test/java/dev/morphia/test/generated/<context>/`, one class per operator, so diffs stay
  local;
- `core/src/test/resources/dev/morphia/test/generated/fixtures/<context>/<operator>/<example>/scenario.json`.

A legacy case cut over to the runner (slice 5) keeps its test source where it is. Its fixture
directory's `data.json`, `action.json` and `expected.json` are replaced by a `scenario.json` in the
same reviewed change, so an example never has two fixtures. The manifest owns those converted
scenarios as `converted` entries: the fixture directory of each is a manifest-owned path, checked
and rewritten like generated files, under the same conflict rule; the handwritten test that uses
it is protected.

### Migrated fixtures

The audits checks guard legacy fixture bytes: `NameBackport.verify`, the legacy-bindings drift test,
`FormMigration.verify` (fixture digests and retired locks) and the curated repair inventory all fail
when a bound fixture file disappears. So removing or replacing a legacy file needs its own reviewed
declaration, as retiring a lock does. `audits/catalog/fixture-migration.json` lists each removed or
replaced legacy file with its legacy digest and its new owner: a `converted` or generated manifest
entry. Those checks accept a missing legacy file only when this ledger names it with the matching
digest and the manifest owns its replacement.

`core/src/test/resources/dev/morphia/test/generated/manifest.json` lists every generated file with
its SHA-256, the generator, schema and binding-table versions, and each case's source pins. Nothing
outside these paths is written by generation. A file in them that the manifest does not list is a
conflict, not something to overwrite.

An offline core test checks completeness both ways:
- every manifest case has exactly one `@Test` and one `scenario.json`;
- every generated test and scenario is in the manifest.

The legacy `ExampleBindingsTest` keeps covering the legacy packages.

### Candidates, integration and promotion

A candidate records:
- the base core revision and the accepted manifest's digest;
- the SHA-256 of every file it writes, deletes or protects;
- the source pins;
- the generator, schema, binding-table and adapter versions.

- **`GenerateMainKt candidate <repo> <revision> <out>`** emits one, writing nothing into the
  checkout.
- **`GenerateMainKt integrate <candidate>`** creates a temporary worktree at the base revision and
  stages the candidate in place of the accepted generated files. It then builds core, compiles the
  generated suite and runs it under Critter, then reflection. It writes an attestation binding the
  candidate digest to its inputs and to the mapper and server matrix it ran. It never edits the
  caller's checkout.
- **`GenerateMainKt promote <candidate> <attestation>`** first checks that the target checkout
  matches the candidate's base: revision, manifest digest and every protected file hash, including
  uncommitted edits. A mismatch is refused: regenerate and integrate again; a pass against another
  tree is never reused. It deletes a stale file only when the previous manifest owns it and its hash
  still matches. It then applies every change in one step, rolling back on any failure.

## Slices

1. **Runner, scenario and one case.**
   - **Core:** `OperatorCaseRunner`, `OperatorCase` and the scenario reader, with the three checks
     restricted to exact `documents` expectations and no form rules.
   - **The case:** `$round`'s "round expression operator" example. It is a retired lock whose
     documented source equals its fixture exactly, with one collection and no options. In the same
     change:
     - the migrated-fixture ledger and the four audits checks that consume it;
     - its legacy method and fixture directory are removed;
     - its body moves verbatim into a test in generated form;
     - its `scenario.json` is emitted from the form-migration override;
     - the binding manifest is updated, so the example keeps exactly one owner.
   - **Vectors:** the first golden vectors.
   - No generator yet.
2. **Registry, manifest and completeness.** The identity statuses and the one-owner registry check,
   the manifest schema with `converted` entries, and the two-way completeness test.
3. **Compatibility adapter.**
   - **Descriptor:** built for a legacy case from the binding manifest and backport ledger.
   - **Scenario:** emitted from its reference (see
     [References for legacy cases](#references-for-legacy-cases)).
   - **Legacy test options:** mapped, not dropped:
     - `orderMatters` becomes `comparison.order`;
     - `removeIds` becomes `comparison.ignore`;
     - `skipDataCheck` and `skipActionCheck` become reported missing checks with their reason;
     - `findOptions`, `updateOptions` and `serverVersion` become action options and requirements.
4. **Form rules and printed constraints in core**, each ported with its golden vectors, so cases that
   need them can be checked rather than reported as gaps. Coming before cutover, they let
   cases with declared rules or printed results convert in one pass.
5. **Legacy cutover by context.**
   - **Per context:** convert the bound fixtures, replacing their legacy files, and route the cases
     through the adapter. Each case then gains the behavior check and the strict comparator.
   - **Failures:** a case whose checks fail is not converted. It stays on the legacy path with the
     status `LEGACY_ONLY` and the failing check. The coverage report does not count it as supported
     until it converts.
6. **Candidate, integration and promotion** commands, with the attestation, stale-file and rollback
   checks and their tests.
7. **Bindings and generator.** Emit Java and `scenario.json` for unowned examples, as a candidate
   only until it is integrated and promoted through slice 6. The first family is the one with the
   most unowned, environment-independent examples at that time.
Each slice's acceptance: its cases pass under both mappers; no generated test lacks a real DSL body;
a second generation run changes no file; and the coverage report counts a case as supported only
when its checks pass, showing every missing check with its reason.

## Decisions for review

- **Legacy cases' references are their documented sources**, through the form-migration overrides,
  so that cutover adds real parity instead of comparing Morphia with fixtures shaped to it.
- **Converted scenarios replace legacy fixture files** in the cutover change, and the manifest owns
  them, so each example keeps one fixture. A migrated-fixture ledger declares each removed file, so
  the existing audits checks keep guarding fixture bytes.
- **Legacy cases join the runner through an adapter instead of being regenerated.** The adapter
  keeps every reviewed body and still gives each case behavior parity and the strict comparator.
  Regenerating them would replace reviewed code with output from bindings that do not exist yet.
- **One `scenario.json` per case** instead of the legacy `data.json`, `action.json` and
  `expected.json` set. One file carries types, collections, requirements and comparison policy
  together, and the legacy loader is not involved.
- **Separate databases for behavior parity**, not separate collections, so that operations naming
  other collections stay within one run.
- **Golden vectors shared by the Kotlin and Java comparators**, instead of a shared module: the plan
  defers a shared test-support module until it is justified, and vectors catch drift without one.
