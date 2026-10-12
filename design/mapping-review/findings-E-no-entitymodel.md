# Findings E — Could Morphia work without `EntityModel`/`PropertyModel`?

(Agent E report, saved by the coordinator because the agent's own file write was blocked.)
Paths relative to `core/src/main/java/dev/morphia/` unless stated. **[read]** = verified in code; **[measured]** = JMH on this machine; **[guess]** = reasoned, unmeasured.
Prototype: `scratchpad/no-entitymodel/` (README has exact commands). Byte-for-byte checks: 152/152 reflection + critter-runtime, 76/76 critter AOT.

## Summary
- **Inventory**: ~60 main files in core, 1 kotlin file, 1 critter-maven test, 24 test files (263 lines) use the models. Needs come in five shapes: (1) a fixed per-type codec action; (2) small per-type metadata; (3) a property table looked up by Java or stored name (path translation); (4) mapper-level relationships that grow as types are mapped (subtypes, discriminators, collections); (5) a mutable model at mapping time (conventions, validation). Only 3 and 4 need dynamic runtime lookup. `EntityModel` mixes all five — and also holds per-datastore codecs.
- **Where the time goes**: the generic decode/encode *protocol* built around the model, not the model objects: discriminator pre-scan with mark/reset on every document/embedded doc/list element; a reader mark per property; a buffering instance creator + `Conversions.convert` on every set; boxing + megamorphic dispatch per property. Critter replaced accessors but kept the protocol → critter ≡ reflection.
- **Measured** (`impl=morphia → flat → handwritten`, ns/op, B/op in parens): SIMPLE decode 1603 (2240) → 833 (1024) → **725 (976)**; SIMPLE encode 815 → 667 → **560**; NESTED decode 4573 → 2641 → **2377**; NESTED encode 2546 → 2258 → **1894**. Fixed floors: ~150 ns / 992 B for an empty encode (buffer + writer); ~394 ns to walk the SIMPLE document. Flat interpreter gets ~88% of the decode gain, ~45–58% of the encode gain.
- **Verdicts**: O1 (generated codecs) fastest (2.2× SIMPLE decode, 1.46× encode) but can't stand alone (runtime generation can fail; reflection mapper needs a codec) → needs an interpreter fallback; most code; worth it only as an optional backend. O2 (model compiled to flat runtime form): most of the decode gain, ships in small steps, compatible — best speed per effort. O3 (immutable descriptors): no speed change alone, biggest clarity win, the right shape for O2's runtime form. O4 (no model): strawman.
- **Recommendation: O5 = O3 + O2, O1 as optional critter backend.** First steps: differential test harness (modeled on `Verify.java`) → rewrite the decode protocol inside `EntityDecoder`/`EntityEncoder` (no API change) → move per-datastore codecs off `PropertyModel` → introduce descriptors.

## 1. Consumer inventory
**Dyn** = lookup by name/path/discriminator/collection at use time. **Fixed** = one per-type action, precomputable/generatable.

### (a) Encode/decode — Fixed per type (except subtype hand-off)
- `mapping/codec/pojo/EntityEncoder.java:42-61, 82, 100-112, 115, 136-143`: type check; id property, accessor, id generation (`:121-130`); discriminator flag/key/value; ordered properties (`EntityModel.properties()` `EntityModel.java:346`); per property accessor, `shouldSerialize`, mapped name, resolved codec.
- `EntityDecoder.java:46-53, 60-80, 84-94, 96-118, 121`: discriminator, `getProperty(name)` (`EntityModel.java:355-356`), per-property codec, `TypeData` for fallback conversion, instance creator.
- `MorphiaCodec.java:72, 200-208`: resolves property codecs and **stores them on the shared `PropertyModel`** (`PropertyModel.java:70, 317-322, 418-424`).
- `LifecycleEncoder.java:36-43`, `LifecycleDecoder.java:37-58`: lifecycle methods; switch to subtype model.
- `MergingEncoder.java:69-82` (used at `MorphiaDatastore.java:489`): same walk, emits `$set`/`$unset`.
- `MorphiaCodecProvider.java:66-80` (picks lifecycle codecs), `:92-115` (refresh: decode into existing instance).
- `InstanceCreatorFactoryImpl.java:40-66`, `internal/ConstructorCreator.java:72-147, 188` (`bestConstructor` `:107`), `NoArgCreator`.
- `MorphiaPropertySerialization.java:36-50`: config flags, `isFinal`, `@LoadOnly`.
- kotlin `ReadWritePropertyCodecProvider` reads `TypeData`.

### (b) Query/update/projection/sort path translation — Dyn
- `internal/PathTarget.java:158-215`: `getProperty` by Java **or** mapped name; `getSubtypes()` search on miss; `isReference`, `isMap`, `getMappedName`; `getNormalizedType()` → nested type's model.
- `Filter.java:160-171`, `OperationTarget.java:47-73`: encode filter values with the target property's codec (`specializeCodec`).
- `Projection.java:129-170`: known fields, `@Entity.useDiscriminator`.
- `FindOptions.java:143-148` (sort), `UpdateOperator.java:87-122`: translate paths.
- `Operations.java:63-75`, `SetEntityOperatorCodec.java:29-31`: version property's mapped name.
- `MorphiaQuery.java:193, 248-256, 369`: discriminator on upsert; `updateQueryWithDiscriminators`.
- `IndexHelper.java:215-220`: index field paths.

### (c) Collection/index/shard/validation metadata — Fixed (by `Class` or collection name)
- `MorphiaDatastore.java:245-299, 324-332, 405-417, 534-560, 640-660, 722-737, 827-832, 841-860`: collection name; `@Entity` cap + write concern, `@Validation`; shard keys + values; id/version `getValue`/`setValue`.
- `IndexHelper.java:69, 188-300`: `@Indexes`, `getProperties(Indexed/Text)`, superclass chain.
- `AbstractMapper.java:165-186, 260-272, 361-378`: collection → models; `WriteConcern`.
- `ReferenceCodec.java:107-124`: collection names.

### (d) Lifecycle — Fixed
- `EntityModel.java:198-212, 435-462`: listeners; reflective invoke via `EntityListenerAdapter.java:56-79` (allocates `ArrayList` + array per call).
- `MorphiaCodecProvider.java:71-76`: **any global listener switches every entity type to the Document-based lifecycle codecs**.
- `MorphiaDatastore.java:902`.

### (e) References & lazy proxies — Fixed per property; target type dynamic
- `ReferenceCodec.java:87-92, 198-224, 259-305, 329-464`, `BaseReferenceCodec.java:35-70`: `@Reference`, `TypeData`, target model, owner/property names for the ByteBuddy proxy class.
- `PropertyModel.java:274-283`, `AbstractMapper.java:207-222, 282-288`: unwrap `MorphiaProxy`.

### (f) Polymorphism — Dyn, mapper-level, grows as types map
- `DiscriminatorLookup.java:68-95`; `EntityDecoder.java:96-118`, `LifecycleDecoder.java:39-51`; `AbstractMapper.java:338-352` (polymorphic queries), `:387-413` (linking); `EntityModel.java:373-386` mutable subtypes/`superClass`, `getSubtypes` re-flattens per call.

### (g) Aggregation stages — Fixed, `collectionName()` only
- `UnionWithCodec.java:39`, `OutCodec.java:27`, `MergeCodec.java:35`, `LookupCodec.java:34`, `GraphLookupCodec.java:34`.

### (h) Mapping-time validation & conventions — build time only, mutable model
- `EntityModel.java:550-569` (`MappingUtil`): `MorphiaDefaultsConvention` → `FieldDiscovery`/`MethodDiscovery` → `ConfigureProperties` → every `ServiceLoader` `MorphiaConvention` (mutate the model).
- kotlin `KotlinDelegatedPropertiesDiscovery.kt:10-23` renames a property, re-derives mapped name, swaps accessor, re-adds.
- Validation rules: 784 LOC.
- Critter is a second, parallel discovery pipeline: `critter/parser/PropertyFinder.java`, `critter/conventions/PropertyConvention.java:38` duplicate mapped-name rules in `FieldDiscovery.java:72-92`. **`MorphiaConvention`s never run for critter models** (only `ServiceLoader` call is `EntityModel.java:564`) → kotlin convention doesn't apply under critter. [read]

### (i) Public API exposure
- `Mapper` is `@MorphiaInternal` (`Mapper.java:25-30`). Public `Datastore` has no `getMapper()` in 3.0 (`docs/.../morphia-2.5-3.0-table.adoc:111`); only `MorphiaDatastore.getMapper()` (`:315`), and `MorphiaDatastore` is itself `@MorphiaInternal` (`:98-104`) → users must cast. Docs example `queries.adoc:142` stale.
- **`MorphiaConvention` is a public SPI** (`MorphiaConvention.java:8`, not internal) typed on `EntityModel` — the real compatibility surface; kotlin implements it.
- `EntityModelImporter` internal/experimental, tests only. `ObjectFactory` public, takes `PropertyModel`, unused in main. `rewrite.yml:159-161` has recipes renaming `EntityModel` methods → 2.x users did call them. Benchmarks' `MapperVariant` uses `getEntityModel` for tier checks.

### Takeaways
1. Only (b), (f) and part of (e) need dynamic lookup.
2. (f) belongs in a mapper-level registry, not per-type objects; keeping it on the model forces mutability and copy logic (`EntityModel(EntityModel)` `:121-167`; reflective ctor in `CritterMapper.java:146-159`).
3. **Datastore-bound codecs are cached on objects the mapper shares**: a `Mapper` passed to a second datastore (`MorphiaDatastore(client, config, mapper)` `:176-179`) reuses the first's property codecs incl. `ReferenceCodec`s bound to it; tests avoid this by copying (`TestBase.java:166-174`). [read, not exercised]
4. Only (h) needs a mutable model, and it is the only public SPI.

## 2. Where per-document time goes
### Encode `SimpleEntity` (reflection, 146 bytes)
1. `MorphiaCodec.encode` → `EntityEncoder.encode`.
2. Setup: type check (`:43`), `LOG.debug` (`:44`), capturing lambda via `CodecHelper.document` (`:46`).
3. Id: `Field.get` on a non-constant `Field` (`:102`, `FieldAccessor.java:36-42`).
4. `shouldSerialize` (`MorphiaPropertySerialization.java:36-50`): 3 config getters, `instanceof`, `@LoadOnly` scan.
5. `encodeWithChildContext` (`:142`): interface call with `DEFAULT_CONTEXT` (javap).
6. Write `_t`.
7. Per property: `PropertyModel.equals` (`:56`, overridden `:296-310`); `getAccessor().get` (`:82`) **boxes** primitives; `shouldSerialize`; **megamorphic** `codec.encode`.
8. `LocalDateTime` walks a config chain per value (`MorphiaLocalDateTimeCodec.java:40`).
Critter only changes step 7 (virtual `getAccessor()` across 8 generated subclasses → `__readX` + box); boxing and megamorphic dispatch remain.

### Decode `SimpleEntity`
1. Child contexts always `DEFAULT_CONTEXT` → `getCodecFromDocument` (`:96-118`) pre-scan: `getMark()` (~48 B `BsonBinaryReader$Mark` + `ByteBufferBsonInput$1`); read names (allocating Strings) until `_t`; `DiscriminatorLookup.lookup` (`:83-95`, `containsKey`+`get`); `registry.get` via `fromRegistries(fromCodecs(this), …)` (`MorphiaCodec.java:72`); `reset`, second decode with `CHECKED_DISCRIMINATOR`.
2. Instance creator: new `ConstructorCreator`, param array clone, **2 `ArrayList`s** (`ConstructorCreator.java:45-46`) — even for no-arg ctors (vacuous `bestConstructor` match, `:107-127`).
3. Per field: `readName` (String); `getProperty` **two `LinkedHashMap` lookups** (`EntityModel.java:356`); **new mark per property** (`EntityDecoder.java:64`); megamorphic `codec.decode` → boxed (`:69`); `ConstructorCreator.set` (`:188`) position lookup by name → append to pending lists.
4. `getInstance` (`:137-147`): reflective `newInstance`; per pending value `PropertyModel.setValue` (`:386-390`) → `Conversions.convert` (`Conversions.java:145-166`) — for primitive targets `value.getClass() != int.class` → `computeIfAbsent` + `get`; then `Field.set`.
Allocation: Morphia 2240 B/op vs flat 1024 B/op — 9 marks × ~64–72 B ≈ 600 B; creator + lists ≈ 200 B; Strings re-read by the pre-scan ≈ 150 B.

### Other models
- NESTED: each of 4 embedded docs repeats the whole protocol; each embedded encode goes through `EntityEncoder` again.
- COLLECTIONS: protocol once per `Address` element; map keys via `conversions.convert` (`MorphiaMapPropertyCodecProvider.java:79, 94`).
- POLYMORPHIC: per element `registry.get(value.getClass())` on encode (`EntityEncoder.java:62-66`); pre-scan + lookup + registry get on decode.
- LIFECYCLE: encode via `Document` then re-encode; decode into `Document` then decode again via `DocumentReader` (`LifecycleEncoder.java:35-43`, `LifecycleDecoder.java:37-58`) — forced for **all** entity types when a global listener exists.

### Indirections and which design removes each
| # | Indirection | O2/O3 (flat interpreter) | O1 (generated) |
|---|---|---|---|
| 1 | Pre-scan per doc/embedded doc/element (`EntityDecoder.java:46-53, 96-118`) | removed (optimistic `_t` check) | removed |
| 2 | Mark per property (`:64-76`) | removed (check BSON type first) | removed |
| 3 | Buffering creator (`ConstructorCreator.java:45-46, 137-147, 188`) | removed for no-arg | removed |
| 4 | `Conversions` on every set | slow path only | slow path only |
| 5 | Two hash lookups per field name (`EntityModel.java:356`) | one `HashMap` lookup | string `switch` |
| 6 | Boxing | **remains** | removed |
| 7 | Megamorphic per-property dispatch | **remains** | removed |
| 8 | Config re-reads per property | precomputed | constant |
| 9 | Child context + registry for embedded/polymorphic | direct codec reference | direct |
| 10 | Lambda + debug call | removed | removed |
| 11 | Document round trip + reflective lifecycle hooks | direct `MethodHandle`s | generated calls |
Rows 1–5 and 8–11 are protocol issues fixable **without** removing `EntityModel`. Only 6–7 need generated code.

## 3. Design options
Runtime needs: **R1** a codec per datastore and type; **R2** immutable per-type metadata; **R3** a property table by Java name and stored names; **R4** a mutable mapper registry (subtypes, discriminators, collections); **R5** a mutable builder at mapping time.

### O1 — Generated per-entity codecs + slim generated metadata
- (a) generated `T$$Codec implements CollectibleCodec<T>`, created per datastore with a context (registry, `Conversions`, flags, lifecycle dispatcher): straight-line writes; `switch` on field name for reads; `decodeInto(T)` for refresh; `encodeMerge` mode for `MergingEncoder`; direct lifecycle calls; cold paths in a shared support class.
- (b)/(e) generated property table (names, load names, flags, nested-type refs, `get`/`set(int slot, …)` switches) — serves `PathTarget`, shard keys, version, id, merge without a class per property.
- (c)/(g) generated constants; annotation instances via existing builders (as `EntityModelGenerator.java:142-166` does).
- (d) generated hook calls. (f) stays in the mapper registry; each codec checks `_t` optimistically. (h) model still needed at build time. (i) adapters.
- Pure O1 makes generation mandatory — not viable: runtime generation already fails for types Morphia can't `privateLookupIn` (`CritterMapper.java:218-238`, `NestmateAccessException`); generated code must cope with final fields under JEP 500 → O1 needs O2's interpreter for the reflection mapper and as fallback.
- AOT can call woven public `__readX`/`__writeX` (javap on AOT `SimpleEntity` shows `__readAge()I`). Runtime: **one hidden nestmate per entity** instead of one per property (`CritterGenerator.java:142-165`) + 2N+1 model classes.
- Compat: older critter-maven output (`{pkg}.__morphia.{type}.{Type}EntityModel`, loaded `CritterMapper.java:199-211`) ignored → falls back to runtime (safe; AOT lost until rebuild). AOT models bake build-time naming/discriminator config and **aren't checked against runtime config today**; new metadata should carry `generationKey` (`CritterMapper.java:373-390`). Kotlin convention must run at generation time.
- Speed: per doc **measured** decode 2.2× (SIMPLE), 1.9× (NESTED); encode 1.46×, 1.34×; decode allocation ~56% lower. Startup [guess]: 1–2 classes per type vs 2N+1 (SimpleEntity has 17 AOT classes; 11-type benchmark set has 91). Cold start (~314–321 ms both mappers) dominated by framework init.
- Simplicity [guess]: removes ~1k LOC (encoders, decoders, creators, accessors) + ~1.3k LOC of critter per-property generators; adds a 2–4k LOC generator covering every shape (generics, references, polymorphism, ctors/records/finals, `@AlsoLoad`/`@LoadOnly`, `@ExternalEntity`, Kotlin delegates, merge, refresh); still needs a matching interpreter. Net: more code, harder to debug.
- Migration: only after O2/O3; generate simple shapes first, interpreter for the rest. Risks: backend drift (needs differential harness), generics, finals, modules, baked AOT config.

### O2 — `EntityModel` as build-time IR compiled to a flat runtime form
- Conventions + validation unchanged. After `register`, compile each type to: slot arrays (stored name, load names, `MethodHandle` accessors, `TypeData`, flags); id/version slots; creator plan; lifecycle bits + `MethodHandle`s; discriminator data.
- Per-datastore `EntityCodec` interpreter holds resolved codecs by slot → **takes codecs off `PropertyModel`**. Other consumers migrate gradually.
- Both mappers produce the same form; critter's benefit becomes faster mapping/accessors; per-property model subclasses unnecessary. No new generation. Compat excellent (`Mapper`, SPI, kotlin, old AOT output keep working).
- **Measured (`flat`)**: SIMPLE decode 1603 → 833 ns, 2240 → 1024 B; NESTED decode 4573 → 2641 ns, 6920 → 3048 B; encode SIMPLE 815 → 667 ns, NESTED 2546 → 2258 ns → ~88% of generated decode gain, 44–58% of encode gain. Startup roughly neutral. Simplicity modest unless the runtime form *is* O3's descriptor. Low risk; protocol fixes can land first inside `EntityDecoder`/`EntityEncoder`.

### O3 — Immutable descriptors replacing `EntityModel`/`PropertyModel`
- `EntityDescriptor` record: type, collection, discriminator, annotations, `PropertyDescriptor[]`, id/version/shard slots, lifecycle info, creator plan, maps by Java name and stored name. Separate builder serves (h). (f) → mapper `TypeRegistry`. Codecs per datastore.
- Critter emits one descriptor factory per type instead of 2N+1 subclasses whose setters throw.
- Speed neutral unless the protocol changes; also fixes generated `getTypeData()` allocating per call (`GenerationUtils.java:352-368`).
- Biggest clarity win: one discovery pipeline; no copy constructors; no datastore state on mapper objects; descriptors shareable.
- Breaks public `MorphiaConvention` SPI and kotlin module → bridge with `EntityModel` as the builder for a deprecation cycle; keep `getEntityModel` as an adapter. Migrate (g) → (c) → (b) → (e) → (a).

### O4 — Truly no model
- Lazy reflection at each use site: repeats reflection on hot paths (`PathTarget` per query), spreads rules across ~40 consumers, no validation point; caching it rebuilds a model.
- Delegating to the driver's `PojoCodecProvider`/record codec: same interpretive `ClassModel`/`PropertyModel` design (Morphia already implements its `PropertyAccessor`/`PropertySerialization`, `PropertyModel.java:45-46`) → no speed gain; lacks references/lazy proxies, `@AlsoLoad`/`@LoadOnly`/`@Version`, lifecycle, `@ExternalEntity`, storeNulls/storeEmpties/ignoreFinals, `DateStorage`, `Conversions` fallbacks, map-key conversions; paths/indexes/shards still need a model. **Strawman.**

### O5 (recommended) — builder → immutable descriptor → per-datastore codec plan
- O3's shape with O2's codec; O1 as optional critter backend implementing the same codec contract.
- Speed: ~1.9× decode / 1.2× encode with the interpreter; up to ~2.2× / 1.46× with generation (SIMPLE). Larger gains expected for POLYMORPHIC/COLLECTIONS/LIFECYCLE [guess].
- LOC net down ~1.5–2.5k without the generator [guess]. Current sizes: `codec/pojo` 2,488; critter 3,574; creators/accessors/conventions/mappers/`PathTarget`/discriminators 2,401; validation 784; core main total 42,749.

## 4. Prototype
Dir `scratchpad/no-entitymodel/`; sources `src/dev/morphia/benchmarks/handwritten/`; compiled `classes/` (incl. JMH-generated).
- Hand-written codecs (`SimpleEntityCodec`, `NestedEntityCodec`, `AddressCodec`, `ProfileCodec`): straight-line writes, constant names, inline `_t`; `switch` decode; `static final VarHandle`s; no boxing; embedded codecs called directly. Semantics kept for tested cases: `_id` generation, storeNulls/storeEmpties, nulls skipped on read, lenient numerics, `Conversions` slow path, optimistic `_t` check with reset + subtype hand-off, unknown fields skipped, single value read into a `List`. Shared support in `CodecSupport`.
- `FlatModelCodec` (O2/O3 simulation): flat slots, `MethodHandle`s, one `HashMap`, one shared loop, same protocol.
- `Verify` main: encodes byte-for-byte equal to Morphia's; decoding interchangeable both directions; storeNulls off/on, sparse entities, foreign documents (shuffled, unknown fields, int64→int, int32→long/double, explicit nulls, no `_t`, single value for a list), id generation. **reflection + critter-runtime 152/152; critter AOT 76/76.**
- Side finding: Morphia's own decode→encode isn't idempotent — an empty list skipped under `storeEmpties=false` comes back null, and `storeNulls=true` then writes null.
- `HandwrittenCodecBenchmark` mirrors `CodecBenchmark` with `@Param impl=morphia|flat|handwritten` (setup fails unless encoding identical). `HarnessFloorBenchmark` measures what no codec can remove.

### Results (`-f 1 -wi 3 -i 5 -w 1 -r 1 -prof gc`; single fork — differences < ~20% are noise). ns/op (B/op)
| | SIMPLE decode | SIMPLE encode | NESTED decode | NESTED encode |
|---|---|---|---|---|
| coordinator baseline, CodecBenchmark reflection | 1676 (2276) | 877 (1184) | 4825 (6920) | 2903 (1856) |
| same run, CodecBenchmark | 1845±1509 (2240) | 784±119 (1184) | 4452±567 (6920) | 2869±2132 (1856) |
| same run, impl=morphia | **1603**±236 (2240) | **815**±127 (1184) | **4573**±803 (6920) | **2546**±298 (1856) |
| flat (O2/O3) | 833±135 (1024) | 667±245 (1184) | 2641±406 (3048) | 2258±425 (1856) |
| handwritten (O1 bound) | **725**±91 (976) | **560**±92 (1136) | **2377**±534 (3136) | **1894**±320 (1856) |
| floor | walk 394 (672) | empty 150 (992) | walk* 291 (528) | empty 154 (992) |
\* NESTED walk skips embedded docs whole → not a true floor for NESTED decode.
- Decode: above the walk floor, Morphia spends ~1210 ns on SIMPLE vs ~330 ns hand-written.
- Encode: dominated by driver writer + harness (992 of 1184 B/op is the 512-byte buffer + writer); the model accounts for ~255 of 815 ns.
- Not covered: COLLECTIONS, POLYMORPHIC, LIFECYCLE, references, `@AlsoLoad`, constructor-based creation.
- Raw results: `scratchpad/results/handwritten-codec-reflection.{log,json}`, `scratchpad/results/harness-floor.{log,json}`.

## 5. Recommendation — O5 in stages
0. **Differential test harness** over every test entity (byte-for-byte encode, cross-implementation decode equivalence, foreign documents); template `Verify.java`. Gates every later step.
1. **Rewrite the decode protocol inside `EntityDecoder`/`EntityEncoder`** (no type/API change): optimistic `_t` check instead of pre-scan; BSON-type check instead of a mark per property; no-arg creator fast path; skip `Conversions` when the value already has the target type or its wrapper; single name lookup; precomputed serialization flags. Expected ≈ `flat` row for decode (~1.9×), more for polymorphic/collection elements. Overlaps the codec hot-path track.
2. **Move per-datastore codecs off `PropertyModel`** into the per-datastore codec; Document round trip for lifecycle only when a hook/global listener needs a `Document`.
3. **Immutable `EntityDescriptor` + mapper `TypeRegistry`**, compiled from `EntityModel` after validation; migrate (g) → (c) → (b) → (e) → (a).
4. **Critter emits a descriptor factory per type** instead of 2N+1 classes.
5. **Optional generated codec backend** with interpreter fallback — only if the remaining gap over flat (~110 ns per SIMPLE doc each way) matters.
6. **Retire `EntityModel` from runtime APIs**: v2 `MorphiaConvention` on the builder, kotlin bridge, deprecated `getEntityModel` adapter.

Pays off only if: workloads are CPU-bound on (de)serialization (~0.8 ms CPU saved per 1,000 SIMPLE docs [guess]); the differential harness pins edge cases (`Conversions` fallbacks, `_t` anywhere, subtype hand-off, PreLoad before properties set, `@AlsoLoad`/`@LoadOnly`, storeNulls/storeEmpties, refresh); for step 5, the team accepts two codec implementations in lockstep for ~13% (decode) / 16–20% (encode) over flat.

**Confident** [measured/read]: per-document overhead is in the protocol and creation path, not in having a model; critter's generated models don't change per-doc cost; a flat interpreter gets most of the decode gain; only generated code removes boxing and megamorphic dispatch; §1 consumer needs.
**Guesses**: startup savings, LOC estimates, gains for COLLECTIONS/POLYMORPHIC/LIFECYCLE, effect on real applications. Next useful measurement: hand-written/flat codecs for polymorphic and lifecycle models.
