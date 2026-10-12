# Type-Mapping Review — Speed, Simplicity, Clarity

Review of how Morphia maps types: model building, the encode/decode hot path, critter, runtime lookups, and whether
`EntityModel` is needed at all. Done at `76697be` on JDK 21 with 4 vCPUs. Five agents analyzed the code in parallel,
then the coordinator spot-checked their claims against the source and re-ran the key measurements. Detailed per-area
findings, with `file:line` evidence, are in [`mapping-review/`](mapping-review/):

| File | Area |
|---|---|
| [findings-A-model-building.md](mapping-review/findings-A-model-building.md) | Class → `EntityModel`: conventions, `TypeData`, validation, package scan |
| [findings-B-codec-hot-path.md](mapping-review/findings-B-codec-hot-path.md) | Per-document encode/decode |
| [findings-C-critter.md](mapping-review/findings-C-critter.md) | Critter vs reflection, structural duplication |
| [findings-D-runtime-lookups.md](mapping-review/findings-D-runtime-lookups.md) | Mapper/model lookups per operation, path translation, sessions |
| [findings-E-no-entitymodel.md](mapping-review/findings-E-no-entitymodel.md) | Can Morphia work without `EntityModel`? Includes a measured prototype |

Evidence tags: **[measured]** = JMH or an allocation probe; **[verified]** = reproduced by running code; **[read]** =
traced in source.

---

## 1. Baseline

`CodecBenchmark` / `MappingBenchmark`, 2 forks × 5 iterations, `-prof gc`:

| | reflection | critter (AOT) |
|---|---:|---:|
| decode SIMPLE (8 scalars) | 1.68 µs · 2.3 KB | 1.71 µs · 2.3 KB |
| decode NESTED | 4.8 µs · 6.9 KB | 5.0 µs · 6.9 KB |
| decode COLLECTIONS | 15.1 µs · 20.9 KB | 16.1 µs · 20.9 KB |
| decode POLYMORPHIC | 9.5 µs · 13.2 KB | 10.1 µs · 13.2 KB |
| encode SIMPLE | 0.88 µs · 1.2 KB | 0.83 µs · 1.2 KB |
| encode NESTED | 2.9 µs · 1.9 KB | 2.9 µs · 1.9 KB |
| warm mapping (11 types) | 101 µs · 166 KB | 58 µs · 84 KB |
| cold start | 321 ms | 314 ms |

**Per document, critter and reflection run at the same speed.** Critter's generated accessors aren't the bottleneck.

## 2. Headline conclusions

1. **The per-document cost lives in the decode/encode *protocol*, not in the model objects.** Four things dominate:
   - every document, embedded document and list element is pre-scanned for `_t`, then rewound and read again;
   - a reader mark (80 B) is taken for every property;
   - a buffering instance creator is used even for no-arg POJOs;
   - `Conversions` runs on every set.

   A flat interpreter that fixes these, still model-driven but without code generation, decodes **1.7–2.2× faster with
   ~55% less allocation**. A hand-written "ideal generated" codec is only ~8–9% faster still (§3). **[measured]**
2. **Sessions and transactions are expensive (D1/C7).** Every `startSession()`/`withTransaction()` deep-copies the
   mapper: every `EntityModel`, a fresh `Conversions`, about 140 codec objects, a ServiceLoader scan, and cold codec
   caches.
   - **Root cause:** per-datastore codecs (e.g. `ReferenceCodec`) are cached on the shared `PropertyModel`.
   - **Side bug:** a mapper handed to a second `MorphiaDatastore` reuses the first datastore's codecs.
   - **Cost:** 33–80 µs for 11 types **[measured]**, growing with entity count.
3. **The default `morphia.packages=.*` scans and *initializes* the whole classpath (A1).** On 17 ordinary jars that
   means ~3.8 s and ~9,400 static initializers run, with failures swallowed. Filtering on annotations first loads
   ~1,500 classes instead. **[measured]**
