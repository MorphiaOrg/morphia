# Operator audits

This module is being rebuilt around the [operator audit design](../design/mongodb-operator-audits.md).
The foundation provides offline regression tests and a read-only inventory of the
existing curated corpus. The source-inventory milestone adds compiler-resolved public API metadata. It does not yet provide the new operator-coverage comparison,
pinned docs acquisition, descriptive-name migration, or `gh` publisher.

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
coverage comparison belong to the next milestone: `aggregation.expression` alone cannot
prove accumulator/window support, and a test directory proves no API coverage.
