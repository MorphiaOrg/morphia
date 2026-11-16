# Operator audits

This module is being rebuilt around the [operator audit design](../design/mongodb-operator-audits.md).
The current milestones provide offline regression tests, a read-only corpus inventory,
compiler-resolved public API metadata, artifact-based capability comparison, and descriptive
example identity proposals. Pinned docs acquisition, existing-test migration, executable
generation and the `gh` publisher remain planned.

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
