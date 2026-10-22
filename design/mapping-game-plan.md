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
    model allowed to fall back to reflection is `HarnessArrays`, until 4.8 adds array support;
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
  `benchmarks/results/baseline-bd70cba8d20/`. `MapperVariant` now requires `ArraysEntity` to be critter's only
  reflection fallback (4.8). `MappingBenchmark` maps the original 11 types: with `ArraysEntity` included, every new
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

### 2.5 `PathTarget` leaves `context` on the last subtype tried (D2)
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

### 3.6 Precompute serialization flags and the encode order (#6, B11/A7)
- Give `MorphiaPropertySerialization` final booleans.
- Put `encodeOrder` in the plan, without the id property and without properties that are always skipped.
- Use `==` instead of `equals(idModel)`. The deep `equals` is already gone after 2.7.

### 3.7 Skip `Conversions` when the value already fits (#7, B8)
- Store a boxed target type per slot. When `boxedType.isInstance(v)`, set the value directly.
- Use `get` instead of `computeIfAbsent`, and remove the double conversion on the fallback path.

### 3.8 `PathTarget` fixes (#8, D2)
- Replace the regex with a hand-written check.
- Iterate a cached, flattened subtype list. The hierarchy grows as subtypes are mapped lazily, so
  `EntityModel.addSubtype` must invalidate the cache on the model and on every ancestor (D5). Test: a path that
  only resolves through a subtype mapped after the first lookup.
- Cache `UpdateOperator.field()`.
- `Projection.knownFields` should return mapped names directly.
- Includes correctness fix 2.5.

### 3.9 `ClassValue<Boolean>` for `isMappable` (#9, D3)
- Walk interfaces only; `@Entity`/`@ExternalEntity` are `@Inherited`.
- Keep the `MorphiaProxy` rule inside the `ClassValue`.

**Exit criterion for Phase 3:** decode on SIMPLE/NESTED is within ~10% of the prototype's `flat` row (1.7–2.2×), the
harness passes, and both mappers agree.

### 3.10 Package scan (#10, A1)
- `enableClassInfo().ignoreClassVisibility()` plus `Class.forName(name, false, …)`, then the annotation pre-filter.
- Collect into a `LinkedHashSet` for deterministic order, and merge `map(String)`/`mapPackage`.
- Per decision 1.2, keep the `.*` default and log a one-time warning when it is in effect.

### 3.11 ServiceLoader and validation once per mapper (#11, D15/A8)
- Load conventions once per mapper with the loader from decision 1.3, and keep one `MappingValidator` per mapper.
- Build the rule list once as a static, and run one pass over the properties.
- Remove the two rules that can never fire (deprecate them, since they are public), and fix the `TreeSet`
  comparator.

### 3.12 Lifecycle dispatch (#12, D9)
- Guard `Sofia` debug formatting with `isDebugEnabled()`, precompute argument kinds, use a lifecycle bitmask, and
  delete `UntypedEntityListenerAdapter`.

### 3.13 Collection and map instantiation (#13, B9)
- Resolve a `Supplier` once per codec instead of `getDeclaredConstructor().newInstance()` on every decode.

### 3.14 Date codecs (#13, B12)
- Resolve a fixed `ZoneOffset` once per codec for UTC storage, and keep the current path for `SYSTEM_DEFAULT`.

### 3.15 Lazy-reference proxies (#13, B10)
- Resolve the proxy class, its constructor handle and its `handler` field handle once per `ReferenceCodec`.
  Compatible with the deferred #4339 decision.

### 3.16 Primitive array codecs (#20, B7)
- Independent of the rest of the plan, and can land at any time.
- Specialize `TypedArrayCodec` per primitive component type, keeping the boxed-codec fallback for numeric
  conversions.
- Measured gain on embedding vectors: 3× encode, 2.4× decode.

### 3.17 Discriminator and `_id` single pass, step 2 (B2)
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

---

