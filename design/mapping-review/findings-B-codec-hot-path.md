# Findings B — Codec hot path (entity ↔ BSON)

(Agent B report, saved by the coordinator because the agent's own file write was blocked.)
Read-only; no repo edits, no Maven. Read every in-scope file plus driver `ProvidersCodecRegistry`, `CodecCache`, `BsonBinaryReader.Mark`, `NumberCodecHelper`, `DecoderContext` (javap, bson-5.13.0) and ByteBuddy `TypeCache.SimpleKey`.

## Probes (plain Java, single run, JDK 21 — timings rough, byte counts reliable; probe sources not included)

| Probe | Result |
|---|---|
| `CodecRegistry.get(Class)` on `fromRegistries(fromCodecs(x), fromProviders(...))`, warm | **48 B/op, ~33 ns** vs `ConcurrentHashMap<Class,Codec>.get` **0 B, ~16 ns** |
| `BsonBinaryReader.getMark()` in a `decodeModel`-shaped loop (16 props) | **2,672 B / 1,131 ns per doc with a mark per property vs 1,392 B / 930 ns without** (80 B/property; `reset()` +40 B) |
| Discriminator pre-scan, 8-field `{_id,_t,...}` doc | current (mark, scan, lookup, `registry.get`, reset, re-read): **1,040 B, ~700 ns**; self-match instead of lookup+registry: 992 B; single pass: **680 B, ~420 ns** |
| `double[1536]` via `TypedArrayCodec` pattern vs direct primitive I/O | encode **166 µs vs 55 µs (+37 KB boxing)**; decode **100 µs vs 42 µs (+26 KB)**; `Array.get/set` are plain natives, not intrinsics |

Both mappers share the codec code (`EntityEncoder`, `EntityDecoder`, `ConstructorCreator`, `EntityModel.getProperty`…) — findings apply equally to reflection, critter AOT and critter runtime unless stated.

## Ranking
| ID | Title | Category | Effort | Conf |
|---|---|---|---|---|
| B1 | Reader mark taken for every decoded property | alloc / hot-path | S–M | high (measured) |
| B2 | Every (embedded) document pre-scanned for discriminator, rewound, re-read | hot-path / alloc | S (step 1) / M (step 2) | high / med-high |
| B3 | Lifecycle codecs materialize a full `Document` per encode/decode; any global listener forces it on all entities; probable double callback | hot-path / alloc / bug | M (log guard S) | high mech / med magnitude |
| B4 | `ConstructorCreator` buffers every property even for no-arg ctors; `NoArgCreator` unreachable | alloc / dead-code | S | high |
| B5 | Per-value `CodecRegistry.get` allocates in the driver | alloc / hot-path | S | high (measured) |
| B6 | `EntityModel.getProperty` always 2 lookups (= D4) | hot-path | S | high |
| B7 | Primitive/object arrays: reflection + boxing per element | hot-path / alloc | M | high (measured) |
| B8 | `Conversions.convert` per decoded property; primitive/interface types pay 2 probes for nothing | hot-path | S | high mech / small gain |
| B9 | Collection/map codecs instantiate concrete types reflectively per decode | hot-path / alloc | S | high |
| B10 | Lazy references build proxy reflectively per decoded reference | hot-path / alloc | S–M | high mech / est. magnitude |
| B11 | Encode loop recomputes constants per property | hot-path / clarity | S | high / small gain |
| B12 | Date codecs allocate intermediates per value | alloc | S | medium |
| B13 | Structure: codec state spread across shared models + overlapping encoder/decoder subclasses | simplicity | M–L | medium |
| B14 | Dead code & small clarity fixes | dead-code | S | high |

## B1 — Reader mark per decoded property, scalars included
- **Locations**: `EntityDecoder.java:59-79` (`getMark()` at 64, fallback 73-75); codec set in `MorphiaCodec.java:199-210`.
- **Current**:
  ```java
  final BsonReaderMark mark = reader.getMark();      // before even the NULL check
  try {
      if (reader.getCurrentBsonType() == BsonType.NULL) { reader.readNull(); }
      else { instanceCreator.set(decoderContext.decodeWithChildContext(model.getCodec(), reader), model); }
  } catch (BsonInvalidOperationException e) { mark.reset(); ... registry.get(Object.class) ... convert ... }
  ```
  Mark = `BsonBinaryReader.Mark` (48 B) + `BsonInputMark` (24 B) → 80 B measured. #4350 deliberately left it (driver can't rewind otherwise).
- **Proposed**: skip the mark when the codec can't consume input then throw. At codec specialization compute a per-property `int directBsonTypes` bitmask from a static table keyed on the **exact codec class** (user codecs not in table → today's path). Table (checked against `NumberCodecHelper` bytecode): `StringCodec`→STRING,SYMBOL; `ObjectIdCodec`→OBJECT_ID; `IntegerCodec`→INT32 only (INT64/DOUBLE/DECIMAL128 read then throw on precision loss); `LongCodec`→INT32,INT64; `DoubleCodec`→DOUBLE,INT32; `BooleanCodec`→BOOLEAN; `Decimal128Codec`→DECIMAL128; driver `DateCodec`→DATE_TIME; Morphia `EnumCodec`→STRING; `MorphiaLocalDateTimeCodec`→DATE_TIME,INT64; `MorphiaDateCodec`→DATE_TIME.
  ```java
  if (model == null) { reader.skipValue(); return; }
  BsonType t = reader.getCurrentBsonType();
  if (t == BsonType.NULL) { reader.readNull(); return; }               // hoisted above the mark
  if ((model.directBsonTypes & (1 << t.ordinal())) != 0) {
      instanceCreator.set(decoderContext.decodeWithChildContext(model.getCodec(), reader), model); return;
  }
  BsonReaderMark mark = reader.getMark(); ... existing try/fallback ...
  ```
  Optional: for a trusted leaf codec meeting a type it rejects *before* reading (e.g. `StringCodec` on INT32) go straight to fallback — avoids a throw/catch with stack trace per drifted field per doc.
