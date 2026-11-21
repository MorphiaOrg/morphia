# Operator audits

This module is being rebuilt around the [operator audit design](../design/mongodb-operator-audits.md).
The current milestones provide offline regression tests, a read-only corpus inventory,
compiler-resolved public API metadata, artifact-based capability comparison, and descriptive
example identity proposals, pinned docs acquisition, and an offline structured document
adapter and evidence-backed capability discovery. Strict BSON comparison, shell values and declared ordered scenario programs are now available.
Automatic documentation grouping, existing-test migration, executable generation and the `gh`
publisher remain planned.

Run the tests from the repository root:

```sh
./mvnw -Daudits -pl audits verify
```

After Maven dependencies are cached, the same command works with `-o`. It does not require
MongoDB, a docs checkout, `gh`, or a GitHub token. Selecting `-Daudits` adds the module to
the reactor; it no longer changes whether other modules run their own tests. Use `-pl audits`
when only these tests are wanted.

The previous clone/pull Maven profiles and the live, mutating operator audit entry points
have been removed. Builds cannot regenerate core fixtures, rewrite Java tests or legacy
AsciiDoc tables, bypass lock files, or publish GitHub issues. The manual/PR workflow runs
only these offline tests with read-only repository permissions. The unrelated legacy
`SinceAudit` binary-history command remains separate and is not invoked by the test suite.

`CorpusInventory.capture(repositoryRoot, revision)` returns a deterministic preservation
record: example names, all fixture-file hashes, lock/lockd contents, operator exclusions,
and Java source hashes for the test package, including shared handwritten helpers and
test options. Shared aggregation/query resources and unexpected operator-level files are
recorded separately so they cannot escape the preservation baseline. It records incomplete
and descriptively named directories too; existence is not support.
`audits/catalog/legacy-corpus.json` is the initial reviewed-input snapshot. It is a baseline
for future migration, not an assertion that all entries are runnable or implemented.

`dev.morphia.audits.CorpusInventoryMainKt` accepts an explicit repository root and revision
and writes JSON to stdout through a pinned Maven exec plugin. After compiling the module:

```sh
./mvnw -q -Daudits -pl audits exec:java \
  -Dexec.args=". <revision>"
```

Run this from the repository root and redirect output to a candidate artifact for review;
the command never replaces the accepted snapshot. The supplied revision is recorded as
caller-provided metadata, while individual hashes describe the actual working-tree files.

The retained legacy parser now preserves raw snippet text without sanitizing JavaScript,
dates, Decimal128 values, comments, strings, or operator names. Include and external snippet
directives raise an explicit unsupported-source diagnostic until the pinned resolver is
implemented.
Its heading/example heuristics and character-count indentation helper remain legacy code;
the helper does not expand RST tab stops. Do not use this parser as a complete upstream
operator inventory. The fixtures under `src/test/resources/parser` capture the known
extraction failures independently of live documentation.

## Source API inventory

The source inventory uses the [JDK compiler API](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.compiler/com/sun/source/util/JavacTask.html)
to parse and analyze explicit Java inputs. Java resolves inherited names, generic bounds,
annotations, and overload erasure; the audits module does not implement a second Java
resolver. Public source declarations are frozen before analysis adds synthesized members.
Record components, compact constructors, effective modifiers, annotation defaults, sealed
permits lists, Javadoc evidence, and source locations are retained.

The inventory never invokes compiler generation or source execution. Annotation processors
and implicit source discovery are disabled. The classpath defaults to empty, rather than
inheriting Maven's runtime classpath. Missing dependencies and Java errors invalidate the
candidate instead of guessing a qualified name. Ordinary offline audit tests use small
self-contained fixtures and need no core build, MongoDB, docs checkout, or GitHub access.

To inventory the complete core, first compile it and resolve its dependency classpath
(from the repository root, after compiling the audits module):

```sh
./mvnw install -pl :morphia-core -am -DskipTests -Ddeploy.skip=true -Dinvoker.skip=true
./mvnw -pl core org.apache.maven.plugins:maven-dependency-plugin:3.7.1:build-classpath \
  -DincludeScope=compile -Dmdep.outputFile="$PWD/audits/target/core-dependencies.classpath"
./mvnw -q -Daudits -pl audits exec:java \
  -Daudits.mainClass=dev.morphia.audits.api.ApiInventoryMainKt \
  -Dexec.args=". <revision> --classpath-file audits/target/core-dependencies.classpath --classpath-entry core/target/classes core/src/main/java annotations/src/main/java"
```