## Phase 4: Structural (Tier 2)

### 4.1 Codecs off `PropertyModel`; sessions share the mapper (#14, D1/B13/C7)
- Finish moving codecs into the 3.0 plan and remove `PropertyModel.codec`. `SessionDatastore` reuses
  `datastore.mapper`.
- Optionally share the stateless filter, expression, update and aggregation providers between the parent and its
  sessions.
- **Fixes:** the per-session mapper copy (33–80 µs for 11 types, growing with entity count); codecs reused across
  datastores; lost importers and `Conversions` (supersedes 2.9).
- **Tests to change:** `TestDatastore.testDatastoreClones`, `TestCritterMapper.testSessionDatastoreCopyPattern` and
  `TestMapper.testMapperCopying` assert isolation that is no longer needed.
- After this, `refresh()` can reuse the normal codec with an "existing instance" creator.

### 4.2 Lifecycle streaming path (#15, B3)
- Compute `needsDocumentOnPersist` and `needsDocumentOnLoad` per codec. Stream directly unless a hook or a global
  listener takes a `Document`. Gate on event coverage, not `hasListeners()`.
- Build on 2.6.
- Plan the removal of `addInterceptor`, which has been deprecated for removal since 2.4.0.

### ~~4.3 Shared registration protocol in `AbstractMapper` (#18, A3/D7)~~
- `synchronized final mapEntity` → `protected abstract buildModel(Class)`.
- ~~Use `putIfAbsent`, and roll back with `remove(name, model)` when validation fails.~~ As implemented (#4390):
  `register()` runs under the mapper's lock and keeps an in-progress model in a lock-guarded `registering` map
  (visible only to the registering thread's recursive lookups). It publishes to `mappedEntities` only after
  validation, discriminator registration and hierarchy linking succeed. On failure it unlinks the model from any
  parents it was linked under while it was built. `DiscriminatorLookup.addModel` no longer overwrites on a duplicate.
- **Fixes the §5 issues:** an invalid model staying registered, and duplicate-discriminator races.
- Ship it early, possibly within Phase 2, because it is a correctness fix and small.

### 4.4 One generic-type resolver (#19, A6)
- Bind `TypeVariable` to `Type` by identity along the superclass chain. Handle `ParameterizedType`,
  `GenericArrayType` and wildcards, including `? super`. Cache per class with `ClassValue`.
- Use it in the reflective builder and in `TypeData`. Critter works from classfile signatures, not
  `java.lang.reflect`, so `PropertyModelGenerator` needs an equivalent binding by type-variable name over
  signatures. Shared test cases keep both sides in step, and the 4.5 parity test covers it.
- **Fixes:** the `MyMap<V,K>` key/value swap, `? super` being ignored, and the critter/reflection divergence.

### 4.5 Critter/reflection parity test (C3)
- Map every test entity with both mappers and compare the resulting models:
  - names, mapped names and load names;
  - types and `TypeData`;
  - annotations;
  - id, version and discriminator settings;
  - accessor behavior.
- Known divergences are listed as expected failures, and each one is removed in 4.8.
- **Why first:** it is the guard that lets critter keep its own discovery without drifting again.

### 4.6 Shared discovery rules (C10)
- Pull the pure rules into one place that both `FieldDiscovery`/`MethodDiscovery` and
  `PropertyFinder`/`PropertyConvention` call: mapped-name derivation, transient rules, naming strategy,
  `titleCase`/`methodCase`.
- The rules take plain inputs (names, modifiers, annotation values), so critter keeps working from classfiles
  with no reflection.

### 4.7 Generated code builds plain models (C1)
- Generated factories construct ordinary `EntityModel`/`PropertyModel` instances from constants, instead of
  subclasses that override about 17 getters each.
  - `Class`/`TypeData` are resolved once, through the entity's own loader, not the TCCL.
  - Base getters become `final`, and the hot-path call sites become monomorphic.
  - No per-call `TypeData`/`List` allocation and no `Class.forName`.
- Since models are no longer immutable subclasses, ServiceLoader `MorphiaConvention`s (Kotlin) can run on critter
  models the way they do for reflection. **Flag for review:** whether they run at generation time (AOT) or at
  load time.
- Old AOT output fails to load against `final` getters and falls back to runtime generation. 4.12 makes that clean.

### 4.8 Close the remaining divergences (C3)
- `@Property(concreteClass)`, interface-level annotations (shared with A11's annotation utility),
  shadowed fields (use `FieldInfo.declaringClass`), fluent setters in AOT weaving, and array properties.
- One PR per divergence if any of them turns out to be large. Each one removes an expected failure from 4.5.

### 4.9 Runtime generation efficiency (C5/A9)
- Cache `ClassModel` per class for the duration of a generation pass, so shared superclasses are parsed once.
- Use `declaringClass` instead of exception-driven `getDeclaredField` walks.
- Check eligibility before emitting classes.

### 4.10 Tier decision caching (C6)
- Cache configuration-independent facts globally in a `ClassValue`: whether an AOT model exists and its
  constructor.
- Cache runtime-generation success or failure keyed by the existing `CritterMapper.generationKey`. Generated
  output depends on property discovery, naming and discriminator strategies, and annotation providers, so one
  configuration's failure must not be applied to another. Configurations that can't be keyed (custom strategies)
  stay unshared, as `RuntimeModels` does today. This stops mappers with the same configuration from retrying and
  re-logging.
- critter-maven reports the entities it skips.
- Fall back per property, not per entity.

### 4.11 Generated instantiators (C8)
- Emit an instantiator: a woven `__new()` for AOT, a nestmate at runtime, and a constructor call with `@Name`
  arguments. This removes `Constructor.newInstance` from every decode. Coordinate with 3.2's creator.

### 4.12 AOT validity guard (#17, C4)
- Emit a format version, generation key and weave hash. Check them at load time, and fall back with one log line
  when they don't match. Must land before GA.

### 4.13 Critter API surface (C11), before GA
- Mark `dev.morphia.critter.*` `@MorphiaInternal` and add a `package-info`.
- Make the `Critter` lists immutable.
- Fix the `CritterEntityModel` javadoc and `design/GeneratedCode.adoc` item 2, which recommends the constant-getter
  approach that C1 shows the JIT can't optimize.

---

## Phase 5: Clean-up (any time, low risk)

Bundle these into one or two PRs. Most of it is `@MorphiaInternal` dead code.
- **A10/B14/D16:**
  - `DiscriminatorLookup.packages` and `searchPackages`;
  - `PropertyModel.isTransient()`, `getDocumentValue` and `loadFromDocument`;
  - `TypeData.nestedTypeParameters`;
  - unreachable collection codec providers;
  - `IndexHelper.collectIndexes(parentModels)`;
  - commented-out blocks;
  - `Mapper.findIdProperty`, `MorphiaDatastore.hasLifecycle(EntityModel, Class)` and the static
    `ReferenceCodec.encodeId`;
  - `ArrayCodec.getEncoderClass()` returning null.
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
- **A11:** extract `ReflectiveEntityModelBuilder` so the `EntityModel` constructor only assigns fields. Use one
  annotation-collection utility with documented precedence, whose rules `EntityModelGenerator` also follows (feeds
  4.8's interface-annotation fix). This is groundwork for Tier 3.

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
8. 4.1 sessions share the mapper → 4.2 lifecycle streaming
9. 4.4 generic resolver
10. Phase 5 A11 builder/annotation utility → 4.5 parity test → 4.6 shared rules → 4.7 plain models → 4.8 divergences →
    4.9–4.11 → 4.12 AOT guard + 4.13 API surface (**before 3.0 GA**)
11. 3.17 single-pass discriminator and `_id`
12. Phase 5 clean-ups throughout; Phase 6 re-evaluated after 3.0 GA
