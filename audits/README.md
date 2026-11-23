# Operator audits

Checks Morphia's aggregation, query and update operators against the MongoDB manual: which
operators exist, which documented examples Morphia's tests cover, and whether Morphia builds and
runs each one exactly as documented. The [design](../design/operator-audits-design.md) explains
how the parts fit together; the [component reference](../design/operator-audits-reference.md)
describes each part and its contract; the [plan](../design/mongodb-operator-audits.md) records
why the old audit was replaced and what remains.

Nothing here runs during an ordinary build, fetches documentation implicitly, rewrites core's
tests, or files issues. Every networked or writing step is a command you run on purpose.

## Running the tests

```sh
./mvnw -Daudits -pl audits verify
```

They need no MongoDB, docs checkout, `gh` or token. Run them with `-Dmorphia.mapper=critter`, then
`reflection`, like core's. Two parser integrations skip unless given the isolated parser runtime
(see `.github/workflows/audits.yml`), and the `gh` sandbox tests skip unless
`-Daudits.sandbox=<owner/repo>` names a disposable repository.

Every command below runs through the exec plugin from the repository root, after compiling:

```sh
./mvnw -q -Daudits -pl audits compile exec:java \
  -Daudits.mainClass=<main class> -Dexec.args="<arguments>"
```

## What lives where

| Path | Role |
| --- | --- |
| `audits/catalog/documented-examples.json` | Every example on the current operator pages, with its owner; unowned examples keep their source text. |
| `audits/catalog/dsl-bindings.json` | Reviewed MQL-to-Morphia factory bindings the generator uses. |
| `audits/catalog/example-names.json` | Reviewed names for examples with generic titles, and owners for examples several fixtures match. |
| `audits/catalog/generation-review.json` | The generated families, the reviewed exclusions with their evidence, and the sample-dataset collections. |
| `audits/catalog/fixture-migration.json` | The ledger of legacy cases moved onto the case runner, with their documented sources. |
| `audits/catalog/cutover.json` | Legacy cases that stay on the legacy path, with the check that fails and its first difference. |
| `audits/catalog/form-review.json`, `form-migration.json` | Reviewed equivalences that retired MORPHIA_FORM locks, and the gaps left. |
| `audits/catalog/legacy-references.json` | The documented page behind each never-locked legacy fixture. |
| `audits/catalog/curated-repairs.json`, `repair-review.json` | Every lock and exclusion in the legacy corpus, categorized. |
| `audits/catalog/legacy-bindings.json`, `binding-policy.json` | The frozen import of which legacy test owns which fixture, and its reviewed exceptions. |
| `audits/catalog/name-backport*.json` | Descriptive names given to the legacy fixtures. |
| `audits/catalog/legacy-corpus.json` | The preservation snapshot of the legacy corpus. |
| `audits/catalog/publication-policy.json` | Labels the publisher applies. |
| `core/src/test/resources/dev/morphia/test/generated/manifest.json` | Every case on the runner and every file the generated namespace owns. |
| `core/src/test/java/dev/morphia/test/generated/` | Generated test classes; never edit them by hand. |

## Weekly discovery

`.github/workflows/operator-discovery.yml` runs `audits/discover.sh` every Monday against the head
of mongodb/docs, then its Publish job files the findings as issues in this repository:

- **Bind `<node>` for generated operator tests** (`enhancement`, `aggregation`, `proposed`): an
  MQL node that documented examples need and `dsl-bindings.json` lacks, with the examples it blocks.
- **Cover the documented `<operator>` example "…"** (`Task`, `proposed`): an example that appeared
  upstream since the committed inventory, with no test.
- **Generated operator cases fail integration** (`bug`), naming the failing tests.

Each issue carries a hidden marker, so a rerun updates the publisher's section of an open issue
instead of filing it again, and keeps any edits outside that section. The journal of filed issues
carries over from run to run. A closed issue is a decision and is not reopened. A manual run can
set `publish` off to see what it would file. Open the run's summary and its `operator-discovery`
artifact:

- **The inventory matches**: nothing changed upstream that the inventory sees.
- **`inventory.diff` is not empty**: examples were added, renamed or removed. Accept it locally:
  clone the docs as the workflow does, run `audits/discover.sh <docs> <out>`, copy
  `<out>/documented-examples.json` into `audits/catalog/`, then regenerate (below) and review both
  changes together.
- **Integration failed**: a generated case no longer passes. Its log is in the artifact; treat it
  like a failure under "Generating cases".

To run discovery locally, clone the pages the same way (about 5 seconds):

```sh
git clone -q --filter=blob:none --no-checkout https://github.com/mongodb/docs.git /tmp/docs
git -C /tmp/docs sparse-checkout set --no-cone \
  /content/manual/manual/source/reference/operator/ /content/manual/manual/source/includes/ \
  /content/code-examples/tested/
git -C /tmp/docs checkout -q
audits/discover.sh /tmp/docs /tmp/discovery --integrate
```

It works in a throwaway worktree, so your checkout is untouched.

## Generating cases

Generation goes through a candidate: it is written outside the checkout, integrated in a temporary
worktree, and only then promoted. With core built and its dependency classpath written:

