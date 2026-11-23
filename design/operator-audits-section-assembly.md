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

`COMMANDS_THEN_RESULT` classifies each block a section owns by what the closed shell processor
makes of it, read together with the setup accepted before it. The processor's internal inspection
mode permits declaration-only input and returns operation classifications, not an executable
artifact. No JavaScript is evaluated, no fake query is appended, and no source is rewritten.

| Block | Role |
|---|---|
| Inserts, index builds and pure declarations | Setup for the actions after it |
| One find, aggregate or update command | An action: its own scenario |
| A document or document list directly after a read action | That action's published result |
| A document or document list where nothing the scenario runs inserts data | A seed for the next action's collection (`LEGACY_DOCUMENTS` setup in that namespace) |
| Anything else | Skipped, with a reason |

Skipped source stays visible on its scenario (`skipped`) or, for shared setup, on the page
(`sharedSetupSkipped`). The reasons are:

- `SYNTAX_TEMPLATE`: the block names operands in angle brackets, `{ $add: [ <expression1>, ... ] }`;
- `SAMPLE_DOCUMENT`: a displayed document where the scenario already inserts its data;
- `OPERATOR_EXPRESSION`: a displayed expression such as `{ $regexMatch: { ... } }`, which is neither data nor a result;
- `UPDATE_RESULT`: the documents a page prints after an update, which come from a read it runs
  next, not from the update; they wait for the update's own read;
- `UNREADABLE_RESULT`: a printed result the result grammar rejects, such as a truncated `...`
  listing; the scenario keeps its action and runs serialization-only;
- `SEVERAL_COMMANDS`: a block holding more than one action;
- `MIXED_SECTION_ROLES`: a block holding setup and an action together, which one role can't describe;
- `TRAILING_SETUP`: setup or a seed no action follows;
- `UNSUPPORTED_SOURCE`: anything else the processor can't read, such as a binding used before it is
  declared.

Each action becomes its own scenario with the setup and candidate seeds before it; earlier actions
are other scenarios. A read that follows an update in the same section depends on that update, so it
reports `AFTER_EARLIER_WRITE` and has no program of its own; pairing an update with the read a
page runs after it is the next step for update examples. Seeds are settled per scenario against the setup it actually runs, shared and
local, for section-owned and native input/output groups alike: where that setup inserts, the
documents only illustrate (`SAMPLE_DOCUMENT`); otherwise they seed the action's collection. The
language check ignores documents and printed results, so the action's own label always counts. An action without a result directly after it has no published expectation. A section
with no action reports `MISSING_COMMAND_OR_RESULT`. The final result uses the explicit
`SHELL_RESULTS` grammar; JSON-labeled and unlabeled results are accepted, since that is how pages
print them. Code in another programming language never reads as a document, even when it opens with
a bracket (a C# `[BsonIgnoreExtraElements]`).

These rules replaced a stricter contract that accepted only one trailing command and one trailing
result per section and refused to seed from displayed documents. That contract left most operator
pages without a runnable scenario; the rules above recover them while recording every block they
leave out.

`SOURCE_ONLY` owns source for descriptive naming while leaving local roles unassigned and the
program absent. It is useful for pages that display a sample document, an expression or a dataset
without defining executable setup. Under `SOURCE_ONLY` a displayed sample never becomes an insert. Source-only and unsupported semantic states remain visible separately
from source grouping completeness. `READY` means complete source grouping for naming, not execution,
serialization parity, setup sufficiency or API coverage.

## Shared setup

`sharedSetupSections` lists exact authored anchors in dependency order. Each selector must resolve
to exactly one direct standalone owner. It selects that section's direct code even outside the
example selection; it does not implicitly select that section's descendants. Setup selectors must
not be excluded. Missing/duplicate ownership, unresolved languages/selectors and non-setup commands
block promotion and complete naming. Selected raw source remains visible when validation fails.

Tabs and `selected-content` branches start separate owners; a selection naming a reference tab
reads as reference, one naming a foreign tab as foreign. A shared section with one branch per
language shares its reference branch, and shares nothing when only other languages show code.
The shared source contributes only its setup blocks, classified as above; its commands, templates and unreadable blocks are skipped with their reasons. Where it inserts nothing, its documents seed the collection of each action that consumes them. Verified setup
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
those docs show a sample triangle document without inserting it. (The probe records the earlier, stricter contract; under the current rules that sample seeds the `trigonometry` collection.)

The probe repeats extraction byte-for-byte, proposes three names in synthetic fixture destinations,
and verifies source/runtime inputs before and after replay. The earlier native IO probe verifies
its sixteen existing name proposals remain byte-identical. Neither probe is a physical core name
inventory or an execution attestation.

Existing generated tests, resources, locks and curated data remain unchanged. The next migration
must build a complete old-method/resource/name manifest, update coverage/resource discovery, and
coordinate method/folder/reference changes while proving fixture bytes and both mapper behaviors
are preserved. Additional layouts, external dataset contracts and action ownership with multiple
commands require explicit future contracts rather than broadening this one implicitly.