The annotations root supplies same-package source types used by core; the compiled classes
supply generated types such as Sofia. Each source directory must be explicit, disjoint,
repository-relative, and nonempty. Symlinked source entries, invalid UTF-8, Java errors,
duplicate declarations, and inventories without public operator tags fail. Classpath archives with manifest `Class-Path` entries are rejected to prevent dependencies
outside the recorded inputs. Classpath files contain entries separated by the host platform's path separator. Additional entries can be
supplied explicitly with `--classpath-entry`; their order is preserved.

The command emits UTF-8 candidate JSON on stdout and never replaces an accepted catalog. Paths
are repository-relative. Source hashes cover the same immutable bytes passed to the compiler.
Classpath files/directories are hashed in order and rechecked after analysis; a changed
classpath invalidates the candidate. The supplied revision is caller-asserted metadata:
capture does not verify HEAD or require a clean worktree. The hashes identify actual inputs.
Parser engine, runtime version/vendor, and language level are recorded; byte reproducibility
requires the same inputs and toolchain. Analysis uses `--release 17` for both syntax and SDK
availability, including when the command runs on a newer JDK.

Callable IDs use fully qualified erased parameter types, while generic signatures remain
separate metadata. `modifiers` are compiler-resolved effective modifiers; `parserModifiers`
retain parse-time flags. Annotations and annotated source type spellings remain declared
text, with per-file imports for interpretation. Javadoc values consistently exclude the tag
name; malformed comments retain `documentationErrors`.

The artifact inventories declared public members and record header components; it does not
invent implicit accessors, constructors, bridges, or copies of inherited methods. It makes
no operator-support or category-completeness claim. Expected inventory manifests and
coverage comparison are described below: `aggregation.expression` alone cannot
prove accumulator/window support, and a test directory proves no API coverage.

## Capability contracts and coverage reports

The coverage package compares a versioned capability catalog, an independent coverage plan,
and the source API inventory. It does not acquire documentation or rewrite accepted inputs.
The catalog contains pinned sources, source-span/digest evidence, context-specific capability
keys, facets, all lifecycle events, constraints and example-extraction state. First observation,
operator introduction, feature introduction, Morphia `since`, and server requirements remain
separate facts. Unknown/conflicting version evidence is reported without guessing a release.
A lifecycle removal notice does not erase a current observation; a deliberate policy exclusion
can suppress support checks while preserving that evidence.

The plan declares all eight contexts, accepted identities and lower bounds, explicit evidence
for empty contexts, API input scope, binding exceptions, exclusions, and reviewed omissions.
New identities are allowed. Missing accepted identities, lost categories, and unexplained
count drops invalidate their discovery domain. A broken example does not hide an independently
verified new capability. Invalid/empty API inventories invalidate the comparison.

Unambiguous method-level tags map ordinary operator APIs automatically. All compatible
discovery contexts must be healthy before inference; broad or conflicting claims remain
`UNKNOWN_MAPPING`. Explicit bindings select every public overload of a factory/builder member
where aliases or multiple contexts require a decision. Unresolved selectors and overload
groups claiming different operator tokens produce binding errors and a partial report,
rather than a false missing-API finding. Raw BSON bindings have a distinct state. Version checks inspect every selected overload and retain class/method annotation
origins. A later minimum on an inferred API can indicate a feature variant requiring a binding,
so it is ambiguous rather than automatically incorrect.

API presence, version annotations, serialization, behavior, and example extraction have
independent report states. This milestone has no execution attestations: serialization and
behavior are always `NOT_ASSESSED`, including for existing fixture directories and raw BSON.
`COMPARED` means discovery/comparison completed, not that all operators are supported or tested.

After compiling the module:

```sh
./mvnw -q -Daudits -pl audits exec:java \
  -Daudits.mainClass=dev.morphia.audits.coverage.CoverageMainKt \
  -Dexec.args="catalog.json plan.json api-inventory.json"
```

Add `--markdown` before the three paths for a human summary. JSON is the default output.
Both outputs use UTF-8. Exit status is 0 for `COMPARED`, 3 for `PARTIAL`, and 2 for `INVALID`;
the command still emits a structured report for input failures. Store output as a candidate
artifact and retain all three inputs. A partial result cannot replace the accepted catalog;
future publication evaluates healthy domains individually.

The wire definitions are in `coverage/Contracts.kt`, with explicit `schemaVersion: 1`.
Strict JSON decoding rejects unknown fields, duplicate keys, numeric enum ordinals, null
collection elements, scalar coercion, missing required fields and trailing input. API compiler
provenance must be explicit. Source records use immutable Git revisions or content digests,
parser digests and a normalizer version; evidence references and source spans are checked.
Input order is canonicalized where it is not semantic; no clock values enter report hashes.

