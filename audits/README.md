# Operator audits

Reads the MongoDB manual's operator pages at a pinned commit and turns each documented example
into a scenario: its seed data, its command and its printed results. Core's tests check Morphia
against those scenarios. The [design](../design/operator-audits-simplified.md) explains the whole
audit and the order it is built in.

This module reads the docs, writes scenarios, and checks that every documented example has a
scenario and every scenario has its test. Nothing here runs during an ordinary build, fetches
documentation implicitly, or writes core's tests.

## Running the tests

```sh
./mvnw -Daudits -pl audits verify
```

They need no MongoDB, docs checkout or token. Run them with `-Dmorphia.mapper=critter`, then
`reflection`, like core's. The parser integration skips unless given the isolated parser runtime
(see `.github/workflows/audits.yml`).

## The audit

`ScenarioCoverageTest` runs with this module's tests, offline:
- **Every in-scope example has a scenario,** or an `exclude` override with a reason. There is no
  baseline for this: an example the reader can't extract fails until the reader learns it or a
  reviewer excludes it.
- **Every scenario has its test,** `Test<Operator>#test<Stem>`, or a baseline entry. Each run
  prints the untested scenarios by reason and writes all of them to
  `audits/target/untested-scenarios.json`, with the test each needs: the work queue.

`ApiCoverageTest` reads Morphia's operator tags (`@aggregation.expression $x`,
`@aggregation.stage`, `@query.filter`, `@update.operator`) from core's sources:
- every run reports the documented operators with no tagged factory, with the untested scenarios
  each blocks, in `audits/target/missing-api.json`;
- a `NO_MORPHIA_API` baseline entry that names only an operator fails once that operator is
  tagged: its test can be written now. An entry that names an operator and an option stays until a
  test proves the option, since a tag says nothing about options.

A docs bump: pin the new commit in `config/manual-baseline-sources.json`, bundle and parse the
pages, run `ExtractMainKt`, then review the scenario, inventory and baseline diffs.

## Weekly discovery

`.github/workflows/operator-discovery.yml` runs `audits/discover.sh` every Monday against the head
of mongodb/docs. In a throwaway worktree, it bundles and parses the pages, runs extraction, and
runs core's operator tests under both mappers against the result. The Publish job then files the
findings as issues, each keyed so that a rerun updates its issue instead of filing it again:

| Finding | Filed for |
|---|---|
| `MISSING_API` | A documented operator with no tagged Morphia factory, listing the scenarios it blocks. |
| `NEW_EXAMPLE` | An example the docs gained since the committed inventory, with no scenario. |
| `MISSING_SCENARIO` | An in-scope example with no scenario and no exclusion. |
| `FAILING_AUDIT` | Operator tests failing against the current docs' scenarios. |

Run it locally, without publishing:

```sh
audits/discover.sh <docs-checkout> <out-dir> [--test]
./mvnw -q -Daudits -pl audits exec:java -Daudits.mainClass=dev.morphia.audits.publish.PublishMainKt \
  -Dexec.args="publish <out-dir>/findings.json audits/catalog/publication-policy.json <journal.json> pom.xml <OWNER/REPO>"
```

Without `--publish`, `publish` only reports what it would file. The publisher's mutation tests run
against a sandbox repository: `-Daudits.sandbox=<OWNER/REPO>`.

## Commands

Each runs through the exec plugin from the repository root:

```sh
./mvnw -q -Daudits -pl audits compile exec:java \
  -Daudits.mainClass=<main class> -Dexec.args="<arguments>"
```

| Main class | Arguments | What it does |
|---|---|---|
| `dev.morphia.audits.sources.SourcesMainKt` | `<request.json> <new-bundle-dir> (--repository <git-dir> \| --fetch-to <new-git-dir>)` | Copies the docs pages at the requested commit into a verified, content-hashed source bundle. `config/manual-baseline-sources.json` is the request. |
| `dev.morphia.audits.documents.DocumentsMainKt` | `<bundle> <content-sha256> <python> <adapter.py> <page-id>…` | Parses pages with the docs' own Snooty parser (`parser/`), includes resolved, into a document AST report. |
| `dev.morphia.audits.scenarios.ScenariosMainKt` | `<bundle> <source-sha256> <documents.json> <plan.json>` | Assembles each example section into seed data, command and printed results, with descriptive names. `catalog/scenario-plan.json` is the plan. |
| `dev.morphia.audits.extraction.ExtractMainKt` | `<repository> <bundle> <content-sha256> <documents.json>` | Inventories the pages into `catalog/documented-examples.json` and writes a `scenario.json` for every example that has none, with its reviewed override applied. A committed scenario is never rewritten. Exits 3 when an example can't be extracted. |
| `dev.morphia.audits.publish.PublishMainKt` | `findings <tree> <committed-inventory.json> <audit.log\|->` or `publish <findings.json> <policy.json> <journal.json> <pom.xml> <OWNER/REPO> [--publish]` | Computes a discovery run's findings, or files them as issues. |
| `dev.morphia.audits.shell.ProgramsMainKt` | `<program-request.json>` | Lowers mongosh source into typed commands and BSON, without running it. |
| `dev.morphia.audits.naming.NamesMainKt` | `<accepted-registry.json> <request.json>` | Proposes descriptive names for examples, checked against the accepted registry. |

## Packages

| Package | Role |
|---|---|
| `sources` | Pinned, verified docs source bundles. |
| `documents` | The Snooty AST of each page. |
| `shell` | mongosh → BSON through a pinned Rhino AST, never evaluated. |
| `scenarios` | Example sections → ordered programs: seed data, command, printed results. |
| `published` | Printed results as constraints on what the shell output proves. |
| `semantics` | Strict BSON comparison and BSON paths. |
| `naming` | Descriptive example names and their collision rules. |
| `extraction` | The inventory, the scenario writer, overrides and the completeness checks. |
| `publish` | Findings, and filing them as GitHub issues with a journal and the release milestone. |
| `coverage` | Shared JSON contracts and validation helpers. |

## Catalogs

| File | Holds |
|---|---|
| `catalog/scenario-plan.json` | The pages and example destinations extraction reads. |
| `catalog/documented-examples.json` | Every example on the current operator pages, and the scenario that holds it; the source text of every example without one. |
| `catalog/example-overrides.json` | Reviewed overrides, each keyed by its example's page, title and block digest: exclusions, digest-guarded patches, skips, order and rules. Also the sample datasets an example may query. |
| `catalog/publication-policy.json` | The labels each finding type's issues get. |
| `catalog/untested-examples.json` | The reviewed baseline of scenarios with no test, each with a reason: `NOT_WRITTEN`, `NO_MORPHIA_API` (with the `operator`, and `option` for an option gap) or `OUT_OF_SCOPE`. |
| `catalog/example-names.json` | The 93 reviewed names for examples whose page title is generic (`main`) or shared. |
| `catalog/name-backport-review.json` | Reviewed stems for name collisions. |
| `catalog/name-backport.json` | The ledger that renamed master's `testExampleN` tests and `exampleN` fixtures to descriptive names. |

The golden vectors in `core/src/test/resources/dev/morphia/test/vectors/` hold this module's strict
and printed-result comparisons and core's Java ports of them (`StrictBson`, `PrintedResults`) to the
same verdicts.
