# Standalone section ownership and shared setup

The native extractor now has an opt-in section assembly contract. Native input/output ownership
remains the default. A page rule can declare `sectionAssembly` with `layout`, ordered
`sharedSetupSections`, `scenarioTabs`, and `additionalShellLanguages`. These are reviewed policy,
not choices inferred from a candidate's current block count or successful parser retries.
The normalizer is `native-sections:1`; extraction plans and report schemas remain version 1 with
additive fields. Old plans decode with section assembly disabled.

## Ownership and roles

Each selected section owns its direct standalone code, retaining the authored order through
transparent containers. Nested sections and tab directives start separate owners. Native IO
parents keep their explicit ownership; their code is never duplicated into a standalone owner.
All source text, report-local locators, attributes, ancestors, snippet receipts and physical
origins remain visible. Locators and block indexes do not establish persistent identity.

`COMMANDS_THEN_RESULT` declares a single trailing published result. The preceding blocks must
parse together with the existing closed shell processor. The processor's internal inspection
mode permits declaration-only input and returns operation classifications, not an executable
artifact. The public normalizer still rejects programs with no action. No JavaScript is evaluated,
no fake query is appended, and no source is rewritten.

Under this contract, insert/index operations and pure declarations are setup. Exactly one
find/aggregate/update command must follow setup, in its own final input block. Mixed setup/action
blocks, multiple actions, unsupported input and missing command/result pairs retain their owned
source and a normalization reason. The final result uses the explicit `SHELL_RESULTS` grammar;
a malformed or unrepresentable published result remains unsupported in the program report.
No last-action heuristic assigns an ambiguous expectation.

`SOURCE_ONLY` owns source for descriptive naming while leaving local roles unassigned and the
program absent. It is useful for pages that display a sample document, an expression or a dataset
without defining executable setup. A displayed sample never becomes an insert with an inferred
namespace or input BSON type. Source-only and unsupported semantic states remain visible separately
from source grouping completeness. `READY` means complete source grouping for naming, not execution,
serialization parity, setup sufficiency or API coverage.

## Shared setup

`sharedSetupSections` lists exact authored anchors in dependency order. Each selector must resolve
to exactly one direct standalone owner. It selects that section's direct code even outside the
example selection; it does not implicitly select that section's descendants. Setup selectors must
not be excluded. Missing/duplicate ownership, unresolved languages/selectors and non-setup commands
block promotion and complete naming. Selected raw source remains visible when validation fails.

The combined shared source must parse as a closed declaration/insert/index program. Verified setup
is retained in the page's `sharedSetup` field even if every consumer disappears. It is prepended,
in the declared order, to standalone and native IO reference candidates. The module never borrows
state from the first sibling or treats section ancestry as proof of a shared dependency.
Every emitted setup occurrence is charged against the source budget before normalization. Embedded
program bytes share the report byte cap, which is checked incrementally before retaining another
program. The final report cap also covers provenance and repeated containers. Large inputs still
need explicit parser/extraction batches.

Owner language/container classification precedes semantic role inspection. Foreign/unresolved
standalone owners bypass that inspection; shared source cannot be reparsed repeatedly for
observations that later discard it. Reference copies are charged before inspection.

All setup/action languages participate in reference classification, including local setup. Unknown
or foreign setup cannot be parsed as reference shell merely because its text resembles JavaScript.
`additionalShellLanguages` permits explicitly reviewed legacy `bash`/`sh` highlighting labels;
this neither invokes a shell nor broadens the closed parser. Data formats cannot be declared foreign
programming languages. Unknown containers and undeclared tabs remain unresolved.

## Authored names

`scenarioTabs` declares tab IDs that represent separate scenarios rather than language variants.
Those IDs must be disjoint from reference/foreign language-tab policies. Authored tab arguments
supply display titles when the nearest section heading is generic. A descriptive nested heading
supplies the primary title with the tab title available as qualifying context. This works for both
standalone and native IO groups.

A generic standalone owner such as “Example” can otherwise use a descriptive authored ancestor,
including the operator page title. The shared naming function turns these display titles into
legal Java/folder identifiers; the existing registry persists them. Stable authored tab IDs extend
the enclosing identity context. Reordering sibling sections/tabs leaves naming evidence unchanged.
Ambiguous identities or names still require reviewed reconciliation; no ordinal resolves them.
The full scenario fingerprint remains absent until dataset/setup/constraint closure is established.

## Pinned probe and limits of this slice

`audits/probes/section-assembly/replay.py` uses the existing pinned source bundle and owned native
parse. It verifies that the three `$abs` blocks acquire setup/action/expected roles automatically,
with the same source digests and physical origins as the preceding reviewed raw-source experiment.
Its program retains two commands and four published rows. The two `$acos` scenario tabs retain
three raw blocks each, obtain authored descriptive names, and remain explicitly source-only:
those docs show a sample triangle document without inserting it.

The probe repeats extraction byte-for-byte, proposes three names in synthetic fixture destinations,
and verifies source/runtime inputs before and after replay. The earlier native IO probe verifies
its sixteen existing name proposals remain byte-identical. Neither probe is a physical core name
inventory or an execution attestation.

Existing generated tests, resources, locks and curated data remain unchanged. The next migration
must build a complete old-method/resource/name manifest, update coverage/resource discovery, and
coordinate method/folder/reference changes while proving fixture bytes and both mapper behaviors
are preserved. Additional layouts, external dataset contracts and action ownership with multiple
commands require explicit future contracts rather than broadening this one implicitly.