No production MongoDB catalog is fabricated here. The offline tests construct small synthetic
catalogs; the pinned acquisition/parser milestone will supply the real accepted observations.
Shared example identity contracts are described below; the existing-test backport follows
structured extraction.

## Descriptive example identities

`naming/DescriptiveNames.kt` is the shared naming contract for the future generator and
existing-test migration. AST plain-text titles become ASCII lowerCamelCase stems and
`test`-prefixed methods: “Calculate Median Ratings by Year” becomes
`calculateMedianRatingsByYear` / `testCalculateMedianRatingsByYear`. The bare stem is a legal
Java identifier and portable folder name. Keywords, restricted Java identifiers, device names
and numeric starts receive a `case` prefix. Unicode uses NFKD, drops combining marks, and
escapes remaining letters/symbols as `unicode` plus the hexadecimal code point. Acronyms use
`Locale.ROOT`; names over 80 characters need a reviewed bounded abbreviation. Repository-relative
resource paths have a 240-byte UTF-8 budget, reserving 32 bytes plus a separator for fixture
leaves; emitters use `fixturePath` to enforce that leaf contract. The future emitter must also
validate its checkout-root prefix and Java output paths. Nothing is silently truncated.

`NameReconciliation.propose(acceptedRegistry, request)` is pure and emits a candidate registry,
resolutions and diagnostics. Each request declares complete capability/language scopes and
already-grouped reference scenarios, source evidence, recovered context titles and physical
namespace inventory. Document names must be stable source-root-qualified logical paths;
snippet identities must include their logical source path/label. Fingerprints represent the
normalized scenario **and dependencies**, supplied by the future parser/semantic layer.
Equivalent language tabs are grouped upstream; distinct scenarios must not share a candidate.
This module does not invent fingerprints or infer equivalence from matching titles.

Reconciliation matches within the same capability/language: explicit labels or referenced
snippets first, then a unique anchor plus enclosing context, then a unique fingerprint.
Contrary match sets that exclude the chosen identity require review. Numeric auto-anchor endings need fingerprint corroboration; historical observations retain provenance but only
the latest observation supplies automatic identity matches.
Established stems persist across cosmetic title edits and content changes with stable labels.
No ordinal, line number or nearest-match score can select an identity. Multiple matches,
competing observations, unexplained disappearances and automatic destination moves remain
unresolved. Reviewed assignments can establish a new name, choose an existing identity, or
rename/move it; renames retain previous qualified names as aliases. Splits/merges require
explicit new assignments and dispositions. Retired names remain reserved. The request pins
the current catalog; assignment and retirement rationale/evidence persist in registry decision
records so later reconciliations retain the review trail.

Collision checks cover capability-qualified keys, shared resource directories, target classes,
accepted aliases/tombstones, previous physical bindings after moves, and supplied folder/method
inventory, including inherited helpers. Destination namespaces also compare without case.
Folder comparisons normalize with NFKC and root-locale case conversion. All colliding
new scenarios advance together through meaningful enclosing-title qualifiers; every new
duplicate must disambiguate, while established names and reviewed assignments take precedence.
Encounter-order counters never decide a winner. An entry may ignore only its proven existing binding.
Ambiguities need reviewed names rather than overwriting a neighbor. Source titles and
observations remain separate from canonical names. The namespace inventory is explicit input;
the future migration/generator must obtain it from the physical corpus/compiler, not guess it.

After compiling the module:

```sh
./mvnw -q -Daudits -pl audits exec:java \
  -Daudits.mainClass=dev.morphia.audits.naming.NamesMainKt \
  -Dexec.args="accepted-names.json naming-request.json"
```

All three artifacts have strict schema version 1 contracts in `naming/NameContracts.kt`.
The command emits UTF-8 JSON to stdout: exit 0 `READY`, 3 `UNRESOLVED`, 2 `INVALID`.
A partial proposal cannot replace the accepted registry; retain its inputs for review.
No command changes a registry, test method, fixture directory or source checkout. This PR
contains synthetic identity tests; the production registry and coordinated legacy rename
are populated after structured extraction. The existing resource-discovery adapter's default
locale conversion must be migrated to `Locale.ROOT` and tested with the actual helper during
that backport; this module does not claim it has already renamed or repaired core tests.


## Pinned source bundles

