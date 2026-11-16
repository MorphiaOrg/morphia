# Operator audit implementation progress

Current branch: `audits-pinned-sources`; PR base: `audits-example-identities`.
Stack (PR head branches): #4512 `audits-pipeline-design` → #4513 `audits-source-inventory` →
#4514 `audits-coverage-contracts` → #4515 `audits-example-identities` → pinned sources (in progress).
Each PR targets its predecessor; #4512 targets `master`.
Use `git worktree list` to locate the checkout on this machine.

Design: [mongodb-operator-audits.md](mongodb-operator-audits.md).
The reviewed plan is committed as `3c78eb7b4cccf5c661bc2183d25cf3984ddda3ad`.

## Authorization and scope

- Commit the plan and begin implementation; replace/delete existing code when appropriate.
- Additional implementation commits and a push/PR are authorized. Do not merge unless asked.
- The user authorized stacked PRs on 2026-11-16: once the preceding PR passes CI and
  agent review, continue automatically on a branch based on it. Do not wait for a merge.
  This supersedes the repository's usual user-review gate between implementation PRs.
- Milestone 0 is complete in PR #4512. Milestone 1 is split into focused, dependent PRs:
  first the read-only source API inventory, then capability/naming/reporting contracts.
- The completed system must automatically create/update issues through `gh` in a separate
  explicitly enabled CI publisher. Ordinary builds/tests/report generation must never
  publish. The publisher is milestone 6 and is not implemented in this foundation.
- Test methods and data directories must eventually use descriptive legal identifiers
  synthesized from docs titles, including backporting existing cases. This is milestone 3a;
  no mass fixture renaming has been attempted in milestone 0.

## Implemented

- Restored Kotlin API compatibility to 2.1 with Java 17 target.
- Removed automatic Maven docs clone/pull profiles and the global test-skipping behavior
  of `-Daudits`.
- Removed legacy live/HTML audits, issue publisher, broken Java/Javadoc/AsciiDoc generation,
  mutating tests, unused resources, and unused module dependencies. The separate old
  binary-history `SinceAudit` remains; no test invokes it.
- Added a read-only offline PR/manual workflow without write credentials.
- Added an offline test namespace and Surefire inclusion/fail-if-empty configuration so
  stale compiled legacy tests cannot execute.
- Retained limited read-only inline parsing with explicit file inputs. Raw strings,
  whitespace, comments, numeric constructors, operator spellings, and uneven indentation
  are preserved. Includes/external snippets require the future resolver and fail explicitly.
  This is not yet a complete documentation parser or operator-coverage checker.
- Added a deterministic corpus inventory and stdout-only CLI, with exec plugin pinned to
  3.6.3. The inventory retains partial/named cases, markers and exclusion reasons, fixture
  hashes, all Java sources in `dev/morphia/test`, and additional aggregation/query resources.
- Updated IDE commands and workflow instructions, removing obsolete chained audit runs.
- Added offline parser fixtures and inventory regression tests.

## Preservation baseline

`audits/catalog/legacy-corpus.json` records the plan commit as caller-provided metadata.
Its hashes describe the actual files at capture time; it is not a coverage assertion.

- 556 example directories: 555 numbered cases and the named `changeStream/changeStream` case.
- 235 exact `lock` markers, one `lockd`, and 17 operator exclusions, including their contents.
- 513 Java test-package sources, including shared helpers and options.
- Three additional/shared aggregation/query resources.
- Every aggregation/query fixture file is accounted for, with no duplicate file paths.
- Core sources, fixtures, and legacy documentation remain unchanged.

Reproduce the accepted snapshot after compiling (keep its recorded revision):

```sh
./mvnw -q -Daudits -pl audits exec:java \
  -Dexec.args=". 3c78eb7b4cccf5c661bc2183d25cf3984ddda3ad" > /tmp/operator-corpus.json
cmp audits/catalog/legacy-corpus.json /tmp/operator-corpus.json
```

## Validation and local review

- Offline Maven verification passes 18 tests with Critter first and reflection second.
  The audit module is mapper-independent; both selections follow repository policy:
  `./mvnw -B -ntp -o -Daudits -pl audits verify -Dmorphia.mapper=critter`
  (then repeat with `reflection`). Java 17 is required.
