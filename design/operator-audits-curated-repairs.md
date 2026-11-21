# Curated repair inventory

The first slice of step 4 of [the operator audit plan](mongodb-operator-audits.md). It is the
migration manifest the plan asks for before any lock is replaced. Every legacy correction marker and
operator exclusion gets a reviewed category and disposition. Every bound test's `ActionTestOptions`
and the assertion paths it actually executes are inventoried, so later migration can measure
coverage by examples and paths rather than by method calls. No lock, fixture or test changes here.

## Inputs and output

`audits/catalog/repair-review.json` is the reviewed classification:

- every distinct lock reason line (trimmed, lower-cased) maps to one category;
- every operator `ignored` file maps to one exclusion category.

`CuratedRepairs.capture` reads core's binding descriptor, the fixture markers and the bound test
sources, which it only parses with javac (no attribution or execution). It writes
`audits/catalog/curated-repairs.json`, keyed by descriptive fixture name. A lock reason or exclusion
that is not reviewed, a reviewed entry that nothing uses any more, a blank lock and an unknown
category all fail. An audits test requires the committed inventory to equal a fresh capture.

## Categories

Lock reason categories, each with its intended retirement:

| Category | Locked examples | Retire when |
| --- | --- | --- |
| `EXTRACTION` | 68 | the pinned scenario extractor produces an equivalent scenario natively |
| `MORPHIA_FORM` | 77 | a declared command-equivalence rule or compatibility decision replaces it |
| `DOCS_ERROR` | 38 | never automatically; kept as a reviewed correction pinned to the source digest |
| `NUMERIC_TYPE` | 26 | typed BSON comparison proves the documented values, otherwise a typed override |
| `SHELL_SYNTAX` | 25 | shell normalization lowers the source to the same typed BSON |
| `FIXTURE_CURATION` | 21 | kept as an explicit setup or expectation override |
| `UNTESTABLE` | 2 | kept as a scoped exemption |

A lock with several reason lines has several categories, so the column sums exceed the 236 locked
examples (235 `lock`, 1 `lockd`).

Exclusion categories, used by the 17 operators excluded from example testing: `POLICY_EXCLUSION` 11,
`ENVIRONMENT_UNAVAILABLE` 3 (Atlas), `NON_OPERATOR` 1, `TESTED_ELSEWHERE` 1 and `DEFERRED` 1.
`querySettings` ("for now") has no recorded rationale and stays visible as deferred.

## Dispositions and assertion paths

- `PRESERVED_CURATED` (234): the curated fixture is the baseline, and its upstream match is unknown
  until an extracted replacement proves equivalence. Old locks are removed only with a passing
  replacement.
- `UNCORRECTED` (320): no lock.
- `NOT_EXECUTED` (2): the marker-only change-stream fixtures.

Each test's assertion paths come from the template helper it calls directly and from the literal
options. A qualified helper call (`this.testPipeline`), a `skip*Check` call that is not chained on
`new ActionTestOptions()`, or a non-literal skip argument fails the inventory instead of being
approximated. A helper reached through a private method is not followed and would read as `CUSTOM`;
no test does that today.

- `SERIALIZATION` (509): the generated command is compared with the fixture action, unless
  `skipActionCheck(true)`.
- `BEHAVIOR` (466): the server result is compared with the fixture's expected output, unless
  `skipDataCheck(true)`.
- `CUSTOM` (2): handwritten assertions that bypass the helpers.

Each test's extra collections are recorded by data file: `loadData("airports", 2)` loads
`data2.json` into `airports` (11 tests). Only that literal form is read; a non-literal argument, a
qualified call or a data file loaded twice fails the inventory. `data.json` always goes to the
collection the action runs against.

A `@Disabled` test executes nothing: 7 bound tests are disabled and report no paths. Another 26
bound tests have empty bodies and also report none. They are bound, but they are not coverage.

Reason lines are filed by how they can be retired. Morphia's own enum spellings count as
`MORPHIA_FORM`. Broken upstream source text and wrong documented values count as `DOCS_ERROR`, even
where the lock describes the edit in shell or numeric terms.

## Next

The `MORPHIA_FORM` family is migrated in [the form migration](operator-audits-form-migration.md):
46 locks are retired as `OVERRIDDEN`, and the inventory now counts 190 locked examples. The other
families follow one at a time, each expressed as a declared equivalence or override applied to the
extracted scenario and proven against the preserved curated fixture before its lock is removed.