The source package captures immutable Git objects into a standalone, verified bundle. Ordinary
Maven builds/tests never fetch a docs repository. Offline source tests use small local Git
repositories; they require native Git with `--no-lazy-fetch` support (verified with Git 2.53).
No `gh`, MongoDB server, Python parser, credentials or real docs checkout is needed for them.

`audits/config/manual-probe.json` is a reproducible selected-source request at the reviewed
MongoDB docs pin. It is a raw-source probe, not a production operator catalog.

A strict schema-1 `GitSourceRequest` declares the repository URI/full commit, project root,
source label/role/channel, selected paths, allowed symlink dependency roots, and file/byte
budgets plus a per-command timeout. Object capture has its own session deadline. Labels are
caller-provided metadata, not a release inferred from the path. Capturing
reads raw objects rather than a dirty checkout. Git replacement objects, checkout filters and
implicit blob fetching cannot change those bytes. Git version is retained separately from the
content hash; changing execution budgets does not change content identity. Identity, label,
role and channel are pinned because they affect interpretation. Commits and every selected
parent tree are hash-checked through native Git; no second Git-format parser is maintained.

The explicit acquisition command supports an existing offline object cache or a new filtered
fetch. Run it only after compiling the module, with fresh output directories whose parents exist:

```sh
./mvnw -q -Daudits -pl audits exec:java \
  -Daudits.mainClass=dev.morphia.audits.sources.SourcesMainKt \
  -Dexec.args="source-request.json audits/target/source-bundle --repository /path/to/git-cache"
```

Replace `--repository /path/to/git-cache` with `--fetch-to audits/target/source-git` to explicitly
acquire the full pinned commit's tree and hydrate selected/dependency blobs in bulk. It creates
a new bare Git cache without checkout. Existing cache/bundle directories are never replaced.
The command emits a UTF-8 `SourceAcquisitionReport`: exit 0 `READY`, 3 `PARTIAL`, 2 `INVALID`.
A partial manifest records missing, undeclared or cyclic source dependencies; it is never a
claim that an operator or example is absent. Keep the report and inputs with the candidate.

Bundles contain a completed manifest and flat SHA-256-named blobs. Tracked file modes,
original Git object IDs, raw symlink text and per-file hashes are retained. The completed
manifest is published only after verification; a failed invocation cleans its new candidate.
`READY` means the selected raw Git objects/link closure are captured; document-level, generated
and remote dependencies are assessed by the following parser/resolver milestone.
`SourceBundle.open(path, expectedContentSha256)` verifies the pin, every blob and dependency
health. `read(repositoryRelativePath)` resolves virtual links only through declared resources
and returns canonical origin plus the link chain. Symlinks are metadata, not filesystem links. Embedded `.`/`..` navigation after a named
component and trailing-slash targets remain explicitly unresolved; the resolver never treats
lexical path cleanup as filesystem symlink semantics.
Reads recheck bytes to detect drift. No source scripts or examples execute during acquisition.

This is the acquisition boundary, not the documentation parser. Remote shared includes still
need explicit immutable source/resource pins; no parser is permitted to fetch ambient URLs.
The next PR prototypes the pinned Snooty adapter and connects operator/example provenance.
The real manual and parser pins plus the symlink/shared-include observations are recorded in
`design/operator-audits-progress.md`; production catalog generation remains pending.

## Pinned structured document adapter

`documents/DocumentParsing.kt` exports a verified `SourceBundle` snapshot to the isolated
helper in `parser/adapter.py`. The helper validates every transported resource hash and
uses MongoDB's Snooty parser at commit `0c26866083a02177a52ade1ccb17cc61a2816be7`,
CPython 3.12.16, and the exact dependencies recorded in `parser/uv.lock`. The explicit
`typing-extensions` dependency repairs an upstream omission. No AST database or publisher
is needed. The runtime checks the exact platform-selected dependency versions and installed RECORD
hashes (detecting local drift; no toolchain attestation), and records adapter/lock digests, source
content pin and exact transport digest. The standalone helper trusts the source pin in its
input; use the Kotlin coordinator to bind it to a verified source manifest.

Prepare the isolated environment explicitly (installation may use the network):

```sh
uv sync --frozen --project audits/parser
```

Run its real parser regressions offline after installation:

```sh
audits/parser/.venv/bin/python -I -B audits/parser/tests/test_adapter.py
```

The ordinary JVM test suite does not install or require Python; its real parser integration
case skips unless `audits.parser.python` and `audits.parser.adapter` are explicitly supplied.
CI runs the locked parser suite and this coordinator integration under both mapper settings. Tests exercise nested includes, IO ranges and whitespace, external-file
provenance, Giza extracts, substitutions, language tabs, unknown directives, missing resources,
and network/read confinement. The Kotlin suite checks transport/provenance, input drift,
exit status, deadlines, output bounds and strict JSON. No example code is executed.