4. **Critter costs a second model hierarchy (2N+1 classes per entity) and buys nothing per document (C1/C2).**
   - Its constant-getter overrides make the shared encoder/decoder call sites megamorphic.
   - Some of those getters allocate on every call (`getTypeData`, `getLoadNames`) or call `Class.forName`.
   - Its discovery code has drifted from the reflection mapper's (C3): it ignores `@IdField`,
     `@Property(concreteClass)`, Kotlin delegated properties and interface annotations.
   - The simpler design is one model with critter as an *accessor strategy* (~1.5–2k LOC removed).
5. **Several correctness bugs turned up on the way (§5).** Fix these regardless of any performance work.

## 3. The `EntityModel` question

**Short answer:** having no model at all isn't viable. Having no `EntityModel` *at runtime* is viable and worthwhile.
The recommended form is a mutable builder at mapping time, compiled to an immutable descriptor plus a per-datastore
codec plan.

### What consumers actually need ([E §1](mapping-review/findings-E-no-entitymodel.md), [D appendix](mapping-review/findings-D-runtime-lookups.md))
- **Dynamic lookups at runtime.** Only four:
  1. user path → mapped path and nested type, used by `PathTarget`;
  2. discriminator → class;
  3. collection → root class;
  4. runtime class → per-type descriptor.
- **Everything else is fixed per type:** collection, write concern, id/version/shard-key accessors, discriminator list,
  lifecycle invokers and index metadata.
- **Only mapping-time conventions and validation need a mutable model.** The public SPI that touches the model is
  `MorphiaConvention`, which the Kotlin module implements.
- **`Mapper`, `EntityModel`, `PropertyModel` and `MorphiaDatastore` are all `@MorphiaInternal`.**

### Measured prototype ([E §4](mapping-review/findings-E-no-entitymodel.md))
Three codecs were benchmarked against each other:
- `morphia`: today's codec.
- `flat`: an O2/O3 simulation. No `EntityModel`; flat slot arrays, `MethodHandle` accessors, one shared interpretive
  loop.
- `handwritten`: the ideal codec a generator would emit.

All three produce byte-identical BSON. Decoding is interchangeable in both directions, including "foreign" documents
(reordered or unknown fields, numeric widening, missing `_t`): 152/152 checks on reflection and critter-runtime,
76/76 on critter AOT. **[verified]**

ns/op ± error (B/op), 2 forks × 5 iterations, `-prof gc`, reflection mapper. A separate 1-fork run by the prototype
author agreed within noise.

| | SIMPLE decode | SIMPLE encode | NESTED decode | NESTED encode |
|---|---|---|---|---|
| morphia (today) | 1748 ± 206 (2240) | 854 ± 84 (1184) | 4760 ± 379 (6920) | 2810 ± 347 (1856) |
| flat interpreter, no `EntityModel` | **809 ± 52 (1024)** — 2.16× | 651 ± 40 (1184) — 1.31× | **2733 ± 359 (3048)** — 1.74× | 2363 ± 244 (1856) — 1.19× |
| hand-written (generation bound) | 743 ± 69 (976) — 2.35× | 572 ± 59 (1136) — 1.49× | 2484 ± 331 (3136) — 1.92× | 1942 ± 186 (1856) — 1.45× |
| harness floor (1-fork run) | walk 394 (672) | empty 150 (992) | — | empty 154 (992) |

- The flat interpreter captures **~90% of the possible decode gain** (93% SIMPLE, 89% NESTED) and 51–72% of the
  encode gain.
- Encode is mostly the driver's writer and the output buffer: 992 of 1184 B/op is the buffer.
- Only generated code removes the last two costs, boxing and megamorphic per-property dispatch. That is worth about
  65–80 ns per SIMPLE document in each direction.

### Options

