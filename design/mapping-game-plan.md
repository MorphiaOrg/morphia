# Type-Mapping Game Plan

This is the action plan derived from [`mapping-review.md`](mapping-review.md). Item IDs (A1, B2, D10, …) point into the
lane files in [`mapping-review/`](mapping-review/). Numbers in `#` refer to the review's ranked list (§4).

## Evaluation of the review

**What holds up.** Spot-checked against the source at `153ee9d`. Every claim checked was accurate:
- `EntityModel.getProperty` evaluates its `getOrDefault` fallback eagerly (`EntityModel.java:356`).
- `PathTarget` compiles a regex for every segment (`PathTarget.java:166`).
- `bestConstructor` matches no-arg constructors vacuously, so the `NoArgCreator` branch in
  `InstanceCreatorFactoryImpl` is unreachable.
- `AbstractMapper.register` does a get and then a put, and `ReflectiveMapper.mapEntity` is unsynchronized.
- Sessions call `mapper.copy()` (`MorphiaDatastore.java:360`), and the copy constructor skips `importModels()`.
- Datastore-bound codecs are cached on `PropertyModel` (`PropertyModel.java:70, 420-422`).
- `LifecycleEncoder` runs `@PrePersist` before `EntityEncoder` hands subtypes off. The double-callback bug is real
  as far as reading the code shows.
- The package scan uses `enableAllInfo()` and `Class.forName(…, true, …)`.

The central argument is backed by measurements and is sound: per-document cost is in the decode protocol, not in the
model objects. It means most of the speed can be had without an API change.

**Where I'd change the plan:**
1. **Some correctness fixes would be done twice.** §6 step 1 says "fix §5 issues", but several of them are fixed by
   structural Tier 2 items: A3/D7 by #18, A6 by #19, and C3 by the critter track (4.5–4.8). Do those as the
   structural change, not as a patch followed by a rewrite.