After compiling the audit module, invoke `dev.morphia.audits.documents.DocumentsMainKt`
with the bundle directory, expected source-content SHA-256, absolute isolated Python path,
absolute `adapter.py` path, and one or more project-source-relative `.txt` page ids. For example,
pass `reference/operator/aggregation/median.txt` for the selected manual property. Output is
UTF-8 JSON: READY exits 0, PARTIAL exits 3; invalid input/runtime fails with a diagnostic on
stderr and exits 2. The coordinator's defaults are a 120-second deadline and 64 MiB stdout
budget (1 MiB stderr); its library API permits explicit bounded budgets. Outputs are candidates;
accepted catalogs, curated tests, locks and fixture directories are never replaced.

The versioned Morphia-owned tree retains raw and expanded pages, plain text, identifiers,
directives and arguments, options, language/code attributes, nested include chains, and
external-file dependencies checked against captured raw hashes (both Snooty digest sizes).
The coordinator independently cross-checks returned origin paths/hashes against the bundle. Additional scalar/structured
attributes survive as canonical JSON strings; downstream normalization must explicitly handle
node kinds before claiming category completeness. `reportedLine` is a parser hint at the
owning RST directive, **not** an invented location within an external snippet. Generated YAML
content has an explicit unknown/generated position. Raw trees preserve the evidence before
postprocessing resolves includes/substitutions. The currently supported property layout is
an exact pinned `snooty.toml` plus `source/`; other configured source roots diagnose as unsupported.

Parsing materializes a private source tree from exported bytes. It blocks remote HTTP,
sockets, child processes, and reads outside the private source and controlled Python/adapter
runtime roots; writes are restricted to a private cache. These Python audit hooks constrain
the trusted pinned parser; they are not an OS sandbox for arbitrary Python programs. Snooty's
process pool is replaced with one worker thread, and ambient MIME tables are excluded.
Snooty file enumeration uses the verified private bundle as its path jail so declared shared
RST links outside the selected property can resolve; its nested-project/cycle traversal rules
remain in force. Unhealthy links themselves are suppressed without hiding healthy ancestor
links or sibling includes. The native coordinator additionally enforces time/output bounds
and kills descendants on failure.
Unresolved links, remote inventories/shared includes, missing files, unknown directives, parser
exceptions and incomplete dependencies remain explicit PARTIAL results. All diagnostics are
retained, including unrelated captured pages; this adapter makes no operator-absence claim.
A later normalizer will scope domain health and classify nonsemantic cross-reference failures.

To upgrade, change the immutable parser revision, supported Python pin and dependency lock
in one reviewable change; update coordinator/runtime guards together. Run both JVM mapper
configurations, these real parser regressions, and the pinned median/lookup/setWindowFields
probe. Review changed nodes/diagnostics and source provenance before accepting new artifacts.
Never silently retry with an unpinned parser or fetch dependencies during parsing.

The immutable manual probe can be checked with
`python -I -B audits/parser/verify_manual_probe.py <document-report.json>`. It requires
exactly the configured source pin and three page ids, both trees, complete captured example
file dependencies, definition identifiers and all root/feature version events. The deliberately
incomplete bundle must remain PARTIAL. At the recorded pin the adapter retains 3/34/21 code
blocks and 2/20/20 external files for median/lookup/setWindowFields respectively.

## Capability discovery from document evidence

`discovery/CatalogDiscovery.kt` normalizes a verified source bundle and the owned document
model into a candidate `CapabilityCatalog`. `DiscoveryPlan` is an independently reviewed
policy: logical definition roots, catalog pages, AST section/header selectors, execution
contexts, supported reference roles and count lower bounds. It contains no encounter-order
operator/example names. `manual-discovery-probe.json` includes current MQL catalogs and
window pages omitted by the earlier three-page probe; `manual-discovery-plan.json` is the
matching eight-context probe policy. They are evaluation inputs, not an accepted production
coverage baseline.

Only table first-column names and declared operator-reference lists count as catalog members.
Description mentions do not. A narrow list adapter supports leading names and colon-labeled
reference groups under an explicit catalog section. Unexpected shapes, uncovered operator
tables, missing pages, count drops and unresolved role/context assignments stay actionable.
Update modifier sections are distinct from ordinary update operators. Definition roles alone
do not establish availability: some window-only functions still use `group`, and expression
catalogs can refer to accumulator definitions. Positional `$`, `$[]` and `$[<identifier>]`
are canonical identities in coverage and naming; a concrete `$[element]` is application syntax.

