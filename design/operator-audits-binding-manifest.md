# Physical example bindings before descriptive renaming

The existing preservation catalog has 556 resource cases and 513 broad test-source hashes, but no
actual method binding. Compiler AST inspection of the four operator test packages finds 260
classes and 566 JUnit methods: 259 template-base classes with 565 methods, plus one independent
manual class. The legacy naming convention identifies 554 resource/method pairs without duplicates.
Two change-stream resource cases contain only `name` and `lock` and have no method. Eleven
methods in template classes have no corresponding resource directory: ten manual tests and one
empty percentile placeholder. A last-N bound method is also empty. Disabled/missing/mismatched
DisplayName annotations must remain visible rather than being silently repaired during import.

This slice creates a compiler-AST physical binding manifest and replaces the ordinal coverage scan.
It retains existing names. Implemented by `dev.morphia.audits.bindings.LegacyBindings`, the reviewed
`audits/catalog/binding-policy.json`, the evidence catalog `audits/catalog/legacy-bindings.json`
and core's `ExampleBindings` descriptor loader (`dev/morphia/test/example-bindings.json`). The coordinated descriptive method/folder/reference cutover follows
only after complete naming reconciliation.

## Import and preservation

The one-time importer uses the full JDK parser with processors and implicit source discovery
turned off. It parses explicit files in the four operator packages and captures actual class,
method, annotations, literal display title, raw body digest, source ranges, source-file digest,
options/source evidence and fixture ownership. This is declared source evidence, not execution
coverage. Only this legacy import may use the old loader's class/method convention to propose a
physical pair; runtime loading and coverage use explicit accepted bindings afterwards.

Retain the immutable original legacy catalog and its pin. Compare every curated operator source
and every case/exclusion/auxiliary resource to that baseline during import. Framework changes to
TestCoverageExtension and new manifest support are intentional changes outside the four curated
operator source roots, not reasons to bless unrelated body/data changes. The broad historical
source snapshot is not regenerated to hide these deltas.

Every physical case and observed JUnit method must have exactly one disposition. Reviewed policy
explicitly pins the two unbound marker-only cases and non-fixture manual/placeholder methods;
new missing or competing ownership cannot inherit those exceptions. Method body, annotations,
resource hashes and exclusion reasons remain visible. Empty methods and disabled methods do not
acquire behavioral support merely by existing. Inventory completeness, binding completeness and
behavioral coverage remain separate facts.

## Core discovery

Accept a closed JSON descriptor in core test resources, produced/validated separately by audits;
ordinary core builds have no audits dependency or network requirement. Runtime coverage checks
manifest membership in both directions rather than scanning testExample*. Unknown classes/methods,
missing fixtures, duplicate owners and invalid/empty descriptors fail. Known preserved-unbound
cases remain explicit, with their marker evidence; they are never counted as complete bindings.
Non-fixture manual tests remain ordinary tests and cannot accidentally load a fixture by convention.

An offline global contract test is needed as well as per-class AfterAll validation: removal of a
whole class or its last test must not bypass completeness. It checks physical resource membership
and accepted classes/methods; the four packages are pinned in core so a descriptor cannot drop one,
and an unbound fixture may hold only `name`/`lock`/`lockd` markers. Fixture byte preservation is
checked against the immutable corpus catalog by the audits gate, and an audits test requires the
committed descriptor to equal what the reviewed policy produces from the sources. Descriptor lookup
supplies the method/resource round trip for the template loader; original packages/operator
directories remain unchanged. Inherited test
ownership must be explicit or rejected, never resolved by encounter order.

## Delivered results

The real import reproduces the investigation: 554 `BOUND`, 11 `MANUAL` (ten handwritten tests plus
the empty percentile placeholder) and one `INDEPENDENT` collection-stats test across 260 classes, with
the two marker-only change-stream fixtures pinned as unbound. 30 bound methods have empty bodies and
7 are `@Disabled`; the catalog records both facts, and neither counts as behavioral coverage. A fresh
`CorpusInventory` capture matches the accepted catalog's examples, exclusions and auxiliary resources
exactly; the only test-source deltas are `TemplatedTestBase` and `TestCoverageExtension` plus the new
`ExampleBindings` files. Policy staleness, unowned fixtures/methods, ignored operator folders with
fixtures and computed display titles fail the import (synthetic tests in `LegacyBindingsTest`).
Removing a class entry or adding an unbound fixture fails core's offline `ExampleBindingsTest`.

## Verification

Use synthetic Java/resource fixtures to test source parsing, preservation, missing/duplicate
ownership, exceptions becoming stale, placeholders, display annotations, renamed descriptive
methods, invalid descriptors and omitted physical cases. Run the real import deterministically
against all 556 resources and compare all fixture/lock bytes. Test core discovery without extending
TestBase or opening MongoDB. Run relevant mapper checks with Critter first, reflection second,
then local independent reviews, remote review and exact-head CI before the next stacked child.
