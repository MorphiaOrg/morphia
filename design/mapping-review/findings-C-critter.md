# Findings C — Critter mapper

(Agent C report, saved by the coordinator because the agent's own file write was blocked.)
HEAD 76697be. Read-only: code + git history; nothing built, edited or run.

## Top findings (summary)
1. **C1 – Generated getter overrides slow the hot path.** Each property is its own generated class overriding ~17 getters (`getAccessor`, `getMappedName`, `getName`, `getType`, `isFinal`, …) → shared call sites in `EntityEncoder`, `EntityDecoder`, `PropertyModel`, `NoArgCreator` make 3–5 non-inlinable virtual calls per property per document (inlined field loads in the reflection mapper). Also `getTypeData()` builds a new `TypeData` tree per call; `getLoadNames()` allocates a list per call; for non-public types `getType()`/`getNormalizedType()`/`getTypeData()` do `Class.forName(name, false, TCCL)` per call (hit by `PropertyModel.setValue`) — also a correctness bug when the TCCL can't see the entity. Explains why #4338 measured critter at parity with reflection.
2. **C2 – Unify the model.** One `EntityModel`/`PropertyModel`; critter becomes an accessor strategy. Runtime tier = reflective discovery + nestmate accessors cached per `Class` in a `ClassValue`. Accessors don't depend on config, so `RuntimeModels`, `generationKey`, `NestmateAccessorRegistry`, runtime use of `CritterClassLoader`, `EntityModelGenerator`, `PropertyModelGenerator` and the critter model classes go (~1.5–2k lines). Existing AOT `XAccessor` classes loadable by name → today's AOT jars keep working.
3. **C4 – AOT models have no validity guard.** They bake collection/discriminator/property names from build-time config, never checked at runtime (only the runtime tier keys on config). Stale weaving (`NoSuchMethodError: __readXxx`, cf. #4370) not detected at load; no format version. Core is `3.0.0-SNAPSHOT` → format can still change; add the version before GA.
4. **C3 – Duplicated discovery has drifted from reflection.** Critter ignores `@Property(concreteClass)`, `@IdField`, ServiceLoader conventions (Kotlin delegated properties map as `x$delegate`), interface-level annotations; name lookup picks the wrong field when a subclass shadows it; AOT method weaving assumes `void` setters; one array field sends the whole entity to reflection.
5. **C6/C7 – Tier and copy costs.** AOT misses and runtime failures aren't cached globally → each new mapper retries and re-logs. Every session copy rebuilds every model by reflection and creates a new `Conversions`, losing custom converters.

## Published numbers used (from commit messages, not re-measured)
- Per-document: parity with reflection after #4338 (decode SIMPLE 2.4 vs 2.6 µs, NESTED 12.0 vs 12.1 µs).
- Mapping: AOT warmMapping 38 µs (#4343) vs ~56 µs reflection (derived from #4349).
- Cold start: AOT 125.4 ms vs reflection 128.6 ms (#4344); critter-runtime 214–230 ms (#4344/#4349).
- Runtime generation: `Address` 809 µs runtime vs 11.5 µs AOT (#4342).
→ Critter today ≈ reflection speed, at the cost of a second model hierarchy and 2N+1 generated classes per entity.

## Structural picture
**Today** — two parallel stacks meet at `AbstractMapper`.
- Reflection: `ReflectiveMapper` → `new EntityModel(mapper, type)` → `MappingUtil` (hierarchy, type params, `MorphiaDefaultsConvention`, `FieldDiscovery`/`MethodDiscovery`, `ConfigureProperties`, ServiceLoader conventions incl. Kotlin). Plain `PropertyModel` with fields set once + `FieldAccessor`/`MethodAccessor`/`ArrayFieldAccessor`/`ReadWritePropertyAccessor`.
- Critter: `CritterMapper.mapEntity` tries AOT `Class.forName("<pkg>.__morphia.<x>.<X>EntityModel")` → runtime generation (`RuntimeModels` → `CritterGenerator` → `PropertyFinder` → `EntityModelGenerator`/`PropertyModelGenerator`/accessor generators) → `new EntityModel`. Generated `XEntityModel extends CritterEntityModel`, `XPropertyModel extends CritterPropertyModel` override getters with constant bytecode; setters throw.
- Classes per entity (N props): AOT 2N+1 + woven `__readX`/`__writeX` in entity and superclasses; runtime N+1 in a `CritterClassLoader` + N hidden nestmate accessors in a static registry.
- Duplicated logic: `PropertyFinder` ≈ `FieldDiscovery`/`MethodDiscovery`; `PropertyConvention.mappedName` ≈ `FieldDiscovery.discoverMappedName`; `CritterEntityModel.configureProperties` ≈ `ConfigureProperties`; `CritterMapper` ~300 lines of tier/sharing logic over `ReflectiveMapper`'s 82.

**Unified design** — one model, one discovery/convention pipeline for every tier; a last convention swaps each property's accessor: AOT `XAccessor` if the weave marker matches → else runtime nestmate accessor from a `ClassValue` shared by all mappers → else reflective accessor, per property. Getters become `final` field reads; only megamorphic call left is `PropertyAccessor.get`/`set`.

**If EntityModel/PropertyModel went away** (per-entity `Codec<T>` + slim metadata):
- Already emitted: direct accessors (woven `__readX`/`__writeX` + `XAccessor` for AOT; hidden nestmates at runtime) and every per-property constant (names, load names, `TypeData`, flags, annotations, collection, discriminator + key). A generated codec could call `entity.__readX()` directly — fully monomorphic.
- Still to emit: _id generation; `MorphiaPropertySerialization` rules (storeNulls, storeEmpties, ignoreFinals, `@LoadOnly`) — config-dependent; decode switch over mapped + `@AlsoLoad` names; BsonReaderMark + `Conversions` fallback; instantiation matching `ConstructorCreator` semantics; discriminator dispatch; lifecycle calls; lazy child-codec lookup (`ReferenceCodec` needs the datastore).
- Obstacles: metadata consumers (`PathTarget`, validation, indexes, references, `Datastore.save` id/version, `updateQueryWithDiscriminators`, subtypes) all read models → metadata stays, only the codec changes. `EntityModelImporter` already plugs in models + a `MorphiaCodecProvider` (ready-made seam). Reflection tier still needs a generic codec → two codecs must stay identical (C3 shows how fast duplicates drift). AOT config → anything baked needs C4's guard. Kotlin: `KotlinDelegatedPropertiesDiscovery` mutates `PropertyModel`s → codec generation must run after ServiceLoader conventions. Module access: runtime generation still needs full-privilege access (#4365).

## Findings (ranked: C1, C2, C4, C3, C6, C7, C5, C8, C9, C10, C11, C12)

### C1 — Generated getter overrides → megamorphic hot path; some allocate or `Class.forName` per call
- **Category**: speed-hot-path, allocation, correctness
- **Locations**: `PropertyModelGenerator.java:360-361, 430-517`; `EntityModelGenerator.java:172-198`; `GenerationUtils.java:313-336` (`emitClassRef`), `352-368` (`emitTypeData`). Callers: `PropertyModel.java:274-283, 386-390`; `EntityEncoder.java:183-188, 210, 265`; `EntityDecoder.java:87`; `NoArgCreator.java:33-35`; `ReferenceCodec.java:224, 261, 301, 354`.
- **Current**: Encode per property: `equals(idModel)` → `getName()` ×2; `getAccessor()` vtable + itable `get`; `isFinal()` when `ignoreFinals`; `getMappedName()`. Decode per field: `useDiscriminator()`/`discriminatorKey()` per-entity overrides; `getAccessor()`; `setValue` → `getType()`. Per-call allocation/lookup: `getTypeData()` emits a new `TypeData` tree; `getLoadNames()` new `Arrays.asList`; non-public classes → `emitClassRef` does `Class.forName` with TCCL. Generated `accessor`/`entityModel` fields duplicate base fields.
- **Proposed**: (1) generated code configures plain models once (resolve `Class`/`TypeData` once, entity's own loader); no getter overrides. (2) base getters `final`. (3) `EntityEncoder:184` → `propertyModel == idModel`. (4) add mixed-entity and non-public-type cases to `CodecBenchmark` (each shape runs in its own fork today, hiding megamorphic sites).
- **Impact**: 3–5 megamorphic calls per property per doc; est. 3–10% of encode/decode (unmeasured) + `TypeData` alloc / class lookup per call on some paths.
- **Risk**: `@MorphiaInternal`. Old AOT output fails to verify against `final` getters; `tryLoadPregenerated` catches `Throwable` and falls back; C4 makes it clean. No JEP 500 impact.
- **Effort** M — **Confidence** high (mechanism) / medium (magnitude).

### C2 — One model + pluggable accessor strategy
- **Category**: simplicity, speed-startup
- **Locations**: `CritterEntityModel`, `CritterPropertyModel` (setters throw: `CritterPropertyModel.java:22-67`, `CritterEntityModel.java:49-66`); `EntityModelGenerator`; `PropertyModelGenerator`; `PropertyFinder`; `CritterMapper.java:263-390`; `NestmateAccessorRegistry`; `CritterClassLoader`.
- **Why it works**: hidden accessors are already defined in the entity's own nest/loader (`CritterGenerator.java:149-152`) and depend only on the field/method; only generated models depend on config.
- **Proposed**: final `CritterAccessors` convention after `new EntityModel(mapper, type)` picking AOT accessor via `Class.forName(critterPackage + titleCase(prop) + "Accessor")` → else `ClassValue`-cached nestmate → else reflective per property (keep `ArrayFieldAccessor`, Kotlin accessor).
- **Impact**: runtime-tier mapping 570–810 µs/entity → reflection cost + N hidden-class definitions; AOT loses ~1.6 µs/entity of precomputed discovery; model getters monomorphic; semantics match reflection by construction.
- **Risk**: tests/benchmarks detect tiers by model class (`TestCritterMapper.assertRuntimeGenerated`, `MapperVariant.java:66`) → need an explicit marker. Old `XEntityModel` ignored. No new leak surface.
- **Effort** L — **Confidence** medium-high.

### C3 — Discovery semantics have drifted
- **Category**: simplicity, correctness
1. `@Property(concreteClass)` applied in `ConfigureProperties.java:44-45`; nothing in critter (`Hotel.java:26` uses it).
2. `@IdField` applied in `ConfigureProperties.java:31-35`; missing in critter.
3. ServiceLoader conventions run at `EntityModel.java:564`, never for critter. Kotlin convention calls setters critter makes throw → delegated properties map as `x$delegate`. Kotlin tests never run with critter.
4. Interface-level annotations collected at `EntityModel.java:608-620`, not at `EntityModelGenerator.java:58-72`.
5. Shadowed fields: `PropertyModelGenerator.findField` (148-158), `NestmateAccessorGenerator.findDeclaringClassInHierarchy` (80-91) search by name → can pick a subclass's transient field though `FieldInfo.declaringClass` has the right one.
6. AOT weaving invokes setter as `void` (`AddMethodAccessorMethods.java:85`) but `findSetterMethod` ignores return type → fluent setter → `NoSuchMethodError`. Runtime path handles it (`NestmateAccessorGenerator.java:235-242`).
7. Arrays: reflection uses `ArrayFieldAccessor`; critter skips/throws → whole entity falls back.
- **Proposed**: C2. Failing that: generate from the reflective `EntityModel` (`CritterProcessor.kt:23` already builds a `ReflectiveMapper` but only uses its config), pass `declaringClass` through, add a two-mapper parity test.
- **Effort** S per item — **Confidence** high (1–4, 7), medium (5–6, not reproduced).

### C4 — AOT tier has no validity guard
- **Category**: clarity, risk
- **Locations**: `CritterMapper.java:199-211`; `EntityModelGenerator.java:205-222`; `PropertyConvention.java:38-55`; `CritterMapper.java:373-390` (only runtime tier keys on config).
- **Problems**: programmatic config / different runtime properties silently get build-time names (core naming tests miss it — their entities are `private static`, never AOT-generated, see C6). Stale weaving fails with `NoSuchMethodError` at first encode, not load. Generated classes depend on ~25 core signatures, no format version.
- **Proposed**: emit format version + generation key + weave hash in the model/marker class + synthetic hash field woven into the entity; `tryLoadPregenerated` compares and falls back to runtime tier with one log line.
- **Effort** S–M — **Confidence** high that no guard exists.

### C5 — Runtime generation does more work than reflection (only if C2 deferred)
- **Category**: speed-startup
- **Locations**: `PropertyFinder.java:72-74, 275-308, 335-378`; `PropertyModelGenerator.java:69-73, 148-243`; `NestmateAccessorGenerator.java:52, 80-91`.
- **Current**: class file read/parsed 2D−1 times for hierarchy depth D, no cache for shared superclasses; then the same members go through reflection again (exception-driven `getDeclaredField` walks ×2 per field, `TypeData.get`, annotation reads); defines 2N+1 classes; synchronized registry lookup per property; missing superclass `.class` resource → traversal stops at DEBUG, properties silently lost.
- **Proposed**: reflection-only at runtime or cache `ClassModel`; reuse `declaringClass`; pre-check eligibility before emitting.
- **Effort** M — **Confidence** high.

### C6 — Tier and fallback logic
- **Category**: speed-startup, clarity
- **Locations**: `CritterMapper.java:94, 142, 182-238, 339-348`; `CritterProcessor.kt:47-64`.
- **Current**: AOT misses throw `ClassNotFoundException` per type per mapper; some always miss (`@ExternalEntity` stand-ins — processor scans `@Entity` only, `CritterProcessor.kt:61`; non-public entities — ClassGraph without `.ignoreClassVisibility()`; AOT-ineligible) — unreported. Runtime failures cached per mapper only → each new mapper (e.g. per tenant) retries + re-logs WARN; partial work left behind. Fallback per entity, not per property.
- **Proposed**: global `ClassValue` tier decision (ctor / absent / failed + cause); eligibility checks before emitting; per-property fallback; critter-maven reports skips.
- **Effort** S–M — **Confidence** high.

### C7 — Session copy rebuilds every model by reflection
- **Category**: speed per session/txn, correctness
- **Locations**: `CritterMapper.java:98-160`; `AbstractMapper.java:84-89, 114-129`; `MorphiaDatastore.java:360`; `SessionDatastore.java:59-60`.
- **Current**: every session `new Conversions` (loses custom converters; javadoc 102-108 admits it); every entity reruns reflective ctor + full generated ctor (annotation builders, serialization objects, reflective lifecycle scan); javadoc 99-100 says references shared, code 144-146 creates new instances.
- **Proposed**: `protected copyModel` hook on `AbstractMapper` so `CritterMapper` can share `conversions`; fix javadoc. (See also D1: root cause is the per-datastore codec cached on `PropertyModel`.)
- **Effort** S — **Confidence** high.

### C8 — Remaining per-document reflection: instantiation
- **Category**: speed-hot-path, startup
- **Locations**: `InstanceCreatorFactoryImpl.java:43-56`; `NoArgCreator.java:27-30, 40`; `ConstructorCreator.java:140`.
- **Current**: each decoded doc → `Constructor.newInstance`; `NoArgCreator` built per document and calls `setAccessible(true)` each time. Generated models also replay reflection at mapping time (TypeData/annotations read reflectively and re-emitted; non-Morphia annotations round-trip through `RuntimeVisibleAnnotations`, `PropertyModelGenerator.java:402-425`; lifecycle scanning and 12 validator rules run reflectively per model).
- **Proposed**: emit an instantiator (woven `__new()` AOT, nestmate at runtime, ctor call with `@Name` args). Moving lifecycle/validation to build time = low value.
- **Effort** M — **Confidence** medium.

### C9 — Dead code (grep-verified)
- `VarHandleAccessorGenerator` (399 lines, test-only; handles in instance fields not `static final` → not JIT-friendly anyway); `PropertyModelGenerator.typeData`/`typeDataFromSignature`/`typeDataFromTypeArg` (253-309, tests only); `Critter` instance members (100-115), `identifierCase`, `propertyAnnotations`; `CritterParser` (wrapper, one caller); `ExtensionFunctions.titleCase`/`snakeCase`; `CritterClassLoader.bytes()`; `CritterEntityModel.entityAnnotation` and `mapper` fields; generated `entityModel` field + `getEntityModel()` override; redundant `setType` call; `CritterPropertyModel` pass-through overrides (71-73, 91-93); unused overloads (4/6 `PropertyModelGenerator` ctors, 4/6 `CritterGenerator.propertyModelGenerator`, `generate`/2, `entityModel`/3, `PropertyFinder.find`/2); `signature` field on `FieldInfo`/`MethodInfo`.
- Plugin: `generate-criteria` mojo is a TODO stub (`CritterSourceProcessor.kt:120-129`) still pulling in `roaster-jdt`.
- **Effort** S.

### C10 — Duplicated helpers & tangled responsibilities
- Duplicates: `mappedName` ×2; transient rules ×3; `titleCase`/`methodCase` ×2 each; 4 primitive-descriptor switch tables; 3 hierarchy field-lookup walks; final fields written with `Field.set` in 3 places (one place better for JEP 500 policy).
- Tangled: `CritterGenerator` and `PropertyFinder` construct each other and interleave discovery with emission (`PropertyFinder.java:57, 81-98`); generators take a `Mapper` only for its config.
- **Effort** S–M.

### C11 — API surface & docs
- Most of `dev.morphia.critter.*` public without `@MorphiaInternal`/`package-info`; `public static final` lists in `Critter` are mutable `ArrayList`s; `CritterEntityModel` javadoc is just "0"; child-first `dev.morphia.critter.*` rule in `CritterClassLoader.shouldRegister` (148-157) is test plumbing in production; `design/GeneratedCode.adoc` item 2 recommends the constant-getter approach C1 shows is JIT-hostile.
- **Proposed**: mark internal before 3.0 GA, immutable lists, fix docs. **Effort** S.

### C12 — Build-time waste in critter-maven (low priority)
- `PropertyFinder.java:152-169, 210-267`: class files re-read and shared superclasses re-woven per entity. Per-run `ClassModel` + weave cache. **Effort** S.