- **Impact**: 80 B + ~13–17 ns per scalar property per document (top-level and embedded). SIMPLE 8 leaf props ≈ 640 B (largest remaining item); NESTED 15 ≈ 1.2 KB (~17%); COLLECTIONS 41 ≈ 3.3 KB (~15%); POLYMORPHIC 30 ≈ 2.4 KB (~17%). Probe: ~20% less time per doc.
- **Risk**: @MorphiaInternal; identical behavior for table codecs on matching types (they can't throw `BsonInvalidOperationException` there). Store the bitmask in base `PropertyModel`, set in `codec(...)`; critter doesn't override that.
- **Effort** S–M — **Confidence** high (measured). **Measure**: `CodecBenchmark.decode -prof gc` SIMPLE/NESTED/COLLECTIONS, all variants; add a "schema drift" model.

## B2 — Discriminator pre-scan + rewind + re-read on every (embedded) document
- **Locations**: `EntityDecoder.java:43-57` (decode), `95-118` (`getCodecFromDocument`: mark 101, `registry.get(discriminatorLookup.lookup(reader.readString()))` 106, reset 115), `88` (`_t` re-read via `readString()`); `DiscriminatorLookup.java:83-97`; `MorphiaCodec.java:72`.
- **Current**: `DecoderContext.decodeWithChildContext` always passes `DEFAULT_CONTEXT` (`hasCheckedDiscriminator` false, javap-confirmed) → every embedded entity (field, collection element, map value) and every root runs the pre-scan; `@Entity` defaults `useDiscriminator=true`. Per doc: mark (80 B) → read names to `_t` discarding `_id` → `readString` → `DiscriminatorLookup.lookup` (`containsKey`+`get`) → `registry.get(Class)` (48 B) → `reset()` (new `Context`, 40 B) → full decode re-reading `_id`/`_t` + another discarded `_t` string at line 88. A doc **without** `_t` is read twice in full (once with `skipValue`).
- **Proposed**: **Step 1 (S)**: `MorphiaCodec.codecForDiscriminator(String d)` → `this` when `d.equals(entityModel.discriminator())`, else per-codec `ConcurrentHashMap<String, Codec<?>>` filled via existing lookup; in `decodeProperties` replace `reader.readString()` with `reader.skipValue()`. **Step 2 (M)**: single pass for self-match — read in order, buffer values before `_t` (normally only `_id`); if `_t` is self, continue; if subtype and nothing consumed before `_t`, hand off to the subtype decoder via an internal "continue after discriminator" entry (Morphia writes `_t` first in embedded docs); else reset (mark still taken at start). Abstract/interface models never self-match → keep scan + step-1 cache. Doc without `_t` decodes in one pass.
- **Impact**: per root/embedded doc. Step 2 ≈ **360 B + 250–300 ns per 8-field doc**; step 1 alone ≈ 48 B, ~17 ns, two lookups. Docs per op: SIMPLE 1, NESTED 5 (~1.8 KB of 7.1 KB), COLLECTIONS 11 (~4 KB of 21.7 KB); POLYMORPHIC 12 subtype elements → mostly step-1 cache / step-2 hand-off.
- **Risk**: self-match ≡ today (`registry.get(ownType)` already returns `this`: `fromCodecs(this)` first at `MorphiaCodec.java:72`; discriminators unique since `addModel` throws on dupes). Protected `getCodecFromDocument` signature changes (@MorphiaInternal). Critter `discriminator()`/`useDiscriminator()` are constants. Step 2 must not run user ctors speculatively — buffer pre-`_t` values (interacts with B4).
- **Effort** S / M — **Confidence** high (step 1), med-high (step 2; needs tests for `_t` placement and missing `_t`). **Measure**: decode NESTED/COLLECTIONS/POLYMORPHIC/SIMPLE `-prof gc`; add a fixture without `_t`.

## B3 — Lifecycle codecs materialize a `Document` per encode/decode; global listeners force it everywhere
- **Locations**: `MorphiaCodecProvider.java:71-76` (`|| mapper.hasListeners()`); `LifecycleEncoder.java:31-44`; `LifecycleDecoder.java:36-61`; `EntityModel.java:198-207` (eager `LOG.debug(Sofia.callingInterceptorMethod(...))` at 204); `EntityListenerAdapter.java:56-79` (`collectArgs`); `OnEntityListenerAdapter.java:26-44`; writer `DocumentState.end()`, `RootState.getDocument()`; reader `DocumentState.startDocument()`.
- **Current**: **Encode**: `new Document()` → PrePersist → `DocumentWriter` (`NameState` + `SingleValue` per field, boxing); `DocumentState.end()` builds a `Document`, `RootState.getDocument()` copies into a second; `writeDateTime` longs → `LocalDateTime`; PostPersist; `registry.get(Document.class)`; `DocumentCodec` re-encodes with a registry lookup per value. **Decode**: `registry.get(Document.class).decode(...)` → full `Document`/`ArrayList`/boxed tree; `DocumentReader` rebuilds a state chain with streams (`List.of` per entry) → decoded property by property; with `_t` also a per-doc `registry.get(discriminatorClass)`. Nested lifecycle entities rebuild their subtree per level. `mapper.hasListeners()` (any `addInterceptor`) puts **every** entity codec (embedded too) on this path. Per global listener/event/doc `Sofia.callingInterceptorMethod` runs `MessageFormat.format` eagerly regardless of log level. `collectArgs` allocates per call (`getParameterTypes()` clone, `ArrayList`, `toArray`).
- **Probable bug**: `LifecycleEncoder<Super>` runs callbacks *before* `EntityEncoder.encode` delegates a subtype value to `LifecycleEncoder<Sub>`, which runs inherited callbacks again → @PrePersist/@PostPersist fire twice for polymorphic values (e.g. `List<Shape>` with `Shape` @PrePersist). Needs a test.
- **Proposed**: compute `needsDocumentOnPersist` / `needsDocumentOnLoad` at codec creation (true only if a global listener `hasAnnotation(event)` or a model listener method for those events declares a `Document` param; `EntityListenerAdapter.usesDocument(event)` from param types). When false, stream: **encode** — delegate subtype *first* (fixes double callback) → PrePersist(entity,null) → `super.encode(realWriter)` → PostPersist(entity,null); **decode** — resolve discriminator B2-style → `creator.getInstance()` → PreLoad(entity,null) → `decodeProperties(realReader)` → PostLoad(entity,null). Gate on event coverage, not `hasListeners()`. S-effort extras: `LOG.isDebugEnabled()` guard; precomputed argument-kind array per callback.
- **Impact**: every doc of a lifecycle entity, and of **all** entities once an interceptor is registered; document materialized ~2× per direction + registry lookup per value. LIFECYCLE should drop near a plain entity's per-field cost (estimate, unprobed).
- **Risk**: callbacks taking a `Document` and all global `EntityListener`s keep the buffered path → semantics unchanged; null `Document` only reaches internal adapters. @MorphiaInternal; same for both mappers.
- **Effort** M (log guard S) — **Confidence** high mech / medium magnitude; double-callback needs a test. **Measure**: LIFECYCLE encode/decode; add a no-op global `EntityListener` variant (via `mapper.addInterceptor`) on SIMPLE/NESTED.

## B4 — `ConstructorCreator` buffers every property even for no-arg ctors; `NoArgCreator` unreachable
- **Locations**: `InstanceCreatorFactoryImpl.java:39-66`; `ConstructorCreator.java:45-46, 63-67, 137-149, 187-202`; `ConstructorCreator.bestConstructor:107-127`; `NoArgCreator.java`.
- **Current**: `bestConstructor` picks the no-arg ctor whenever one exists (`allMatch` on zero params is vacuously true) → POJOs (SimpleEntity, NestedEntity, CollectionsEntity, Drawing, LifecycleEntity) get a `ConstructorCreator` with an empty plan. Per decode: zero-length `zeroValues.clone()`, two `ArrayList`s, two `Object[10]` once anything is added (growth past 10). Per property: `plan.positions.get(name)` miss → `pendingModels.add` + `pendingValues.add`; `getInstance()` replays through `setValue`. The `else` branch's `getDeclaredConstructor()` always throws `NoSuchMethodException` → `new NoArgCreator(...)` + its `LOG.info` are dead.
- **Proposed**: when `getParameterCount() == 0`, a direct-set creator (instantiate lazily on first `set`/`getInstance`; set via `model.setValue(instance, v)` so conversions still apply — note `NoArgCreator` skips conversions). Lazily allocate pending lists (only partial ctors need them). Optionally `int[]` by property ordinal instead of `positions` HashMap (B13). Delete `NoArgCreator` + dead branch.
- **Impact**: ~176 B + 1 hash probe per property per no-arg-built entity/embedded doc (roots of all 5 models). Full-ctor types (Address, Profile, Circle) still allocate ~48 B of empty lists per instance.
- **Risk**: @MorphiaInternal. B2 step 2 must buffer pre-`_t` values before this creator instantiates. `LifecycleDecoder` and refresh already call `getInstance()` first.
- **Effort** S — **Confidence** high. **Measure**: decode SIMPLE/LIFECYCLE B/op; NESTED/COLLECTIONS/POLYMORPHIC roots.

## B5 — Per-value `CodecRegistry.get` allocates in the driver
- **Locations**: `EntityDecoder.java:106`; `EntityEncoder.java:62-66` (subtype values); `LifecycleEncoder.java:43`; `LifecycleDecoder.java:37,44`; `ObjectCodec.java:58,65`; `MorphiaMapCodec.java:51` (per entry); `ArrayCodec.java:58,77,79` (per element); `ReferenceCodec.java:175-181` + encode path; `MorphiaDatastore.buildRegistry` (`fromProviders`, ~line 241).
- **Current**: driver `ProvidersCodecRegistry.get(Class)` allocates `ChildCodecRegistry`, `CodecCacheKey`, `Object[]` for `Objects.hash`, `Optional`, capturing lambda per call — 48 B, ~33 ns post-JIT vs 0 B, ~16 ns for a CHM. POLYMORPHIC pays it 12× per doc on encode (`EntityEncoder:63`) and 12× on decode. `ArrayCodec.readValue` looks up the *fixed* component codec per element.
- **Proposed**: (a) per-`MorphiaCodec` `ConcurrentHashMap<Class<?>, Codec<?>>` for subtype values on encode + B2 discriminator cache. (b) wrap the datastore registry in a memoizing `CodecRegistry` (`get(Class)` checks a CHM first; other overloads delegate) — semantics unchanged (driver `CodecCache` already caches per class with `putIfAbsent`); also speeds the driver's own per-op lookups and query encoding. (c) `ArrayCodec`: resolve component codec once, as `TypedArrayCodec` does.
- **Impact**: ~48 B + ~17 ns per lookup; POLYMORPHIC encode and decode each save ~600 B/op.
- **Risk**: low; caches never go stale (registry + discriminator bindings fixed per datastore). Wrapper must implement the `CodecProvider` overloads if nested inside `fromRegistries`.
- **Effort** S — **Confidence** high (measured). **Measure**: POLYMORPHIC, LIFECYCLE.

## B6 — `EntityModel.getProperty` always 2 lookups (same as D4)
- `EntityModel.java:355-357`, called from `EntityDecoder.java:90`: `getOrDefault(name, propertyModelsByName.get(name))` evaluates the default eagerly → second `LinkedHashMap` probe per decoded field (with full `String.equals` when mapped name == Java name).
- **Fix**: `PropertyModel p = byMapped.get(name); return p != null ? p : byName.get(name);` Optional: "expected next index" hint (Morphia writes in model order) to skip hashing in the common case.
- **Impact**: ~5–10 ns per field per doc. **Risk** none. **Effort** S — **Confidence** high.

## B7 — Primitive/object arrays: reflection + boxing per element
- **Locations**: `TypedArrayCodec.java:28-40` (`Array.get` 33), `56-70` (`ArrayList` then `Array.set` 68); `ArrayCodec.java:30-80`; `ArrayFieldAccessor.java:40-90` (`Array.set` 53, 64, 83).
- **Current**: encode — `Array.get` (JNI, boxes) → boxed codec per element; decode — box + `ArrayList` add → `Array.set` (JNI). `ArrayCodec` returns `Object[]`; `ArrayFieldAccessor` converts element by element via `Array.set` + `Conversions`.
- **Proposed**: specialize `TypedArrayCodec` per primitive component (typed loop `writer.writeDouble(arr[i])`; decode into a growable primitive array with native-BSON-type fast path and boxed-codec fallback); wrapper arrays via `list.toArray(new T[0])`. `ArrayCodec`: cache component codec (B5); build the typed array directly when all elements are instances of the component type.
- **Impact**: `double[1536]` (embedding vectors): encode 3.0×, decode 2.4× faster, ~24 B/element saved. Narrow usage but large for vector-search users.
- **Risk**: numeric widening/narrowing must match boxed codecs (fallback keeps it). Package-private, internal.
- **Effort** M — **Confidence** high (measured). **Measure**: add an ARRAYS model.

## B8 — `Conversions.convert` per decoded property
- **Locations**: `PropertyModel.setValue` (`PropertyModel.java:386-390`); `ConstructorCreator.java:194`; `Conversions.convert` (`Conversions.java:145-166`, `computeIfAbsent` 153-155); `EntityDecoder.java:75`.
- **Current**: `fromType.equals(target)` fails for `int`/`Integer`, `List`/`ArrayList`, `Set`/`HashSet`, `Map`/`HashMap` → `computeIfAbsent(fromType, ...)` + `.get(target)` miss → unchanged value. `computeIfAbsent` also inserts an empty CHM per new value class. Fallback path converts twice (`EntityDecoder:75` then `setValue`).
- **Proposed**: precompute a boxed type per `PropertyModel` and `Plan` parameter; `value != null && boxedType.isInstance(value)` → set directly. `get` not `computeIfAbsent`. Drop double conversion.
- **Impact**: ~10–20 ns per primitive/interface-typed property per doc (SIMPLE 4, COLLECTIONS 5, NESTED ≥1).
- **Risk**: a registered conversion from a type to its own supertype would be skipped (no defaults like that). **Effort** S — **Confidence** high / small gain.

## B9 — Collection/map codecs instantiate concrete types reflectively per decode
- **Locations**: `CollectionCodec.getInstance` (`CollectionCodec.java:69-85`, `getDeclaredConstructor().newInstance()` at 81); `MorphiaMapPropertyCodecProvider.MapCodec.getInstance` (~111-122: `getDeclaredConstructor()`, `setAccessible(true)`, `newInstance()`).
- **Current**: per decode of a declared concrete type (`ArrayList<String>`, `LinkedHashSet`, `HashMap`, `TreeMap`) — ctor lookup copying the `Constructor`; maps also a caller-sensitive `setAccessible` check. Interface types: cheap `isInterface()`/`isAssignableFrom` per call.
- **Proposed**: resolve a `Supplier` once in the codec ctor (`ArrayList::new`, `HashSet::new`, `HashMap::new`, or cached ctor/`MethodHandle`); optionally precompute the map key converter (identity for `String`).
- **Impact**: a few hundred ns + a `Constructor` copy per concrete collection/map property per doc. **Effort** S — **Confidence** high. **Measure**: models use interfaces only → add concrete `ArrayList`/`LinkedHashMap`/`TreeMap` variant.

## B10 — Lazy references build the proxy reflectively per decoded reference
- **Locations**: `ReferenceCodec.java:257-271` (`createProxy`), `183-185` (`getCacheKey`), `175-181` (decode, registry lookup per value).
- **Current**: per lazy ref decode — `getCacheKey` (`getInterfaces()` clone, ByteBuddy `SimpleKey`: `ArrayList` copy, `CompoundList`, `HashSet` of names) → `TypeCache.findOrInsert` → `getDeclaredConstructor().newInstance()` → `getDeclaredField("handler")` (Field copy) + `setAccessible` + `Field.set`. Proxy type depends only on `propertyModel.getType()` (fixed per codec).
- **Proposed**: resolve once lazily per `ReferenceCodec`: proxy class + ctor `MethodHandle` + `handler` field handle (or ByteBuddy ctor taking the `InvocationHandler`); cache BSON-type → codec (B5).
- **Impact**: ~1 µs and ~1 KB per lazy reference (estimate). **Risk**: strong ref to proxy class lives as long as the codec. **Effort** S–M — **Confidence** high mech / est. magnitude. **Measure**: add LAZY_REFERENCE model (no DB call at decode).

## B11 — Encode loop recomputes constants per property
- **Locations**: `EntityEncoder.java:41-67` (deep `propertyModel.equals(idModel)` at 56, capturing lambda at 46), `114-118`; `MorphiaPropertySerialization.java:36-59`.
- **Current**: `PropertyModel.equals` deep (name, `TypeData`, mapped name…) where identity is meant. `shouldSerialize` calls 3 config getters per call (`Boolean` unboxing + `orDefault`), loops annotations calling `annotationType()` (JDK proxy invocation for reflection-mapped annotations) — all constant per property.
- **Proposed**: precompute non-id `PropertyModel[] encodeOrder` per codec, compare with `==`; precompute final booleans in `MorphiaPropertySerialization` (`storeNulls`, `storeEmpties`, `skip = loadOnly || (ignoreFinals && isFinal)`); drop always-skipped properties from `encodeOrder`; inline `writeStartDocument`/`writeEndDocument` instead of the lambda.
- **Impact**: a few ns per property per encode; also cleans the copy ctor sharing `serialization`. **Effort** S — **Confidence** high / small gain.

## B12 — Date codecs allocate intermediates per value
- **Locations**: `MorphiaLocalDateTimeCodec.java:33,35,40`; `MorphiaDateCodec.java:31,37`.
- **Current**: `ofEpochMilli(ms).atZone(zone).toLocalDateTime()` / `value.atZone(zone).toInstant().toEpochMilli()` → `Instant` + `ZonedDateTime` (+ `ZoneRules` lookup for "UTC" region); zone fetched via `datastore.getMapper().getConfig().dateStorage().getZone()` each time.
- **Proposed**: for fixed-offset storage (UTC default) resolve a `ZoneOffset` once per codec; `LocalDateTime.ofEpochSecond(...)` / `value.toEpochSecond(offset)`. Keep current path for `SYSTEM_DEFAULT`.
- **Impact**: ~60–70 B + tens of ns per date value (unprobed). **Effort** S — **Confidence** medium.

## B13 — Structure: codec state spread across shared models + overlapping subclasses
- **Locations**: `MorphiaCodec.java:146-210` (mutable `setEncoder`/`setDecoder`, lazy fields, `specializePropertyCodecs`); `PropertyModel.java:317-323, 418-424, 445-447`; `LifecycleEncoder`; `LifecycleDecoder`; `MergingEncoder` (raw `extends EntityEncoder`, no-op hooks); `MorphiaCodecProvider.getRefreshCodec` (92-115, used from `MorphiaDatastore.java:514`).
- **Current**: property codecs stored on the shared `PropertyModel`; `specializeCodec` returns whichever was set first → a mapper passed to two `MorphiaDatastore`s via the public `(client, config, mapper)` ctor reuses the first datastore's codecs incl. a `ReferenceCodec` bound to it (cf. D1). `LifecycleDecoder` resolves the discriminator its own way (via the `Document`'s `_t`), ignoring `hasCheckedDiscriminator`. `LifecycleEncoder` runs callbacks before the type check (B3). `refresh()` rebuilds a `MorphiaCodec` + registry + `PropertyCodecRegistryImpl` per call wrapped in anonymous decoder/creator classes; its creator skips `Conversions`.
- **Proposed**: each `MorphiaCodec` gets an immutable plan built once: `PropertyModel[]`, `Codec<?>[]`, `directBsonTypes[]` (B1), `boxedTypes[]` (B8), ctor positions (B4), `encodeOrder` (B11), lifecycle flags (B3), discriminator + subtype caches (B2, B5). Lifecycle = flags + optional buffering step, not subclasses. `MergingEncoder` → callback on encode output. Refresh → a creator wrapping an existing instance, used with the normal codec. Keeps `PropertyModel` immutable after mapping (thread safety) and gives B1/B4/B6/B8/B11 a single home.
- **Impact**: enabling; little direct speed. **Effort** M–L — **Confidence** medium.

