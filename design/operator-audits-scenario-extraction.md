# Native documentation scenario extraction

The ordered-program normalizer accepts explicit block roles. This stage obtains those roles from
owned documentation structure. It selects reviewed page/section scopes and projects native
`io-code-block` parents into source-preserving input/output groups, then connects reference groups
to the program model and persisted descriptive-name reconciliation. It never pairs neighboring
blocks or assumes two language variants are equivalent because their titles match.

## Source policy and ownership

`ScenarioExtractionPlan` schema 1 declares a catalog pin and page rules. Each rule binds a capability
scope and physical destination to selected/excluded AST heading anchors, an independent minimum
reference-group count, exact reference/foreign language and tab labels, shell dialect and result
order. These decisions are explicit transport fields. A plan is reviewed policy, not an accepted
baseline automatically inferred from the current candidate's counts. Capability assignments and
the catalog pin are caller-supplied declarations; this stage does not establish API support.

The coordinator checks the source bundle, document input pin, owned parser revision and origins.
An owned parse is candidate evidence tied to those inputs; checking its input pin does not
independently authenticate every parsed byte. Retain the parser artifact and source bundle for
reproduction/review. This command does not promote or hydrate an accepted executable artifact.

Only explicit native IO ownership assigns action/expected roles automatically. The group retains
raw code, attributes, ancestor containers, heading titles/anchors, direct labels, include origins
and snippet references/range options. Explicit std labels bind to their authored following sibling.
A section label is identity evidence only when that leaf section owns one group. Broader labels
cannot become competing identities for every example in the section.

A hosting RST directive origin and a referenced snippet's physical origin are different facts.
The latter is resolved offline through the verified bundle, including its symlinks, and matched
to the page's dependency receipt. Snippet identity includes its logical resource and authored
marker selections; a reported page line is not fabricated into an exact span of the external snippet.
Unresolved references remain visible and block complete naming input. No command fetches assets.

Foreign language policies use an explicit programming-language label allowlist (validated
case-insensitively; actual selection still uses the declared spelling). Data/display formats such
as JSON, XML, CSV and none cannot declare a foreign programming language. Command-shell labels
(sh/bash/PowerShell) also remain unresolved, since they may carry shared setup or mongosh invocation;
without an explicit foreign tab they remain unresolved. Reference ancestry uses an allowlist
of understood directive/container kinds, so new selector directives require explicit support.
Unknown/missing input languages or tabs, mixed reference/foreign inputs, unknown selector containers,
nested IO owners and ambiguous source roles remain unresolved. Explicit foreign variants retain
their blocks and provenance without creating executable reference scenarios or competing names.
Standalone selected code is retained too. Reference/unknown standalone blocks block completeness:
setup and earlier variables must be grouped deliberately rather than silently omitted. Non-IO
legacy sections therefore remain incomplete until explicit grouping/context support is added.

Structural grouping does not prove that setup or a sample dataset is available, that constraints
are satisfied, or that a scenario can execute. The complete source dependency list remains in the
page report. Environment/dataset and execution attestations still belong to later stages.

## Normalization and names

Reference IO inputs become ordered action blocks and outputs become expected blocks in the closed
shell program model. Multiple actions require unambiguous expected ownership under that model.
Output language uncertainty has an explicit normalization reason; JSON display text is not relabeled
as legacy fixture data. Unsupported programs retain their own problems and no partial executable
commands. Displayed integral numbers cannot acquire Int32/Double types from input-shell defaults.
Nothing adds constructors or edits source just to obtain successful normalization.

Source grouping completeness and program normalization are independent. A fully owned example
with an unsupported operator form or ambiguous displayed result can retain a descriptive source
identity. `READY` means the selected source groups can supply naming input; it proves no execution,
serialization, assigned-name readiness or MongoDB/Morphia coverage.

The nearest descriptive section title supplies the display name. A generic heading such as
“Example” can recover a descriptive name from one named input snippet, including snippet-specific
filenames. When one heading contains several reference IO pairs, distinct authored input-snippet names can
name those subexamples. Foreign variants do not affect reference name selection. Ambiguous/missing
titles require review. Source titles remain distinct from canonical
Java/folder names, which the existing shared naming function synthesizes and the registry persists.

Report-local AST locators and content-addressed candidate handles are diagnostic references, not
folder names or identity matching preferences. `NamingScenario` carries explicit labels, qualified
snippet identities, anchor/enclosing context and a reserved fingerprint field for the later
complete semantic-dependency model. The registry keeps its existing stable-ID/anchor/fingerprint match
rules and collision protections. This slice does not supply a scenario fingerprint: complete
semantic setup/dataset/constraint closure has not been established, and unrelated page-wide
assets must not serve as that closure. The standalone program semantic hash remains available.
Positional line selections never become stable snippet IDs; raw options, verified physical
resource identity and dependency receipts remain visible separately. Dedent/prepend/append
transformations do not become authored identities. No ordinal suffix resolves duplicate groups or names.

`ScenarioExtraction.namingRequest` requires explicit physical name inventory. It refuses incomplete
scopes; a caller can request independent healthy scopes from a partial report, but all selected
page rules in each requested scope must be complete. Actual reconciliation still checks occupied
names, aliases, accepted disappearances and reviewed assignments. It only proposes a registry.

## Operations and verification

After compiling the audits module:

```sh
./mvnw -q -Daudits -pl audits exec:java \
  -Daudits.mainClass=dev.morphia.audits.scenarios.ScenariosMainKt \
  -Dexec.args="/path/to/bundle <source-sha256> /path/to/documents.json /path/to/scenario-plan.json"
```

The CLI emits candidate JSON on stdout, with exit 0 for complete grouping and 3 for partial grouping.
Invalid inputs exit 2 with a diagnostic on stderr. Requests decode strictly. The full owned input report has a 500,000-node/128-depth default
budget before origin validation; use a selected-page parser batch or the API's explicit limits
(up to one million nodes) for larger reports. Traversal, diagnostic classification and metadata
expansion share work/size budgets. A streaming check caps serialized reports at 64 MiB by default
before allocating JSON text, including repeated ancestry. Failed stdout writes exit 2. Reports include outbound BSON snapshots and are
write-only. Regenerate them from their source inputs; no operation-class or encoded-BSON reader
is introduced. Ordinary tests use synthetic owned trees and verified fixture resources, never the
live documentation repository or a MongoDB process.

Regression coverage includes persisted names through rearrangement/title/anchor changes, named
snippet title recovery, label scope, foreign observations, unknown selectors, unassigned setup,
parser errors versus cross-reference errors, independent healthy naming scopes, duplicate groups,
count/section drops, dependency failures, argument edges, input pins and resource bounds. The
progress journal records native pinned-document probes and local review outcomes.

Remaining work includes explicit ordered section/setup grouping for legacy layouts, semantic
language-equivalence decisions, dataset/constraint contracts, actual frozen-corpus comparison and
descriptive-name backport, curated repairs, generation and execution. Existing core tests, fixtures,
ordinal names and locks remain unchanged in this slice.
