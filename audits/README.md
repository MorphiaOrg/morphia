# Operator audits

This module is being rebuilt around the [operator audit design](../design/mongodb-operator-audits.md).
The first milestone provides offline regression tests and a read-only inventory of the
existing curated corpus. It does not yet provide the new operator-coverage comparison,
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
