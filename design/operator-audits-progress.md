# Operator audit implementation progress

Current branch: `audits-curated-repairs`; PR base: `audits-descriptive-names` (#4530).
Stack (PR head branches): #4512 `audits-pipeline-design` → #4513 `audits-source-inventory` →
#4514 `audits-coverage-contracts` → #4515 `audits-example-identities` → #4516 `audits-pinned-sources` →
#4517 `audits-snooty-adapter` → #4518 `audits-capability-normalization` →
#4519 `audits-bson-comparison` → #4520 `audits-shell-semantics` → #4521 `audits-scenario-programs` →
#4522 `audits-scenario-extraction` → #4523 `audits-published-expectations` →
#4524 `audits-legacy-scenarios` → #4525 `audits-section-assembly` → #4529 `audits-binding-manifest` →
#4530 `audits-descriptive-names` → curated repair inventory (in progress).
Each PR targets its predecessor; #4512 targets `master`.
Use `git worktree list` to locate the checkout on this machine.

## Current status at a glance

- Published infrastructure: #4512 foundation, #4513 API inventory, #4514 coverage contracts,
  #4515 persisted names, #4516 pinned acquisition, #4517 structured docs and #4518 capability
  discovery. Discovery is deliberately PARTIAL where upstream evidence is unresolved.
- #4519 strict BSON comparison is published and all five exact-head checks pass at
  `27a3fa7c1db2d22a6b0fc6e1c3d0f53f0e45f257`; local review is complete.
- This branch adds shell BSON literals: 215 audit tests pass in both requested configurations,
  including 27 literal tests. Two existing Python integrations skip without explicit properties.
  Three local review rounds are complete; the final round found no remaining correctness issues.
  Check publication/CI with `gh pr view audits-shell-semantics`; its base is #4519.
- Still required: complete ordered scenarios and result streams, independently reviewed current
  coverage expectations, actual descriptive-name backport, curated repair/lock migration, typed
  executable generation, monitoring, enabled gh issue publication and obsolete-path cleanup.
  No core fixture/test naming migration or lock retirement has occurred. No merges are authorized.

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
- Acquisition is committed/pushed in #4516; worktree source files and its recorded head are
  authoritative. Do not rerun old /tmp prototypes or restore deleted source resolvers.

## Pinned acquisition published checkpoint

- PR [#4516](https://github.com/MorphiaOrg/morphia/pull/4516) targets #4515.
- Head: `42ac900713020d0f904aba5d8e804747f7614b0b`; final 120-test verification under both
  mappers, three local review rounds, formatting, graph update, protected-input checks and
  real byte-identical offline replay are complete.
- Copilot CLI remains quota-blocked. Remote review was requested with
  `gh pr edit 4516 --add-reviewer '@copilot'`; GitHub accepted it with no review/pending request.
- All CI passed on the recorded head: OfflineTests, Code Audits, full Build and Critter AOT.
  No delayed review findings were present. The next branch started after these gates passed.
  Do not wait for merge; do not merge without authorization.
- The post-publication status was carried into `audits-snooty-adapter`; the #4516 worktree
  is clean. No merge was requested or performed.

## Snooty adapter checkpoint (in progress)

- Branch `audits-snooty-adapter` starts at #4516 head after all gates passed. Phase 2b must
  prove a pinned parser boundary and a Morphia-owned interchange with source/dependency
  provenance, explicit diagnostics and no ambient network. Production capability extraction
  may need its own focused stacked PR after the adapter is proved.
- Installed isolated CPython 3.12.16 at `/tmp/morphia-audits-python` with uv, using cache
  `/tmp/morphia-audits-uv-cache`. Host Python versions are too new for the pinned package.
- Prototype venv `/tmp/morphia-snooty-prototype-venv` installs Snooty at immutable commit
  `0c26866083a02177a52ade1ccb17cc61a2816be7`. Installation log `/tmp/morphia-snooty-install.log`,
  initial dependency freeze `/tmp/morphia-snooty-prototype-freeze.txt` (must regenerate/lock
  after resolving missing dependencies). No global Python environment was changed.
- First import found the undeclared `typing_extensions` dependency; the committed project
  now pins it explicitly. `audits/parser/uv.lock` freezes the parser Git commit and transitive
  package versions/hashes. CPython is pinned at 3.12.16; actual installed distributions are
  checked against the lock and included in output provenance.
- The prototype proved both raw and expanded ASTs: 3 representative real pages, include roots
  (including Giza YAML), 2/20/20 external dependencies for median/lookup/setWindowFields,
  language variants and separate root/feature version events. The broad selected snapshot
  produces thousands of unrelated missing-reference diagnostics; preserve these as PARTIAL,
  then classify health by evidence/domain in the normalization milestone.
- `audits/parser/adapter.py` and Kotlin `documents/` now implement the boundary. The coordinator
  verifies a SourceBundle, exports exact bytes, invokes an absolute isolated runtime with
  cleaned environment and bounded deadline/output, checks response scope and provenance,
  and returns candidates only. Ordinary JVM tests/builds do not install Python or parse live docs.
- The owned tree keeps plain title/code text, directives/arguments, identifiers, language/tabs,
  options and canonical JSON extra attributes, raw and expanded trees, nested include chains,
  raw resource hashes and parser dependency hashes. Generated positions stay unknown; RST
  line numbers are explicitly parser-reported hints, not external-snippet spans.
- Helper guards block network, processes, source writes and reads outside private source and
  controlled runtime roots. This constrains the trusted pinned parser, not arbitrary Python.
  Upstream always creates a process pool, even with one worker; use one worker thread instead.
  Ambient `/etc/mime.types` caused a real blocked read; fixed by initializing the runtime MIME
  table without host files. Runtime setup and coordinated upgrades are documented in README.
- The real coordinator run succeeds offline with raw and expanded trees for all three pages,
  no PARSER_FAILED diagnostic, and explicit PARTIAL results for missing resources/inventories.
  `/tmp/morphia-adapter-real-report.json` is exploratory evidence and must be regenerated after
  adapter changes. The source pin remains `7ca545e9212f459b6b8e2cc18e8141d5338d5f158aa158facc68d1944776d6d1`.
- Current verification: 127 JVM tests pass with Critter and reflection; 11 actual pinned-parser
  fixture tests pass. Formatting is applied, graph refreshed, protected inputs unchanged.
  Real replay after the final fixes passed twice with byte-identical JSON (7,429,246 bytes;
  SHA-256 `4eedd7eb4edd6a0bae785b5edaed02ce9da06653b4a27820770776a5abc72355`).
  All three pages have complete captured example-file dependencies; 2,765 deduplicated
  diagnostics and 32 blocked URLs keep overall source health explicitly PARTIAL. The executable
  `audits/parser/verify_manual_probe.py` preserves acceptance checks for the immutable real
  median/lookup/window probe: definitions, root/feature version events, raw/expanded trees,
  dependency closure and code provenance. Expected code block counts are 3/34/21.
- Review round 1 found valid issues in static-asset digest size (Snooty uses 32-byte and 64-byte
  Blake2), missing include-dependency records defaulting to empty, page/conversion failure
  isolation, ambiguous diagnostic path bases, environment version completeness, link ordering/
  unhealthy-link traversal, and missing coordinator origin checks. All are fixed. Source
  diagnostics are validated at the input boundary; malformed transport is deliberately INVALID
  rather than a PARTIAL parse. Per-page adapter compatibility failures retain explicit diagnostics.
- Runtime checks now compare the marker-selected lock dependency graph exactly (including
  duplicate/missing distribution detection) and installed RECORD hashes. These detect local
  drift; archive hash verification belongs to frozen installation, not an independent toolchain
  attestation. Controlled runtime + parser-hook constraints remain the documented threat model.
- Link materialization reaches a fixpoint; links in source-error chains are suppressed rather
  than traversed. Bundle-relative requested diagnostic/include paths and canonical resource
  origins are distinct from explicit project-source-relative page/dependency file ids.
  The coordinator rejects unknown/mismatched node/dependency origin hashes. Unicode page order
  uses code-point-compatible UTF-8 comparison on both sides. Guard tests directly exercise file,
  socket and process denial; directory listings/chdir/dir_fd open were also constrained.
- Remaining review notes are deliberate limits: report-wide conservative PARTIAL until the
  planned domain-health normalizer, explicit whole-snapshot transport cap, and polled native
  process budgets like the existing Git boundary. Source text/attribute values are intentionally
  preserved exactly rather than blindly sanitizing literal text; the private root is canonicalized
  before use to avoid temp-path aliases. Oversized candidates fail closed and cannot replace data.
- CI installs the frozen runtime explicitly in a separate ParserTests job; the offline JVM job
  remains independent. This is internal tooling; no product-doc edits needed. Copilot was retried
  and remains monthly-quota blocked. Round 2 is running at `/tmp/morphia-adapter-claude-2.jsonl`;
  prior concerns are recorded above and `/tmp/morphia-adapter-claude-1.log`.
- Round 2 confirmed every earlier fix, then found that suppressing every link in an error
  chain also suppressed healthy ancestor directories. Fixed to suppress only the offending path.
  The new real-parser regression also exposed upstream's property-root traversal jail: declared
  shared RST outside the property was skipped. Enumeration now uses the verified private bundle
  as its jail while preserving upstream cycle/nested-project traversal. Healthy sibling content
  survives a bad descendant link, and the report stays explicitly PARTIAL.
- Fixed the low-priority posix_spawn and dir_fd mutation guard gaps; source-relative diagnostic
  ids now map deterministically to normalized bundle paths (project config special-cased).
  Kotlin validates diagnostic path shapes, requires captured sources for parser-reported origins,
  and requires nonnull source/digest evidence for complete dependencies. Added relevant fixtures.
- Rejected duplicating Blake2 inside Kotlin: the coordinator independently binds SHA-256 raw
  resource origins; Python checks upstream Blake2 variants against those exact bytes. Upstream
  digests remain parser metadata. A duplicate crypto implementation/library would add cost
  without improving the stated independent raw-origin check; malformed/missing digests reject.
- Current tests: 127 JVM cases pass Critter after these changes; reflection is rerunning.
  All 13 actual-parser tests pass. Round 3 (final allowed local round) is running in
  `/tmp/morphia-adapter-claude-3.jsonl`. The final real replay is being repeated after these
  path/traversal changes; the earlier report digest above is superseded until updated below.
- All three allowed local review rounds are complete. Round 3 confirmed prior fixes and no
  unsafe READY, and found one final valid cross-boundary defect: a failed literalinclude could
  identify the suppressed symlink itself as a raw resource, causing Kotlin to discard an otherwise
  useful PARTIAL report. Fixed by giving unavailable/symlink-only origins explicit null path/hash
  and unknown precision. Python regression now includes the bad literalinclude and healthy sibling.
- Added an actual Kotlin → locked Python → Kotlin integration regression using a locally created
  Git source bundle with a healthy shared directory link and bad descendant link. It verifies
  the healthy explanation survives, failed dependencies have unknown origins, and PARTIAL returns
  normally. No optional runtime is installed by ordinary Maven; this case skips unless explicit
  runtime/adapter properties are given. ParserTests CI supplies them and runs both mapper settings.
- Final targeted checks in progress: 128 JVM cases including integration under both mappers,
  13 parser fixtures, immutable real replay twice, formatting/graph/protected-input checks.
  All review findings were evaluated and fixed or recorded as intentional above; no fourth
  local review round is needed under the three-round maximum. Copilot remains unavailable.
- No implementation commit or PR exists yet. After final checks, commit/push a PR targeting
  `audits-pinned-sources`. Do not start the next PR until this one passes CI and available
  reviews; do not wait for merge or merge without authorization.


## Structured parser final verification

- All 128 JVM tests pass under Critter and reflection with the explicit locked runtime,
  including the real Git-bundle → Kotlin → Python → Kotlin blocked-link integration.
  All 13 real-parser fixtures pass. Without explicit runtime properties only the integration
  case skips; ordinary offline JVM tests still need no Python installation.
- The final manual replay passed twice with byte-identical JSON: 7,429,246 bytes, SHA-256
  `78c72e7709dc085a042f74e8bc5cfcd88b6a7151e082d31f324e180b2eea8684`.
  This supersedes intermediate adapter-report hashes above (source bundle pin unchanged).
  The executable probe verifies 3/34/21 code blocks and 2/20/20 external files, definition
  identifiers and distinct root/feature version evidence. Overall PARTIAL remains correct:
  2,765 deduplicated diagnostics, 32 blocked URLs, all requested example dependencies captured.
- Three local review rounds completed; all concrete findings fixed and verified, deliberate
  limits and the rejected duplicate-crypto suggestion documented above. Final review's only
  blocker was the unresolved symlink origin, now covered by both parser and full coordinator
  integration. Copilot remains monthly-quota unavailable; request remote review after publication.
- Repository formatting, AST graph refresh, whitespace checks and protected core/annotations/
  util/legacy-doc/corpus-catalog comparisons pass. No accepted data, tests, names or locks moved.
- Ready to commit/push `audits-snooty-adapter` targeting `audits-pinned-sources`, then gate
  the next branch on CI/reviews. Follow with production capability/scenario normalization and
  domain health, then descriptive test migration, curated repairs, typed generation and monitoring/
  opt-in issue publication. Do not merge without authorization.


## Structured parser published checkpoint

- PR [#4517](https://github.com/MorphiaOrg/morphia/pull/4517) targets #4516
  (`audits-pinned-sources`). Head `8748bc6969de3176997de4a1a71b0f3e92f1c794`.
- Final 128-test JVM verification (both mappers, including actual parser integration), 13 parser
  regressions, three local review rounds with all findings addressed, source preservation,
  formatting/graph refresh and byte-identical real replay are complete. Source files at this
  committed head are authoritative; do not restore old /tmp adapter prototypes.
- Remote review requested using `gh pr edit 4517 --add-reviewer '@copilot'`; GitHub accepted
  it but again lists no pending request/review. Local CLI remains monthly-quota exhausted.
- All exact-head CI passed: OfflineTests 1m41s, ParserTests 1m56s, Code Audits 2m45s,
  full Build 5m46s and Critter AOT 6m03s. No delayed remote findings were present.
  The normalization branch started after these gates passed.
- This status update was carried into `audits-capability-normalization`; the published parser
  worktree is clean. No merge was authorized or performed.

## Capability normalization checkpoint (in progress)

- Branch/worktree `audits-capability-normalization` starts at #4517 head after all gates passed.
  No normalization production code exists yet. Build evidence-backed capabilities and version
  events from owned document trees, with explicit source/page/context health and cross-checks
  against documentation catalogs. Keep example extraction health independent of definitions.
- The verified source bundle already contains 310 `.txt` operator pages (not just the three
  retained probe outputs), plus includes and tested examples. Current probe verifies three
  representative pages. Measure full extraction size/runtime and inspect actual definition/catalog
  shapes before choosing batch/normalization boundaries; do not guess context from filenames.
- Contracts in `coverage/Contracts.kt` and `naming/NameContracts.kt` are authoritative. Evidence
  spans need raw-source checks: `DocumentOrigin.reportedLine` is only a parser hint, particularly
  for substitutions and generated content. Preserve unknown introduction/version/facet scope.
- Full operator probe succeeded through the existing coordinator: 310 retained pages, PARTIAL,
  2,765 diagnostics, 190,978,095 bytes of pretty JSON, 68.87 seconds. Native helper compact output
  fits the explicit 128 MiB budget; pretty artifacts are larger. `/tmp/morphia-audits-all-operator-documents.json`
  uses the original source pin. Temp launcher `/tmp/FullDocumentsProbe.java` is exploratory only.
- The original probe omitted MQL catalogs. Explicitly fetched a new immutable bundle including
  `reference/mql`: receipt `/tmp/morphia-normalization-source-receipt.json`, root
  `/tmp/morphia-normalization-source-4jepdujh`, 6,132 resources, content pin
  `9632a61a4f77b83161d81b7da8fc63e11ab94112d7b73c3bcf49da6ced5e165a`.
  MQL-only parsing retained 40 pages, 17,188,869-byte pretty JSON, 37.38s, PARTIAL/2,510 diagnostics.
  Artifact `/tmp/morphia-audits-mql-documents.json`; its source pin differs from the operator probe,
  so do not pretend those two reports are one coherent snapshot. Reparse operators or all 350
  pages under the new source pin for final combined validation.
- Actual AST tables are `directive/list-table` → `list` → row `listItem` → `list` → cell `listItem`.
  First-column references establish membership; description-column operator mentions do not.
  Catalog headers include Name/Stage; Command tables are outside the eight modeled contexts.
  Update's Array Update Modifiers section distinguishes four modifiers from other update operators.
- Definition roles in the old operator scope: expression157, group37, pipeline46, query39,
  update22, projection3 (plus115 data fields). Do not equate every group role with accumulator:
  12 are window-only legacy definitions. MQL introduces explicit `window` definitions for24
  common functions and a 25-row accumulator catalog. Supplemental setWindowFields operator
  catalogs and MQL query subcatalogs need inspection, not guessed category inference.
- MQL expression catalog references some absent `exp.*` targets whose only definition is `grp.*`
  (avg, median, concatArrays, etc.). Context membership can prove availability even if an upstream
  cross-reference is broken. Never borrow an accumulator introduction for an expression/window
  variant: preserve unscoped/uncertain notices with UNKNOWN confidence until evidence assigns them.
  Atlas search/searchMeta/vectorSearch catalog rows also lack local definitions in the selected
  bundle; missing metadata/examples must not become an invented absent operator.
- Generated YAML catalog nodes have unknown positions. Use honest raw-file evidence and include
  provenance, or verify an exact raw-source span; never invent a YAML/RST line from a hint.
  Diagnostics in the old full probe: TargetNotFound1637, CannotOpenFile1086, FetchError29,
  UnnamedPage13. Classify cross-reference/config inventory failures separately; scope missing
  input/output/literalinclude problems to examples when the owning directive proves that scope.
- Shared token validators currently accept only `$identifier`; actual positional `$`, `$[]`,
  `$[<identifier>]` need explicit supported identity semantics and regression cases, not omission.
- Reuse the locked parser interpreter from the parser worktree when useful; point adapter at this
  worktree. Source bundle receipt and final real report paths are recorded above. Keep ordinary
  Maven offline and protect curated core/source/docs/lock/data files until their explicit migration.


## Discovery implementation checkpoint (in progress)

- New production code: `discovery/DiscoveryContracts.kt`, `DocumentFacts.kt`,
  `SourceEvidenceIndex.kt`, `CatalogDiscovery.kt`, `LifecycleFacts.kt`,
  `DiscoveryDependencies.kt`, `DiscoveryMain.kt`; strict AuditJson discovery codecs;
  shared `coverage/OperatorTokens.kt` now also validates naming scopes. Positional `$`, `$[]`,
  `$[<identifier>]` are canonical identities; concrete `$[element]` is syntax, not a new operator.
- `audits/config/manual-discovery-probe.json` selects MQL + operators at the same immutable pin;
  `manual-discovery-plan.json` explicitly assigns all eight contexts, AST section/header selectors,
  role fallbacks, and independent lower bounds. Both are probes, not accepted production catalogs.
- Coherent 350-page artifact `/tmp/morphia-audits-discovery-documents.json` uses the new 9632... pin:
  207,511,569 pretty-JSON bytes, 71.38s, 2,510 diagnostics, PARTIAL. Native compact transport fits
  128 MiB. Read with a JVM heap such as `-Xmx3g`; ordinary fixture tests stay small/offline.
- Initial normalizer output contained 324 capability/context keys. Real window-stage catalog is
  a labeled operator-reference list (not one bullet per operator); added a narrow explicit list
  adapter that accepts leading operator names or colon-labeled reference groups, never arbitrary
  prose references. This should add the legacy window-only functions on the next full replay.
- Definition roles alone never assign execution context. First-column catalog membership does;
  expression aliases can use group definitions but their introductions remain UNKNOWN unless
  independent context-specific source evidence establishes them. Root/feature versions stay separate;
  generated positions remain unknown; actual raw spans/hashes or honest whole-resource evidence
  are retained. Raw source directive arguments, including substitutions, are preserved where provable.
- First local review is complete: `/tmp/morphia-discovery-claude-1.log`; detailed notes also at
  `/home/evanchooly/.claude/plans/read-only-review-of-the-staged-tiger.md`. Seven valid concerns:
  persisted origin checks, unverified catalog evidence, fallback roles hiding native-context gaps,
  root/feature ownership, nested list versions, lost include diagnostics/unknown dependency kinds,
  and inconsistent primary-role policies. Fixes are in progress, with regression coverage.
- Applied fixes: reuse internal DocumentParsing origin validator; verify catalog names against raw
  references/first-cell text; track native primary contexts independently of fallback use; limit
  confirmed roots to owned definition body/section; preserve nested features and skip nested-list
  catalog introductions; validate per-context primary policies before scanning. Source/hash mismatch
  deliberately remains INVALID, rather than accepting a partially trusted artifact. A unique verified
  raw YAML line is legitimate evidence; it is not an invented generated RST span.
- `DiscoveryDependencies` retains include attempts even when they yield no nodes, scopes known
  failed literal files separately, retains all outside-scope diagnostics, and treats unexplained
  incomplete dependency graphs as definition uncertainty. Do not classify every incomplete graph
  as merely an example error. Canonical policy + document input hashes bind discovery provenance.
- Last complete suite before final review fixes: 138 cases/1 optional parser integration skip.
  Current tests add forged membership, stale origins, failed includes, dependency uncertainty,
  positional rows, nested Definition headings/page notices and nested-list lifecycle regressions.
  Current build log `/tmp/morphia-discovery-tests.log`; validate it before relying on a test count.
- No discovery commit/PR exists. Remaining: finish review fixes, add missing focused regressions,
  full real replay + catalog contract/coverage validation, docs/design status, both mapper suites
  with explicit parser integration, formatting/graph/protected checks, at most two more local review
  rounds (Copilot CLI quota still unavailable), then commit/push targeting `audits-snooty-adapter`.
  Do not start the following branch until its CI/reviews pass; no merge authorization exists.


## Discovery verification update

- Current suite: 153 tests pass under Critter with both optional locked-parser integrations
  enabled (25 discovery regressions). The previous 151-test checkpoint passed reflection;
  rerun reflection for the final current suite after review fixes. ParserTests CI now selects
  both DocumentParsingTest and CatalogDiscoveryTest under both mapper settings.
- Real direct `DiscoveryMainKt --parse` completed successfully (PARTIAL, exit3), retaining336
  capability/context observations and915 raw evidence records. Current expected counts:
  predicates37, projections4, update operators18, modifiers4, stages47, expressions165,
  accumulators25, window functions36. The supplemental window list recovered the12 legacy
  window-only group declarations. Native-context ambiguity remains explicit rather than guessed.
- Current 350-page artifact normalization repeated byte-identically: 1,645,229-byte report,
  SHA-256 `99037863374a5974abe2b5e5555aed94006e2c00bcb5ef5b8c9098a68d71d7b7`.
  Receipt `/tmp/morphia-discovery-replay-receipt.json`; policy pin
  `1d9ae9c826ea059de850b542d4e86411d751eeff12d761e46522b811d4edf118`.
  These supersede earlier unreviewed normalization outputs. Keep source/document inputs.
- Real catalog handed off to the existing CoverageMain and compiler API inventory (3,159 public
  declarations): PARTIAL/368 root+facet rows, not INVALID. The temporary schema-smoke plan was
  deliberately self-derived only to validate transport/contracts; it is NOT an accepted absence
  baseline and must never be committed as one. Permanent regression now verifies catalog→API
  inventory→coverage contracts using a small explicit fixture policy.
- Stored report origins are rechecked before evidence use. Raw role/name references support
  catalog members; fabricated names cannot produce observations. Version source arguments are
  read back independently, retaining unresolved substitutions instead of upgrading them to dates.
  Page notices, nested Definition headings and nested operator lists cannot introduce a root.
- Known doc-link CannotOpenFile failures and sharedinclude failures explicitly owned by Example(s)
  require AST ownership plus raw source syntax before being classified as reference/example errors.
  Unknown dependency graphs and unclassified includes remain definition uncertainty. All unused
  diagnostics remain OUTSIDE_SCOPE, not silently discarded. No production source facts were massaged.
- Canonical policy and parse-input pins are now present in both the report and catalog tool trace.
  Report decoding checks state/context/catalog/problem consistency. The direct CLI validates policy
  before invoking the native parser; it needs no temporary Java launcher.
- Round2 is running: `/tmp/morphia-discovery-claude-2.jsonl`. Copilot was retried for this unit and
  remains monthly-quota blocked (`/tmp/morphia-discovery-copilot-1.log`). Max one further local
  round after this. README/design status updated; graph refresh performed, final refresh pending.
- No discovery commit/PR yet. Finish round2 findings, final tests/replay/format/graph/preservation
  checks, final available review if needed, then publish targeting #4517. Do not merge the stack.

## Discovery round 2 outcomes (2026-11-16)

- Round 2 completed (`/tmp/morphia-discovery-claude-2.log`; notes in
  `/home/evanchooly/.claude/plans/round-2-read-only-review-twinkling-river.md`). It confirmed every
  round-1 fix and raised six main and four lower-priority findings. All were evaluated:
  1. Multi-line `:doc:` links: fixed. The raw check reads the whole paragraph from the reported line.
  2. Example sharedinclude ownership: fixed. Owners are filtered per including page.
  3. Stored parse-input pin: fixed. `normalize` recomputes the export hash and rejects a mismatch.
  4. Hint-miss evidence: fixed. A parser-reported hint is satisfied only by that raw line or a line
     inside the same raw paragraph. The first strict version (exact line only) made 26 real window
     catalog members unverified because Snooty reports labeled-list roles at the paragraph start;
     the paragraph span restores them without accepting matches elsewhere in the file.
  5. Deep-target root notices: fixed. A notice in the target's own body is a root event at any depth.
  6. Undocumented exit on VM errors: intentionally not caught, consistent with the acquisition
     boundary. INVALID (2) means invalid input; out-of-memory/stack overflow exit nonzero with the
     JVM's diagnostic. README now states this. READY exit 0 now has a CLI test.
  7. Feature facets: fixed. Heading evidence requires a raw section underline at the reported
     line (Snooty reports headings at the underline; indented tab headings included).
  8. Multi-operator pages: fixed. Section notices outside every target stay UNKNOWN when the page
     defines more than one operator (real effect: one `$regex` 6.1 feature notice).
  9. Report decoding: fixed for normalizer identity, evidence references and page capability keys.
     Canonical list ordering is not re-verified on decode; output ordering is deterministic.
  10. Plan selectors: fixed. Repeated selectors/roles and select+exclude overlaps are rejected.
- Ten new regressions (35 discovery cases). The test fixture now reports headings at their
  underline, matching the real parser.

## Discovery final verification (2026-11-16)

- 163 tests pass under Critter and reflection with both optional locked-parser integrations;
  without parser properties only those 2 skip. All 13 actual-parser fixture tests pass.
  Logs: `/tmp/morphia-discovery-final-{critter,reflection,noparser}.log`.
- Real replay of the coherent 350-page artifact (source pin `9632...`, plan pin `1d9ae9c8...`)
  repeated byte-identically: 1,638,636 bytes, SHA-256
  `66072a31caf92de7acd821a293a9823f02417d5363a77f832794a85d6f72dd92`, exit 3/PARTIAL,
  `/tmp/morphia-discovery-r3/report-{1,2}.json`. This supersedes the `9903...` report above.
  Same 336 capability/context keys, 915 evidence records, 32 facets and 165 version events;
  the only event change is the intended `$regex` feature notice becoming UNKNOWN.
- QUERY_PROJECTION, UPDATE_OPERATOR, UPDATE_MODIFIER and WINDOW_FUNCTION are healthy.
  Definition-level `CannotOpenFile` fell from 25 to 14 (8 moved to REFERENCE). The remaining 14
  come from `:doc:` links in generated YAML extracts and included fragments (merge/out/text/
  redact) that have no owning AST node on the page, so they stay definition uncertainty rather
  than being guessed. GROUP_ACCUMULATOR keeps its 12 legacy `grp.*` window-only definitions as
  explicit unassigned review items; a reviewed disposition list belongs in an accepted plan.
- Spotless, `git diff --check`, graph update, protected-path diff against `3c78eb7`
  (core/docs/util/annotations/critter-maven) and byte-identical legacy corpus reproduction pass.
  `actionlint` is not installed in this environment; the workflow change is one `-Dtest` list.

## Discovery published checkpoint

- PR [#4518](https://github.com/MorphiaOrg/morphia/pull/4518) targets #4517 (`audits-snooty-adapter`).
  Implementation head `36de9b54db2385ef2499ae0ea31d584565d3e323`. The user asked to publish before round 3 and to push any
  round-3 fixes as follow-up commits once Codex can review.
- Remote review requested with `gh pr edit 4518 --add-reviewer '@copilot'`. CI pending.

## Discovery review round 3 (Codex, after publication)

- Codex reviewed `8748bc6...HEAD` once its usage limit reset (`/tmp/morphia-discovery-codex-3.log`).
  Two valid P2 findings, both fixed with regressions:
  1. A blank hinted line still started the paragraph span, so a stale/forged origin could borrow
     evidence from the following paragraph. The span now stops at the first blank line, including
     the hinted line itself.
  2. Report decoding never tied `catalogMembers` to the context's capabilities; an emptied member
     list decoded. Members must now be distinct, sorted and equal to the context's capability tokens.
- 163 tests pass under Critter and reflection with parser integrations. The real replay is unchanged
  and byte-identical (`66072a31...`). Three local review rounds are complete; no fourth is needed.
  Fixes pushed to #4518 as a follow-up commit.

## Discovery checkpoint next steps (historical)

1. Discovery is published as #4518 at `4fdbe24fc8106efe275e8ea6dd9aef772ddba529`.
   All five exact-head checks passed; no remote comments/reviews or pending review requests.
   Local review findings are resolved. This satisfies the user's gate for the next stacked PR.
2. Active worktree: `~/dev/worktrees/audits-bson-comparison`, branch `audits-bson-comparison`,
   based on #4518. First step 3 slice: strict BSON comparator, explicit path policies, bounded
   multiset matching and controlled old/new comparison probes. No core/data migration yet.
3. Then: accepted coverage plan/expectations, shell AST scenario semantics and naming backport,
   curated repairs, executable generation, monitoring and the opt-in publisher.
   Do not wait for merges; do not merge without a user request.

## Temporary local evidence

Logs under `/tmp/morphia-audits-step0-*` contain build, CLI, formatting, graph, and review
output. Earlier `/tmp/morphia-audits-design-*` and `/tmp/morphia-audits-additions-*` logs cover
the design reviews. These are optional local evidence and may disappear after reboot;
the reproducible commands, findings, outcomes, and accepted catalog are preserved here.
Old implementation scripts under `/tmp` predate later fixes: do not rerun them blindly.

The separate 3.0 product-doc checkout was consulted; this internal audit foundation needs
no product-documentation changes. Legacy operator AsciiDoc tables are intentionally untouched.

## BSON comparison implementation checkpoint (2026-11-16)

- Confirmed the takeover against the design and GitHub: #4518 implements capability discovery,
  not scenario semantics or fixture migration. The 336-key catalog remains honestly PARTIAL.
- Added `semantics/BsonComparison{,Contracts}.kt` and offline regressions. BSON is pinned to the
  repository driver version; JSONAssert is test-only for controlled compatibility probes.
- Contract and migration gate: `design/operator-audits-bson-comparison.md`.
- In progress: both-mapper test runs, required local review, graph/format/preservation checks,
  then commit and publish targeting `audits-capability-normalization`.

## BSON comparison review round 1

- Claude found a real boundary issue: raw/lazy BSON containers permit duplicate keys, implicit
  decoding and uncounted linear lookups. They are now rejected before traversal; driver BSON
  exceptions return INVALID. Plain materialized containers remain the supported input contract.
- Added negative/relative Decimal128 and negative-zero tests, work exhaustion during candidate
  comparison/matching, raw/corrupt/lazy container tests and the missing old-mode probe rows.
- Corrected stale stack metadata and mismatch wording; cached the pointer-validation regex.
  Changed matching to iterative augmentation so result count cannot increase the call stack.
- Kept the single audits-only BSON version reference to `${driver.version}` instead of widening
  parent dependency management for one consumer; it uses the same version property as the driver.
- Copilot CLI attempted; unavailable due monthly quota (`/tmp/morphia-bson-copilot-1.log`).
- Audit tests do not instantiate either mapper. Both requested mapper configurations are still run
  to follow repository verification policy; this is not a claim of serialization/behavior parity.

## BSON comparison review round 2

- All round-1 findings verified resolved or documented. The independent reviewer confirmed
  iterative matching and budget counts against the exhaustive oracle.
- Fixed Java null array values and null result-list elements escaping validation; null BSON
  input is INVALID and distinct from `BsonNull`. Added a regression for both input forms.
- Removed the now-unnecessary broad BSON exception catch after rejecting raw/lazy containers,
  so unexpected implementation errors remain visible. Corrected remaining mismatch wording.
- Final local review and final test/format/preservation checks pending before publication.

## BSON comparison final verification

- Final local review (round 3) found no remaining correctness issues. Applied the optional
  wording/wrapping nits. All reviewer findings and the rejected dependency-management nit
  are recorded above; no fourth round is needed.
- 188 audit tests pass with both Critter and reflection configurations, including 25 comparator regressions.
  Two unchanged native-parser integrations skip without their explicit Python properties.
  Reflection final run: `/tmp/morphia-bson-reflection.log`; Critter: `/tmp/morphia-bson-critter.log`.
- The independent small-multiset oracle checks 729 input pairs. The comparator does not invoke
  a mapper or attest actual server behavior; running both configurations follows repo policy.
- The legacy corpus reproduces byte-identically and protected core/annotations/util/docs/
  critter-maven paths remain unchanged. Root Spotless passes. Product docs on docs-redesign
  were consulted; this internal comparator requires no product documentation change.
- Publish this branch targeting #4518, request remote Copilot review, then check exact-head CI
  and any remote comments. No merge is authorized. Next: pinned shell parser compatibility
  spike and scenario lowering, followed by descriptive-name migration and curated repairs.

## Shell semantics start (2026-11-16)

- #4519 published at `27a3fa7c1db2d22a6b0fc6e1c3d0f53f0e45f257`. All five exact-head checks
  passed: OfflineTests 1m24s, ParserTests 2m07s, CodeAudits 2m34s, Build 6m00s, Critter 6m05s.
  Remote review was requested; no pending request/review/comment was returned (Copilot unavailable).
- New worktree `~/dev/worktrees/audits-shell-semantics`, branch `audits-shell-semantics`,
  based on #4519. No merges. Next slice selects/pins a maintained shell AST parser using
  source-backed corpus probes, then lowers supported syntax into typed BSON/scenario operations
  while preserving raw blocks and explicitly rejecting unsupported constructs without execution.
- Parser candidates: Rhino public Java AST (fits Java 17, no extra runtime) and Acorn ESTree
  (requires an isolated pinned Node runtime). Primary release/source documentation is being checked;
  no parser has been selected or added to the production dependency graph yet.

## Shell literal implementation checkpoint

- Selected Rhino 1.9.1 public AST after comparing it with Acorn 8.19.0. Candidate JAR SHA-256:
  `12dbb224cea124a5d32968722e8eb161c967bf15f3c9a795d1b05592b1147975`.
  The parser is a compile dependency only in audits; no JavaScript program is compiled or evaluated.
  Regex syntax validation uses the pinned engine's regex compiler without matching or running code.
- Added initial `shell/ShellLiteral{Contracts,Constructors}.kt` and `ShellLiterals.kt`, plus
  17 offline tests. Single-value lowering is a focused slice, not a complete scenario model.
  Profiles distinguish MONGOSH, LEGACY_MONGO and UNSPECIFIED; defaults cannot silently change
  int/double/undefined types. Explicit supported constructors retain BSON types and key order.
- First compile succeeded. Initial tests exposed unusable Rhino offsets on synthetic wrapper
  expressions; invalid hints are now omitted while retaining the entire raw source. Rerun pending.
  Local reviews, reflection verification, docs/benchmark harness, formatting and PR publication
  are still pending; do not treat this worktree as reviewed or complete.
- Benchmark artifacts (all read-only, no code evaluation): `/tmp/morphia-shell-spike-{corpus.jsonl,input.tsv,receipt.json}`,
  `/tmp/morphia-shell-{rhino,acorn}-results.tsv`, `/tmp/MorphiaShellSpike.java`,
  `/tmp/morphia-shell-acorn-spike.cjs`, `/tmp/morphia-shell-prepare-spike.py`.
  4,798 cases = 3,130 owned doc code nodes + 1,660 hash-verified legacy JSON files + 8 probes.
  Corpus SHA `cb074470e038340aeccfc4126a02ec0545bd7b46a1b335884c537bdc4da7570d`;
  TSV SHA `0177bc33e5723cc1fcac603c0865ba4cb3a1a2d88d8705ba5626699e1a15c7b4`.
- Syntax acceptance is not normalization success. Ten acceptance differences include mathematical
  assignments, two C# blocks that happen to tokenize as JS, a shell command and a PCRE literal.
  Lowering rejects unsupported syntax, computed values, calls and placeholders atomically.
- Rhino accepts 1,343/1,660 legacy files as a script or value; Acorn accepts 1,342. The 317 Rhino
  failures are mainly newline-separated result documents (235 expected + 72 data files), plus
  7 actions and 3 indexes. 160/704 locked JSON files fail this single-script/value benchmark.
  This is evidence for an explicit result-stream adapter, not permission to strip or reshape data.

## Shell literal verification checkpoint

- 206 audit tests pass with Critter, including 18 literal regressions. Reflection is running.
  Logs: `/tmp/morphia-shell-critter-final.log`, `/tmp/morphia-shell-reflection.log`.
- Fixed Rhino's `undefined` keyword representation and nullable invalid span hints. Added
  JS regex syntax validation (compile only, never match/evaluate a program), regex resource
  guards, BSON NUL checks and strict UTF-8 handling for source/decoded strings and keys.
- Reproducible benchmark checked in under `audits/probes/shell/`; it verifies dependency and
  legacy fixture hashes and performs no acquisition. Its 4,798 cases reproduce both input
  hashes above and the same ten acceptance differences with IDE recovery disabled.
- Decision/limitations documented in `design/operator-audits-shell-literals.md`; audit README
  and main design link to it. Individual literals are not complete scenarios or a wire DTO.
- Local review 1 running (`/tmp/morphia-shell-claude-1.jsonl`); Copilot availability attempted
  (`/tmp/morphia-shell-copilot-1.log`). No review findings have been applied yet. Formatting,
  graph refresh, protected-path/corpus checks, final review and PR publication are still pending.

## Array metadata correction during local review

- A focused public-AST probe showed `destructuringLength` is 1 for `[]` and 2 for `[1,]`;
  treating it as element count wrongly rejected both. Sparse arrays actually carry explicit
  EmptyExpression nodes. Switched to that evidence and added empty/trailing-comma/hole cases.
- This is an additional valid implementation finding; the first independent review is still
  running. Final full tests must be repeated after this fix before any commit/publication.

## Shell literal review round 1 resolutions

- Verified the high-priority Long finding against pinned official js-bson and mongosh sources:
  numeric Long uses low bits with zero high bits. Corrected this separately from NumberLong/
  string Long; added negative, large and wraparound cases. Source pins are in the literal design.
- Double string coercion is now unsupported, avoiding Java float grammar replacing JavaScript.
  NumberLong numeric rejection wording now identifies a conservative policy rather than a shell
  capability assertion. Canonical 64-bit strings reject forms the native parser would not accept.
- Aligned production/benchmark Rhino to VERSION_ECMASCRIPT; Acorn remains pinned at ECMA 2026.
  Re-ran the checked-in probe: same 4,798 inputs/hashes, 10 differences and table counts. The
  machine receipt preserves helper/runtime provenance after final formatting.
- UNSPECIFIED now accepts unambiguous Double values while rejecting ambiguous Int32 defaults.
  Representation catches are local to explicit parsing boundaries, not the entire dispatcher.
- Added constructor/regex/nested-array span tests. They exposed a second upstream position bug:
  UnaryExpression ignores operatorPosition, omitting the sign even with an in-range hint. Omit
  unary spans and retain whole raw blocks; do not infer offsets by scanning source text.
- First review validated no program execution, wrapper rejection, recursion handling, getters/
  methods, regex s-flag support, dependency pin and date/binary/timestamp ranges. The empty-array
  correction above was already applied. Current final run is pending after the unary hint fix;
  review round 2 has not started. Nothing on this branch is committed or published yet.
- The temporary literal probe measured all 4,798 inputs with zero unexpected exceptions;
  initial 844 doc blocks and 615 legacy files normalized under an explicit MONGOSH assumption.
  That assumption is not a verified source/legacy interpretation or migration acceptance.

## Shell literal review round 2 resolutions

- All eight first-round findings verified resolved. Four new findings were valid and fixed:
  native JS regex dotAll `s` is not serialized as BSON `s`; RegExp string `source` can differ
  from its argument; Java integer/decimal parsers accept Unicode digits that js-bson does not;
  Decimal128 special-value parsing depended on the default locale.
- Verified pinned official source with gh (the web fetcher's cache missed these revisions):
  serializer blob `08e81eca3d151dce91f547baf4149c66be9d150c`, decimal blob
  `8f491b3ce975444863e607d77032591d6f4b7a02`, mongosh shell wrapper blob
  `690994f62e941d7c3b8a1041d4a89e1cecd91d68`. Commit links in the design are valid.
- JS regex flags limited to i/m. Explicit BSONRegExp retains s/x. Empty/slash/line-terminator
  RegExp string source forms remain unsupported instead of guessing canonical source escapes.
- ASCII numeric grammars, bounded decimal input and Locale.ROOT special-value handling added;
  NaN uses js-bson's canonical positive encoding. Added Unicode, Turkish locale and regex-source
  regressions. Tests must pass and the third/final local review is pending before publication.
- Root formatting/graph/protected paths and benchmark helper identities passed before these
  fixes. Repeat required checks for the final implementation. #4519 remains green at 27a3fa7c;
  rechecked remote feedback and found no reviews/comments/pending requests.

## Shell literal final verification

- Final local review (round 3) is clean. It verified round-2 fixes against js-bson 7.3.3 bundled
  with local mongosh 2.14.1 and Java BSON 5.14.0, including regex serialization, ASCII strings,
  locale-independent special values and finite Decimal128 byte equality for edge cases. This
  is additional native-library evidence, not execution of the pinned source revision or docs code.
- 215 audit tests pass with both Critter and reflection configurations, including 27 literal tests.
  Two unchanged native-parser integrations skip without their explicit properties. Logs:
  `/tmp/morphia-shell-critter-final.log`, `/tmp/morphia-shell-reflection.log`.
- Final temporary literal probe: all 4,798 inputs completed without unexpected exceptions,
  `/tmp/morphia-shell-literal-final-results.tsv`. Its MONGOSH profile remains an explicit probe
  assumption, not a source/legacy interpretation or acceptance of migrated expectations.
- Checked-in comparative receipt verifies all helper/parser identities and reproduces input hashes.
  Legacy corpus reproduces byte-identically; core/annotations/util/docs/critter-maven and its
  frozen catalog remain unchanged. Final root formatting/graph/preservation checks precede commit.
- Copilot CLI unavailable due monthly quota. Request remote review after publication, inspect
  comments and exact-head CI, then continue without a merge once the existing gate is satisfied.
- Next slice: complete scenario normalization (ordered setup/actions, explicit dependencies,
  multi-collection/index/query/update/pipeline forms, expected results/errors and source grouping).
  Use the persisted names and strict comparator; do not silently reshape legacy document streams.

## Ordered scenario programs start (2026-11-16)

- #4520 published at `8dd1ef154f7fff04a24ceeaca2c407e56e31c8c1`. All exact-head checks passed:
  OfflineTests 1m25s, ParserTests 2m11s, CodeAudits 2m10s, Build 6m04s, Critter 7m10s.
  Three local reviews completed clean; remote review requested but no comments/reviews/requests
  returned (Copilot quota unavailable). No merges performed or authorized.
- New worktree/branch: `~/dev/worktrees/audits-scenario-programs` / `audits-scenario-programs`,
  based on #4520. Literal implementation and its 215 tests are committed on the parent.
- Next slice: typed ordered program model, explicit source units/block roles, pure variable
  bindings/dependency provenance, collection aliases, setup inserts/indexes and find/aggregate/
  update operations. Preserve unsupported code and prevent any partial program from becoming
  executable coverage. Documentation grouping/result streams/naming integration still need work.
- Important design constraint: a JS primitive number is not the same as an explicit BSON Int32/
  Double/Long constructor object. Do not cache a binding's serialized BSON value and then reuse
  it as a constructor argument. Retain the pure initializer AST and captured bindings, re-lower
  with an explicit storage/initializer/constructor-argument mode, and record dependencies.
  Constructor shadowing, later assignments and forward references require explicit handling.
- Parser spans use a source-unit prefix offset (0 for programs, 1 for literal envelopes).
  Continue omitting invalid wrapper/unary hints; never infer exact spans by text scanning.
- This was the initial branch checkpoint; implementation progress is recorded below. Commits/push
  remain authorized, but complete tests/reviews before publication. Frozen core data remains preserved.


## Ordered scenario programs implementation checkpoint (2026-11-17)

- Uncommitted implementation on `audits-scenario-programs`, based on green #4520. No new PR yet.
- Added bounded ordered-program normalization with explicit source roles/formats and shell dialect,
  captured pure initializer ASTs, binding/dependency provenance, namespace aliases, typed inserts,
  indexes, finds/cursor modifiers, aggregation and updates, explicit expected-result/error ownership.
  Failures preserve raw sources and diagnostics but discard all executable commands/expectations.
- Shared literal lowerer now resolves captured bindings separately in storage/initializer/constructor
  argument contexts. BSON snapshots retain exact bytes/type/key order and decode fresh copies.
  Legacy fixture documents use two declared BSON-reader framing grammars without brace counting,
  source sanitization or inferred JavaScript semantics. No source/test-data mutations or execution.
- Added strict source-request decoding and a read-only JSON CLI. Snapshot reports are write-only;
  no untrusted encoded-BSON input reader or arbitrary operation-class deserialization is accepted.
- Verified pinned mongosh API names and reserved database members. Cursor `projection` is supported;
  `toArray` is terminal. Legacy insert retains document-versus-batch intent. Ambiguous result owners,
  even a mixed write/read pair, require an explicit selector. Private `_bsontype` dispatch is blocked.
- Latest Critter-configured audit run passes 238 tests, two optional native integrations skipped;
  log `/tmp/morphia-program-critter.log`. Includes CLI exit/input preservation, snapshot isolation,
  literal regressions and ordered-program tests. Reflection/final formatting/graph/corpus checks pending.
- Still pending before publication: real-corpus bounded probes, design/README and representative
  fixtures, independent local review rounds and fixes. Automatic docs source grouping/identity
  reconciliation, actual legacy-name migration and behavior attestations remain later slices.

## Ordered program probe and review preparation

- Real-input exploratory replay completed: 4,798 blocks, zero unexpected exceptions. Under an
  explicit MONGOSH assumption and one-action-per-block framing, 1,043/3,130 documentation blocks,
  140/1,660 legacy files and 4/8 deliberate probes normalized. These are structural measurements,
  not source language attribution, grouped scenarios or approved migration coverage.
- Explicit legacy document format accepts 1,484/1,660 frozen fixture files without brace counting
  or rewriting. Remaining inputs stay unsupported. Scratch helper `/tmp/MorphiaProgramProbe.java`,
  results `/tmp/morphia-program-probe.tsv`; input identities match the parent shell-probe receipt.
- Added the program design/README and a synthetic multi-collection JSON request fixture. The CLI
  accepts requests only and emits write-only snapshot reports. Added a regression that unsupported
  programs retain every validated raw source block, including those after the rejected block.
- Final formatting and both requested mapper configurations are next, then independent review.
  Source grouping, actual legacy semantic assessment and descriptive-name migration remain pending.


## Ordered program local review round 1 resolutions

- Independent review found five valid defects, all reproduced and addressed. Expected shell output
  now has its own mode: displayed whole numbers in Int32 range cannot prove Int32 versus Double,
  so they remain unsupported rather than borrowing input serialization defaults. Undefined is
  likewise unresolved. Typed source constructors/declared reviewed legacy data remain available;
  grouping must never inject such evidence. Added both-shell-profile and typed/fractional cases.
- Malformed ObjectId/base64 legacy constructors now produce unsupported parser verdicts instead
  of escaping as IllegalArgumentException. Pinned mongosh namespace predicates are enforced for
  shell and declared legacy namespaces. Aggregate explain is rejected pending a separate model.
- JSON requests must explicitly provide resultOrder and every block format, even though the
  in-process Kotlin API retains convenience defaults. Added missing-field transport regressions.
- Review validated captured binding scopes, primitive/constructor re-lowering, insert-copy semantics,
  cursor ordering/materialization, ownership, all-or-nothing failure and bounded snapshots/framing.
  Complete findings: `/tmp/morphia-program-claude-1.log`; upstream probes `/tmp/review-probe/`.
- Both initial configurations passed 240 tests; post-fix verification/review round 2 still pending.
  Copilot CLI availability was attempted and returned monthly quota exhausted.

## Ordered program post-review verification

- Corrected suite passes 244 tests with Critter (two optional native integrations skipped).
  Added CLI regression for malformed legacy constructors, requiring exit 3 and empty stderr.
  Reflection and resumed independent review round 2 are running; source is frozen for review.
- Final 4,798-input replay reproduced the initial counts and zero unexpected exceptions after the
  fixes: `/tmp/morphia-program-final-probe.tsv`. No acceptance/migration claim derives from it.
- Root formatting passed after fixes. Program design now records display-type uncertainty, pinned
  namespace validity, separate explain semantics and mandatory JSON transport decisions.

## Ordered program final local verification

- Review round 2 is clean: all five verified findings fixed, no new actionable issues. Independent
  Java 17 probes covered both failure and typed/unambiguous success paths, namespace names, explain,
  transport omissions and retained sources. Full summary `/tmp/morphia-program-claude-2.log` (the reviewer could write only its plan file).
- Final Critter and reflection runs each pass 244 audit tests with two optional native-parser
  integrations skipped locally. Logs `/tmp/morphia-program-{critter,reflection}.log`.
- Formatting, graph refresh and protected-path/corpus checks complete before commit. No core data,
  test names, locks or accepted catalogs are changed. Internal audit design requires no product
  documentation changes in the separate morphia-docs repository.
- Copilot CLI unavailable due monthly quota; request remote review and inspect feedback after
  publication. The next slice starts only after exact-head CI and available review feedback pass.
- Remaining next slice: group owned native documentation blocks using explicit language/tab and
  source roles; preserve all setup/include/snippet dependencies; distinguish display uncertainty;
  reconcile persisted descriptive identities. Then assess/migrate the frozen legacy corpus before
  replacing its ordinal names and lock-based generation. No merges authorized for this stack.

## Ordered program publication checkpoint

- PR #4521: https://github.com/MorphiaOrg/morphia/pull/4521
  branch `audits-scenario-programs`, base `audits-shell-semantics`, published head
  `5c3ee24502ff2e9a080e98b2dc1b5414e479a20c`.
- Exact-head OfflineTests, ParserTests, CodeAudits, Build and Critter CI are running. Remote Copilot
  review requested; no comments/reviews/pending requests returned on initial inspection.
- This checkpoint is intentionally uncommitted while CI runs; do not reset the published head just
  to record status. Carry the checkpoint into the next child worktree after the gate is green.
- No next implementation branch has started yet. Continue grouping/identity work after exact-head
  CI and available review feedback pass; no merge is required or authorized.

## Native scenario extraction starts (2026-11-17)

- #4521 is green at `5c3ee24502ff2e9a080e98b2dc1b5414e479a20c`: all five exact-head checks
  passed, including the two native parser integrations in CI. Local review round 2 clean; remote
  review requested, no comments/reviews/pending requests. Proof `/tmp/morphia-program-pr-final-status.json`.
- New child branch/worktree `audits-scenario-extraction`, based on #4521. Carried the publication
  checkpoint here and restored the parent journal after saving `/tmp/morphia-program-published-progress.md`.
- Next implementation: explicit structured documentation block/source ownership and grouping,
  language/tab roles, dependencies and provenance, program normalization integration and persisted
  descriptive identity proposals. Untyped display output must never be relabeled as legacy data
  or given constructors to bypass ambiguity. No next-slice implementation or review started yet.
- Ten published PRs now (#4512–#4521); expected total 16–20, depending on legacy migration scope.
  Existing tests/data/locks/names and accepted catalogs remain untouched. No merges performed.

## Native extraction investigation checkpoint

- Examined owned DocumentContracts/DocumentParsing, discovery source evidence/dependency ownership,
  naming contracts and the plan's scenario/migration gates. No next-slice production code written yet.
- Pinned native median has an explicit io-code-block containing input/output children with snippet
  references; setWindowFields has nine such pairs inside language tabs and descriptive sections.
  Lookup retains both syntax examples and multilingual example sections. Use native parent ownership,
  not nearest-block pairing. Preserve unknown/foreign blocks and malformed language labels.
- Important: code node origins for external io inputs/outputs can identify the hosting RST directive;
  the referenced snippet path and dependency receipt supply separate provenance. Retain both, including
  selection/range options and include chains, rather than treating the reported page line as the
  physical snippet body location. Existing report is `/tmp/morphia-audits-discovery-documents.json`.
- Shell output numeric ambiguity is present in real median/window results, not only synthetic probes.
  Do not reinterpret these outputs as LEGACY_DOCUMENTS or synthesize constructors. Structural pairing,
  semantic normalization, dependency completeness, naming and execution coverage remain separate states.
- Avoid creating an inventory-only PR merely to increase the stack: next slice should connect native
  source ownership/grouping to meaningful scenario/name proposals, with explicit unsupported states.
  Actual migration, typed execution/generation and publishing remain unfinished.

## Native scenario extraction implementation checkpoint

- Uncommitted production code now exists under `audits/.../scenarios`: explicit reviewed page/section,
  language/tab and count policies; bounded owned-AST traversal; native io input/output ownership;
  separate hosting-directive and snippet resource/dependency provenance; pure program normalization;
  and NamingScenario construction plus a guard against partial-scope name reconciliation.
- Unknown/foreign variants remain explicit and are never assumed equivalent by matching titles.
  Selected standalone code remains visible; reference/unknown blocks outside an io group block
  completeness instead of being guessed as setup or a neighboring output. JSON output is not
  silently treated as shell/legacy data. Program failure and source-grouping completeness differ.
- Initial compile and existing 244 tests passed. New ScenarioExtractionTest regressions cover typed
  io/name proposals, display ambiguity, snippet-derived generic titles, persisted names through
  rearrangement/title edits, unassigned setup, foreign tabs, count drops, duplicate groups, output
  language uncertainty, argument edges, input pins and budgets. Their first compile is in progress.
- Log `/tmp/morphia-extraction-critter.log`. No local review, final formatting/graph/preservation
  checks, docs or CLI yet. Explicit-label association and additional source-health checks still need
  attention before considering this slice complete. No commit or new PR on this branch yet.

## Native scenario extraction verification and probe

- 262 audit tests pass with both Critter and reflection, including 18 new extraction regressions;
  two optional native integrations skip locally. Root formatting and optional graph refresh passed.
  Logs `/tmp/morphia-extraction-{critter,reflection,spotless,graph}.log`.
- Native source health now preserves explicit diagnostic dispositions. Confirmed cross-reference
  failures and diagnostics owned exclusively by declared foreign tabs do not invalidate a healthy
  reference scope. Unknown/mixed errors remain blocking. Added isolation/mixed-ownership tests.
- The native three-page probe finds median 1, lookup 7 and window 8 reference groups, plus two foreign
  groups. Window's ninth IO group is foreign; corrected this exploratory reference-count policy.
  All 16 reference naming inputs are structurally complete. Fourteen programs remain unsupported
  solely for EXPECTED_TYPE_AMBIGUOUS; two normalize. No server execution or migrated coverage claim.
- Same-heading concise/verbose lookup examples are distinguished by authored snippet names, with
  the original section title retained. Native name proposal now resolves all 16 without ordinals.
  `audits/probes/scenarios/` records the exploratory policy, provenance and reproducibility contract.
- Coherent native inputs are unchanged. The catalog pin is recomputed canonically through
  AuditJson.hash, separately from its transport-file hash. Final probe hashes/receipt are pending
  the canonical-pin replay; first independent local review has not started yet. Source is frozen.
- Next semantics work should model the numeric uncertainty of published output explicitly (while
  retaining strict typed reference-versus-DSL comparison), rather than manually editing 14 outputs.
  Legacy section/setup grouping, corpus migration and full generation remain pending too.


## Native canonical replay receipt

- Canonical catalog pin `61c0879656464120f041fc857467efe06e5381ad28427f3f1e8726baf6c9ac09`;
  transport-file pin `8d805200d1eb6d94797f3330babd6b7b4cce2b96986c0f2b24f37e4cbe044928`.
- Final two extraction runs are byte-identical: 1,100,933 bytes, SHA
  `f8853465838381ac477c4a2860627720012005e6f0fdc7ad0c5eba7e2a7ce384`.
  Report/repeat `/tmp/morphia-scenario-probe-review{,-repeat}.json`, name proposal
  `/tmp/morphia-scenario-probe-names-review.json` (READY, 16 resolutions). Probe receipt updated.
- Independent local review round 1 starts now; Copilot availability will be checked. No production
  code changes during the review. Commit/push remain authorized after fixes and verification.


## Native extraction review round 1 resolutions in progress

- Independent review reproduced the exact native receipt and found nine actionable concerns.
  Full record `/tmp/morphia-extraction-claude-1.log` and reviewer plan file outside the worktree.
- Added streaming serialized-size enforcement (including repeated ancestor metadata) and stdout
  failure detection; directive allowlists; data/display-format rejection as foreign languages;
  true leaf section-label scope; and label identity validation after label association.
- Scenario fingerprints are deferred until semantic setup/dataset/constraint closure is known.
  Page-wide foreign/unrelated assets must not masquerade as that closure. Program hashes remain.
  Authored references supply display names, while verified canonical resources remain provenance.
- Diagnostic traversal/paragraph work shares the extraction budget, with cached raw lines.
  The full-input node/depth limit is deliberate before whole-report origin validation; documented
  batching/API limit choices instead of treating unselected nodes as unbounded trusted input.
- An additional verified finding: positional line ranges cannot be durable snippet IDs. Physical
  verification and authored marker identity are now separate; formatting transformations do not
  define identity. Added targeted regressions for these cases and verified test bundle manifests.
- Existing 262 tests passed after production fixes. Expanded regression compile/run in progress;
  final native replay/receipt, both configurations and follow-up independent review remain pending.


## Native review regression verification

- 269 audit tests passed after the Unicode writer-boundary correction. Added an authored-symlink
  title test; its fixture initially lacked the explicit symlink dependency root required by the
  verified bundle contract. The fixture now declares that root; both mapper configurations rerun.
- Streaming size verification now uses the same character generator and UTF-8 encoding as the
  emitted artifact, including supplementary Unicode. The stdout failure test uses a fully verified
  synthetic bundle and checks `/dev/full` where available. No source authenticity claim is made
  for the synthetic AST fixtures.
- Review findings 1–7 and 9 addressed; 8's traversal/paragraph costs now charged/cached. Its
  full-artifact node-bound observation is an intentional input-validation constraint, documented
  with selected-page batching and explicit API limits. No unsupported claim of selected-only
  validation remains. The positional-identity finding is also addressed.
- Final native replay/receipt and independent follow-up review are still required before commit.

## Native extractor post-fix state

- The full expanded suite passed 270 tests in both configurations (two optional native integrations
  skipped locally). A final factual-evidence cleanup is being reverified: null/positional identity
  is never printed as a verified source identity; physical snippet evidence remains separate.
- All nine first-review items have concrete resolutions. The complete-input AST limit remains
  intentional and documented; diagnostic work is now charged and raw paragraphs cached. Reports
  have a 64 MiB streaming serialization cap and failed stdout writes return exit 2.
- Normalization-independent naming behavior is preserved. Positional ranges have no STABLE_ID;
  authored markers do. Labels associate before identity validation, and broad sections cannot give
  one child their whole-section label. Unknown directives and data-format labels remain unresolved.
- Final native replay/receipt update and resumed review round 2 are next. Current source is frozen;
  all changes remain uncommitted on audits-scenario-extraction, based on green #4521. No new PR yet.

## Native replay guard caught a default-namespace regression

- The stricter directive allowlist initially omitted Snooty's empty-string default namespace (unit
  fixtures had used null). Native replay returned PARTIAL with count-drop/unknown-language diagnostics,
  so no naming request or accepted artifact was promoted. Default namespace support now explicitly
  includes empty string, with a native-shaped regression. Unknown namespaces remain unresolved.
- Current combined command reruns Critter, reflection and the native replay sequentially to avoid
  reading changing compiled classes. Log paths remain `/tmp/morphia-extraction-{critter,reflection}.log`;
  coordinator `/tmp/morphia-extraction-native-replay.py`; final artifacts `/tmp/morphia-scenario-final-*`.
- After it passes: copy `/tmp/morphia-scenario-final-receipt.json` to the checked-in probe receipt,
  refresh graph, then resume local review session `0979b5cb-d047-4a4d-b26f-59bef55879f7` for round 2.
  Source remains uncommitted; no #4522/next PR yet. Do not treat earlier replay hashes as current.

## Native post-review final replay

- 271 audit tests pass in each mapper configuration, two optional native integrations skipped.
  `/tmp/morphia-extraction-{critter,reflection}.log` contain the final runs.
- Native default-namespace regression is fixed. Two final extraction runs are byte-identical:
  1,101,995 bytes, SHA `4bd4b94743ab5773429f06ece5102fa2be1d181810b6efbf2c49ef33918cf659`.
  Grouping READY: 16 reference plus 2 foreign groups. Name proposal READY: 16 resolutions.
  Program states remain 2 NORMALIZED and 14 EXPECTED_TYPE_AMBIGUOUS, with no fabricated types.
- Receipt copied from `/tmp/morphia-scenario-final-receipt.json`; actual report/repeat/names are
  `/tmp/morphia-scenario-final-{report,repeat,names}.json`. No accepted catalog/name registry changes.
- First-review concerns preserved in `/tmp/morphia-extraction-review1-concerns.md` before resuming
  the reviewer. Follow-up review round 2 starts now; source remains frozen and uncommitted.

## Native extraction final review round

- Round 2 verified every prior fix and the exact native receipt. It found one remaining low issue:
  case-sensitive/data-format foreign labels. Replaced the denylist with a known programming-language
  allowlist, validated with Locale.ROOT while preserving explicit spelling for actual selection.
  Regression rejects JSON/Json/XML/CSV/MIME formats and unknown labels; known Python/C#/SQL remain valid.
- 272 audit tests now pass in both configurations (two optional native integrations skipped).
  Final native replay remains READY with 16 names, 16 reference plus 2 foreign groups, and byte
  identity across repeats. Receipt refreshed; it now also records AuditJson's source digest.
- Third/final independent local review starts now, focused on the last policy fix and final receipt.
  Source is frozen. No commit or PR on this branch yet; publication remains authorized after review.

## Final-round command-shell finding

- Round 3 verified the language allowlist and current receipt, then identified one additional low
  issue: sh/bash/PowerShell snippets may be setup or mongosh invocation, so they cannot be silently
  declared foreign examples. Removed those labels and added rejection plus concrete mongoimport
  setup preservation regressions. No other actionable findings remained in the third review.
- Completing the third round with targeted verification of this small correction, not starting
  a fourth broad review. The combined command reruns both configurations and the native receipt.
  Source stays frozen afterward. Publication remains pending this verification and final checks.

## Native extraction final local gate

- Three broad review rounds completed. The sole remaining round-3 command-shell finding is fixed
  and its exact correction confirmed by the reviewer (`/tmp/morphia-extraction-claude-3-verification.log`).
  All prior findings resolved; intentional fingerprint/global-input-bound decisions documented.
- Final suites: 272 tests pass with Critter and reflection, two optional native integrations skipped.
  Native replay remains byte-identical at 1,101,995 bytes / SHA
  `4bd4b94743ab5773429f06ece5102fa2be1d181810b6efbf2c49ef33918cf659`;
  16 reference + 2 foreign groups, 16 descriptive names, 2 normalized programs and 14 explicit
  published-number type ambiguities. Receipt includes all five changed production source hashes.
- Final root formatting/graph refresh, protected-path checks and byte-identical legacy corpus pass.
  No core changes, names/data/locks changes, accepted catalog or registry promotion, or issue writes.
- Copilot CLI remains unavailable due monthly quota; request remote review after publication,
  inspect exact-head CI/feedback, then continue to the next stacked slice without a merge.
- Next implementation target: model published numeric uncertainty explicitly so common outputs
  become automated without fabricated BSON types; strict typed reference-versus-DSL comparison
  remains separate. Then legacy setup/section grouping and the actual descriptive-name backport.

## Native extraction publication checkpoint

- PR #4522: https://github.com/MorphiaOrg/morphia/pull/4522
  branch `audits-scenario-extraction`, base `audits-scenario-programs`, head
  `9b17912cb1bca306cc70772bec03b6c000293f93`.
- Eleven PRs published in the stack. All five exact-head checks are running; remote Copilot review
  requested, no initial comments/reviews/pending requests. No merges performed or authorized.
- This status checkpoint remains uncommitted to preserve the reviewed/published CI head. Carry it
  into the next child worktree after CI and available review feedback pass, then restore this parent
  journal only after saving a copy. Next slice: explicit published numeric uncertainty patterns.

## Published expectation semantics start (2026-11-17)

- #4522 is green at `9b17912cb1bca306cc70772bec03b6c000293f93`: all five exact-head checks pass.
  Three broad local reviews completed and every finding resolved/verified. Copilot quota unavailable;
  remote review requested, no comments/reviews/pending requests. No merges authorized or performed.
- New branch/worktree `audits-published-expectations`, based on #4522. Carried the publication
  checkpoint here; parent journal restored only after saving `/tmp/morphia-extraction-published-progress.md`.
  Exact-head proof `/tmp/morphia-extraction-pr-final-status.json`. Eleven PRs published so far.
- Next slice: explicit published-value uncertainty, beginning with finite whole printed numbers
  that can denote Int32 or Double. Preserve source and encode the allowed type/value alternatives;
  never fabricate a single expected BSON type or loosen typed reference-versus-DSL comparison.
- Integrate this secondary published-expectation model with ordered programs while keeping unknown
  syntax/all-or-nothing failures explicit. Preserve result order, duplicate counts, field presence
  and nested array/document order. No expectations generated from the implementation under test.
- Native baseline: 16 reference groups/names, 2 normalized programs, 14 blocked solely on
  EXPECTED_TYPE_AMBIGUOUS. Use the same pinned replay to measure the improvement, not as an accepted
  behavioral baseline. Dataset/constraint/setup closure and actual migration still remain later work.
- No expectation-pattern implementation or review started yet on this branch.

## Published expectation implementation checkpoint

- Implemented closed published constraints and bounded matching, integrated into ordered-program
  output schema/parser version 2. No typed-comparator changes. Pinned modern read-result profile;
  unknown/legacy display profiles and BSON-decoding overrides fail explicitly. Existing typed legacy
  document fragments retain their path. Shell strings also account for legacy BSON Symbol promotion.
- Shared closed AST lowering now produces either concrete BSON or explicit published constraints;
  captured bindings retain context/dependencies. Fixed ECMAScript numeric-key enumeration in input
  objects, with typed BSON comparison still preserving the full original BSON order.
- Added expanded-text/artifact bounds, NaN/Decimal128 display constraints, non-greedy multiset
  matching and regression coverage. First compile passed; three old expectation assertions updated
  to the new representation. Critter suite is running; reflection, native replay and reviews remain.
- Current branch has uncommitted implementation only. No new PR or merge. Resume from this branch;
  do not rerun /tmp patch scripts against already changed files. Native replay and receipt still
  need refreshing in a separate published-expectations probe; prior receipt documents prior output.

## Published expectations local verification and review start

- 296 audit tests pass with Critter and reflection; the two optional native parser integrations skip
  locally. Required root formatting and AST graph refresh pass. Corpus reproduction is byte-identical;
  protected core/annotations/util/docs/critter paths, locks/data and strict typed comparator are unchanged.
- Checked-in offline replay coordinator now reproduces the native experiment: 16 reference programs
  NORMALIZED, two foreign variants, 16 byte-identical descriptive names. Repeated report is 1,259,256
  bytes / SHA `8fa09867b37405275bbe578c262b56702910d0c7d929c8ed49f3485e892fa79d`.
  Receipt `audits/probes/published-expectations/receipt.json`; artifacts `/tmp/morphia-published-probe`.
  The improvement is 2 to 16 normalized programs; no execution or migration is claimed.
- Copilot CLI quota check again returned monthly quota exceeded; log
  `/tmp/morphia-published-copilot-1.log`. Proceed with the available independent local reviewer and
  request remote review after publication. Source is frozen for the first review round.
- Current branch is still uncommitted/unpublished. Next: verify findings, repeat available reviews
  (maximum three broad rounds), rerun affected checks/replay, commit and publish stacked on #4522.

## Published expectations first review fixes

- First independent review completed: four actionable findings, all verified and fixed. Full notes
  `/tmp/morphia-published-review1-concerns.md`, output `/tmp/morphia-published-claude-1.log`, review
  session `83da5bc8-644f-4318-a9f5-4ff001f5bf32`. No findings rejected.
- Exact-scalar matching now rejects type mismatches before calling typed comparison, honors caller
  limits and charges both leaf costs. Regression covers a valid depth-150 array against a scalar
  under depth-200 limits. Per-candidate long-key comparison is also charged to the shared budget.
- Both meaning and full normalized-artifact size checks now run inside the program failure boundary:
  ARTIFACT_LIMIT preserves source/provenance without executable commands, and extraction continues.
  A real default-limit control-character expansion regression exercises this path end to end.
  The final output-cap check remains a documented fail-closed backstop for an oversized failure artifact.
- Published DBRef-shaped documents are conservatively unsupported at every depth (both $ref/$id
  keys); input storage is unchanged. Verified the finding against pinned official deserializer/db_ref
  sources, not only the installed shell. Undefined remains unsupported; null is not widened because
  the pinned decoder preserves undefined distinctly. Corrected an interim null-display assumption.
- Replay now records ordered compiled classpath file/tree hashes, every audits Kotlin source hash,
  Java/Python versions, Git HEAD/dirty status. Source and runtime identities remain separate evidence.
  Meaning hashes explicitly include model version 2 and published profile. Naming remains independent.
- 300 tests pass with Critter/reflection after these fixes. A final small malformed-field-budget cleanup
  is under the same sequential verification/replay command; second review starts once it completes.
  No new commit/PR yet. Copilot CLI remains unavailable due quota; publication remains authorized.

## Published expectations second review gate

- Final malformed-field checks now validate/charge each field incrementally rather than scanning
  an uncharged list first. Both 300-test suites pass, formatting/graph refresh pass, and the refreshed
  native replay is repeat-identical: 1,259,256 bytes, SHA
  `ebc6bd0bcf598c48271b0ec63dfd54ff1797a616f7e8f47d076fc3472f233703`.
  All 16 reference programs normalize; two foreign variants and the 16 identical names are preserved.
- The report hash changed from the first checkpoint because model version 2 is now part of meaning
  identity; the naming proposal hash remains `5615ff95a85717df3aac0d9e87fe4dfebce36a0b679110017ca194f42460d8e1`.
- Source and receipt are frozen for round 2. Verify all first-round corrections, profile source pins,
  shared limits and the strengthened compiled-runtime receipt. No commit/PR on this branch yet.

## Published expectations second-round correction and final review

- Round 2 confirmed every prior finding fixed and verified the exact pinned decoder/provider rules,
  all 64 source digests, 21 compiled classpath identities and the native receipt. One new valid
  concern: published text/keys bypassed the stored-value budget, allowing sub-cap expanded outputs
  to overflow the aggregate report. Notes `/tmp/morphia-published-review2-concerns.md`, result
  `/tmp/morphia-published-claude-2.log`. No substantive findings rejected.
- Renamed the program limit to `maximumStoredValueBytes`: concrete snapshot bytes and full published
  roots now share its 8 MiB default. Count encoded UTF-8 JSON once per root, including escaped
  controls, Unicode, field names and type metadata; exact leaves conservatively pay both retained
  snapshot and serialized-root costs. Exhaustion is a scoped STORED_VALUE_LIMIT, not partial commands.
- Added shared typed/published quota coverage and a two-program default-limit extraction regression
  (600 references × 10,000 control characters per program) proving source grouping survives the
  amplification case. The independent aggregate report limit remains an explicit batch boundary;
  it cannot promise an unbounded number of otherwise valid programs. Narrowed the design accordingly.
- 302 tests pass under both mappers, two optional native integrations skip. Root formatting, graph
  refresh and native replay pass; report SHA remains `ebc6bd0bcf598c48271b0ec63dfd54ff1797a616f7e8f47d076fc3472f233703`,
  all 16 references normalize, 2 foreign groups and all 16 names remain unchanged. Receipt refreshed.
- Source is frozen for the third/final broad local review. Verify this correction and existing fixes;
  publication remains pending that gate. No commits or PR on this branch yet.

## Published expectations final local gate

- Three independent local review rounds completed. Round 1's four findings and round 2's shared
  quota finding are fixed; round 3 confirms no actionable findings and verifies all prior fixes,
  all 64 source hashes, the probe, all 21 compiled classpath identities and the native report.
  Final review `/tmp/morphia-published-claude-3.log`; concerns/verification retained in
  `/tmp/morphia-published-review3-concerns.md`. No fourth broad round is needed.
- Final verification: 302 tests pass with both Critter and reflection, two optional native parser
  integrations skip locally. Required root formatting and graph refresh pass. Legacy corpus
  reproduction is byte-identical; protected project paths and strict typed comparator remain unchanged.
- Native report repeats at 1,259,256 bytes / SHA
  `ebc6bd0bcf598c48271b0ec63dfd54ff1797a616f7e8f47d076fc3472f233703`;
  16 reference programs normalize, two foreign groups retained, 16 descriptive names byte-identical.
- Publish this branch stacked on #4522, request remote review and inspect exact-head CI/feedback.
  Copilot CLI remains unavailable due monthly quota. No merges authorized or performed.
- Next slice after available reviews and exact-head CI pass: legacy section/setup grouping, followed
  by actual descriptive-name backport and curated-repair migration. Later executable DSL generation,
  execution attestations, scheduled monitoring, the enabled separate gh publisher and cleanup remain.

## Published expectations publication checkpoint

- PR #4523: https://github.com/MorphiaOrg/morphia/pull/4523
  branch `audits-published-expectations`, base `audits-scenario-extraction`, head
  `2d7b5c942dfedc72f3257a8db9dec99b25ab0289`.
- Twelve PRs are now published. All five exact-head checks are running: OfflineTests, Code Audits,
  ParserTests, Build and Critter (AOT). Remote Copilot review requested; no reviews, comments or
  pending requests appeared, consistent with its unavailable quota. Local final review is clean.
- Keep the reviewed/published head fixed while checks run. This publication checkpoint is deliberately
  uncommitted; carry it into the next child worktree after CI and available feedback pass, then restore
  this parent journal only after saving a copy. No merges performed or authorized.

## Legacy scenario grouping start (2026-11-17)

- #4523 is green at `2d7b5c942dfedc72f3257a8db9dec99b25ab0289`: OfflineTests, ParserTests,
  Code Audits, Build and Critter all pass. The conditional Dependabot automerge job is skipped.
  All three local review rounds completed with a clean final round. Remote Copilot was requested
  but quota remains unavailable; no comments, reviews or pending requests to address.
- Exact-head proof `/tmp/morphia-published-pr-final-status.json`, inline feedback proof
  `/tmp/morphia-published-pr-final-comments.json`. Twelve PRs published. No merges authorized/performed.
- New branch/worktree `audits-legacy-scenarios`, based on #4523. Saved the parent's publication journal
  to `/tmp/morphia-published-publication-progress.md`, copied it here, then restored only that parent
  journal. Parent is clean. This next branch has no implementation/commit/PR yet.
- Next scope: replace the old first-example data/index inheritance with provenance-backed native
  section/setup grouping. Preserve ancestor-owned setup order and variables; never borrow from an
  unrelated sibling or pair output by ordinal/proximity alone. Keep foreign/unknown variants and
  ambiguity explicit; retain every original block. Reuse shared closed program normalization and
  descriptive identity reconciliation, not the legacy destructive parser.
- Existing inventory has 556 cases and 378 distinct display labels; 110 labels are only `main`.
  Actual name backport therefore needs recovered native context or reviewed mappings, not blindly
  sanitizing today's labels. Core coverage discovery/resource lookup and retained ordinal readers
  must be updated before any real method/folder cutover. Do not change protected corpus bytes yet.
- First implementation work remains: inspect representative owned AST/section shapes and define
  explicit assembly policy/roles, then implement/test and capture a native replay. Final execution,
  curations, registry promotion and issue publishing are still later milestones.

## Legacy printed-result implementation checkpoint

- Native inspection found a prerequisite for section assembly: `$abs` has real insert/aggregate
  commands plus four newline-separated result documents. The old `main` label loses its useful
  source context. Preserved owned blocks/metadata in `/tmp/morphia-abs-native-blocks.json` and the
  selected-page projection `/tmp/morphia-legacy-selected-pages.json`; the original 350-page report
  remains `/tmp/morphia-audits-discovery-documents.json` with the same source/input pins.
- Implemented explicit expected-only SHELL_RESULTS framing, parser identity ordered-program:3;
  output/meaning schemas stay 2. The maintained lexer finds boundaries, then the existing AST
  lowerer validates every untouched fragment and retains published type uncertainty. No source/data
  rewrites, namespace inference, automatic parser fallback or execution. Section assembly is next.
- The adapter requires an isolated package bridge to pinned Rhino TokenStream. A real Unicode
  regression exposed logical cursor undercounting in line comments: structural boundaries now use
  verified physical UTF-16 cursor positions. New fragments with surrogate code units omit uncertain
  AST hints; existing literal diagnostic behavior is preserved. Documented the tradeoff and classpath
  scope in design/operator-audits-shell-results.md for independent review.
- First 18 new framing tests plus the previous suite pass under Critter: 320 total, 2 optional skips.
  Added a pinned raw-source $abs fixture/test; next run will include it. Reflection, native replay,
  root formatting, graph refresh and independent reviews remain before commit/publication.
- The rate-limit interruption rejected the earlier test-writing/build command before execution.
  Resumed after reset; tests now exist. No review or implementation work was lost, no approval
  check was bypassed. All current implementation is uncommitted on audits-legacy-scenarios.

## Printed-result verification and first review gate

- Explicit native IO output ownership now selects SHELL_RESULTS automatically; native-io:2 records
  that policy revision without using encounter ordinals for names/identity. Ordinary SHELL requests
  retain their format/diagnostic behavior. Standalone legacy section ownership is not implemented yet.
- 323 audit tests pass with Critter and reflection (two optional native integrations skip). Root
  formatting and graph refresh pass. Tests cover the raw $abs fixture, IO integration, Unicode,
  constructors/type uncertainty, malformed tails, duplicate/order preservation and shared limits.
  The formatting-character regression asserts rejection; lexer and framing can validly report
  different syntax codes. The earlier over-specific assertion was corrected, not source behavior.
- Native $abs replay retains 2 commands and 4 rows, repeats byte-identically at SHA
  `5c93e8400bf9a0285bf99e4588356438e37bae424fe4adad970f4de5417a9c06`.
  Source extraction honestly remains PARTIAL with 3 unassigned blocks; explicit reviewed roles are
  matched by digest/provenance/text, not ordinal. Receipt audits/probes/shell-results/receipt.json.
- The prior IO experiment remains 16 normalized references + 2 foreign groups, 16 byte-identical
  descriptive names. Repeated report 1,259,384 bytes / SHA
  `57cfd44777a4c8e0cb95167c76c10a2da1e4a0a9ca7031963ebc1fbe7eba982d`.
  Receipt audits/probes/shell-results/native-io-receipt.json; the prior checkpoint is retained.
- Shared probe provenance now hashes all audits source namespaces (including the lexer adapter),
  ordered compiled classpath entries and build inputs. The native stream probe verifies those inputs
  remain fixed through execution. Source/runtime identities remain distinct, not a source authenticity
  claim for compiled code. Native artifacts /tmp/morphia-shell-results-probe and
  /tmp/morphia-shell-results-io-probe; logs /tmp/morphia-legacy-{critter,reflection,native,io-native}.log.
- Copilot quota check with valid read-only flags again reports monthly quota exceeded;
  /tmp/morphia-legacy-copilot-1.log. Source and receipts freeze for the first available independent
  review. No commit or PR yet on this branch. Preserve core corpus bytes until actual migration.

## Printed-result review findings and final local gate

- The first external CLI review wrote complete findings before its final response hit the weekly
  limit. Saved notes /tmp/morphia-legacy-review1-concerns.md and interrupted output
  /tmp/morphia-legacy-claude-interrupted-notes.md. Its adapter/physical-cursor analysis accepted the
  pinned classpath design; this was input for corrections, not an approval of the final code.
- Five substantive findings are fixed: HTML-like/hashbang comments can no longer hide rows/fields;
  every outer gap is validated as whitespace/comma or a whitelisted lexer-owned comment (including
  invisible format characters around comments); source selection is a strict bijection; format docs
  are current; both probes emit schema 2 and verify runtime/source/file inputs before/after execution.
- Two nits resolved: per-token sets are hoisted; lexer-contract drift deliberately aborts a batch
  and is documented as an upgrade incompatibility. The informational existing ordinary-SHELL
  supplementary-character AST-hint limitation is tracked separately; hints never establish identity
  or rewrite authority. New printed fragments omit uncertain hints. No substantive finding rejected.
- Added regression cases for hidden rows/fields, comment/trivia boundaries, and source-selection/input
  mutation. Final suites: 325 tests with Critter and reflection, 2 optional integration skips each;
  6 Python probe contract tests pass and are included in ParserTests CI. Root formatting/graph refresh
  pass. The strict comparator, protected project paths, locks and legacy corpus remain unchanged;
  exact corpus reproduction passes.
- With both external CLIs quota-limited, a fresh independent review process verified the frozen
  corrections (round 2). No actionable findings; it ran the six probe tests and 22 additional
  compiled-runtime cases, and verified current source/runtime/artifact digests. Notes
  /tmp/morphia-legacy-independent-review2.md. No further broad review round is needed.
- Final schema-2 receipts retain the same report identities: native abs
  5c93e8400bf9a0285bf99e4588356438e37bae424fe4adad970f4de5417a9c06 (2 commands / 4 rows),
  native IO 57cfd44777a4c8e0cb95167c76c10a2da1e4a0a9ca7031963ebc1fbe7eba982d
  (16 normalized references / 2 foreign groups / unchanged 16 names). No execution coverage claimed.
- Publish stacked on #4523, request remote review, and inspect exact-head CI/feedback before the next
  child. No merges authorized/performed. Next scope remains section/setup assembly and recovered
  descriptive context for legacy names, followed by the actual method/folder cutover.

## Printed-result publication checkpoint

- PR #4524: https://github.com/MorphiaOrg/morphia/pull/4524
  branch `audits-legacy-scenarios`, base `audits-published-expectations`, head
  `febc1d9ad81b6b97448b0e3b85d08c93ef7ffbcb`.
- Thirteen PRs published. All five required exact-head checks are running. Remote Copilot review
  requested; initial feedback/reviews/requests are empty. Local round-2 review is clean, after the
  retained first-round findings were fixed. Claude/Copilot CLI quotas remain unavailable.
- Keep this reviewed head unchanged while CI runs. Carry this uncommitted publication checkpoint
  into the next child only after exact-head CI and available feedback pass, saving a copy before
  restoring this parent journal. No merges authorized/performed.
- Next implementation: real legacy section/setup ownership and descriptive source context. Native
  abs source grouping is still PARTIAL despite its now-normalizable raw result stream. Existing
  556 cases (including 110 generic `main` labels) and all fixture/lock bytes remain protected until
  the manifest/coverage-loader migration can perform a coordinated method/folder/reference cutover.

## Section assembly start (2026-11-17)

- #4524 is green at febc1d9ad81b6b97448b0e3b85d08c93ef7ffbcb. All five required checks pass;
  conditional Dependabot automerge is skipped. No remote comments/reviews/pending requests or inline
  feedback. Exact-head proof /tmp/morphia-legacy-pr-final-status.json and
  /tmp/morphia-legacy-pr-final-comments.json. Thirteen PRs published, no merges authorized/performed.
- Created audits-section-assembly from that head after the gate. Copied the predecessor publication
  journal here and saved /tmp/morphia-legacy-publication-progress.md before restoring that parent
  journal. Parent is clean; this child has no implementation/commit/PR yet.
- Next scope is source ownership for standalone legacy sections, typed setup/command inspection,
  explicit reviewed layout policies, and authored section/tab/page titles for descriptive naming.
  Never infer a seed namespace/type from a printed sample or borrow the first sibling's state.
  Reference-only source and ambiguous semantics must remain visible independently of naming.
- Existing raw inputs/probes remain available: /tmp/morphia-audits-discovery-documents.json,
  /tmp/morphia-legacy-selected-pages.json, /tmp/morphia-abs-native-blocks.json and the pinned bundle
  /tmp/morphia-normalization-source-4jepdujh/bundle. The abs example is a concrete first target.
- CLI review quotas remain unavailable; the independent review agent is available for required
  design/code verification. Do not label that as completed Claude approval.

## Section assembly implementation checkpoint

- Opt-in SectionAssemblyPolicy now owns direct standalone section/tab code, with reviewed
  COMMANDS_THEN_RESULT and SOURCE_ONLY layouts. Internal closed-shell inspection classifies
  declarations/inserts/indexes separately from one final query/aggregate/update action. Public
  normalization retains NO_ACTION. No source rewriting, JavaScript execution or parser fallback.
- Explicit ordered shared setup selectors replace sibling inheritance and apply to standalone/native
  IO groups. Setup is retained at page level, including zero-consumer cases; replicated source and
  embedded programs share global budgets. Authored page/tab title recovery feeds existing naming.
- First independent design round found four valid gaps: foreign/missing local setup languages,
  setup selection outside example scopes, zero-consumer source retention, and missing native-IO
  tab name/context propagation. All have corrections and regressions. Notes
  /tmp/morphia-sections-design-review.md. Initial 335-test Critter pass preceded those corrections;
  expanded suite is running. No final review, commit or publication yet.
- New pinned probe plan/replay under audits/probes/section-assembly targets automatic abs ownership
  and two explicitly source-only acos tabs. Probe/receipts still need execution after the final
  build. Existing native IO names and physical corpus bytes must remain unchanged.

## Section assembly native proof and second review

- First full gate passed 343 tests with both mapper flags, six Python probe tests, root formatting,
  graph refresh, exact corpus reproduction and protected-path checks. New native report SHA
  34addd51d6425f843fccf603b77effb6729516cdcbd15bf3a7824b5d6d49fc20 (680,983 bytes):
  3 reference groups, zero unassigned, 1 normalized abs program / 2 commands / 4 rows, 2 source-only
  acos groups, 3 names: absExpressionOperator, inverseCosineOfValueInDegrees,
  inverseCosineOfValueInRadians. Abs semantic SHA matches the prior explicit reviewed program:
  ef4be2463922b87de2d8b6d421ca70dbba3682326959f303de77849fb2a78724.
- Prior IO probe retains 16 normalized references + 2 foreign groups and byte-identical 16 name
  proposals. New report SHA 3934078559427ad8e7e8220542231dc99fffc09178eb0a5f11c180e9eed41408,
  1,259,561 bytes (new normalizer/additive policy/page fields). Default-policy abs remains honestly
  unassigned, and its explicit printed-result program SHA is unchanged. Artifacts live under
  /tmp/morphia-section-assembly-{probe,io-probe,printed-probe}; source/build/probe/helper/classpath
  provenance verified by the second review. Native receipts still need copying after final gate.
- Round 2 found one valid medium issue: shared JS setup made language-only Python observations
  mixed/unresolved, blocking an otherwise healthy reference scope. Verified in compiled standalone
  and native IO cases. Fixed by classifying the owner's inputs independently, retaining strict
  local SETUP language checks, and omitting shared shell blocks from FOREIGN candidates. Declared
  published tails do not classify input language. Shared setup remains at page level. New regression
  covers standalone, IO, actual Python syntax and JSON-highlighted output.
- Current final gate is running sequential Critter/reflection/format/probes/graph with that correction.
  Expect 344 tests; do not claim final approval yet. Round 3 remains, then commit/push stacked PR14
  on #4524, request remote review and await exact-head CI before starting actual backport work.
- Claude weekly quota remains until Nov 19; Copilot availability check still reports monthly quota
  exceeded (/tmp/morphia-sections-copilot-availability.log). Independent review notes:
  /tmp/morphia-sections-{design-review,code-review2}.md. No merges authorized/performed.

## Final section assembly review correction

- Three broad local rounds are complete. Round 3 verified the foreign-classification correction and
  final receipts, then found a valid budget issue: role inspection still parsed shared source before
  discarding it from foreign observations. A bounded 500-owner/200k-literal probe demonstrated the
  repeated source work. Notes /tmp/morphia-sections-code-review3.md. No substantive finding rejected.
- Corrected the call order: one ownVariant helper classifies owner language/container evidence before
  assignment and again at emission. FOREIGN/UNRESOLVED standalone owners bypass semantic inspection.
  Shared setup is attached only to reference owners and charged before inspection; native IO charges
  retained setup before normalization. Foreign standalone blocks retain unassigned semantic roles.
- A final serialized verification gate is running. After it passes, refresh the three receipt copies,
  obtain focused completion verification (/tmp/morphia-sections-focused-verification.md), and publish
  PR14. Do not run a fourth broad review. Current compiled evidence is being regenerated; no commit,
  push or PR yet. Preserve the 556-case physical corpus and all lock/fixture bytes until backport.

## Section assembly final local gate

- Final suites pass: 344 tests with Critter and reflection, zero failures/errors and two optional
  integration skips each; six Python probe contract tests pass. Root formatting, graph refresh,
  exact legacy-corpus reproduction and protected-path checks pass. No core, fixture, lock,
  strict-comparator or accepted legacy-catalog bytes changed.
- Final schema-2 receipts are copied under audits/probes/section-assembly. Repeated native reports
  and names retain the identities above. The new plan produces 3 reference groups / 3 descriptive
  names / zero unassigned blocks, with one normalized abs program and two honest source-only acos
  observations. Prior sixteen names and explicit abs program remain byte-identical.
- Focused completion verification is clean after the maximum three broad rounds. It verifies the
  final call order, four compiled cases (including 500 foreign owners with a 200k shared literal),
  local missing/foreign setup rejection and all copied source/runtime/artifact identities. Notes
  /tmp/morphia-sections-focused-verification.md. All six substantive findings across the rounds
  were fixed; none rejected. No remaining actionable finding in scope.
- Predecessor #4524 remains OPEN at febc1d9ad81 with all five required exact-head checks successful;
  conditional Dependabot automerge skipped. Proof /tmp/morphia-sections-parent-status.json.
  Publish this branch as stacked PR14, request remote review, then check exact-head CI/feedback.
  Next scope is a complete physical binding/name migration manifest and descriptor-based coverage
  discovery before coordinated method/folder/reference renames. No merge authorization.

## Section assembly publication checkpoint

- PR #4525: https://github.com/MorphiaOrg/morphia/pull/4525
  branch audits-section-assembly, base audits-legacy-scenarios (#4524), head
  8259898a66e5558068f1a571fe1e1d80dc952cb5. Fourteen PRs published; no merges performed.
- Local reviews and final gate pass as recorded above. Requested remote review; initial feedback,
  reviews, pending requests and inline comments are empty. Exact-head CI is running. Proof files
  /tmp/morphia-sections-pr-{status,comments}.json. Keep this reviewed head unchanged during CI.
- Carry this uncommitted publication checkpoint into the next child only after required exact-head
  CI and available feedback pass. Save a copy before restoring this parent journal. Next work:
  complete binding/name migration manifest, manifest-based coverage discovery, then the coordinated
  descriptive method/folder/reference cutover with fixture/lock bytes preserved.

## Binding manifest start (2026-11-17)

- #4525 is green at 8259898a66e5558068f1a571fe1e1d80dc952cb5. All five required checks pass;
  conditional Dependabot automerge skipped. No remote comments, reviews, inline feedback or pending
  requests. Proof /tmp/morphia-sections-pr-final-{status,comments}.json. Fourteen PRs published;
  none merged by this task.
- Created audits-binding-manifest from that reviewed head, carried the publication checkpoint here,
  saved /tmp/morphia-sections-publication-progress.md and restored the clean parent journal.
- Next scope: a complete physical Java method/resource binding manifest and manifest-based core
  coverage discovery before coordinated descriptive method/folder/reference backport. Provisional
  investigation notes /tmp/morphia-binding-manifest-notes.md need design/code review before adoption.
- Existing discoverResourceName already supports general test-prefixed stems; TestCoverageExtension
  still scans only testExample* and silently returns for zero methods/no resource folder. The
  historical corpus inventories files/titles/markers but lacks actual method/body/options bindings.
  Replacing framework code changes files in its broad historical source list: preserve the original
  snapshot and every curated fixture/lock byte, recording intentional framework deltas separately.
- No implementation, commit or PR on this child yet. Available independent reviewer is idle;
  external CLI quotas remain as recorded above. Do not claim completed Claude approval.

## Physical binding investigation

- Read-only Javac AST probe /tmp/InspectLegacyBindings.java produced
  /tmp/morphia-legacy-java-bindings.json: 260 classes / 566 JUnit methods in the four operator packages.
  259 template classes / 565 methods; one independent TestCollectionStats method.
- Legacy-convention comparison finds 554 unique bound resource cases, no duplicate owners; two
  unbound marker-only cases: tsIncrement/example2 and tsSecond/example2 (awkward watch examples).
  Eleven template methods have no resource: ten manual cases plus empty TestPercentile#testExample5.
  Bound TestLastN#testExample5 is empty; TestFirstN#testExample5 disabled/missing DisplayName;
  disabled TestExpr#testExample2 display title differs from its name resource. Preserve and report
  these facts explicitly; do not infer behavioral coverage or manufacture missing tests.
- Initial slice design in design/operator-audits-binding-manifest.md is pending independent review.
  Current implementation has not started. Original historical catalog remains immutable; a later
  intentional framework-source delta must be distinguished from unchanged curated case bytes.

## Binding manifest implementation (Claude, 2026-11-20)

- Resumed after Codex quota exhaustion (Codex until 2026-11-21 14:51; Copilot monthly quota also
  exhausted). The /tmp investigation files were gone; the probe was rebuilt from source.
- Implemented `dev.morphia.audits.bindings.LegacyBindings` (javac parse-only import),
  `BindingsMainKt`, reviewed `audits/catalog/binding-policy.json`, evidence catalog
  `audits/catalog/legacy-bindings.json` and core descriptor
  `core/src/test/resources/dev/morphia/test/example-bindings.json`. Core `ExampleBindings` validates
  the closed descriptor; `TestCoverageExtension` and offline `ExampleBindingsTest` check membership in
  both directions; `discoverResourceName` resolves bound fixtures through the descriptor.
- Real import: 554 BOUND, 11 MANUAL, 1 INDEPENDENT over 260 classes; two unbound marker-only
  fixtures. 30 bound methods have empty bodies, 7 are disabled; recorded as evidence only.
- Gates: 5 LegacyBindingsTest + 3 ExampleBindingsTest pass; all four operator packages pass with
  Critter then reflection (569 tests, 0 failures, 7 disabled skips each). Mutations (extra fixture,
  removed class entry) fail ExampleBindingsTest. Fresh CorpusInventory capture equals the accepted
  catalog for examples/exclusions/auxiliary resources; only the intended framework sources changed.
- Next: descriptive method/folder/reference backport (step 3a). Survey: 111 generic `main`/missing
  titles, at most one per operator page, so the pinned page title (e.g. `$abs (expression operator)`
  at docs a5d960b9) supplies context; four duplicate-title groups plus two `[1]` meta titles need
  reviewed qualifiers. No title exceeds the 80-character stem cap.
- Local review: Codex and Copilot CLIs unavailable (quota); an independent review agent ran two
  rounds. Round 1 found three valid gaps (unbound fixtures not checked as marker-only, committed
  descriptor not tied to the reviewed policy, descriptor package list not pinned) plus two lows
  (default-locale lowercasing, late duplicate-owner detection); all fixed with regressions. Round 2
  verified the fixes and found the audits workflow did not trigger on core operator test/fixture
  changes; its path filter now includes them. No finding rejected.

## Binding manifest publication checkpoint

- PR #4529 (branch audits-binding-manifest, base #4525) is green on all five required checks; no
  remote comments or reviews (Copilot review requested; its quota appears exhausted). No merges.

## Descriptive-name backport (step 3a cutover, 2026-11-20)

- Branch audits-descriptive-names from #4529. `NameBackport` plans from the frozen
  legacy-bindings/legacy-corpus snapshots plus `name-backport-review.json` (110 pinned page titles at
  docs a5d960b9 for generic labels, 7 reviewed stems). Ledger `name-backport.json`: 556 renames
  (439 name-file titles, 110 page titles, 7 reviewed). `apply` moved 2450 fixture files as pure
  renames and edited only method declarations and "test data" Javadocs.
- Self-found defects before review: (a) a method without Javadoc took the previous method's Javadoc
  (fixed with an annotation/modifier-only check plus a verify rule that every "test data" line names
  its own method's fixture); (b) "main :: <tab>" titles produced testMain... names (generic section
  dropped). The tree was regenerated from a clean reset after each fix. Note: exec:java does not
  recompile; run `compile exec:java`.
- Gates: core operator packages 569 tests, 0 failures, 7 skips with Critter then reflection; audits
  357 tests (2 optional skips) with both mapper flags. Review round 1 (independent agent): ledger
  provenance not enforced, ledger not tied to the reviewed inputs, and no negative verify tests; all
  fixed. No `exampleN`/`testExampleN` remains in the operator packages.
- Next: steps 4 (lock/override migration) onward.
- Review round 2 verified all fixes; nothing significant remains. Publishing as stacked PR on #4529.

## Curated repair inventory (step 4 slice 1, 2026-11-20)

- PR #4530 published (stacked on #4529). Branch audits-curated-repairs adds `CuratedRepairs`,
  reviewed `repair-review.json` (182 distinct lock reasons in 7 categories, 17 exclusions in 5) and
  `curated-repairs.json`. 236 locked examples: 234 PRESERVED_CURATED, 2 NOT_EXECUTED; 320 UNCORRECTED.
  Assertion paths: 509 SERIALIZATION, 466 BEHAVIOR, 2 CUSTOM; 7 disabled and 26 empty bound tests.
- Self-found defect: a shadowed helper set made every method report all three helpers; fixed before
  committing the inventory, with a synthetic test per assertion path.
- Next: migrate the MORPHIA_FORM family through declared equivalences/overrides.
- Review round 1 (independent agent): disabled tests were counted as executing (fixed, DISABLED
  count), nine reasons misfiled by retirement rule (Morphia enum spellings -> MORPHIA_FORM; broken
  source and wrong documented values -> DOCS_ERROR), plus lows (ActionTestOptions workflow trigger,
  unsupported helper/non-literal option forms now fail). All fixed.
- Review round 2 verified the fixes; its one remaining doc overstatement was fixed (unchained skip calls now fail). Publishing stacked on #4530.

## Curated repairs publication checkpoint

- PR #4531 (branch audits-curated-repairs, base #4530) is green on all five required checks. A
  Copilot review was requested; none has arrived (quota). No merges.

## MORPHIA_FORM lock migration (step 4 slice 2, 2026-11-20)

- Branch audits-morphia-form from #4531. The pinned manual is not the fixtures' source (its `$cond`
  page now uses another dataset), and Morphia history has no pre-curation copy (fixture and lock were
  committed together), so each fixture's source is its `mongodb/docs` page as of the Morphia commit
  that created (`first`) or last regenerated (`last`) it. `fetch.py` acquires pages and tracked
  includes via `gh`; generated `/includes/extracts/` files are not tracked and stay as directives.
  The manual's later move to `content/manual/manual/source/` is handled; one page has no commit
  holding it at its `last` date and keeps its `first` source.
- `FormEquivalence` defines ten canonicalizing rules applied to both sides; `FormMigration` compares
  action (with rules), data, expected and index (exactly) with the strict comparator and requires the
  declared rules to be sufficient and each necessary. Review `form-review.json`: 43 declarations
  (39 `first`, 4 `last`; 3 need no rule) and 23 gaps (8 residual, 9 source, 4 fixture, 2 page).
  `form-migration.json` records source text, digests, page/include blob ids and retired lock text.
  Spot checks of page and include blob ids against GitHub matched.
- Applied: 43 lock files deleted; no other core file changed. Inventory: 43 OVERRIDDEN, 191
  PRESERVED_CURATED, 193 locked. NameBackport.verify accepts a lock as absent only when the migration
  retired it with the legacy-corpus digest.
- Self-found: BsonDocument.equals ignores key order, so rule tests compare with the strict
  comparator; the JSON reader reads `{$regex: "p", $options: "o"}` as a regex literal (harmless,
  both sides are canonicalized). `EXISTS_BOOLEAN` was drafted and dropped as unexercised.
- Gates: core operator packages 569 tests, 0 failures, 7 skips with Critter then reflection; audits
  377 tests (2 optional skips) with both mapper flags.
- Review round 1 (independent agent): all 43 retirements confirmed sound. Valid findings, all fixed:
  provenance was not checkable offline (pages now committed and re-extracted by the drift test;
  fetch.py verifies every blob id against GitHub), ALTERNATIVE_COMMANDS asserted an unchecked
  equivalence (now needs a reviewed justification), SINGLE_ARGUMENT_ARRAY reached query `$size`,
  `$literal` content was canonicalized, and untracked includes were indistinguishable from failed
  downloads (now recorded as unexpandedIncludes).
- Review round 2 verified all fixes and flagged that the committed mongodb/docs pages are CC BY-NC-SA
  3.0, unsuitable for this Apache-2.0 repository. The pages were removed; the reviewed
  form-review.json now pins every page and include blob id, capture fails on a mismatch, and the
  networked recheck (fetch.py pages + FormMainKt capture) reproduces the committed report.
- Maintainer decision (2026-11-20): the documentation pages are not committed.

## Multi-collection data in the form migration (step 4 slice 3, 2026-11-20)

- Branch audits-form-collections from #4532. The repair inventory now records each bound test's
  literal `loadData("<collection>", N)` calls as `collections` (11 tests); other call forms fail the
  inventory. The form migration groups documented data by collection, compares the action's with
  `data.json` and every other one exactly with the data file bound to that collection name, and
  records the bindings in each override; verify fails if the inventory's change.
- Three fixtures kept as "data for several collections" now reproduce from their recorded pages
  (`$graphLookup` across collections, two `$lookup` subqueries) under `NAMED_ARGUMENTS`, one also
  under `PROJECT_ID_FIRST`; their locks are retired. Totals: 46 overrides, 20 unresolved; inventory
  46 OVERRIDDEN, 188 PRESERVED_CURATED, 190 locked. No other gap changed on recapture.

- PR #4533 (branch audits-form-collections, base #4532) published. Maintainer direction
  (2026-11-20): continue the plan PR after PR without pausing for review between them; never merge.

## NUMERIC_TYPE lock migration (step 4 slice 4, 2026-11-20)

- Branch audits-numeric-types from #4533. `NumericRetype` declares that every int32/int64/double of
  one type under a pointer of one file becomes another type at exactly the same value; Decimal128 is
  excluded. Retypes are discovered from numeric TYPE differences, merged per file and type change
  into the deepest covering pointer when that still reproduces exactly, and must each be necessary.
- Lock categories (from the inventory, now passed as `FixtureFacts` with the collections) gate the
  override: rules need MORPHIA_FORM, retypes NUMERIC_TYPE. Recapturing the existing 46 overrides
  and 20 gaps gave identical results.
- `fetch.py candidates` takes optional categories. 24 NUMERIC_TYPE locks without other families:
  16 retired (one, `$exp`, exact; 15 int32->double retypes in geometry, `$mod`, `$nearSphere`,
  `$linearFill` and `$percentile`), 8 kept (`$indexOfBytes`/`$indexOfCP` document strings, `$mul`
  uses Decimal128, source/page/fixture gaps). Totals: 62 overrides, 28 unresolved; inventory 62
  OVERRIDDEN, 172 PRESERVED_CURATED, 174 locked.

- PR #4534 (branch audits-numeric-types, base #4533) published.

## Reviewed corrections (step 4 slice 5, 2026-11-20)

- Branch audits-reviewed-corrections from #4534. `FormCorrection(file, pointer, documented,
  curated)` replaces a documented value pinned in extended JSON; it is stale when the page no longer
  documents exactly that value. Discovered after rules and retypes, one field at a time (replace,
  remove, or add in place); never a whole file or top-level row/stage, never key order, never a
  same-value type change; rules are never traded for corrections.
  Only DOCS_ERROR/FIXTURE_CURATION locks may declare them.
- `fetch.py candidates` falls back to the parent of a commit that removed the page (the manual
  move left a month without one) and to the first commit adding a page the fixture predates
  (`$toUUID`). The one `lockd` marker is handled; declarations record the marker name.
- 52 locks with DOCS_ERROR or FIXTURE_CURATION and no other family besides MORPHIA_FORM and
  NUMERIC_TYPE: 29 retired (11 need no correction, 18 corrections each read against the lock
  reason), 23 kept. Totals: 91 overrides, 51 unresolved; inventory 91 OVERRIDDEN, 143
  PRESERVED_CURATED, 145 locked. Recapturing the earlier 62 overrides and 28 gaps was identical.
- Review round 1 (independent agent): corrections taken at a parent document swallowed sibling
  edits and, on one-row files, replaced the whole file (`$degreesToRadians` hid an `_id` removal);
  fixed with field-level remove/add corrections and a row/stage floor. The 11 exact retirements were
  misattributed to later upstream fixes (their pinned pages predate the fixtures); corrected. The
  `$toUUID` source after the fixture's origin is now explicit (`SourceRelation.AFTER_ORIGIN`). Type-
  only and key-order differences are never corrections. Totals unchanged.

- PR #4535 (branch audits-reviewed-corrections, base #4534) published.

## Natively handled extraction locks (step 4 slice 6, 2026-11-20)

- Branch audits-native-extraction from #4535. No code change: `EXTRACTION` and `SHELL_SYNTAX`
  allow no declared change, so a lock retires only when the current pipeline reproduces the
  fixture exactly (plus what its other categories allow). 92 such locks: 25 retired (10 were wrong
  example names the curated name file now selects), 67 kept with gaps. Every locked example except the two UNTESTABLE
  ones is now in the review: 116 overrides, 118 unresolved; inventory 116 OVERRIDDEN, 118
  PRESERVED_CURATED, 120 locked.
- Remaining gaps by cause: 23 generated include extracts, 16 `Date`/typed constructors, 14
  unparseable printed results, 7 JavaScript variable bindings, 28 residual edits.

- Review round 1 (independent agent): layout-only locks (unwrapped arrays, one document per line)
  passed by construction because the comparison parses whole files; every data/expected/index file
  must now also read one document per line as the Java tests read it (all 116 overrides do; no gap
  changed). The wrong-name count (10, not 13) and its attribution were corrected.

- PR #4536 (branch audits-native-extraction, base #4535) published.

## Generated Giza extracts (step 4 slice 7, 2026-11-20)

- Branch audits-giza-extracts from #4536. `GizaExtracts` assembles untracked
  `/includes/extracts/<ref>.rst` targets from `extracts*.yaml` (`ref`, `content`, `inherit`/`source`,
  `replacement`; the child's replacements override inherited ones). Each YAML file used is pinned as
  an include. Includes now match with any whitespace after `include::` (two pages use two spaces).
- `fetch.py` fetches the extracts YAML through GraphQL tree listings and batched blob queries, and
  keeps a verified-blob cache (`<pages>/.verified.json`, absence cached too) so reruns do not spend
  the REST quota, which a full recheck had exhausted.
- Redraft of every reviewed fixture: no existing declaration changed beyond include pins (21 pages
  now splice extracts outside their examples). All 23 `PAGE_UNSUPPORTED` gaps are gone: 7 retire
  (3 EXTRACTION, 3 DOCS_ERROR with no correction, `$near` with an INT32→DOUBLE retype), 16 report
  the gap behind the extract.
- New `REJECTED` gap for a reviewer-rejected draft: `$regexFind`'s email example reproduces only by
  correcting every email, because the extractor reads the page's intermediate `$addFields` result
  rather than the final `$set` one; the lock does not describe that. `capture` accepts a rejection
  only while the fixture reproduces and the reason is not blank.
- Totals: 123 overrides, 111 unresolved; inventory 123 OVERRIDDEN, 111 PRESERVED_CURATED, 113
  locked.
- Review round 1 (independent agent): extracts with fields Giza renders beyond `content` (`title`,
  `style`, `only`, `pre`, `post`, `append`) and refs defined twice in one file now stay unexpanded
  (one page's include pins changed, no gap); `fetch.py` cached any `gh` failure as a permanent
  absence, so it now caches only a 404. All 228 cached absences were purged and the pages
  refetched: 125 were confirmed 404s, and the other 103 keys are no longer requested by the
  review (a recapture changed nothing). The doc's breakdown of the former page gaps was off by one.
  Rejected: pinning every `extracts*.yaml` blob, since the tree comes from an unpaginated GraphQL
  listing and every fetch error now stops the run.
- Review round 2: nothing new.

- PR #4537 (branch audits-giza-extracts, base #4536) published.

## Shell dates and the tested action kind (step 4 slice 8, 2026-11-20)

- Branch audits-date-constructors from #4537. `ISODate` now reads an omitted time as midnight and
  an omitted offset as UTC, and accepts `±HHMM`, as mongosh's `shell-bson.ts` (checked) and the
  legacy `types.js` do; `new Date` accepts a date-only string as UTC midnight (ECMAScript). Local
  `new Date` date-times, compact, minute-only and hour-only-offset forms stay unsupported.
- Redrafting exposed that the fixture's action kind was taken from the page, so the shape check
  could never fail: `$dateAdd`'s "Filter on a Date Range" matched a page block of `updateOne`
  calls and drafted corrections turning the update documents into its `$match`/`$project`.
  `FixtureFacts` now carries the test's helpers, and the documented action must be the kind the
  test runs; a test without exactly one helper is `FIXTURE_UNSUPPORTED` (`$maxDistance`, whose
  test is custom). No existing declaration changed.
- Of the 16 `CONSTRUCTOR_FORMAT` gaps, 5 retire (2 `MORPHIA_FORM` argument orders, 2
  `FIXTURE_CURATION`, 1 `DOCS_ERROR`); the rest show the gap behind the date. One stays: an
  impossible `ISODate("2011-06-31")`.
- Totals: 128 overrides, 106 unresolved; inventory 128 OVERRIDDEN, 106 PRESERVED_CURATED, 108
  locked.
- Review round 1 (independent agent): `ISODate` accepted an offset after a bare date
  (`"2020-12-31Z"`), which both shells reject because their offset group sits inside the time
  group; the offset now needs a time (recapture unchanged). The doc's breakdown of the 11 blocked
  fixtures that did not retire was corrected.
- Review round 2: nothing new.

- PR #4538 (branch audits-date-constructors, base #4537) published.

## Printed results (step 4 slice 9, 2026-11-21)

- Branch audits-printed-results from #4538. Of the 14 expected blocks the document reader rejected,
  7 are mongosh output (`Decimal128(...)`, `Timestamp({ t, i })`, `Long(...)`, comments) and 7 are
  not results (3 bit diagrams, SQL, a malformed page, 2 elided plan-cache outputs).
- An unreadable expected block is now read with the existing `SHELL_RESULTS` format, owned by a
  `find` on the action's collection, and the fixture is checked with `PublishedComparison`
  (Int32-or-Double for printed whole numbers, exact `Decimal128` text). No retype or correction
  applies to printed results.
- Two locks retire (`$tsSecond`, SHELL_SYNTAX; `$mul` mixed types, EXTRACTION). Three printed
  results really differ (`Decimal128` values turned into numbers or rescaled), and two now show an
  earlier difference in data or action.
- Totals: 130 overrides, 104 unresolved; inventory 130 OVERRIDDEN, 104 PRESERVED_CURATED, 106
  locked.
- Review round 1 (independent agent): a `NUMERIC_TYPE` lock that only turned `4` into `4.0` in a
  printed result would have passed, since printed whole numbers accept either type; fixtures filed
  as `NUMERIC_TYPE` are now barred from the printed path (no current gap or retirement changes).
- Review round 2: the guard applies only to a match, so a real printed difference keeps its gap; nothing new.

- PR #4539 (branch audits-printed-results, base #4538) published.

## Implicit globals (step 4 slice 10, 2026-11-21)

- Branch audits-js-bindings from #4539. The nine `ASSIGNMENT_BINDING` gaps were pages that assign
  pipeline stages to undeclared names, which both shells accept in sloppy mode as mutable globals.
  `ShellPrograms` now treats a statement-level assignment to an unbound, unreserved name that way;
  assigning a `const` and redeclaring the name lexically stay rejected.
- Eight `SHELL_SYNTAX` locks ("inlined js variables to pipeline") retire exactly; `$convert` now
  shows a real printed `Decimal128("0")` against the fixture's `0.0`.
- Totals: 138 overrides, 96 unresolved; inventory 138 OVERRIDDEN, 96 PRESERVED_CURATED, 98
  locked.
- Review round 1 (independent agent): nothing to fix. The binding fires only for a top-level `name = ...` statement; the model is stricter than JavaScript where a lexical or `var` redeclaration follows (only adds gaps).

- PR #4540 (branch audits-js-bindings, base #4539) published. Step 4's exit criterion holds: every
  locked example has a disposition (138 retired, 96 kept with a recorded gap).

## Step 5 design (2026-11-21)

- Branch audits-test-generation-design from #4540: `design/operator-audits-test-generation.md`.
- Measured against a 2025 `mongodb/docs` checkout: about 550 documented operator examples, about
  521 owned by legacy cases, about 29 unowned (mostly environment-bound admin stages and the
  positional update operators). Generating only for unowned examples would cover almost nothing, so
  legacy cases join a new runner through a compatibility adapter, with their documented sources
  (form-migration overrides, pinned pages) as independent references.
- Three review rounds (independent agent): identity contract; slice 1 restricted to exact
  expectations; scenario and descriptor fields; separate databases for behavior parity;
  composition on `TestBase`; completeness test; candidate attestation and rollback; curated BSON
  never treated as a reference (`CURATED_REFERENCE`); converted scenarios replace legacy files and
  are manifest-owned; a migrated-fixture ledger for the audits checks that guard fixture bytes;
  harness collection-name corrections reverted; form rules and printed constraints ported before
  cutover.

- PR #4541 (branch audits-test-generation-design, base #4540) published.

## Step 5 slice 1: operator case runner (2026-11-21)

- Branch audits-case-runner from #4541. Core: `OperatorCase` (descriptor: absolute scenario
  resource), `OperatorCaseRunner` (composed on `TestBase`) and `StrictBson`. The runner checks
  serialization (strict BSON against the documented pipeline), behavior (documented pipeline
  through the driver against the Morphia operation, in two separately seeded databases, results
  and every collection after the action) and the published documents, and fails on any scenario
  feature it does not support yet (rules, ignored fields, tolerances, datasets, indexes, server
  requirements).
- `$round`'s example moved: `FormMainKt migrate` wrote its `scenario.json` from the accepted
  override, deleted the legacy fixture and recorded the move (legacy method and body digest,
  removed file digests including the retired lock, the override itself, scenario digest, new
  owner) in `audits/catalog/fixture-migration.json`. Its body moved verbatim into
  `dev.morphia.test.generated.expressions.TestRound`. The binding drift test projects the frozen
  import through the rename ledger and then the migration ledger; the frozen-corpus check accepts
  the removed files by digest; `FixtureMigration.verify` checks the legacy directory is gone, the
  scenario is exactly its override's documented source and the owning test exists.
- Golden vectors (`generated/vectors/strict.json`) run through both `StrictBson` and the audits
  `BsonComparison`; both agree on every vector.
- The form report and the repair review drop `$round` (its lock reason was its own); inventory
  555 examples, 137 OVERRIDDEN, 98 locked.
- Review round 1 (independent agent):
  - The scenario said `UNORDERED`, but `ActionTestOptions.orderMatters` defaults to true, so the
    legacy test compared in order; the ledger now records the legacy ordering and `verify`
    regenerates the scenario from it (the migration was redone with the fixed tool).
  - The source pins lived only in the ledger's copy of the override; the declaration now stays in
    `form-review.json` under `migrated`, and the ledger's override must match it.
  - The runner now also fails closed on a topology other than standalone, an order other than
    `ORDERED`/`UNORDERED`, action fields beyond kind/collection/pipeline, and stages naming
    another database (a self-test scenario under `generated/fixtures/runner/`).
  - Checked: database names are per test class, so `<db>_reference` cannot collide across
    concurrently running classes. Rejected: comparing the moved lambda with the legacy body text,
    since the call around it necessarily changes and the move is a reviewed diff.
- Review round 2: nothing to fix. Deferred to slice 3: a migrated case's `ordered` flag is anchored
  only in the ledger (the frozen catalog pins the legacy body's hash, not its text). The adapter
  slice reads legacy options from source and will store the hash-checked body text so the flag can
  be re-derived offline.

- PR #4542 (branch audits-case-runner, base #4541) published.

## Step 5 slice 2: case manifest and completeness (2026-11-21)

- Branch audits-case-manifest from #4542. `core/src/test/resources/dev/morphia/test/generated/
  manifest.json` lists every case of the generated namespace (name, kind, scenario, test) and every
  file it owns with its SHA-256, as `GENERATED` (scenarios) or `PROTECTED` (moved handwritten
  bodies). `FormMainKt manifest` captures it from the migration ledger.
- Core `GeneratedCases.verifyAll` (offline, `GeneratedCasesTest`) checks both directions: every
  listed case has its `@Test` and a scenario of the same name, every owned file exists with its
  digest, and every generated source, `@Test` and scenario (runner self-tests aside) is listed.
- Audits: the committed manifest must equal a fresh capture, and `CaseManifests.owners` fails if an
  example has more than one owner across the legacy bindings and the manifest.
- `SOURCE_REMOVED` moves to the adapter slice and `AMBIGUOUS`/`UNNAMED` to the generator slice.
- Review round 1 (independent agent): the walk now covers every file under the generated roots
  (the vectors and runner self-tests are `PROTECTED` files, only the manifest is exempt), any JUnit
  test kind through `@Testable` and nested classes; the owners check folds case; the design's
  manifest section matches the schema.
- Review round 2: `getMethods()` missed package-private tests and `isTest` accepted only declared
  `@Test` methods; both now use one scan of declared methods up the hierarchy, of any JUnit kind.
  The owners check folds case with `Locale.ROOT`.

- PR #4543 (branch audits-case-manifest, base #4542) published.

## Step 5 slice 3: compatibility adapter (2026-11-21)

- Branch audits-case-adapter from #4543. `TemplatedTestBase.testPipeline` runs a converted case
  (its fixture directory holds only `scenario.json`) through `OperatorCaseRunner` with its
  unchanged lambda, after checking the test's options against the scenario (order, removeIds and
  skips, version requirements); `testQuery`/`testUpdate` refuse a converted fixture until they are
  supported.
- `FormMainKt convert` converts a case in place (kind `CONVERTED`): the ledger keeps the legacy
  method, the hash-checked legacy body text (now also backfilled for `$round`, closing slice 1's
  deferred item) and the result order re-derived from it. `trunc/truncExpressionOperator` is the
  first converted case; it passes all three checks under both mappers.
- The manifest lists converted cases (scenario `GENERATED` at its legacy path, test owned by the
  binding manifest); the owners check treats the legacy binding and its converted case as one
  owner; core's completeness walk covers `scenario.json` files in legacy fixture directories.
  `FixtureMigration.retiredLocks` (shared by `RepairsMain` and its test) keeps a converted case's
  retired lock classifying it in the inventory.
- Totals: form report 136 overrides (two cases moved), inventory 555 examples, 137 OVERRIDDEN, 98
  locked.
- Review round 1 (independent agent): converting deleted the `name` file, so a converted case no
  longer checked its `@DisplayName`. Scenarios now carry the documented example's `title`, and
  `GeneratedCases` checks every case's `@DisplayName` against it offline (both scenarios were
  regenerated through `FixtureMigration.scenario`, with their ledger digests). The ordering regex
  only recognizes a literal `orderMatters(false)`, but fails closed: `convert` cross-checks the
  inventory and the runner checks the real options at run time.
- Review round 2: nothing new.

- PR #4544 (branch audits-case-adapter, base #4543) published.

## Step 5 slice 4a: form rules in core (2026-11-21)

- Branch audits-case-rules from #4544. `FormRules` ports `FormEquivalence` to Java (every rewriting
  rule; `ALTERNATIVE_COMMANDS` selects a command, so the runner still refuses it). Golden vectors
  (`generated/vectors/rules.json`, 25 cases with positives and negatives, expected outputs produced
  by the Kotlin rules) run through both ports; the audits test also requires every rewriting rule
  to have a vector. Expected outputs are stored as extended JSON text, because a
  `{$regex, $options}` document would be read back as a regular expression.
- The runner compares serialization after applying the scenario's rules to both sides, and
  `FixtureMigration.scenario` writes an override's rules. `cond/condExpressionOperator`
  (`COND_ARRAY`) is converted and passes under both mappers.
- Totals: form report 135 overrides; inventory 555 examples, 137 OVERRIDDEN, 98 locked.
- Review round 1: the port is equivalent and the runner and emission changes are sound. Low
  findings, all missing vectors, were added: a string pattern with unsorted options, field
  operators inside `$or`/`$and`/`$nor`, `$eq` and a regex through a pipeline `$match`, a
  `$unionWith` pipeline, projection flags in `$project`, and `{f: {$eq: {}}}`.
- Review round 2 (medium): the string-pattern vector never reached the string branch, because the
  JSON reader turns `{$regex, $options}` into a regular expression, which also sorts its options.
  Vector inputs now spell such keys with `#` (`{"#regex": "p", "#options": "mi"}`), and both
  harnesses and the generator read `#` as `$`. The string branch and option sorting are now
  covered in both ports.
- Review round 3: nothing new.

- PR #4545 (branch audits-case-rules, base #4544) published.

## Step 5 slice 4b: printed constraints in core (2026-11-21)

- Branch audits-printed-constraints from #4545. `PublishedEncoding` writes published constraints
  as one-key documents naming their kind; `scenario.json` carries them as `expected.printed`.
  `FormMigration.documented` reads results the page prints as mongosh prints them into those
  constraints, using the same normalization as the examination (`published`, split out of
  `printed`).
- `PrintedResults` ports `PublishedComparison` to core over that encoding, with
  `StrictBson` for exact scalars and a maximum matching for unordered results. Golden vectors
  (`generated/vectors/printed.json`, 30 cases, verdicts produced by the Kotlin comparison) run
  through both; the audits test also requires every constraint kind and every verdict to appear
  and the encoding to round-trip. The Java port checks constraints, not the work and depth limits
  or the actual-side BSON validation, which guard the audits against untrusted input; scenarios
  are generated and reviewed.
- The runner checks both results against `expected.printed`; a runner self-test
  (`runner/printedMismatch`) shows a Long failing a printed `4`.
- `toDecimal/toDecimalExpressionOperator` (printed `Decimal128("50")`) is converted and passes
  under both mappers. Form report: 134 overrides.
- Review round 1: the port reaches the Kotlin verdicts, and the printed-or-documents choice, the
  runner wiring and the toDecimal constraints are right. Low findings applied: `StrictBson`
  compares doubles by raw bits as the audits comparator does (NaN payloads differ; extended JSON
  cannot spell one, so `StrictBsonTest` asserts it directly), the `PrintedResults` Javadoc states
  what it does not validate, and the runner requires exactly one of `documents`, `printed` and
  `none`. Rejected: the claim that Kotlin accepts a non-scalar `exact` or non-canonical `decimal`;
  its constructors refuse both, and the vectors show INVALID on both sides.
- Review round 2: nothing new.
