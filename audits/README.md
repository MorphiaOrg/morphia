# Operator audits

Reads the MongoDB manual's operator pages at a pinned commit and turns each documented example
into a scenario: its seed data, its command and its printed results. Core's tests check Morphia
against those scenarios. The [design](../design/operator-audits-simplified.md) explains the whole
audit and the order it is built in.

This module reads the docs, writes scenarios, and checks that every documented example has a
scenario and every scenario has its test. Nothing here runs during an ordinary build, fetches
documentation implicitly, or writes core's tests.

## Running it locally

Everything runs through `make`: `make <target>` here, or `make -C audits <target>` from the
repository root. `make help` lists the targets.

| Target | What it does | Needs |
|---|---|---|
| `make test` | The module's offline tests. | A JDK 17 toolchain |
| `make parser-test` | The parser's regressions, the shell-result probes, and the coordinator's integration with the parser. | uv |
| `make docs` | Checks out the docs at the pinned commit, or moves an existing checkout to it. | Git, network |
| `make extract` | Moves the checkout to `REVISION`, rebuilds the inventory and scenarios in place from it, then runs `make report`. Review the result with `git diff`. | uv |
| `make report` | Writes the missing-scenario report, `target/missing-scenarios.md` and `.json`, without failing on what it lists. | |
| `make core-test` | Core's operator tests against the scenarios in this tree, once, under the default mapper. | Docker |
| `make discover` | The weekly discovery against the docs' head, in a throwaway worktree of the committed `HEAD`: nothing in this checkout changes. The audit's code and catalogs come from `HEAD`, so commit a change before discovery can see it; the `bin/` scripts run from this checkout. `make discover-test` also runs core's operator tests against its result. | uv; Docker for `discover-test` |
| `make all` | `test`, `parser-test`, `extract` (with its report) and `core-test`, in that order. | All of the above |
| `make clean-docs` | Removes the docs checkout. | |

Variables:
- `DOCS`: the docs checkout. The default is `target/mongodb-docs`, so each worktree has its own;
  `mvn clean` removes it, and the next target that needs it checks it out again in seconds.
- `REVISION`: the docs commit `docs` and `extract` check out. The default is the pin in
  `config/manual-baseline-sources.json`; `HEAD` is the docs' head, which `discover` uses unless
  `REVISION` is given. Each target moves the checkout to its own revision, so `make discover
  extract` discovers at the head and extracts at the pin.
- `OUT`: where `extract` and `discover` write their reports (`target/extraction`,
  `target/discovery`).

The tests alone need no MongoDB, docs checkout or token: `./mvnw -Daudits -pl audits verify` runs
them without `make`.

### The scripts

The targets call the scripts in `bin/`, which also run on their own. Each resolves its paths from
where it lives, so it works from any directory; relative arguments are relative to where it's run:

| Script | What it does |
|---|---|
| `bin/checkout-docs.sh [--update] <docs-dir> [<revision>]` | A shallow, blobless, sparse checkout of the source bundle's roots at the revision (the pin by default). `--update` moves an existing checkout to the revision instead of cloning again. |
| `bin/extract.sh <docs-checkout> <out-dir>` | Bundles and parses the checkout's operator pages, then runs extraction with `--refresh` in this repository: the inventory, new scenarios, and refreshed scenarios whose example changed. |
| `bin/discover.sh <docs-checkout> <out-dir> [--test]` | `bin/extract.sh` in a throwaway worktree, then the findings the weekly Publish job files. |

## The audit

`ScenarioCoverageTest` runs with this module's tests, offline:
- **Every in-scope example has a scenario,** or an `exclude` override with a reason. There is no
  baseline for this: an example the reader can't extract fails until the reader learns it or a
  reviewer excludes it.
- **Every scenario has its test,** `Test<Operator>#test<Stem>`, or a baseline entry. Each run
  prints the untested scenarios by reason and writes all of them to
  `audits/target/untested-scenarios.json`, with the test each needs: the work queue.

Each run of the first check also writes `audits/target/missing-scenarios.md` and `.json`: every
missing example grouped by what it waits on, with what that category means, where in the sources it
comes from, how to resolve it, and each example's page link, digest and source blocks. A category
is a unit of work to hand to a person or an agent. `make report` writes it without the test run
failing.

`ApiCoverageTest` reads Morphia's operator tags (`@aggregation.expression $x`,
`@aggregation.stage`, `@query.filter`, `@update.operator`) from core's sources:
- every run reports the documented operators with no tagged factory, with the untested scenarios
  each blocks, in `audits/target/missing-api.json`;
- a `NO_MORPHIA_API` baseline entry that names only an operator fails once that operator is
  tagged: its test can be written now. An entry that names an operator and an option stays until a
  test proves the option, since a tag says nothing about options.

A docs bump: pin the new commit in `config/manual-baseline-sources.json`, run `make extract` (it
moves the existing checkout to the new pin), then review the scenario, inventory and baseline
diffs. After a change to the reader, `make extract` is also what makes the change show up in the
inventory and the missing-scenario report.

## Weekly discovery

`.github/workflows/operator-discovery.yml` runs `audits/bin/discover.sh` every Monday against the
head of mongodb/docs. In a throwaway worktree, it bundles and parses the pages, runs extraction, and
runs core's operator tests against the result. The Publish job then files the
findings as issues, each keyed so that a rerun updates its issue instead of filing it again. An
in-scope example with no scenario or exclusion fails the run instead: it is listed in the run's
summary and `missing-scenarios.txt`, and is not filed.

| Finding | Filed for |
|---|---|
| `MISSING_API` | A documented operator with no tagged Morphia factory, listing the scenarios it blocks. |
| `NEW_EXAMPLE` | An example the docs gained since the committed inventory, with no scenario. |
| `FAILING_AUDIT` | Operator tests failing against the current docs' scenarios. |

Run it locally with `make discover` (or `make discover-test`), then, from the repository root,
see what the Publish job would file:

```sh
./mvnw -q -Daudits -pl audits exec:java -Daudits.mainClass=dev.morphia.audits.publish.PublishMainKt \
  -Dexec.args="publish audits/target/discovery/findings.json audits/catalog/publication-policy.json <journal.json> pom.xml <OWNER/REPO>"
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