Every observed capability has verified raw declaration or catalog-reference/name evidence.
Reported origins and digests are rechecked when loading persisted document reports, and the
stored parse-input pin must match the export recomputed from the bundle. Parser runtime fields
(adapter, lock, interpreter) are reported provenance, not verified offline. Unique verified raw
lines have exact byte hashes, including line endings; ambiguous/generated locations use honest
whole-resource evidence. A parser-reported line is satisfied only by that raw line or, for
inline roles that Snooty reports at their paragraph start, a line within the same raw paragraph;
a match elsewhere in the file never stands in for it. Feature facets require a raw section
underline at the reported heading position. The normalizer does not invent an RST position inside YAML
or an external snippet. Catalog counts/identities still need the independent accepted expectations
in `CoveragePlan`; deriving that baseline from the current candidate would defeat drop detection.

Root lifecycle notices require an owned definition body/section. Nested feature sections,
including a nested heading named Definition, cannot introduce the root operator. Nested catalog
lists cannot introduce their parent. Cross-context aliases, unresolved substitutions, generated
positions and unverified scope preserve UNKNOWN confidence. Section notices outside every target
stay UNKNOWN when the page defines more than one operator. A section's 8.3 feature addition
therefore cannot turn an operator introduced in 5.0 into a new 8.3 operator. Parser-rendered
version values are never substituted for independently available raw source arguments.

Discovery, versions, examples and references have separate diagnostics. A failed `:doc:` link
is a reference problem when an AST doc role owns the reported position and the raw paragraph
contains the role; links from generated YAML extracts or includes without an owning AST node
stay definition uncertainty. Example-include ownership is decided separately for each page. A verified failed
literalinclude/input/output affects examples; an unexplained dependency graph can affect
definition health. Include attempts stay reachable even when they produce no AST nodes.
All other parser diagnostics are retained as outside the selected scope rather than discarded.
Missing inventories and unrelated cross-references do not erase independently verified
capabilities. A definition used by a fallback context does not prove its native context complete.
Unassigned declarations remain in the report for explicit review, without invented API gaps.

The result records canonical policy and document-input hashes, parser/runtime provenance and
source pins; the catalog's normalizer identity carries the same policy/input trace. Context,
catalog and diagnostic states, the catalog's normalizer identity, evidence references and page
capability keys are checked when decoding the report. Plans that repeat selectors or roles, or
both select and exclude a header or section, are rejected because they would change the policy
pin without changing its meaning. Examples remain
NOT_ASSESSED unless source problems prove them unresolved; no serialization/behavior success
is inferred and no shell example executes.

After compiling, run `dev.morphia.audits.discovery.DiscoveryMainKt` with either:

```text
<bundle> <source-content-sha256> <document-report.json> <discovery-plan.json>
<bundle> <source-content-sha256> --parse <absolute-isolated-python> <absolute-adapter.py> <discovery-plan.json>
```

The second form selects captured definition/catalog pages, invokes the locked helper and
normalizes its result directly. Native parsing has a 600-second deadline and 128 MiB compact
output cap; saved document reports have a 512 MiB input budget. The full pinned 350-page probe
parsed in about 71 seconds and produced about 208 MB of pretty document JSON; use a JVM heap
such as `-Xmx3g` for full-scope evaluation. Ordinary tests use small fixtures. The command emits
UTF-8 candidate JSON: READY exits 0 and PARTIAL exits 3. Input/runtime validation failures
exit 2 with stderr diagnostics. Unrecoverable VM errors (for example, out of memory) are not
caught: they exit nonzero with the JVM's own diagnostic rather than being reported as INVALID
input.
It does not replace accepted catalogs or publish issues. Retain the source bundle and policy
when evaluating a result; partial discovery cannot establish operator absence.

The current pinned manual contains genuine catalog/definition disagreements and missing
external metadata. The probe deliberately reports those, including legacy role ambiguity,
instead of rewriting source facts or fixtures to produce READY. An accepted production baseline,
source dispositions and complete pinned external resources follow in subsequent stages.

## BSON scenario comparison

`dev.morphia.audits.semantics.BsonComparison` provides a pure, bounded comparator over driver
`BsonValue` trees and lists of result documents. Defaults preserve BSON types, numeric
representations, document keys, arrays, pipelines and duplicate results. Explicit RFC 6901
paths can relax document key order or finite numeric values of the same BSON type.
`ResultOrder.UNORDERED` relaxes only the top-level result multiset; nested arrays remain ordered.
Invalid/cyclic inputs or resource limits return `INVALID`, never a passing comparison.