2. **A minimal codec plan (#14) has to come before the hot-path items.** B1 suggests storing the `directBsonTypes`
   bitmask on `PropertyModel`. That bitmask comes from the *codec*, and codecs are per datastore, so storing it there
   repeats the D1 bug. The same applies to boxed types (B8) and `encodeOrder` (B11). Introduce the per-`MorphiaCodec`
   plan object first, even if it is thin, so this state has somewhere correct to live.
3. **#16 (critter as an accessor strategy) is rejected (decision 1.1).** Critter exists to remove reflection,
   including at mapping time, and may become the default. So the critter divergences (C3) are fixed inside the
   generators. They are guarded by a two-mapper parity test and shared rule code, and the per-document costs (C1)
   are fixed by having generated code build plain models instead of subclasses that override getters.
4. **Benchmark gaps (§7) should close *before* Tier 1, not after.** Tier 1 has to be measured against POLYMORPHIC,
   mixed-entity, missing-`_t` and schema-drift baselines that don't exist yet.
5. **B2 step 2 (single-pass discriminator) is the riskiest hot-path change.** It interacts with B4's creator. Ship it
   on its own, after the plan and the harness.
6. **The prototype is reference material, not a harness.** It sits outside the Maven build and only covers two
   models. Step 0 has to be a real core test.
7. **Some items are user-facing decisions, not mechanical fixes:** the `.*` default (A1), TCCL vs. mapper loader for
   ServiceLoader (D15), anonymous-subclass semantics (D13), and whether to keep `NoArgCreator`'s
   no-`Conversions` behavior. They are called out below.
8. **Missing from the summary but GA-relevant:** C11 (most of `dev.morphia.critter.*` is public without
   `@MorphiaInternal`). That should be fixed before 3.0 GA.

---

## Process

- Each numbered item (0.1, 2.3, 4.7, …) is one PR against `master`, on its own branch in its own worktree.
- After opening a PR: request a Copilot review, then evaluate and answer every comment. Stop after at most 3 cycles,
  or sooner if Copilot has nothing new.
- Significant implementation decisions are listed in the PR description for the maintainer's review.
- The next item starts only after the maintainer approves the current PR.
- Before implementing, re-check each item's finding against current `master`. The review was done at `76697be`;
  #4257 (DecodeSession) landed afterwards.
- Every PR strikes through its own item's heading in this file.

---

## Phase 0: Safety net and baselines

### ~~0.0 Move the review and this plan to `master`~~
- `design/mapping-review.md`, `design/mapping-review/` and this file, with the decisions recorded.

### ~~0.1 Differential codec harness (core test)~~
- **What:** `CodecHarnessTest`, a JUnit test in `core/src/test` that runs offline over a curated set of harness models
  (`dev.morphia.test.mapping.codec.harness.models`) covering the feature matrix. It checks:
  - encodings and decode→encode round trips against golden fixtures generated from `master`, compared as canonical
    extended JSON (field order and BSON types preserved);
  - that reflection and critter produce identical results, and that critter really uses generated models: the only
    model allowed to fall back to reflection was `HarnessArrays`, until 4.8 added array support;
  - "foreign" documents: reordered and unknown fields, numeric widening and narrowing, explicit nulls, missing `_t`,
    `_t` not in first position, string-for-scalar drift, a single value for a list.
- **Why:** every hot-path change in Phase 3 alters protocol details where regressions are silent. The prototype's
  `Verify.java` already shows the check set.
- **Covers:** storeNulls/storeEmpties on and off, `@AlsoLoad`, `@LoadOnly`, constructor-built entities, records,
  lifecycle entities, polymorphism, a generic superclass binding, and references (encode only).
- **Doesn't cover:** decoding references (that fetches from the database), `refresh()`, or Kotlin entities. Items that
  touch those paths add their own tests.
- **Note:** decode→encode is not idempotent under `storeEmpties=false` + `storeNulls=true` (§5). The fixtures pin
  today's behavior; whether it is a bug is a separate decision.
- **Note:** the harness models use the test config's `className` discriminator, because AOT models bake in the
  build-time discriminator (4.12).

### ~~0.2 Close the benchmark gaps (§7)~~
- **What:** add these to `benchmarks/`:
  - ARRAYS (`double[1536]`, `String[]`);
  - concrete collections;
  - LAZY_REFERENCE;
  - a no-op global listener;
  - no `_t`;
  - schema drift;
  - a **mixed-entity** run (megamorphic sites);
  - `new MorphiaDatastore(ds)` / `startSession()`;
  - `PathTarget.translatedPath()` / `MorphiaQuery.toDocument()`;
  - package-scan startup.
- **Why:** these are the baselines Phases 2–4 claim to improve. The mixed-entity run in particular may show a
  different picture from the per-fork runs.
- **Also:** record a baseline run, both mappers, `-prof gc`, as a checked-in results file so later PRs can quote
  deltas.
- **Investigated:** between the review (`76697be`) and `4d48014`, Morphia's decode allocation rose from 2,240 to
  3,168 B/op on SIMPLE and from 6,920 to 7,928 B/op on NESTED. #4257 is the only commit in that range.
  `MorphiaCodec.decode` installs a new `DecodeSession` whenever none is current, so every top-level decode creates
  one, and every entity decode runs `EntityDecoder.peekId`, a second mark-and-scan for `_id`. A probe on SIMPLE
  confirmed the split: per-decode session ~352 B, `peekId` plus registration ~488 B. Queries share one session per
  cursor, but `CodecBenchmark.decodeInCursor` shows that the steady-state cache insertion and eviction across distinct
  ids cost about as much as the per-decode session, so a document in a query pays roughly the same as a standalone
  decode.
- **Done as:** `CodecBenchmark` gained ARRAYS, CONCRETE_COLLECTIONS and LAZY_REFERENCE plus `decodeInCursor`.
  `DocumentShapesBenchmark` covers no `_t` and schema drift, `ListenerBenchmark` a no-op global listener,
  `MixedCodecBenchmark` all models in one JVM, `DatastoreBenchmark` the session copy, `QueryBenchmark` path
  translation and query rendering, and `MappingBenchmark.scan*` package scanning. Results and findings are in
  `benchmarks/results/baseline-bd70cba8d20/`. `MapperVariant` required `ArraysEntity` to be critter's only
  reflection fallback until 4.8 added array support; now none may fall back. `MappingBenchmark` maps the original 11 types: with `ArraysEntity` included, every new
  critter mapper retries the failed generation and logs a stack trace (C6, 4.10).

---

## Phase 1: Decide

Decided 2026-10-12.

| # | Decision | Outcome |
|---|---|---|
| 1.1 | Adopt #16 (critter = accessor strategy on one model)? | **No.** Critter's purpose is to remove reflection, discovery included, and it may become the default mode. Keep generated models; make them plain and keep them matching reflection (4.5–4.13). |
| 1.2 | `morphia.packages` default `.*` | **Keep `.*`, warn once,** and make the scan cheap (3.10). |
| 1.3 | ServiceLoader loader: TCCL or mapper's loader (D15) | The mapper's loader, falling back to TCCL if nothing is found. |
| 1.4 | Anonymous/synthetic subclasses (D13) | Normalize to the nearest annotated superclass, as is done for `MorphiaProxy`. |
| 1.5 | Pursue Tier 3 (descriptors / `EntityModel` retirement) | Defer until after 3.0 GA. Revisit with fresh numbers. |
| 1.6 | 2.9 interim session fix | Skip it; 4.1 replaces it. |

---

## Phase 2: Correctness fixes that stand alone

Each fix needs a failing test first and must pass under `-Dmorphia.mapper=critter` and `reflection`. Fixes that a
structural item covers are moved to that item (see Phases 4 and 5).

### ~~2.1 `@IdField` decode, and the Kotlin delegated property being encoded twice (A4)~~
- Add an internal `EntityModel.renameProperty(p, newName, newMappedName)` that re-keys both maps. Use it from
  `ConfigureProperties` and `KotlinDelegatedPropertiesDiscovery`, and drop the copy-constructor dedupe that hides
  the problem.
- Tests:
  - `@IdField` round trip on a non-clone mapper;
  - a delegated property encoded exactly once.
- Critter side: generate `@IdField` support in this PR too, so the round-trip test passes under both mappers as
  the phase rule requires.

### ~~2.2 `getSuperClass()` returns an interface (A5/D5)~~
- Set `superClass` only when `parent.getType() == type.getSuperclass()`. Make the field private.
- Stop recording pending subtypes for `Object` and unmappable parents.
- Move interface linking out of `EntityModel.MappingUtil.buildHierarchy()` and into the mapper's
  post-validation `linkHierarchy`, keeping the links to inherited interfaces. Today, building a model links it under
  its mapped interfaces before it's validated, so a lock-free `getSubtypes()` reader can briefly see a model that is
  then rejected (deferred from #4390's review).
- Test: `C extends B implements Tagged` → `getSuperClass() == B`, and `IndexHelper` inherits `B`'s `@Indexes`.

### ~~2.3 Reference filter reads the untranslated name (D10)~~
- In `Filter.getValue`, call `PropertyHandler.encode(value)` directly instead of encoding a throwaway `Document` and
  reading it back by the Java name.
- Tests: `@Reference("r")` and snake_case naming.

### ~~2.4 `getClassFromCollection` picks an arbitrary class (D6)~~
- Compute `rootModelByCollection` at registration time. Warn once, at registration, and only when the roots are
  unrelated.
- Test: `find("shapes")` filters on the root's discriminators.

### ~~2.5 `PathTarget` leaves `context` on the last subtype tried (D2)~~ (in 3.8)
- Restore `context` when no subtype matches. Ship it alongside the D2 performance item (3.8).

### ~~2.6 Double `@PrePersist`/`@PostPersist` for polymorphic values (B3)~~
- Write the test first (`List<Shape>`, where `Shape` has `@PrePersist`).
- Fix it by delegating subtypes *before* running callbacks in `LifecycleEncoder`/`LifecycleDecoder`.

### ~~2.7 `PropertyModel.hashCode` includes the lazy codec (D5)~~
- Delete the `equals`/`hashCode` overrides on `EntityModel` and `PropertyModel`, so both compare by identity.
  This also removes the deep `equals` cost on `CopyOnWriteArraySet.add` and in `EntityEncoder`.
- Audit tests for any that rely on structural equality.

### ~~2.8 `MorphiaCodecProvider.codecs` is a plain `HashMap` (D8)~~
- Switch to `ConcurrentHashMap`, using `get` + `putIfAbsent`. Not `computeIfAbsent`, because codec construction
  recurses into other codec lookups.

### ~~2.9 Session copies lose importer providers and custom `Conversions` (D1/C7)~~ (skipped, decision 1.6)
- Interim fix: call `importModels()` in the copy constructor and share `conversions`.
- The real fix is 4.1 (sessions share the mapper). Do this only if 4.1 is more than one release away.

---

## Phase 3: Hot-path protocol (Tier 1, items 1–9)

**Precondition:** the minimal codec plan (3.0) and the harness (0.1). Each item is its own PR, with before/after
JMH numbers on both mappers.

### ~~3.0 Minimal per-`MorphiaCodec` plan~~
- **What:** an immutable object built once per `MorphiaCodec`. Initially it holds `PropertyModel[] properties`,
  `Codec<?>[] codecs` by slot, and `PropertyModel[] encodeOrder`. Later items add `directBsonTypes[]`,
  `boxedTypes[]`, creator positions, lifecycle flags and discriminator/subtype caches.
- **Why:** it gives B1/B4/B8/B11 a per-datastore home. Without it, those items would put codec-derived state on
  the shared `PropertyModel`.
- **Scope:** encoders and decoders read codecs from the plan. `PropertyModel.codec` remains for now (removed in 4.1).

### ~~3.1 Skip the reader mark for codecs that can't fail part-way (#1, B1)~~
- Hoist the NULL check above `getMark()`.
- Add a per-slot `directBsonTypes` bitmask from a static table keyed on the exact codec class (the table is in B1).
  User codecs keep the mark.
- Expected: ~640 B of SIMPLE's 2.3 KB, and about 15–17% of allocation on NESTED/COLLECTIONS/POLYMORPHIC.
- Optional: send a known-rejected type straight to the fallback, avoiding a throw and stack trace for each drifted
  field.

### ~~3.2 No-arg creator fast path (#2, A2/B4)~~
- When the chosen constructor has no parameters, construct the instance first and set values through
  `model.setValue`, which keeps `Conversions`. Allocate pending lists lazily.
- Delete the dead `NoArgCreator` branch and its `LOG.info`. Delete `NoArgCreator` itself, which also skips
  `Conversions`.
- Use a static `Pattern` for `arg[0-9]+`.

### ~~3.3 Discriminator self-match, step 1 (#3, B2)~~
- Add `codecForDiscriminator(d)`, which returns `this` on a self-match and otherwise uses a per-codec CHM.
- Use `skipValue()` instead of `readString()` on `_t`.
- Step 2, a single pass with no rewind, is moved to 3.17.
- The `DecodeSession` `_id` pre-read (`peekId`) is a second scan per root document, run on every decode because
  `MorphiaCodec.decode` always installs a session. This step doesn't remove it.
  At minimum, have it reuse a constant `DecoderContext` and the codec resolved for the id property instead of
  `registry.get(Object.class)`.

### ~~3.4 `getProperty` single lookup (#4, D4/B6)~~
- A one-line fix. Can merge with any other PR.

### ~~3.5 Memoizing `CodecRegistry` wrapper (#5, B5)~~
- Wrap the datastore registry. `get(Class)` checks a CHM first, and the other overloads delegate.
- Cache `ArrayCodec`'s component codec.
- Add a per-codec subtype-codec cache for encode.

### ~~3.6 Precompute serialization flags and the encode order (#6, B11/A7)~~
- Give `MorphiaPropertySerialization` final booleans.
- Put `encodeOrder` in the plan, without the id property and without properties that are always skipped.
- Use `==` instead of `equals(idModel)`. The deep `equals` is already gone after 2.7.

### ~~3.7 Skip `Conversions` when the value already fits (#7, B8)~~
- Store a boxed target type per slot. When `boxedType.isInstance(v)`, set the value directly.
- Use `get` instead of `computeIfAbsent`, and remove the double conversion on the fallback path.

### ~~3.8 `PathTarget` fixes (#8, D2)~~
- Replace the regex with a hand-written check.
- Iterate a cached, flattened subtype list. The hierarchy grows as subtypes are mapped lazily, so
  `EntityModel.addSubtype` must invalidate the cache on the model and on every ancestor (D5). Test: a path that
  only resolves through a subtype mapped after the first lookup.
- ~~Cache `UpdateOperator.field()`.~~ Not done (#4413): a subtype mapped lazily after the first call can change the
  translation, and nothing would invalidate the cache. Resolution is cheap after this item anyway.
- `Projection.knownFields` should return mapped names directly.
- Includes correctness fix 2.5.

### ~~3.9 `ClassValue<Boolean>` for `isMappable` (#9, D3)~~
- Walk interfaces only; `@Entity`/`@ExternalEntity` are `@Inherited`.
- Keep the `MorphiaProxy` rule inside the `ClassValue`.

**Exit criterion for Phase 3:** decode on SIMPLE/NESTED is within ~10% of the prototype's `flat` row (1.7–2.2×), the
harness passes, and both mappers agree.

### ~~3.10 Package scan (#10, A1)~~
- `enableClassInfo().ignoreClassVisibility()` plus `Class.forName(name, false, …)`, then the annotation pre-filter.
- Collect into a `LinkedHashSet` for deterministic order, and merge `map(String)`/`mapPackage`.
- Per decision 1.2, keep the `.*` default and log a one-time warning when it is in effect.

### ~~3.11 ServiceLoader and validation once per mapper (#11, D15/A8)~~
- Load conventions once per mapper with the loader from decision 1.3, and keep one `MappingValidator` per mapper.
- Build the rule list once as a static, and run one pass over the properties.
- Remove the two rules that can never fire (deprecate them, since they are public), and fix the `TreeSet`
  comparator. As done: only `MultipleId` is removed. Since 2.1, `DuplicatedAttributeNames` fires for an
  `@AlsoLoad` that repeats a name on one property, so it still runs.

### ~~3.12 Lifecycle dispatch (#12, D9)~~
- Guard `Sofia` debug formatting with `isDebugEnabled()`, precompute argument kinds, use a lifecycle bitmask, and
  delete `UntypedEntityListenerAdapter`.

### ~~3.13 Collection and map instantiation (#13, B9)~~
- Resolve a `Supplier` once per codec instead of `getDeclaredConstructor().newInstance()` on every decode.

### ~~3.14 Date codecs (#13, B12)~~
- Resolve a fixed `ZoneOffset` once per codec for UTC storage, and keep the current path for `SYSTEM_DEFAULT`.

### ~~3.15 Lazy-reference proxies (#13, B10)~~
- Resolve the proxy class, its constructor handle and its `handler` field handle once per `ReferenceCodec`.
  Compatible with the deferred #4339 decision.

### ~~3.16 Primitive array codecs (#20, B7)~~
- Independent of the rest of the plan, and can land at any time.
- Specialize `TypedArrayCodec` per primitive component type, keeping the boxed-codec fallback for numeric
  conversions.
- Measured gain on embedding vectors: 3× encode, 2.4× decode.

### ~~3.17 Discriminator and `_id` single pass, step 2 (B2)~~
- Read in order, and buffer values that appear before `_t`. Continue on a self-match. When `_t` comes first, hand
  off to the subtype decoder. Decode a document without `_t` in one pass.
- Fold in #4257's `peekId` pre-scan: `_id` is normally the first field, so read it in the same pass and register the
  entity with the `DecodeSession` (`registerInFlight`/`registerConstructing`) **before** any later property, and
  in particular any reference, is decoded. If `_id` appears after a reference property, fall back to today's
  mark-and-scan, so cycle-safe decoding never regresses. Add harness fixtures for reference cycles with `_id` both
  first and late.
- **Risk:** must not run user constructors speculatively, which interacts with 3.2. Needs harness fixtures with
  `_t` in various positions.
- Expected: ~360 B and 250–300 ns per 8-field document.
- Done in `EntityDecoder`:
  - A document laid out as Morphia writes it (`_id`, then `_t`) is read in one pass. An `_id` stored as an ObjectId,
    string, int32 or int64 is read before `_t`, and decoding carries on from `_t` with this decoder or the subtype's,
    without reading anything twice. The `_id` is read once for both the property and the session key whenever the
    property's codec reads that type directly and the property's class is the one references read it as.
  - The entity is registered with the `DecodeSession` as soon as its id is read, before any later property. When a
    property comes before the `_id`, the id is read ahead from there (the old `peekId`), so cycle-safe decoding never
    regresses.
  - **Differs from the plan:** values before `_t` aren't buffered. One mark is taken at the start of the document, and
    any other layout goes back to it and finds the codec first, as before: fields before `_t`, no `_t`, an `_id` of
    another type, or a subtype whose decoder can't pick up part-way (a lifecycle subtype, a non-entity codec).
    Buffering every value read before `_t` would allocate more than reading those fields twice, and Morphia never
    writes those layouts. Each of them still saves the second scan for the `_id`.
  - No user constructor runs speculatively: nothing is created until the codec is known.
  - `CodecHarnessTest` gains a polymorphic root with an ObjectId id, one with a string id, a lifecycle subtype, and 19
    foreign fixtures for where the `_id` and `_t` sit. The goldens were generated with the old decoder.
    `TestDecodeOrder` checks that the entity is in the session before a later property decodes, whichever order the
    `_id`, `_t` and properties come in, that each constructor runs once, and that Morphia's layout takes one mark.
  - Measured (`CodecBenchmark`, `-prof gc`): SIMPLE decodes with ~500 B less (2,112 → ~1,610 B/op) and 24–28% faster;
    POLYMORPHIC with ~2.7 KB less and 20–26% faster. LIFECYCLE is unchanged.
  - Not done: `LifecycleDecoder` still reads the discriminator and the id ahead of decoding (`peekDiscriminator`,
    `peekId`). Doing the same there is a follow-up.
  - The harness shows that the lifecycle codecs read a few foreign documents differently from the plain ones; master
    already did. Through a `Document`, an id stored as another type, and with either listener, a null `_t`. They're
    listed in `LIFECYCLE_DIVERGENCES`.

---

## Phase 4: Structural (Tier 2)

### ~~4.1 Codecs off `PropertyModel`; sessions share the mapper (#14, D1/B13/C7)~~
- Finish moving codecs into the 3.0 plan and remove `PropertyModel.codec`. `SessionDatastore` reuses
  `datastore.mapper`.
- Optionally share the stateless filter, expression, update and aggregation providers between the parent and its
  sessions.
- **Fixes:** the per-session mapper copy (33–80 µs for 11 types, growing with entity count); codecs reused across
  datastores; lost importers and `Conversions` (supersedes 2.9).
- **Tests to change:** `TestDatastore.testDatastoreClones`, `TestCritterMapper.testSessionDatastoreCopyPattern` and
  `TestMapper.testMapperCopying` assert isolation that is no longer needed.
- After this, `refresh()` can reuse the normal codec with an "existing instance" creator.

### ~~4.2 Lifecycle streaming path (#15, B3)~~
- Compute `needsDocumentOnPersist` and `needsDocumentOnLoad` per codec. Stream directly unless a hook or a global
  listener takes a `Document`. Gate on event coverage, not `hasListeners()`.
- Build on 2.6.
- ~~Plan the removal of `addInterceptor`, which has been deprecated for removal since 2.4.0.~~ Removed in 4.2, along
  with `Mapper.getListeners()` and `hasListeners()`. Global listeners are configured with `MorphiaConfig.listeners()`
  (#4431), fixed once the mapper exists, so every lifecycle codec decides its path when it's built.
- Prerequisites: #4432 (`EntityListener` becomes a marker; per-event listener interfaces show whether a listener
  takes a `Document`) and #4431 (global listeners configured with `MorphiaConfig.listeners()`).
- Opt-in `Document` listeners, as the streaming path's escape hatch: `PostLoadDocumentListener<T>`
  (`postLoad(T, Document, Datastore)`) and `PostPersistDocumentListener<T>` (`postPersist(T, Document, Datastore)`).
  - A separate single-method interface per event, not a `DocumentAware` marker plus overloads. A marker restates what
    the override already says, the old `hasAnnotation` duplication: overriding the overload without the marker never
    calls it, silently. It also covers a whole class rather than one event, and it forces a choice of which overload
    is abstract, which breaks lambdas or allows a listener that implements nothing.
  - Dispatch and the per-codec `needsDocument*` flags check for these interfaces per event, like the others. A class
    may implement only one of each pair for an event; the mapper rejects both at registration.
  - `PostLoad`: the only way to read unmapped fields, or compare the entity with the raw document, after decoding.
  - `PostPersist`: `LifecycleEncoder` runs it on the fully encoded `Document` before writing it, so it's the hook for
    adding fields or signing encoded content.
  - None for `PrePersist`, whose `Document` is still empty (encoding hasn't happened), so a `PostPersist` listener
    covers it. `PreLoad` already takes the `Document`. The lifecycle docs' `DigitalSigner` signs at `@PrePersist`, so
    it signs an empty document: fix that example in the listener docs update that follows #4431.

### ~~4.3 Shared registration protocol in `AbstractMapper` (#18, A3/D7)~~
- `synchronized final mapEntity` → `protected abstract buildModel(Class)`.
- ~~Use `putIfAbsent`, and roll back with `remove(name, model)` when validation fails.~~ As implemented (#4390):
  `register()` runs under the mapper's lock and keeps an in-progress model in a lock-guarded `registering` map
  (visible only to the registering thread's recursive lookups). It publishes to `mappedEntities` only after
  validation, discriminator registration and hierarchy linking succeed. On failure it unlinks the model from any
  parents it was linked under while it was built. `DiscriminatorLookup.addModel` no longer overwrites on a duplicate.
- **Fixes the §5 issues:** an invalid model staying registered, and duplicate-discriminator races.
- Ship it early, possibly within Phase 2, because it is a correctness fix and small.

### ~~4.4 One generic-type resolver (#19, A6)~~
- Bind `TypeVariable` to `Type` by identity along the superclass chain. Handle `ParameterizedType`,
  `GenericArrayType` and wildcards, including `? super`. Cache per class with `ClassValue`.
- Use it in the reflective builder and in `TypeData`. Critter works from classfile signatures, not
  `java.lang.reflect`, so `PropertyModelGenerator` needs an equivalent binding by type-variable name over
  signatures. Shared test cases keep both sides in step, and the 4.5 parity test covers it.
- **Fixes:** the `MyMap<V,K>` key/value swap, `? super` being ignored, and the critter/reflection divergence.
- Done as `TypeResolver`. `PropertyModelGenerator` turned out to read the member's generic type through reflection
  already (only a helper the tests call parses signatures), so it calls the same resolver rather than a second one over
  signatures; the generated models still need no reflection at runtime.
- `? super X` was not a bug after all: such a list can hold any supertype's values (a `List<Object>` with strings in
  it), so its elements still decode as the upper bound, `Object`. Using `X` threw `ClassCastException` on a mixed list.

### ~~4.5 Critter/reflection parity test (C3)~~
- Map every test entity with both mappers and compare the resulting models:
  - names, mapped names and load names;
  - types and `TypeData`;
  - annotations;
  - id, version and discriminator settings;
  - accessor behavior.
- Known divergences are listed as expected failures, and each one is removed by the item that fixes it (4.6 or 4.8).
- **Why first:** it is the guard that lets critter keep its own discovery without drifting again.
- Done as `TestMapperParity`, under both discovery modes. The known divergences are rules (`TestMapperParity.Known`),
  not a per-entity list, since method discovery alone yields hundreds of entities that differ for one reason. Each rule
  names the item that closes it:
  - 4.6: critter switches to method discovery under field discovery when a getter is annotated; under method
    discovery it maps fields when there are no getter/setter pairs, skips private inherited getters, and keeps an
    overriding getter's annotations.
  - 4.8: arrays (critter falls back to reflection), shadowed fields, `@Property(concreteClass)`, fluent setters.
  - 4.8 also takes two reflection bugs it found: method accessors aren't made accessible, so they throw on
    package-private entities, and a method named just `get`, `set` or `is` breaks method discovery.

### ~~4.6 Shared discovery rules (C10)~~
- Pull the pure rules into one place that both `FieldDiscovery`/`MethodDiscovery` and
  `PropertyFinder`/`PropertyConvention` call: mapped-name derivation, transient rules, naming strategy,
  `titleCase`/`methodCase`.
- The rules take plain inputs (names, modifiers, annotation values), so critter keeps working from classfiles
  with no reflection.
- Removes the 4.5 parity test's discovery rules (`ANNOTATED_GETTERS`, `FIELDS_WITHOUT_ACCESSORS`,
  `PRIVATE_INHERITED_GETTERS`, `OVERRIDING_GETTER_ANNOTATIONS`).
- Done as `DiscoveryRules`: mapped names, which fields are ignored, accessor names and getter/setter pairing, and
  `titleCase`/`methodCase`. `PropertyConvention`, `ExtensionFunctions`, `CritterParser` and critter's own copies
  are gone.
- Critter now discovers what reflection does: fields only under field discovery, and under method discovery the
  pairs each class declares, with no fallback to fields. It also rejects an `@IdField` that names no property.
- The shared accessor-name rule ignores a method named just `get`, `set` or `is`, which fixes that reflection bug
  from 4.8.
- Critter's accessors still can't call a fluent setter, a private getter or setter, or a superclass's that isn't
  visible from the entity's package, so it now maps such an entity reflectively instead of mapping its fields. The
  parity test's `FLUENT_SETTERS` and `UNREACHABLE_ACCESSORS` rules hold those for 4.8.

### ~~4.7 Generated code builds plain models (C1)~~
- Generated factories construct ordinary `EntityModel`/`PropertyModel` instances from constants, instead of
  subclasses that override about 17 getters each.
  - `Class`/`TypeData` are resolved once, through the entity's own loader, not the TCCL.
  - Base getters become `final`, and the hot-path call sites become monomorphic.
  - No per-call `TypeData`/`List` allocation and no `Class.forName`.
- Since models are no longer immutable subclasses, ServiceLoader `MorphiaConvention`s (Kotlin) can run on critter
  models the way they do for reflection. **Flag for review:** whether they run at generation time (AOT) or at
  load time.
- Old AOT output fails to load against `final` getters and falls back to runtime generation. 4.12 makes that clean.
- Done. The generated entity model still extends `CritterEntityModel`, so the mapper loads it the same way, but it
  overrides nothing: its constructor sets the collection and discriminator settings and builds each property as a
  plain `PropertyModel`, through one private method per property so a wide entity stays under the JVM's method-size
  limit. `CritterPropertyModel` and the per-property `XxxModel` classes are gone. Annotations other
  than Morphia's, which have no builders, ride on a synthetic field of the generated model.
- ServiceLoader conventions run when the model loads, at the same point as for reflection (after
  `ConfigureProperties`, before listeners): the Kotlin convention needs runtime `KProperty` objects, and conventions
  come from the runtime classpath.
- A non-public class is loaded through the generated model's own class loader, once.

### ~~4.8 Close the remaining divergences (C3)~~
- `@Property(concreteClass)`, ~~interface-level annotations~~ (closed by A11),
  shadowed fields (use `FieldInfo.declaringClass`), fluent setters, private or otherwise unreachable getters and
  setters, and array properties.
- Reflection bugs the parity test found: `MethodAccessor` doesn't make its methods accessible (throws on a
  package-private entity), ~~and method discovery fails on a method named just `get`, `set` or `is`~~ (closed by
  4.6).
- One PR per divergence if any of them turns out to be large. Each one removes an expected failure from 4.5.
- Done in one PR, since none was large. The parity test no longer lists known divergences: every fact must match,
  and critter falling back to reflection for a test entity is itself a difference.
  - `@Property(concreteClass)`: critter applies it when it builds the property's `TypeData`, at generation time.
  - Shadowed fields: under field discovery critter rejects them with a `MappingException`, as reflection does,
    instead of mapping the subclass's field.
  - Fluent setters and private or unreachable accessors: `MethodInfo` carries the pair's setter, so the accessors
    call it by its real descriptor and drop a fluent setter's result. Runtime accessors are nestmates of the class
    declaring the pair, so they can call its private methods; build-time accessors, woven into the entity, can call
    the entity's own private methods and inherited protected ones, and skip AOT otherwise.
  - Arrays: reflection's `ArrayFieldAccessor` became `ArrayAccessor`, which wraps any accessor to convert decoded
    values to the array type. Reflection uses it for fields and, newly, for getter/setter pairs; critter's generated
    models wrap their accessors with it. Critter also keeps a generic array's (`T[]`) array flag.
  - `MethodAccessor` makes its methods accessible.

### ~~4.9 Runtime generation efficiency (C5/A9)~~
- Cache `ClassModel` per class for the duration of a generation pass, so shared superclasses are parsed once.
- Use `declaringClass` instead of exception-driven `getDeclaredField` walks.
- Check eligibility before emitting classes.
- Done:
  - A generation pass is one entity. `PropertyFinder` reads each class file in the hierarchy once and passes it on
    to the build-time weavers. A pass used to read the parent of a two-class hierarchy three times, and the entity twice.
    Entities sharing a superclass still each read it once, so mappers don't hold parsed class files.
  - A superclass whose class file can't be found now fails generation, so the entity falls back to reflection, or
    is skipped at build time. Before, the walk stopped there and the superclass's properties were silently left out.
  - Nestmate and model generators look fields up through `FieldInfo.declaringClass`.
  - Generation checks everything, including full privilege access to each nestmate host, and builds the entity
    model generator before it defines or registers any accessor. An entity that fails a check no longer leaves accessors in
    `NestmateAccessorRegistry`, or woven classes in the build output.

### ~~4.10 Tier decision caching (C6)~~
- Cache configuration-independent facts globally in a `ClassValue`: whether an AOT model exists and its
  constructor.
- Cache runtime-generation success or failure keyed by the existing `CritterMapper.generationKey`. Generated
  output depends on property discovery, naming and discriminator strategies, and annotation providers, so one
  configuration's failure must not be applied to another. Configurations that can't be keyed (custom strategies)
  stay unshared, as `RuntimeModels` does today. This stops mappers with the same configuration from retrying and
  re-logging.
- critter-maven reports the entities it skips.
- Fall back per property, not per entity.
- Done:
  - A `ClassValue` holds each entity's pre-generated model constructor, or its absence, so mappers no longer look
    for a missing model by catching a `ClassNotFoundException` each time.
  - `RuntimeModels`, which is already shared by `generationKey`, remembers the entities whose generation failed.
    Mappers with the same configuration don't retry them, and each failure is logged once. A configuration with a
    custom strategy still gets its own `RuntimeModels`, and so its own failures.
  - critter-maven ends with a list of the entities it skipped and why.
  - A property whose field the generated accessors can't reach, or whose getter and setter they can't call, gets a
    reflective `FieldAccessor` or `MethodAccessor` in the generated model, built by `CritterEntityModel`. Discovery
    stays bytecode-based. Cases: an inherited field that is private or hidden from the entity's package, in a
    superclass that can't be rewritten (build time); a pair critter's accessors can't call, such as a setter taking a
    supertype of the getter's type, or a superclass's private methods (build time); and a stand-in whose target's
    setter is private. Each of these used to send the whole entity to reflection, or skip it at build time.

### ~~4.11 Generated instantiators (C8)~~
- Emit an instantiator: a woven `__new()` for AOT, a nestmate at runtime, and a constructor call with `@Name`
  arguments. This removes `Constructor.newInstance` from every decode. Coordinate with 3.2's creator.
- Prior art: the unmerged local branch `worktree-critter-direct-construction` (worktree
  `.claude/worktrees/critter-direct-construction`, on the since-removed Gizmo backend) generated a
  `MorphiaInstanceCreator` per entity. Reuse its shape, not its code:
  - **Two strategies, chosen at generation time.** For a no-arg constructor, create the instance when the creator is
    constructed and set each value as it arrives. For an all-args constructor, keep one field per constructor
    parameter, filled by name in `set()`, and call the constructor in `getInstance()`.
  - **Zero defaults for primitive parameters** that were never set, so the call doesn't fail when a property is missing
    from the document.
  - **Fall back to `ConstructorCreator`** when the class or the constructor isn't accessible from the generated code.
    The branch required both to be public; a nestmate at runtime and a woven `__new()` for AOT should lift most of that
    restriction.
  - **Keep `Conversions`.** The branch's no-arg `set()` wrote straight through the property accessor, and its all-args
    path cast raw values. Both skip the conversions that 3.2 preserved by going through `model.setValue`. The
    generated creator must convert values before setting or passing them.
  - **Don't create the instance early for constructor-mapped entities.** The lifecycle decoder's early `getInstance()`
    breaks constructors that validate their arguments (see #4431); a generated all-args creator has the same
    constraint.
- Once 4.11 is done, remove that worktree and delete the branch (it was never pushed).
- Done:
  - Critter generates an `Instantiator` for each constructor decoding may choose: the no-arg constructor and those whose
    parameters all name properties. At runtime it's a hidden nestmate of the entity, so it can call a private
    constructor. At build time it calls a synthetic static `__newN(Object[])` woven into the entity, which keeps the
    constructor and its parameter types out of the generated class. Every woven class gets a `__newN` for each of its
    constructors, so a superclass woven again for a subclass entity keeps the ones its own model calls. The generated
    model registers the instantiators by constructor descriptor, and a copied model keeps them.
  - `ConstructorCreator` still chooses the constructor and keeps its two strategies, `Conversions`, pending values and
    eager-instance rule. It calls the generated instantiator for the chosen constructor, or the constructor reflectively
    when there's none: for an abstract type, a build-time entity that isn't public, or one Morphia can't define nestmates
    in.
  - A primitive parameter's argument is widened to its exact wrapper type, as `Constructor.newInstance` would widen it,
    and a missing one defaults to its own zero. Before, every numeric zero was an `Integer`, which reflection can't pass
    to a `char`, `byte` or `short` parameter.
  - A constructor's exception is the `MappingException`'s cause on both paths, rather than an
    `InvocationTargetException` on the reflective one.
  - This removes reflection from decoding, but it isn't a speedup. In a crude loop on JDK 17, a generated no-arg
    instantiator took about 12 ns and `Constructor.newInstance` about 13 ns, and `CodecBenchmark` decode didn't move.
    It costs about 2 KB per new mapper and 1 MB of allocation on a runtime cold start. The benchmarks have no
    constructor-mapped entity, so that path isn't measured.

### ~~4.12 AOT validity guard (#17, C4)~~
- Emit a format version, generation key and weave hash. Check them at load time, and fall back with one log line
  when they don't match. Must land before GA.
- Done. A pre-generated model records what it depends on in public synthetic constants. `CritterMapper` checks them
  in plain Java, once per entity class, when it first loads the model:
  - **Morphia version.** `__morphiaVersion` must equal the running core's version, which comes from a resource that
    Maven filters (`dev/morphia/morphia-version.properties`). Upgrading Morphia means regenerating, which happens
    anyway because critter-maven and core are released together. Nothing has to be bumped by hand. Snapshot builds
    of one version can't be told apart; the other checks, and the `LinkageError` fallback, cover them.
  - **Build ID.** Each critter-maven run (one `CritterGenerator`) picks a random ID. Every class it weaves gets a
    private synthetic static field named `__morphiaBuild$<id>`. The model records the ID in `__buildId`, and the
    woven classes it uses in `__wovenClasses`: the entity and the superclasses whose fields are woven into them.
    Each of those classes must carry the stamp, which is looked up with `getDeclaredField` without being read or
    initializing the class.
    - A class compiled again without critter-maven has no stamp.
    - A class woven again by another run has a different stamp.
    - Either way the model isn't used, so the failure no longer surfaces as a `NoSuchMethodError: __readXxx` at
      the first encode.
    - Encoding the ID in the field's name means the check needs no access to a private field. Because the field is
      private and static, it doesn't change a `Serializable` entity's default `serialVersionUID`.
    - Weaving only rewrites superclasses from the entity's own output directory or jar, so separate main and test
      runs don't restamp each other's classes.
  - **Generation key.** `CritterEntityModel.generationKey(config)` describes the configuration that generation bakes
    in:
    - collection and property naming
    - discriminator function and key
    - property discovery
    - the annotation providers
    - the default locale's case rules: the built-ins convert case with it, but only Turkish, Azerbaijani and
      Lithuanian convert differently, so other locales all count as `root`

    The model records it in `__generationKey`, and each mapper compares it with its own. Strategies are identified
    by class, which only tells configurations apart for Morphia's stateless built-ins. A mapper using any other
    strategy (e.g. a naming strategy with a configurable prefix) therefore uses no pre-generated models; it
    generates them at runtime. Runtime model sharing (4.10) uses the same rule and key.
  - **Fallback.** A model that fails a check is ignored, and its entity's model is generated at runtime:
    - Version and build failures are logged once per entity.
    - A configuration mismatch is logged once per pair of keys.
    - Output from before this check, which has no constants, is reported as generated by an earlier critter-maven.

### ~~4.13 Critter API surface (C11), before GA~~
- ~~Mark `dev.morphia.critter.*` `@MorphiaInternal` and add a `package-info`.~~
- ~~Make the `Critter` lists immutable.~~
- ~~Fix the `CritterEntityModel` javadoc and `design/GeneratedCode.adoc` item 2, which recommends the constant-getter
  approach that C1 shows the JIT can't optimize.~~
- Done:
  - Each of the three `dev.morphia.critter` packages has a `package-info` marking it `@MorphiaInternal`.
  - Every public type in them carries the annotation and the `@hidden`/`@morphia.internal` tags, since revapi and the
    docs read them from the type. `CritterEntityModel`, which generated models extend, is marked the same way.
  - `Critter` no longer has lists. Its unused constructor and fields are removed, and it is now a `final` holder of
    static helpers. `critterPackage` lowercases with `Locale.ROOT`, so a model generated under one default locale is
    found under another (a Turkish locale lowercased `I` to a dotless `ı`).
  - The `CritterEntityModel` javadoc was already fixed by 4.7. `GeneratedCode.adoc` item 2 now notes that the
    constant-getter approach was tried and replaced by plain models.
  - Not done: C11 also calls `CritterClassLoader.shouldRegister`'s child-first `dev.morphia.critter.*` rule test
    plumbing. It is how the test fixtures in `dev.morphia.critter.sources` get woven, so moving it behind a test-only
    hook is left to Phase 5.

### ~~4.14 Annotated lifecycle methods become entity-only (follow-up to #4432)~~
Decided after 4.2: removed outright in 3.0 rather than deprecated. With 4.2's `PostPersistDocumentListener` and
`PostLoadDocumentListener`, every annotated method has an interface to move to, so the cost below no longer applies, and the
new `AnnotatedListenerMethodMigration` recipe moves them: the class implements each event's interface, whose method calls the
annotated methods. Mapping rejects an `@EntityListeners` class that declares annotated methods or implements no interface.
`EntityListenerAdapter` now handles only the entity's own methods; `@EntityListeners` instances are created when the model is
set up and called through `TypedListeners`.

The original notes:
- `@EntityListeners` classes would implement the per-event interfaces only. Their annotated methods are deprecated
  first, not removed. `EntityModel.callLifecycleMethods` then has three paths: the entity's own annotated methods
  (reflective, `OnEntityListenerAdapter`), and `@EntityListeners` instances and global listeners (both
  `TypedListeners`, with no reflection). Critter could later generate direct calls for the entity's own methods.
- Cost: an `@EntityListeners` method that writes the `Document` before a save, or reads it after a save or a load,
  has nowhere to go and must move to the entity's own annotated method. That's a documented pattern (the
  `DigitalSigner` example in the lifecycle docs). The `EntityListenerMigration` recipe can convert the rest and flag
  those.
- `@EntityListeners` instances would be created when the model is set up rather than on first use.

---

## Phase 5: Clean-up (any time, low risk)

Bundle these into one or two PRs. Most of it is `@MorphiaInternal` dead code.
- ~~**A10/B14/D16:**~~
  - ~~`DiscriminatorLookup.packages` and `searchPackages`;~~
  - ~~`PropertyModel.isTransient()`, `getDocumentValue` and `loadFromDocument`;~~
  - ~~`TypeData.nestedTypeParameters`;~~
  - ~~unreachable collection codec providers;~~
  - ~~`IndexHelper.collectIndexes(parentModels)`;~~
  - ~~commented-out blocks;~~
  - ~~`Mapper.findIdProperty`, `MorphiaDatastore.hasLifecycle(EntityModel, Class)` and the static
    `ReferenceCodec.encodeId`;~~
  - ~~`ArrayCodec.getEncoderClass()` returning null.~~
  - Done. The two unreachable collection providers became one `MorphiaCollectionPropertyCodecProvider` (the driver-style
    `CollectionPropertyCodecProvider` is deleted), which takes
    every collection, so a list without one still gets Morphia's handling rather than the driver's.
    `ArrayCodec.getEncoderClass()` returns the array class. `TestGlobalListeners` dropped an assertion on
    `hasLifecycle`; the callbacks it records already show the listener ran.
- **D11:**
  - resolve the model once per save/delete;
  - cache `MongoCollection` per class per datastore;
  - precompute the write concern;
  - drop `Mapper.getWriteConcern`.
- **D12:** a single `get` in `DiscriminatorLookup`, a negative cache for `Class.forName`, and the model's
  discriminator key everywhere (`getClass(Document)`, `Projection.project`).
- **D13** (decision 1.4): key the registry by `Class<?>`, register `@ExternalEntity` under both the stand-in and the
  target, and normalize anonymous subclasses.
- **D14:** move `updateQueryWithDiscriminators` into `MorphiaQuery`, using the cached subtype list.
- **D16:** collapse the five "give me a model" entry points into one nullable lookup plus one throwing wrapper.
- ~~**A11:** extract `ReflectiveEntityModelBuilder` so the `EntityModel` constructor only assigns fields. Use one
  annotation-collection utility with documented precedence, whose rules `EntityModelGenerator` also follows (feeds
  4.8's interface-annotation fix). This is groundwork for Tier 3.~~
  - Done as `ReflectiveEntityModelBuilder` and `ClassHierarchy`, whose per-class caches hold the superclasses and the
    annotations; the entity's own lifecycle methods are cached per class too. Critter now takes its annotations from
    `ClassHierarchy`, so 4.8's interface-annotation divergence is closed here. Critter still keeps only Morphia
    annotations, since only they have builders to emit.

---

## Phase 6: Architecture track, 3.x (Tier 3, #21–#25), deferred per decision 1.5

Only start after Phases 3 and 4 have shipped and been re-measured.
- **#21:** an immutable `EntityDescriptor` plus a mapper `TypeRegistry`, compiled after validation. Migrate in the
  order aggregation → metadata → paths → references → codecs.
- **#22:** builder extraction. Already done if A11 lands in Phase 5.
- **#23:** a critter descriptor factory per type. Relevant because critter may become the default; 4.7 is the
  first step toward it.
- **#24:** a generated-codec backend. Only consider it if mixed-entity, POLYMORPHIC and LIFECYCLE numbers after
  Phase 3 show the remaining ~65–80 ns/doc matters. The cost is a 2–4k LOC generator kept in lockstep with the
  interpreter.
- **#25:** a v2 `MorphiaConvention` on the builder, a Kotlin bridge, and a deprecated `getEntityModel` adapter.

---

## Suggested PR order (summary)

1. 0.0 design docs to `master`
2. 0.1 harness, then 0.2 benchmarks + baseline results
3. 4.3 registration protocol (correctness, small)
4. 2.1–2.8 correctness fixes
5. 3.0 codec plan skeleton
6. 3.1 → 3.9, with JMH deltas
7. 3.10–3.15 startup, lifecycle and codec quick wins; 3.16 primitive arrays (independent)
8. 4.1 sessions share the mapper → #4432 listener interfaces → #4431 configured listeners → 4.2 lifecycle
   streaming; ~~4.14 entity-only annotated methods~~
9. 4.4 generic resolver
10. ~~Phase 5 A11 builder/annotation utility~~ → ~~4.5 parity test~~ → ~~4.6 shared rules~~ → ~~4.7 plain models~~ → ~~4.8 divergences~~ →
    ~~4.9~~ → ~~4.10~~ → ~~4.11~~ → ~~4.12~~ AOT guard + ~~4.13~~ API surface (**before 3.0 GA**)
11. ~~3.17 single-pass discriminator and `_id`~~
12. Phase 5 clean-ups throughout; Phase 6 re-evaluated after 3.0 GA
