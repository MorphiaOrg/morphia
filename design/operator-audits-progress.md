# Operator audit implementation progress

Current branch: `audits-bson-comparison`; PR base: `audits-capability-normalization` (#4518).
Stack (PR head branches): #4512 `audits-pipeline-design` → #4513 `audits-source-inventory` →
#4514 `audits-coverage-contracts` → #4515 `audits-example-identities` → #4516 `audits-pinned-sources` →
#4517 `audits-snooty-adapter` → #4518 `audits-capability-normalization` → BSON comparison (in progress).
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

## Next steps

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