The [comparison contract](../design/operator-audits-bson-comparison.md) documents the policies,
old-versus-new regression probes and the required compatibility assessment before migrating
existing expectations. Core assertions and curated data continue to use their preserved paths;
this is the oracle for the upcoming scenario runner, not an execution attestation.

## Shell BSON values

`dev.morphia.audits.shell.ShellLiterals.normalize` lowers one value through the pinned Rhino AST
and a closed BSON constructor allowlist. It preserves the raw block and requires an explicit
shell dialect. Unknown calls, bindings, computed syntax, invalid regexes and ambiguous defaults
remain unsupported. It never evaluates documentation programs or overwrites fixtures.

The [literal contract and parser decision](../design/operator-audits-shell-literals.md) records
measured compatibility, limits and the remaining ordered-scenario integration. The optional
[parser comparison probe](probes/shell/README.md) accepts explicitly acquired artifacts; ordinary
Maven builds need no Node runtime or upstream documentation checkout.


## Declared ordered scenario programs

The [program design](../design/operator-audits-programs.md) describes the closed AST grammar,
source roles, shell dialects and expected-result ownership. `ShellPrograms.normalize` is pure:
it preserves complete validated source blocks and produces typed, bounded commands without
running JavaScript, contacting MongoDB or writing fixtures. Unsupported input yields no partial
commands or expectations. `NORMALIZED` is a structural result, not executable coverage.

The JSON request names every block's role and format explicitly. `SHELL` uses the pinned Rhino
AST and explicit shell dialect. Expected-only `SHELL_RESULTS` handles printed document streams
without rewriting them; see the [format design](../design/operator-audits-shell-results.md).
`LEGACY_DOCUMENTS` uses the maintained BSON reader as a declared typed fixture format; it does not
infer shell serialization. Legacy setup/pipeline fragments require their logical namespace.
Multiple action commands require an explicit `expectedFor` owner. Expected errors require source-backed
metadata. Published whole numbers retain explicit Int32/Double alternatives; unsupported display
profiles remain visible failures. Typed reference-versus-DSL parity remains strict and independent.

After compiling the module, normalize the synthetic multi-collection fixture:

```sh
./mvnw -q -Daudits -pl audits exec:java \
  -Daudits.mainClass=dev.morphia.audits.shell.ProgramsMainKt \
  -Dexec.args="audits/src/test/resources/programs/join-inventory.json"
```

Output is candidate JSON on stdout; exit status is 0 for `NORMALIZED`, 3 for `UNSUPPORTED` and
2 for `INVALID`. BSON snapshots contain exact encoded bytes, preserving numeric types and key
order. Reports are write-only: regenerate from the source request rather than deserializing
encoded BSON or operation classes. There is no document-grouping heuristic, baseline promotion,
server execution or accepted fixture migration in this command.

## Native scenario groups

`ScenarioExtraction.extract` connects the owned document AST to ordered programs and descriptive
name candidates using explicit IO ownership, selected heading scopes and reviewed language/tab
policies. Raw sources, external snippet receipts, selection options and foreign variants survive.
Unknown grouping/setup or missing dependencies prevent complete-scope name reconciliation.
Program normalization and source grouping have independent states; complete grouping does not
prove execution or assigned names. See the [extraction contract](../design/operator-audits-scenario-extraction.md)
for the plan schema, CLI, provenance and remaining legacy-layout work.

`ScenariosMainKt <bundle> <source-sha256> <documents.json> <plan.json>` emits read-only candidate
JSON. `ScenarioExtraction.namingRequest` requires physical name inventory and can select independent
healthy scopes before calling the existing registry reconciler. No fixture, test, accepted name
registry or GitHub issue is written by either operation.

Standalone legacy layouts can opt into `sectionAssembly`; see the
[section/setup contract](../design/operator-audits-section-assembly.md). It preserves raw source,
uses declared shared setup selectors, and recovers authored page/tab titles for naming. A
`SOURCE_ONLY` group is explicitly non-executable. The pinned `probes/section-assembly/replay.py`
experiment covers automatic `$abs` ownership and descriptive `$acos` tab names without creating
seed data from displayed samples. Existing generated core tests/resources have not yet migrated.

## Physical example bindings