## B14 — Dead code & small clarity fixes
- `NoArgCreator`: unreachable (B4).
- `PropertyCodecRegistryImpl.java:51-52`: `CollectionPropertyCodecProvider` + second `MorphiaCollectionPropertyCodecProvider` unreachable for collections (`MorphiaCodecProvider.java:54-55` already puts one first).
- `DiscriminatorLookup`: `packages` (43) never populated → `searchPackages` (111-120) no-op; `lookup` (84-86) `containsKey`+`get`; unknown discriminators re-run throwing `Class.forName` per doc (no negative cache).
- `UntypedEntityListenerAdapter.java:28`: `invoke(..., Datastore)` overloads rather than overrides → never called.
- `EntityDecoder`: line 88 `readString()` on `_t` → `skipValue()`; line 75 double conversion (B8).
- `MorphiaCodecProvider`: line 38 `codecs` plain `HashMap` mutated during lookups; lines 71-76 `hasListeners()` read once at codec creation.
- `ArrayCodec.getEncoderClass()` returns null. `MergingEncoder.encodeValue` evaluates `shouldSerialize` twice.

## Already efficient — don't re-propose
Property codecs resolved once per `MorphiaCodec` (`specializePropertyCodecs`); collection/map element codecs fixed per property; `Encoder`/`DecoderContext` child contexts are static constants; `DateStorage.UTC` caching, `CHECKED_DISCRIMINATOR` constant and `properties()` view (#4350); `ConstructorCreator.Plan` (#4347); critter `getName`/`getMappedName`/`getType`/`useDiscriminator` are `ldc` constants; `EnumCodec` decodes via the enum constant directory.

## Suggested benchmark additions
ARRAYS (`double[1536]`, `String[]`) — B7; concrete collection types — B9; LAZY_REFERENCE — B10; no-op global interceptor variant — B3; documents without `_t` — B2; schema drift (int stored for String field) — B1. Always `-prof gc`, all three variants.