| Option | Speed | Simplicity | Compatibility | Verdict |
|---|---|---|---|---|
| **O1** generated `Codec<T>` per entity + generated metadata | best (2.35× SIMPLE decode, 1.49× encode) | net more code: a 2–4k LOC generator, and an interpreter is still required as fallback and for the reflection mapper | old AOT output ignored → runtime fallback | optional backend only |
| **O2** `EntityModel` as build-time IR compiled to a flat runtime form | 1.7–2.2× decode, 1.2–1.3× encode | modest | excellent | best speed per unit of effort |
| **O3** immutable `EntityDescriptor`/`PropertyDescriptor` + mapper `TypeRegistry` | neutral alone | **biggest clarity win**: no copy constructors, no datastore state on shared objects, one discovery pipeline | needs a `MorphiaConvention` bridge for one deprecation cycle | the right shape for O2's runtime form |
| **O4** no model / delegate to the driver's `PojoCodecProvider` | none | worse | — | strawman; the driver lacks references, lifecycle, `@AlsoLoad`, `Conversions`, etc. |
| **O5 = O3 + O2, with O1 optional** | 1.7–2.2× decode, up to 1.9–2.35× with generation | net −1.5–2.5k LOC (est.) | bridged | **recommended** |

Most of O5's speed gain comes from fixing the protocol, and that fix can land *first*, inside today's
`EntityDecoder`/`EntityEncoder`, with no API change.

## 4. Ranked improvements

Duplicates found independently by more than one agent are merged; the IDs point into the lane files. "Freq" is how often
the code runs.

### Tier 1: high value, small effort, no API change

| # | Change | Freq | Gain | Evidence | Refs |
|---|---|---|---|---|---|
| 1 | **Skip the reader mark for codecs that can't throw part-way.** Per-property bitmask of the BSON types the codec reads directly; table keyed on exact codec class. Hoist the NULL check above the mark. | per scalar property per doc | 80 B + ~15 ns per property; ~640 B of SIMPLE's 2.3 KB | measured | B1 |
| 2 | **No-arg creator fast path.** `bestConstructor` matches no-arg constructors vacuously, so POJOs buffer every property and replay it. Construct first, set directly, allocate pending lists lazily. Delete the unreachable `NoArgCreator` branch. | per entity / embedded doc | ~216 B, ~150–200 ns | measured | B4 = A2 |
| 3 | **Discriminator self-match.** `codecForDiscriminator(d)` returns `this` when `d` is the model's own discriminator, with a per-codec cache otherwise. `skipValue()` instead of `readString()` on `_t`. (Step 2, a single pass with no rewind, is M effort.) | per doc + embedded doc | step 1 ~48 B, two lookups; step 2 ~360 B + 250–300 ns per 8-field doc | measured | B2 |
| 4 | **`getProperty` single lookup.** `getOrDefault` evaluates its fallback eagerly. | per field per doc | 1 hash probe per field | read | D4 = B6 = A7 |
| 5 | **Memoizing `CodecRegistry` wrapper.** The driver's `get(Class)` allocates 48 B per call. Cache `ArrayCodec`'s component codec. | per subtype value / untyped element / lifecycle doc | ~600 B per POLYMORPHIC op each way | measured | B5 |
| 6 | **Precompute serialization flags and the encode order.** `shouldSerialize` re-reads config and scans annotations per property; `equals(idModel)` is deep — use `==`. | per property per encode | small | read | B11 = A7 |
| 7 | **Skip `Conversions` when the value is already the target type or its wrapper.** Use `get` instead of `computeIfAbsent`; drop the double conversion on the fallback path. | per primitive/interface property | ~10–20 ns each | read | B8 |
| 8 | **`PathTarget`: replace `segment.matches("[0-9]+")`**, which compiles a regex per segment. Restore `context` when no subtype matches (correctness). Cache `UpdateOperator.field()`. | per filter/sort/update/projection field | ~0.5–1 µs per segment | read | D2 |
| 9 | **`ClassValue<Boolean>` for `isMappable`.** "No" answers are never cached today. | per path terminal / `Document` query / raw id | 0.2–0.5 µs | read | D3 |
| 10 | **Package scan:** `enableClassInfo().ignoreClassVisibility()` + `Class.forName(name, false, …)`, then an annotation pre-filter. Warn on the `.*` default; collect into a `LinkedHashSet`. | per datastore start | seconds on real classpaths | measured | A1 |
| 11 | **Load ServiceLoader conventions once per mapper**, with the mapper's class loader. Keep one `MappingValidator` per mapper. | per mapped type | 5–30 µs; ~40% of warm reflective mapping | measured | D15, A |
| 12 | **Lifecycle:** guard the eager `Sofia` debug formatting with `isDebugEnabled()`; precompute argument kinds; use a lifecycle bitmask. | per entity per event | allocation | read | D9, B3 |
| 13 | **Collection/map codecs:** resolve a `Supplier` once instead of `getDeclaredConstructor().newInstance()` per decode. **Date codecs:** precompute the `ZoneOffset`. **Lazy references:** cache the proxy class and its constructor handle. | per property / per value | 0.1–1 µs each | read | B9, B12, B10 |