`LegacyBindings.capture` parses the four operator test packages with the JDK parser (no
processors, attribution or execution) and records every JUnit method's actual fixture ownership:
`BOUND` to exactly one fixture directory, a reviewed `MANUAL` test in a templated class, or an
`INDEPENDENT` test in a class without templated fixtures. Display titles, `@Disabled`, empty bodies
and body digests remain visible as declared source evidence; an empty or disabled bound method is
not behavioral coverage. Reviewed exceptions live in `audits/catalog/binding-policy.json`; a fixture
or method that is neither bound nor listed fails the import, and so does a policy entry that is now
bound or missing. See the [binding design](../design/operator-audits-binding-manifest.md).

`BindingsMainKt <repository-root> <revision> <policy.json> <catalog-out> <descriptor-out>` writes
an evidence catalog and core's runtime descriptor
(`core/src/test/resources/dev/morphia/test/example-bindings.json`) to the explicit paths given.
`audits/catalog/legacy-bindings.json` is the frozen pre-backport import; write new catalogs elsewhere:

```sh
./mvnw -q -Daudits -pl audits exec:java \
  -Daudits.mainClass=dev.morphia.audits.bindings.BindingsMainKt \
  -Dexec.args="$PWD <revision> $PWD/audits/catalog/binding-policy.json \
    $PWD/audits/target/current-bindings.json \
    $PWD/core/src/test/resources/dev/morphia/test/example-bindings.json"
```

Core reads only the descriptor; ordinary core builds have no audits dependency. Its
`TestCoverageExtension` and the offline `ExampleBindingsTest` check descriptor membership in both
directions (declared tests and physical fixture directories), reject unknown or missing classes,
duplicate owners and inherited ownership, and require every bound method to round-trip through the
template loader's naming convention. `TemplatedTestBase.discoverResourceName` resolves fixtures
through the descriptor, so a manual test cannot load a fixture by convention.

## Descriptive-name backport

The existing corpus now uses descriptive method and fixture names; see the
[backport design](../design/operator-audits-name-backport.md). `audits/catalog/name-backport.json` is
the legacy-to-descriptive ledger; `name-backport-review.json` holds the pinned page titles and
reviewed stems it was planned from. `NameBackportMainKt plan <root> <revision>` rewrites only the
ledger. `apply` was the one-time migration; rerunning it on the migrated tree is refused because no
ordinal names remain to rename. `LegacyBindingsTest` keeps the ledger, the pre-backport snapshots
and the live sources consistent.

## Curated repair inventory

`RepairsMainKt <repository-root> <revision> <repair-review.json> <form-review.json> <out>` classifies every lock reason
and operator exclusion through the reviewed `audits/catalog/repair-review.json`, and inventories each
bound test's `ActionTestOptions`, executed assertion paths and extra `loadData` collections. The committed result is
`audits/catalog/curated-repairs.json`; an audits test requires it to equal a fresh capture. See the
[repair design](../design/operator-audits-curated-repairs.md).

## MORPHIA_FORM lock migration

`audits/probes/form-sources/fetch.py` fetches the historical `mongodb/docs` pages behind locked
`MORPHIA_FORM` fixtures (it is the only networked step). `FormMainKt` then works offline:

```sh
python3 -I audits/probes/form-sources/fetch.py candidates "$PWD" first /tmp/first.json
python3 -I audits/probes/form-sources/fetch.py candidates "$PWD" last /tmp/last.json
# Optional trailing categories widen the selection, e.g. ... first /tmp/first.json NUMERIC_TYPE MORPHIA_FORM
python3 -I audits/probes/form-sources/fetch.py pages /tmp/first.json /tmp/pages
python3 -I audits/probes/form-sources/fetch.py pages /tmp/last.json /tmp/pages
./mvnw -q -Daudits -pl audits compile exec:java \
  -Daudits.mainClass=dev.morphia.audits.repairs.FormMainKt \
  -Dexec.args="draft $PWD /tmp/pages mongodb/docs /tmp/form-review.json /tmp/first.json /tmp/last.json"
```

`draft` only proposes a review. Every command reads the locks' categories (form rules need `MORPHIA_FORM`, number retypes
`NUMERIC_TYPE`) and the tests' extra collections
(`loadData("airports", 2)` → `data2.json`) from the committed `audits/catalog/curated-repairs.json`,
so recapture that inventory first when a test's `loadData` calls change. The reviewed `audits/catalog/form-review.json` is consumed by
`capture <root> <revision> <pages> <review> <out>`, which writes `audits/catalog/form-migration.json`,
and by `apply <root> <report>`, which deletes the retired locks. An audits test rechecks the committed
report against the tree without the pages. See the [migration design](../design/operator-audits-form-migration.md).