```sh
./mvnw -q install -pl :morphia-core -DskipTests -Ddeploy.skip=true -Dinvoker.skip=true
./mvnw -q dependency:build-classpath -pl :morphia-core -Dmdep.outputFile=/tmp/core-classpath.txt
# then, with -Daudits.mainClass=dev.morphia.audits.generate.GenerateMainKt:
#   candidate <root> /tmp/candidate /tmp/core-classpath.txt
#   integrate <root> /tmp/candidate
#   promote   <root> /tmp/candidate
```

- `candidate` first checks every binding against core's current source, then writes the
  candidate and `gaps.json`, which lists every example it did not generate and why.
- `integrate` runs the owning tests under Critter, then reflection, and writes
  `attestation.json` and `integration.log`.
- `promote` applies an attested candidate to a checkout still at its base revision, all at once.

A second `candidate` of a promoted tree changes nothing; the offline tests check that.

To cover more examples, read `gaps.json`:

- **UNBOUND** names the first node without a binding, such as `stage $match has no binding`. Add
  the binding to `dsl-bindings.json` with the factory's exact erased parameter types.
- **SOURCE_UNSUPPORTED** means the page's mongosh text does not lower; that is parser work.
- **UNNAMED** and **AMBIGUOUS** need a review in `example-names.json`: a descriptive name, or the
  owner among the matching fixtures, keyed by the action block's `sha256` from the inventory. Then
  rerun `inventory`; `reviews.stale` counts reviews whose example changed.
- **EXCLUDED** is a reviewed exclusion; its reason says what would lift it.

When a generated case fails integration, read why in `integration.log`. Never loosen the scenario
to pass. If the page is wrong or the server cannot run the case, add an exclusion to
`generation-review.json` that states the evidence. If Morphia is wrong, fix Morphia.

## Converting legacy cases

A legacy case joins the runner when its documented source reproduces its fixture:

```sh
# -Daudits.mainClass=dev.morphia.audits.repairs.FormMainKt
#   convert  <root> <revision> <fixture>...   # writes scenario.json, records the ledger entry
#   manifest <root>                           # rewrites the manifest, generated cases included
```

`convert` touches a fixture only after every check passes; a failing fixture is reported and left
alone. Record a case that stays legacy in `cutover.json` with its failing check. References for
never-locked fixtures come from `fetch.py references`, `fetch.py pages` and
`FormMainKt references`; recheck them with `FormMainKt recheck-references`.

## Publishing findings

`PublishMainKt <coverage-report.json> <policy.json> <journal.json> pom.xml <owner/repo>`, or
`PublishMainKt discovery <discovery-out> <committed-inventory.json> <policy.json> <journal.json> pom.xml <owner/repo>`
for a discovery run's findings, only prints what it would file. Add `--publish` to file issues, which needs `gh` authenticated with write access to
issues (or `AUDITS_GITHUB_TOKEN`). Issues join the milestone of the release `pom.xml` is building
(`3.0.1-SNAPSHOT` → `3.0.1`), which is created first if it does not exist. The journal makes reruns idempotent: keep it, and never point a
trial run at MorphiaOrg/morphia. Use the disposable sandbox repository instead.

## Troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| `Invalid Java source or incomplete explicit classpath: … package com.mongodb does not exist` | The binding check needs core's dependency classpath file. |
| `… package dev.morphia.sofia does not exist` | Core is not built; its generated sources come from `core/target/classes`. |
| `Stale bindings: …` | A binding no longer matches core's API. Fix the binding, or the API change it caught. |
| `<out> is not empty` | Candidates are written only into a new or empty directory. |
| `Refusing to promote: …` | The checkout moved, the attestation failed or is for another candidate, or a file changed since generation. Regenerate from the current checkout. |
| `… exists and is not a generated file; it is a conflict` | A handwritten file sits where generation would write. Move it or protect it; generation never overwrites it. |
| `Exclusions of cases that are not generated` | An exclusion outlived its case. Remove it. |
| A removed scenario still runs | Stale `core/target/test-classes`; delete it and rebuild. |
| `Owned file changed` in `GeneratedCasesTest` | A generated or protected file was edited. Regenerate, or run `FormMainKt manifest` after a reviewed edit. |
| `External snippet requires a source resolver` | A code directive inside an example names a file that is not in the pages directory: a remote shared include, or a snippet outside the sparse clone (include `/content/code-examples/tested/`). The page is skipped. |
| `gh` rate limits while fetching pages | Use a git clone and `fetch.py local`, as discovery does, instead of `fetch.py pages`. |

## Legacy paths still in use

These remain because coverage has not fully moved off them yet. Each one goes when the last of
its users does.

- **Core's legacy fixture loader** (`data.json`, `action.json`, `expected.json` through
  `TemplatedTestBase`). It runs 186 legacy fixture directories; 370 cases have moved to the runner.
  Of the 186:
  - 98 are still locked: 96 have no documented source that reproduces them, and are listed under
    `unresolved` in `form-review.json`;
  - 16 are tracked as legacy-only in `cutover.json`;
  - 69 never-locked fixtures have no pinned reference.
- **The legacy RST extractor** (`rst/`, `model/CodeBlock`). Cutover and the example inventory
  read pages through it, with tested snippets spliced in and only the mongosh content of each
  language choice kept. It goes once the structured parser (`scenarios/`) owns example extraction,
  snippets included.
- **The frozen legacy documentation tables** stay until a separate documentation migration retires
  them.