Items 1–4 and 6–7 are, together, the "protocol rewrite" step of §3. They should take decode close to the `flat` row
(1.7–2.2× decode). Gate them on the differential harness (§6, step 0).

### Tier 2: structural, medium effort, large payoff

| # | Change | Why | Refs |
|---|---|---|---|
| 14 | **Move per-datastore codecs off `PropertyModel` into a per-`MorphiaCodec` plan.** The plan holds `Codec<?>[]` by slot, direct-type masks, `encodeOrder`, creator positions, lifecycle flags and subtype/discriminator caches. Sessions then *share* the mapper. | Removes the per-session mapper copy (D1); fixes cross-datastore codec reuse; makes `PropertyModel` immutable after mapping | D1, B13, C7, E |
| 15 | **Lifecycle streaming path.** Use the `Document` round trip only when a hook or global listener actually takes a `Document`. Gate on event coverage, not `hasListeners()`. Delegate subtypes *before* running callbacks. | One `addInterceptor` today forces every entity type through two document materializations; also fixes a probable double `@PrePersist` | B3 |
| 16 | **Critter as an accessor strategy on one model (C2).** AOT `XAccessor` → `ClassValue`-cached nestmate → reflective, chosen per property. Delete `CritterEntityModel`/`CritterPropertyModel`, the model generators, `RuntimeModels`, `NestmateAccessorRegistry`. | ~1.5–2k LOC; monomorphic getters; critter semantics match reflection by construction (fixes all of C3); runtime-tier mapping drops from 570–810 µs to reflection cost | C2, C1, C3 |
| 17 | **AOT validity guard.** Format version, generation key and weave hash, checked at load; fall back with one log line. | Today AOT silently bakes in build-time naming/discriminator config, and stale weaving fails at first encode | C4 |
| 18 | **Shared registration protocol in `AbstractMapper`.** `synchronized final mapEntity` → `abstract buildModel`; `putIfAbsent`; roll back on validation failure. | Fixes A3/D7, and puts the critter and reflection mappers on one code path | A3, D7 |
| 19 | **Single generic-type resolver**, binding type variables by name and cached per class. | Fixes three verified bugs (A6) and the critter/reflection divergence | A6 |
| 20 | **Primitive array codecs.** | `double[1536]` embedding vectors: 3.0× encode, 2.4× decode | B7 |

### Tier 3: architecture (the `EntityModel` track)

| # | Change | Refs |
|---|---|---|
| 21 | Immutable `EntityDescriptor` + mapper `TypeRegistry` compiled from `EntityModel` after validation. Migrate consumers in the order aggregation → metadata → paths → references → codecs. | E O3/O5 |
| 22 | Extract a `ReflectiveEntityModelBuilder`, so the `EntityModel` constructor stops building itself. Use one annotation-collection utility for both mappers. | A11 |
| 23 | Critter emits one descriptor factory per type instead of 2N+1 classes. | E, C |
| 24 | Optional generated-codec backend behind the same codec contract, with interpreter fallback. Only if the remaining ~65–80 ns per SIMPLE document matters. | E O1 |
| 25 | Retire `EntityModel` from runtime APIs: a v2 `MorphiaConvention` on the builder, a Kotlin bridge, a deprecated `getEntityModel` adapter. | E |

## 5. Correctness issues found along the way

