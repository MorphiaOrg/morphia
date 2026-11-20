# Descriptive names for the existing operator examples

Step 3a of [the operator audit plan](mongodb-operator-audits.md): replace the ordinal
`testExampleN`/`exampleN` names in the existing corpus with descriptive names, changing each method
name, fixture directory and "test data" Javadoc line together. Packages, operator directories,
display titles, `name` files, locks, bodies, options and every fixture byte stay as they are.

## Name sources

`NameBackport.plan` derives a stem with the shared `DescriptiveNames` rules from, in order:

1. a reviewed stem in `audits/catalog/name-backport-review.json`;
2. the example's own `name` title. Legacy titles still carry RST inline markup, so
   `plainTitle` drops literal/role markup (`~` roles keep the last component), substitutes the
   generated pages' `|operatorName|`, and removes the ordinal-only `Example N:` prefix, the legacy
   parser's trailing `[N]` disambiguator and a generic `main ::` section in front of a tab title;
3. for a generic label (`main` or no title), the authored page title from the pinned manual
   (mongodb/docs `a5d960b9`), recorded with its source path. No operator page has more than one
   generic example, so the page title identifies the example within its operator directory.

Only ordinal names are backported. The established `changeStream` fixture keeps its name.

Collisions fail the plan instead of being resolved by ordinals. Folder names are compared
case-insensitively across every surviving and renamed fixture in an operator directory. Method
names are checked against every surviving and renamed test in the class, and against the methods
`TemplatedTestBase`/`TestBase` declare (`testPipeline`, `testQuery`, ...). Unused page titles and
reviewed stems are stale and fail too.

Seven reviewed stems resolve the remaining cases:

- Four legacy duplicate copies (`$bucketAuto`, `$planCacheStats`, and two `$project` pairs) get a
  qualifier naming their recorded difference. Examples: the array-wrapped pipeline, the newer
  `planCacheShapeHash` output, the raw shell insert data.
- Two `$meta` `[1]` titles are named after the docs' explanation that no index supports the filter.
- The empty `$percentile` placeholder is marked as a placeholder rather than duplicating the bound
  `$setWindowFields` example's name.

## Ledger and application

`audits/catalog/name-backport.json` is the legacy-to-descriptive ledger: old/new method and fixture,
display title, title source and evidence for all 556 renames (553 bound fixtures, the two marker-only
change-stream fixtures and the manual placeholder). `NameBackportMainKt apply` checks every
destination and declaration before moving anything. It then renames directories and method
declarations, rewrites the Javadoc that belongs to the renamed method, renames the policy entries,
and regenerates the core descriptor.

`legacy-bindings.json` and `legacy-corpus.json` remain the immutable pre-backport snapshots. The
audits drift test requires a fresh import of the renamed sources to equal the legacy import projected
through the ledger: same dispositions, display titles, disabled/empty flags and body digests, with
only names changed. `NameBackport.verify` also checks:

- every preserved fixture file's bytes at its new path;
- that no ordinal directory, method or `example` path remains in scope;
- that every "test data" Javadoc names the fixture of the method it documents.

The registry-based reconciliation in `naming/` still needs capability-scoped docs scenarios, so it
does not drive this backport. The ledger keys are legacy paths. Later generation must import them as
established names (aliases are the old ordinal paths) rather than re-deriving names from scratch.
