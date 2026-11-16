# Operator audits

This module is being rebuilt around the [operator audit design](../design/mongodb-operator-audits.md).
The current milestones provide offline regression tests, a read-only corpus inventory,
compiler-resolved public API metadata, and artifact-based capability comparison. Pinned docs
acquisition, descriptive-name migration, executable generation, and the `gh` publisher remain
planned.

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
Descriptive example naming/identity and the existing-test backport follow in separate work.
