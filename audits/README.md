# Operator audits

Reads the MongoDB manual's operator pages at a pinned commit and turns each documented example
into a scenario: its seed data, its command and its printed results. Core's tests check Morphia
against those scenarios. The [design](../design/operator-audits-simplified.md) explains the whole
audit and the order it is built in.

This module is the reader: step 2 of the design's implementation plan. Nothing here runs during an
ordinary build, fetches documentation implicitly, or rewrites core's tests.

## Running the tests

```sh
./mvnw -Daudits -pl audits verify
```

They need no MongoDB, docs checkout or token. Run them with `-Dmorphia.mapper=critter`, then
`reflection`, like core's. The parser integration skips unless given the isolated parser runtime
(see `.github/workflows/audits.yml`).

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
| `coverage` | Shared JSON contracts and validation helpers. |

## Catalogs

| File | Holds |
|---|---|
| `catalog/scenario-plan.json` | The pages and example destinations extraction reads. |
| `catalog/documented-examples.json` | Every example on the current operator pages; the inventory later steps check against. |
| `catalog/example-names.json` | The 93 reviewed names for examples whose page title is generic (`main`) or shared. |
| `catalog/name-backport-review.json` | Reviewed stems for name collisions. |
| `catalog/name-backport.json` | The ledger renaming master's `testExampleN` tests and `exampleN` fixtures to descriptive names (step 4). |
| `catalog/generation-review.json` | The reviewed page-error exclusions, until step 5 moves them into the overrides file. |

The golden vectors in `core/src/test/resources/dev/morphia/test/vectors/` hold this module's strict
and printed-result comparisons and core's Java ports of them (`StrictBson`, `PrintedResults`) to the
same verdicts.