- An intentionally failing stale test class outside the offline namespace and a non-git
  `audits/target/mongodb-docs` directory were present during verification. Neither executed
  nor triggered a pull. These artificial ignored artifacts can be deleted safely.
- Inventory CLI output is machine-readable and byte-identical to the accepted catalog;
  its file hashes/sizes and complete resource coverage were independently checked.
- Spotless and actionlint passed. The graph was refreshed after code changes.
- A temporary hash comparison verified 3,598 protected core/documentation files unchanged.
  The enduring check is `git diff 3c78eb7 -- core docs`, which must remain empty for this PR.
- Local review rounds found valid issues that were fixed: unpinned CLI execution, missing
  shared-helper/resource coverage, unstable include lookup, obsolete IDE commands and
  workflow instructions, silent truncation of unevenly indented blocks, trailing
  whitespace-only lines, and colon-prefixed code mistaken for directive options. These parser cases have regressions.
- Three local review rounds are complete. All actionable findings were fixed. A note about
  RST tab-stop expansion is deferred to the structured-parser milestone: the retained legacy
  helper counts indentation characters, so it is not a general RST indentation model.
  The live pipeline is disabled and the documented parser limitations remain explicit.

## Published checkpoint

- Foundation implementation: `90588411ea00b7dc662b90edb2e79d46c3d2fa41`.
- Plan and implementation pushed to `origin/audits-pipeline-design`.
- PR: [#4512](https://github.com/MorphiaOrg/morphia/pull/4512), open against `master`.
- Final checks after round 3 fixes passed: 18 tests for each mapper selection,
  formatting, graph update, actionlint, XML, protected inputs, and catalog reproducibility.
- GitHub Copilot review was requested at `lite` effort. Its initial response reported
  insufficient quota, but a later completed review found one stale design-status sentence.
  The design now explicitly identifies milestone 0 as implemented and steps 1–7 as planned;
  its baseline evaluation and next-step description are consistent with this checkpoint.
  No code findings were raised in the remote review. Local review rounds are complete.
- All foundation CI passed on `6ee80464a52a1a88db1e3493c45d426dc909e2bd`: audit tests,
  code audits, full build, and Critter AOT. It is not merged.
- A later remote finding claimed the existing no-tabs include regression would fail.
  Verified and rejected: `removeWhile` returns the prefix saved as `main`, the no-tabs
  branch returns that content, and the regression passes locally and in CI. The thread
  has a reasoned reply and is resolved. There are no unresolved review findings.
- The deliberately created stale-test class and non-git docs-directory sentinel were removed
  after verification. No user fixture, docs checkout, or tracked core file was changed.

## Source inventory published checkpoint

- PR [#4513](https://github.com/MorphiaOrg/morphia/pull/4513) targets
  `audits-pipeline-design` (#4512). Merge order is #4512 then #4513; neither is merged.
- Source inventory implementation commit: `d96700f73b52225d6148bfe17308a94076edc2fe`.
- Current branch/worktree: `audits-source-inventory`, based on the fully checked foundation.
- Code, 40-test verification under both mapper selections, three local review rounds,
  reproducibility, protected-input checks, formatting and graph update are complete.
  All CI passed on `088d719e32bd92db9a52dcfbf0e8ff77f9c5d2ab`. Claude verified the final
  manifest/UTF-8 corrections with no remaining defects. Copilot CLI and GitHub review
  requests are quota-blocked; the earlier available review rounds were completed.
  GitHub rejected the PR review request with HTTP 422 `NO_QUOTA_REMAINING`.
  The user resumed after an unrelated tool-approval usage reset; no code was lost.
- JDK compiler adapter parses and analyzes explicit source/classpath inputs, hashes the
  same immutable source bytes, and emits JSON on stdout. Processors and implicit source
  discovery are disabled; it never invokes generation or source execution.
- Public declarations retain owning types, overload signatures, generics, annotations,
  visibility, nested/interface members, raw class/member Javadoc tags, and source locations.
  It does not infer operator context, merge class tags onto helpers, execute source code,
  or claim serialization/behavior coverage.
- Initial parse-only prototypes passed 35 tests, but review found semantic resolution
  pitfalls. The hand-written resolver/hierarchy has been deleted; compiler attribution
  and erasure now supply qualified types, modifiers, bounds, and overload identities.
  This is a deliberate architecture change: do not restore the discarded resolver.
- Record components, compact constructors, source spellings/imports, Javadoc errors, and
  runtime provenance are retained. Annotation defaults and sealed permits are included.
- Reviews found valid metadata/guard gaps that were addressed. The compact-constructor
  suspicion was rejected using a JDK probe: parsed compact constructors already carry the
  canonical parameters. A regression preserves that contract.
- The compiler-backed suite passes 40 tests with Critter first and reflection second,
  including explicit-classpath hashing, declaration scoping, no execution/generation,
  rejection of manifest-based hidden classpaths, and UTF-8 output under an ASCII runtime.
  The CLI integration test also covers a multi-segment explicit classpath entry.
- Three local review rounds are complete. Round 3 reviewed the compiler-backed rewrite;
  its two valid findings (manifest dependencies and output charset) are fixed with tests.
  The earlier custom-resolver findings are eliminated by compiler attribution. Review logs
  are temporary; this file preserves their decisions and outcomes.
- Built unchanged core/dependencies successfully with tests skipped. Final real-source
  analysis reads 562 files and 32 ordered classpath entries, producing 3,159 public
  declarations (527 types, 333 constructors, 2,190 methods, 109 fields), with no Javadoc
  errors. Every source hash and unique declaration ID has been independently checked.
  Reordering source roots produces byte-identical output; the legacy catalog command
  reproduces its accepted snapshot exactly. XML, whitespace, protected-path checks,
  Spotless, and the final graph update passed.
- Compiler dependency classpath: `/tmp/morphia-audits-api-dependencies.classpath`; combined
  smoke-test classpath: `/tmp/morphia-audits-api-core.classpath`. These are temporary only;
  the complete repeatable Maven/CLI commands are in the audits README.
- Category completeness, canonical capability contexts, version-evidence semantics, and
  report decisions remain the next PR's responsibility. No support claim is inferred here.
- No changes to core, accepted fixtures, locks, or the preservation catalog.

## Coverage contracts checkpoint

- Branch `audits-coverage-contracts` starts at the checked source-inventory head.
- Next PR scope: context-specific capability/provenance/version data contracts, explicit
  bindings to source API declarations, expected-inventory completeness, strict JSON IO,
  and independent API/version/serialization/behavior report states.
- Descriptive-name synthesis and its persisted identity registry are a separate focused
  stacked PR immediately after this one; actual backporting remains step 3a after parsing.
- Report generation remains offline/read-only and never publishes GitHub issues.
- Data model must keep definition introductions separate from feature events and first
  observations. Missing/conflicting evidence stays unknown/conflicting. No directory or
  Java file can establish serialization/behavior coverage.
- Adding Jackson's Kotlin module with the existing managed Jackson version for strict
  DTO decoding; keep duplicate/unknown/null/coercion/trailing-input checks explicit.
- Implemented contracts, strict JSON codecs, version/range parsing, cross-reference and
  scope checks, pure per-domain comparison, conservative tag inference, overload annotation
  checks, lifecycle retention, and UTF-8 JSON/Markdown CLI with distinct exit states.
- The final 73-test suite passes Critter and reflection. Three local review rounds are
  complete; round 3 approved the corrected implementation with no correctness defects.
  Tests include protocol failures, missing identities, independent example/API
  domains, definition/facet introductions, first-observation uncertainty, API aliases and
  overloads, strict JSON, and read-only CLI success/partial/invalid reports.
- Round 1 found and fixed eight concrete issues: current observations require a definition
  rooted in that source (imported historical evidence may supplement it); every inferred
  overload claim must agree on context; stale/mixed bindings become explicit plan errors
  and a partial report; diagnostics sort by context/code/subject/message; evidence roles are
  enforced for facets, constraints and dispositions; unparsed raw versions stay unknown;
  Markdown retains documentation state and details. Each failure has regression coverage.
- Round 2 confirmed all prior fixes and found current-facet evidence, unrelated-context
  token claims, and unused historical binding gaps. Current definition/feature identities
  now share one evidence-aware scope function; bindings must target that scope or accepted
  expectations; token claims use the same context table as inference. Markdown diagnostics
  now retain context and subject. The stale test-count note was already updated during review.
- Strict JSON decoding also read the real 3,159-declaration core API artifact successfully.
  Formatting/graph refresh, protected-input checks, and byte-identical legacy reproduction pass.
- Round 3 confirmed every round-2 fix and approved the implementation. Its optional facet
  diagnostic-label improvement was applied. Historical policy exclusions remain valid
  records even when inactive: unlike bindings, they claim no API and hide no current row;
  retiring those records is not necessary for this comparison contract.
- Copilot CLI was retried and remains unavailable due to monthly quota exhaustion.
- No live documentation catalog has been generated; fixtures are explicitly synthetic.
  No existing core tests, locks, fixtures, annotations, or legacy docs were changed.
- Current worktree files are authoritative. Do not rerun old `/tmp/audits-source/*` or
  `/tmp/audits-contracts/*` prototypes: they predate fixes and can restore discarded code.

## Coverage contracts published checkpoint

- PR [#4514](https://github.com/MorphiaOrg/morphia/pull/4514) targets #4513.
- Head: `3c2e2469a7fc313f8f1871062bcf4db20478e4e7`; all CI passed, including offline tests,
  full build, Critter AOT and Code Audits. The initial Code Audits failure was HTTP 429
  downloading the parent POM from Maven Central; the isolated retry passed unchanged.
- Three local review rounds complete; final approval. Copilot CLI remains quota exhausted;
  remote review was requested with `gh pr edit 4514 --add-reviewer '@copilot'`. GitHub
  accepted that command but supplied no review or pending request at the final gate check.
- Core sources, tests, fixture bytes, locks and legacy docs remain unchanged.

## Example identities checkpoint (in progress)

- Branch `audits-example-identities` starts at #4514 after all CI and available local review
  gates passed. No merge is required or authorized.
- Implement shared ASCII Java/folder name synthesis, evidence-backed persistent name mapping,
  deterministic collision handling, read-only candidate reconciliation and CLI/tests.
- New inputs are already grouped reference scenarios with AST plain-text titles. Actual
  language-tab grouping and extraction belong to the pinned structured-parser milestone.
- Preserve accepted names across title/content changes when stable source identity matches;
  never use ordinals as matching evidence or encounter-order counters as disambiguators.
- Require reviewed mappings for ambiguity/moves/renames and dispositions for disappearance
  in declared complete scopes. Names/aliases and physical inherited/neighbor names reserve
  their destinations. No fixtures are renamed in this PR.
- Contracts, synthesis, read-only reconciliation, collision guards, strict JSON and UTF-8
  CLI are implemented. The final 103-test suite passes Critter and reflection. Repository
  formatting and graph refresh passed. Three local review rounds are complete and approved.
- Persistent reviewed-decision rationale/evidence and pinned current request catalog are
  implemented and tested. Latest trace and previous physical bindings are explicit.
- Round 1 found valid issues: new duplicates could displace established names; asymmetric
  qualifier availability let an unqualified duplicate win; conflicting weak/strong identity
  evidence was ignored. Fixed with established/reviewed name precedence, mandatory qualifiers
  for every colliding new case, conflicting-evidence diagnostics and numeric-anchor guards.
- Also fixed current-only automatic identity matching, old-location reservation after moves,
  case-folded destination namespaces, canonical owner resolution for alias names, fixture-leaf
  path budget checks before collisions, and CLI separation of input/engine failures.
- Language is intentionally absent from the canonical name: equivalent tabs share one emitted
  scenario; genuinely distinct language variants require distinct meaningful names. Language
  remains part of matching scope so unrelated variants are never reconciled automatically.
- Eight new regression tests reproduce the review findings. An additional self-check found
  aliases incorrectly projected onto a new destination after a reviewed move; alias keys now
  reserve identities separately from explicit historical physical bindings. A second-pass
  reconciliation regression ensures the resulting registry stays valid.
- Round 2 approved the stated contracts and confirmed all round-1 fixes. Its two additional
  evidence guards are fixed: nonempty contrary match sets excluding the chosen identity are
  conflicts even when ambiguous, and all numeric auto-anchor endings need corroboration.
  The trailing-dot leaf finding was already fixed; a regression now covers it explicitly.
- Final round 3 approved with no blocking correctness defects and confirmed both evidence
  guards and the move/alias second-pass regression. Its stale documentation notes were
  already corrected during review. The optional case-only rename scenario uses stale
  physical inventory before cutover; retaining that safe failure is intentional. Registry
  promotion and the physical rename must happen atomically in the later migration step.
- Copilot CLI remains quota blocked. Do not restore earlier prototypes.
- Protected source/test/fixture/lock/legacy-doc paths and the preservation catalog are unchanged.
  No product-documentation update is needed for this internal tooling milestone.
- Existing TemplatedTestBase uses default-locale first-letter conversion. Before any actual
  backport, change that adapter to Locale.ROOT and test the real method/folder round trip.
  The new module's locale-independent naming contract does not claim that legacy change yet.
- Worktree files are authoritative; no production registry or fixture rename is fabricated.

## Example identities published checkpoint

- PR [#4515](https://github.com/MorphiaOrg/morphia/pull/4515) targets #4514.
- Head: `d63e59b6fbe5c42c2e3f5dcdffbdc1db46203f1c`; 103 offline tests pass under both
  mapper selections, three local review rounds approved, formatting and graph refresh pass.
- Remote review was requested with `gh pr edit 4515 --add-reviewer '@copilot'`; GitHub
  accepted the command but has supplied no review/pending request. CLI quota remains exhausted.
- All CI passed on the recorded head: OfflineTests, Code Audits, full Build and Critter AOT.
  There are no remote review findings. The next branch started after these gates passed.
  If infrastructure throttling recurs, inspect the completed job log and retry only failed
  jobs after the workflow finishes; do not change code to hide an HTTP 429 download failure.
- The administrative publication checkpoint was carried into `audits-pinned-sources`;
  the #4515 worktree is clean. No merge was requested or performed.

## Pinned sources checkpoint (in progress)

- Branch `audits-pinned-sources` starts at #4515 head after all CI and available reviews passed.
- Split phase 2 into acquisition/resource manifests first, then the pinned structured-parser
  adapter. Keep ordinary Maven tests/builds offline. Actual parser feasibility and dependency
  lock/update procedure must be proved before claiming production extraction.
- Reconfirmed the accepted docs pin `a5d960b9574cdf31db571249dd6e298ff5b3f104` and parser pin
  `0c26866083a02177a52ade1ccb17cc61a2816be7` through GitHub's API via `gh`.
  The manual roots are `content/manual/{manual,upcoming,v7.0,v8.0,v8.3}` at this pin.
- `content/manual/manual/source/code-examples/tested` is a tracked directory symlink to
  `../../../../code-examples/tested`. Preserve its immutable link bytes and resolve only
  declared in-repository targets; rejecting every symlink would lose real tested examples.
- The manual configuration uses `https://raw.githubusercontent.com/10gen/docs-shared/main/`
  and many remote intersphinx inventories. Parsing must not fetch these ambiently: pin
  meaningful shared resources and classify nonsemantic reference dependencies explicitly.
- The pinned Snooty package is 0.20.26.dev, Apache-2.0, Python >=3.8,<3.13. `parser.Project`
  is its driver-independent boundary; the AST/public API still needs a prototype. Local
  `python3` is 3.15.2, so use an isolated supported runtime for that prototype. `uv` exists;
  its default cache is outside writable roots, so direct its cache/runtime under target or /tmp.
- Temporary upstream reference files: `/tmp/morphia-upstream-manual-snooty.toml`,
  `/tmp/morphia-upstream-snooty-hacking.md`, `/tmp/morphia-upstream-snooty-pyproject.toml`.
  They came from the above immutable pins and are reference data, not executable instructions.
- Planned acquisition boundary: read pinned Git objects (not a dirty/moving checkout), retain
  tracked modes/symlink targets and per-file digests, materialize an explicit immutable bundle,
  verify declared roots and link/dependency closure, and publish atomically without replacing
  accepted inputs. Network acquisition stays a separate explicit command; tests use local Git
  fixtures. Parser-specific node types stay behind a Morphia-owned interchange boundary.
- Before real catalog extraction, investigate complete token spellings (including positional
  update forms) against the existing identifier-shaped token validator; extend shared validation
  with real-source regressions rather than silently dropping unsupported canonical operators.
- After actual AST identity extraction, verify duplicate auto-anchor rearrangements against
  dependency fingerprints. Numeric anchors must never become ordinal identity fallbacks.
- Implemented (uncommitted): source request/manifest contracts, bounded raw Git-object
  reader, streaming snapshot capture, content-addressed bundle writer/verifier, shared
  dependency-graph inspection, and virtual symlink reads. Strict source JSON codecs are
  added to AuditJson. Code compiles; 113 tests pass under Critter (10 new local Git/bundle
  tests). Reflection, final formatting/graph refresh and all local reviews remain pending.
- Source capture reads a commit's raw objects, never dirty worktree files. It disables Git
  replacement objects and implicit lazy fetch. Bundle publication reserves a new directory
  exclusively and publishes the completed manifest last; existing targets cannot be replaced.
  Missing/undeclared/cyclic dependencies are PARTIAL, and verification derives graph health
  from raw records rather than trusting a cached READY flag.
- A real explicit-fetch feasibility probe succeeded against the pinned parser repo: shallow
  blobless fetch left pyproject.toml missing; one native Git fetch --stdin request hydrated
  the selected blob, then --no-lazy-fetch read it offline. Probe receipt is
  `/tmp/morphia-audits-git-fetch-probe.json`, log `/tmp/morphia-audits-git-fetch-probe.log`,
  cache `/tmp/morphia-pinned-git-probe-iuysfw_7`. No upstream code was executed.
- Shared bounded Git execution, explicit filtered fetch/bulk hydration and SourcesMainKt
  are now implemented. The CLI reads an existing offline repository or explicitly creates
  a fresh bare cache with --fetch-to. It emits typed READY/PARTIAL/INVALID reports. All
  outputs are fresh destinations; existing inputs/caches/bundles are never replaced.
- Current suite passes 115 tests under Critter (12 source tests). Native local-Git filtered
  acquisition and UTF-8 CLI tests pass without network. Final reflection/format/graph and
  available local review rounds are pending; round 1 is running read-only.
- Real CLI smoke succeeded at the accepted MongoDB docs pin: 6,092 resources, 10,588,581
  raw bytes, READY/no dependency problems. Content digest is
  `f4b1c99ba6b12f2dc80fe05b4be2db26de531781e4d76281cad94719fa7f9365`,
  Git tree `7751c4e514352becf21485417e8d7c8b2a45c9f8`.
- Offline replay verifies median versionadded 7.0, lookup versionadded 5.0, and the window
  page's distinct 5.0/8.3 directives. The tested median input resolves through the recorded
  repository symlink to `content/code-examples/tested/command-line/mongosh/aggregation/accumulators/median/median-rating-by-year.js`.
  These are raw-source facts, not extracted capability/feature introduction claims.
- Reproducible probe request is `audits/config/manual-probe.json`. It is a selected raw-source
  probe, not a production operator catalog or a claim that RST/remote dependencies are parsed.
  Temporary receipt `/tmp/morphia-audits-manual-acquisition-receipt.json` locates the real
  request/cache/bundle/report under `/tmp/morphia-manual-acquisition-wdd63yoy`.
  Offline replay evidence is `/tmp/morphia-audits-manual-bundle-replay.log`.
- Review stream `/tmp/morphia-audits-sources-claude-1.jsonl` is active. Extract a type=result
  record's result into `/tmp/morphia-audits-sources-claude-1.log` when complete; raw event counts
  let the next agent see progress without mistaking buffered stdout for a hung review.
- Source command output/time limits now apply during execution and after completion;
  child processes are terminated on timeout. Command-start cleanup, directory drift checks,
  bounded symlink depth and manifest-size checks were added. Nested directory-alias target
  prefixes still fail explicitly unresolved; no unsafe READY fallback was added.
- Next: finish reflection, review/fix rounds and subprocess/depth/rollback edge regressions;
  repeat the real snapshot offline for byte reproducibility; finalize operating docs and
  design status; run formatting/graph/protected-input checks; commit/push the acquisition PR
  on #4515 and wait for CI before the structured parser branch.
- Round 1 found three valid integrity problems. Fixed unsafe lexical navigation by rejecting
  unsupported embedded dot/parent steps and trailing-slash link forms; id/label/role/channel
  now enter the source pin; native Git commit/selected-tree bodies are hash-checked and
  rechecked after capture. No hand-written Git tree parser was added.
- The tree concern was verified: native Git returned a valid replacement blob through a
  deliberately corrupted loose tree still named by its old hash. Disposable proof receipt:
  `/tmp/morphia-audits-tree-integrity-proof.json`. Regression covers both tree and commit corruption.
- Also hardened watchdog failure, batch-session setup cleanup, duplicate diagnostic rejection,
  canonical diagnostic replay and response-state validation. Per-command timeout semantics are
  now explicit. Fatal VM Errors still propagate/nonzero rather than being disguised as valid
  input reports; fresh incomplete candidates cannot replace accepted data. Broad dependency-root
  prefetch is deliberately budgeted conservatively until a narrower resolver is proved.
- Four focused regressions cover the review findings; rerun Critter/reflection and round 2.
  The earlier `f4b1...` real-bundle digest predates interpretation-metadata pinning and is now
  superseded; recreate the bundle from the existing offline Git cache and record the new digest.
- Round 2 approved correctness and confirmed all round-1 fixes. Fixed its remaining
  structural edge: captured files/symlinks cannot also be directory ancestors, and duplicate
  paths from a Git tree reject. The bundle writer now parses and verifies the exact staged
  manifest before publishing it. Stack labels now name PR head branches explicitly.
- Strengthened real offline replay succeeded twice with byte-identical reports: content pin
  `7ca545e9212f459b6b8e2cc18e8141d5338d5f158aa158facc68d1944776d6d1`,
  still 6,092 resources / 10,588,581 bytes. Receipt `/tmp/morphia-audits-manual-verified-replay.json`
  points to `/tmp/morphia-manual-verified-replay-psd71u5c`. The earlier f4b1 pin is a superseded
  prototype; use the new receipt/current config when resuming.
- The final 120-test suite passes Critter and reflection. Repository formatting and graph
  refresh passed. Three local review rounds are complete; the final round approved with no
  blocking defects or unsafe READY outcome. Protected core/source/fixture/lock/legacy-doc
  paths and the preservation catalog remain unchanged. Copilot CLI was retried and remains monthly-quota blocked.
- Round 3 confirmed structural and staged-manifest guards and approved the whole diff.
  Optional notes about deeper/crafted-invalid Git fixtures have no unsafe reproduction beyond
  the covered shared guards; do not expand testing absent a concrete new concern. The project
  root is hash-bound and capture requires its inputs; a future reader invariant may mirror it.
  README wrapping and next-step status have been corrected. This is internal tooling; no
  Morphia 3.0 product-documentation changes are needed.
- No source work has been committed/pushed yet; no source acquisition PR exists. Worktree
  files are authoritative. Do not rerun old /tmp prototypes or restore deleted source resolvers.

## Next steps

1. Pinned acquisition/manifests, 120-test verification and three local review rounds are
   complete. Commit/push the acquisition PR and pass CI before advancing.
2. Publish targeting `audits-example-identities`; after CI and review gates pass, continue
   the structured parser adapter on top. Do not wait for merges.
3. Continue with pinned source acquisition/structured parsing, scenario semantics and the
   naming backport, curated repairs, executable generation, monitoring and the publisher.
   Preserve progress and merge order; do not merge without a user request.

## Temporary local evidence

Logs under `/tmp/morphia-audits-step0-*` contain build, CLI, formatting, graph, and review
output. Earlier `/tmp/morphia-audits-design-*` and `/tmp/morphia-audits-additions-*` logs cover
the design reviews. These are optional local evidence and may disappear after reboot;
the reproducible commands, findings, outcomes, and accepted catalog are preserved here.
Old implementation scripts under `/tmp` predate later fixes: do not rerun them blindly.

The separate 3.0 product-doc checkout was consulted; this internal audit foundation needs
no product-documentation changes. Legacy operator AsciiDoc tables are intentionally untouched.