| Issue | Evidence | Ref |
|---|---|---|
| `register()` keeps a model that failed validation. The first lookup throws; later lookups silently return the invalid, half-registered model. | verified, both mappers | A3 |
| `@IdField`: the decoder can't find `_id` (the property maps aren't re-keyed). Critter ignores `@IdField` entirely. | verified | A4 |
| Kotlin delegated property registered under two keys, so it is encoded twice. Tests hide this because they run on clones. | read | A4 |
| `getSuperClass()` returns an interface for `C extends B implements Tagged`, so `IndexHelper` collects inherited `@Indexes` from the wrong parent. | verified, both mappers | A5, D5 |
| Generic resolution substitutes by position: `MyMap<V,K> extends HashMap<K,V>` swaps key and value types. `? super` is ignored. | verified | A6 |
| Critter vs reflection divergence: `@Property(concreteClass)`, `@IdField`, ServiceLoader conventions (Kotlin), interface annotations, generic bindings resolved to `Object`, fluent setters (`NoSuchMethodError`), shadowed fields. | read / verified | C3, A6 |
| Reference filter reads back the untranslated field name. `@Reference("r")` or snake_case naming produces `{field: null}`. | read | D10 |
| `getClassFromCollection` returns an arbitrary (HashSet-order) class, so `find("shapes")` can filter on a subclass's discriminators. | read | D6 |
| Non-atomic registration in `ReflectiveMapper` ("duplicate discriminator" under concurrent first use). `MorphiaCodecProvider.codecs` is a plain `HashMap` mutated during lookups. | read | D7, D8 |
| `PathTarget` leaves `context` pointing at the last subtype tried when no subtype matches. | read | D2 |
| Probable double `@PrePersist`/`@PostPersist` for polymorphic values in lifecycle codecs. | read, needs a test | B3 |
| `PropertyModel.hashCode` includes the lazily-set codec, so the hash changes after first use. | read | D5 |
| Session copies lose `EntityModelImporter` providers and custom `Conversions`. | read | D1, C7 |
| Model registry keyed by class *name*; anonymous subclasses (`new User(){{…}}`) mapped as separate entities. | read | D13 |
| Decode→encode isn't idempotent under `storeEmpties=false` + `storeNulls=true` (empty list → null). | verified | E |

## 6. Suggested sequence

0. **Differential codec harness**, modeled on the prototype's `Verify.java`. For every test entity it checks
   byte-identical encoding, decode equivalence across implementations, and "foreign" documents. Every later step is
   gated on it.
1. **Correctness fixes from §5**, each with a test. Run with both `-Dmorphia.mapper=critter` and `reflection`.
2. **Tier 1 items 1–9** (decode/encode protocol plus lookups). Expect 1.7–2.2× decode on SIMPLE/NESTED, and more on
   POLYMORPHIC/COLLECTIONS.
3. **Tier 1 items 10–13** (startup and lifecycle quick wins).
4. **Item 14** (codec plan off `PropertyModel`, sessions share the mapper), then **15**.
5. **Item 16** (critter as an accessor strategy) with **17** (AOT guard), before 3.0 GA, while the generated format can
   still change.
6. **Tier 3** as a 3.x track. Decide on item 24 using measured numbers for POLYMORPHIC and LIFECYCLE.

## 7. Benchmark gaps to close

- An ARRAYS model (`double[1536]`, `String[]`).
- A model with concrete collection types (`ArrayList`, `LinkedHashMap`, `TreeMap`).
- A LAZY_REFERENCE model.
- A no-op global `EntityListener` variant.
- Documents without `_t`.
- A schema-drift model (an int stored for a String field).
- A **mixed-entity** run that exercises megamorphic call sites. Each model currently runs in its own fork, which hides
  them.
- Package-scan startup.
- `new MorphiaDatastore(ds)` / session creation.
- `PathTarget.translatedPath()` and `MorphiaQuery.toDocument()`.
- Hand-written/flat codecs for POLYMORPHIC and LIFECYCLE, to settle the O1 question.

Prototype sources (1,346 LOC) are in [`mapping-review/prototype/`](mapping-review/prototype/). They compile with
`javac` plus the JMH annotation processor against `benchmarks/target/morphia-benchmarks.jar`. The prototype `README.md`
has the exact commands; its paths point at the session scratchpad, so substitute your own jar locations.
