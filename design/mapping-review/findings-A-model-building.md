# Findings A — Model building & mapping-time (startup) pipeline

(Agent A report, saved by the coordinator because the agent's own file write was blocked.)
Backed by code read; some claims confirmed by plain `java` runs (no Maven) against the scratchpad copy of `morphia-benchmarks.jar` (built 14:15). Timings are single runs, not JMH; allocation via `ThreadMXBean`. Harness sources: `scratchpad/{cgtest,bench}/` (`ScanCost.java`, `Harness.java`, `gen/*.java`). No repo edits.

## Ranking
| ID | Title | Category | Impact | Effort | Conf |
|---|---|---|---|---|---|
| A1 | Default `packages=.*` scans the whole classpath with `enableAllInfo` and initializes every class | speed-startup | seconds | S | high (measured) |
| A2 | No-arg entities buffer every decoded property; `NoArgCreator` branch dead (= B4) | hot-path / alloc | ≈ −216 B, −150 ns per decoded entity | S | high (measured) |
| A3 | `register()` keeps a model that failed validation; get-then-put race in `ReflectiveMapper` | correctness | correctness | S | high (verified) |
| A4 | Re-keying registered properties: `@IdField` `_id` not findable; Kotlin delegated property listed twice | correctness | per doc for affected entities | S–M | high / med-high |
| A5 | Two subtype linkers; `getSuperClass()` can return an interface; pending map never drains | clarity / correctness | correctness, small CPU/mem | S | high (verified) |
| A6 | Three generic-resolution implementations, three confirmed bugs | clarity / correctness | correctness | M | high (verified) |
| A7 | Small per-call work fixable at mapping time | hot-path | small | S | high |
| A8 | Validation: two rules can never fire, 8 copy-and-scan passes, comparator breaks contract | simplicity / dead-code | small | S | high |
| A9 | Critter runtime: `PropertyFinder` re-reads superclass classfiles, exception-driven field walks | speed-startup (critter-runtime) | scales with depth | S | med-high |
| A10 | Dead & misleading code bundle | dead-code | clarity | S | high |
| A11 | `EntityModel` builds itself inside its ctor; build-only state stays on the model; annotation sets differ between mappers | simplicity / clarity | clarity, small startup | M | med-high |

## Cross-references (covered by D/C; A's supporting measurements)
- **Mapper copy per session**: 11 benchmark types → `mapper.copy()` 22–37 µs warm (~2–3 µs/entity); `new MorphiaDatastore(ds)` (what `startSession()` does) 33–80 µs, plus lazily rebuilt codecs/creator plans per session. Root cause: `PropertyModel.codec` holds datastore-bound codecs (`PropertyModel.java:317-324`, `MorphiaCodec.java:200-210`, e.g. `ReferenceCodec(datastore)`). Side bug: copy ctor `MorphiaDatastore.java:356-365` skips `importModels()`.
- **ServiceLoader per EntityModel**: one `ServiceLoader.load(MorphiaConvention).forEach` ≈ 30 µs early, 5–7 µs fully warm; warm reflective mapping ≈ 13 µs/entity total → ~40%. Uses TCCL not the mapper's loader. Critter never applies ServiceLoader conventions (Kotlin convention never runs).
- **Deep equals/hashCode, getSubtypes**: `getSubtypes()` on `Shape` (3 subtypes) 1.6–2.3 µs/call; `EntityModel.hashCode` 300–600 ns vs ~1–2 ns identity. `EntityEncoder.java:56` calls `propertyModel.equals(idModel)` per property per encode. Generated annotation builders' `hashCode` (e.g. `EntityBuilder`) breaks the `Annotation.hashCode` contract → equal annotations from critter and reflection hash differently.
- **isMappable**: runs 2–3× on the mapping path (`tryGetEntityModel` → `mapEntity`, `MorphiaCodecProvider.get`).
- **Critter duplicated discovery**: A9 is narrower (repeated classfile reads inside `PropertyFinder`).

## A1 — Default `packages=.*` scans the whole classpath with `enableAllInfo` and initializes every class
- **Locations**: `AbstractMapper.java:415-441` (`enableAllInfo` 421, `Class.forName(..., true, ...)` 435), `:315-334` (`map(String)`, `mapPackage`); `MorphiaDatastore.java:196-201`; `MorphiaConfig.java:409-416` (`@WithDefault(".*")`), `:92-97`; `docs/modules/ROOT/examples/complete-morphia-config.properties:56`.
- **Current**:
  ```java
  ClassGraph classGraph = new ClassGraph().addClassLoader(loader).enableAllInfo();
  ... classGraph.acceptPackages(packageName);            // ".*"
  for (ClassInfo classInfo : scanResult.getAllClasses()) {
      try { classes.add(Class.forName(classInfo.getName(), true, loader)); } catch (Throwable ignored) {}
  }
  ```
  then `mapEntity` → `isMappable` per class. `MorphiaConfig.load()` without `morphia.packages` → `.*`; ClassGraph treats `acceptPackages(".*")` as every package (checked: all 280 classes in the classgraph jar). `enableAllInfo` parses fields/methods/annotations/constant initializers — none used. `initialize=true` runs every class's static initializer; `catch (Throwable)` hides failures.
- **Measured** (17 ordinary jars — driver, bson, jackson, guava, kotlin-stdlib, byte-buddy, smallrye-config, classgraph, slf4j…; 9,545 classes):

  | Variant | Scan | `forName` | Classes loaded |
  |---|---|---|---|
  | Current | 1.25 s | 2.57 s (9,365 initialized, 180 throws swallowed) | ~12.1k |
  | `enableClassInfo().ignoreClassVisibility()` + `initialize=false` | 0.85 s | 1.4 s | ~10.8k |
  | Annotation pre-filter (`getClassesWithAnnotation`) | 1.0–1.1 s | nothing extra | 1,477 total |
- **Proposed**: (1, S, same mapped set) `enableClassInfo().ignoreClassVisibility()` (visibility must stay ignored for package-private entities) + `Class.forName(name, false, loader)`. (2) add `enableAnnotationInfo()`, load only candidates:
  ```java
  ClassInfoList c = r.getClassesWithAnnotation(Entity.class.getName())
          .union(r.getClassesWithAnnotation(ExternalEntity.class.getName()));
  for (ClassInfo i : c.getInterfaces()) c = c.union(r.getClassesImplementing(i.getName()));
  ```
  keep `isMappable` as final check. (3) one-time warning when `packages` is the `.*` default; consider an empty default (lazy mapping already works). (4) `LinkedHashSet` not `HashSet<Class>` (identity-hash order → nondeterministic mapping/error order). (5) merge `map(String)`/`mapPackage(String)`.
- **Impact**: once per datastore construction per configured package; with the default, seconds, growing with the classpath, plus metaspace bloat and library static-initializer side effects. `MappingBenchmark` maps an explicit list → not measured there; add a package-scan benchmark.
- **Risk**: no public API change. `initialize=false` delays entity static initializers to first use. Step 2: interface-only-mappable classes depend on the `getClassesImplementing` union; verify `getClassesWithAnnotation` covers `@Inherited` subclasses and superclasses outside accepted packages (believed yes, unconfirmed). Critter shares `getClasses` → same gain.
- **Effort** S (step 1) / M (2–3) — **Confidence** high.

## A2 — No-arg entities buffer every decoded property; `NoArgCreator` branch dead (same as B4)
- **Locations**: `InstanceCreatorFactoryImpl.java:40-66`; `ConstructorCreator.java:45-46` (pending lists), `:96` (regex), `:106-127` (`bestConstructor`), `:137-149` (`getInstance` replay), `:188-202` (`set`); `NoArgCreator.java`; `EntityDecoder.java:48-50, 69-70`.
- **Current**: `bestConstructor` keeps ctors where `stream(it.getParameters()).allMatch(...)` — vacuously true for no-arg → returned whenever present (unless a larger matching ctor exists). So the `else` branch (`getDeclaredConstructor()` → `new NoArgCreator(declared)`) is unreachable; falls through to `getFullConstructor`. All 10 benchmark models use `ConstructorCreator` (checked). Zero-param plan → every `set()` → `pendingModels.add; pendingValues.add`; `getInstance()` replays via `setValue`.
- **Measured** (`SimpleEntity`, 8 props): creator path (create, set, `getInstance`) 272 B/op, ~350–430 ns; construct-first + direct `setValue` (same conversion path) 56 B/op, ~190–260 ns.
- **Proposed**: `Plan.noArgs = types.length == 0`; when set, create the instance in the creator ctor (or first `set`) and `model.setValue(instance, value)` directly; lazily allocate pending lists; delete unreachable branch + log line (decide whether `NoArgCreator` goes); static `Pattern` for `arg[0-9]+` (line 96).
- **Impact**: per decoded entity and embedded object: ~216 B and ~150–200 ns per no-arg entity ≤10 props (more past 10 as ArrayLists grow); ~5–7% of decode SIMPLE allocation.
- **Risk**: @MorphiaInternal; same `setValue`/`Conversions` semantics; `LifecycleDecoder` already constructs first. Both mappers benefit (`CritterEntityModel` → `EntityModel(Class)` creates the same factory).
- **Effort** S — **Confidence** high.

## A3 — `register()` keeps a model that failed validation; get-then-put race
- **Locations**: `AbstractMapper.java:360-378`; `ReflectiveMapper.java:70-76` (unsynchronized); `CritterMapper.java:167-191` (synchronized, comment explains why); `EntityModel.java:601-604`.
- **Current**:
  ```java
  var existing = mappedEntities.get(name); if (existing != null) return existing;
  mappedEntities.put(name, model);
  if (validate && !model.isInterface()) new MappingValidator().validate(this, model);   // may throw
  discriminatorLookup.addModel(model); ... linkHierarchy(model);
  ```
  No rollback on failure. **Verified** both mappers with `@Entity BadVersion { @Id ObjectId id; @Version String version; }`: first `getEntityModel` throws `ConstraintViolationException`, second returns the invalid model with no error — not in `discriminatorLookup`, `mappedEntitiesByCollection` or the hierarchy. Non-atomic get-then-put + unsynchronized `ReflectiveMapper.mapEntity` → two threads build the same type; the loser is still added as a subtype of interface models during construction (`EntityModel.java:601-604`); both can reach `addModel` → "duplicate discriminator".
- **Proposed**:
  ```java
  EntityModel existing = mappedEntities.putIfAbsent(name, model);
  if (existing != null) return existing;
  try { if (validate && !model.isInterface()) VALIDATOR.validate(this, model); }
  catch (RuntimeException e) { mappedEntities.remove(name, model); throw e; }
  ```
  Move check-build-register into `AbstractMapper`: `synchronized final mapEntity` → `protected abstract buildModel(Class)`.
- **Impact**: correctness (silently using invalid models; intermittent concurrent first-use failure). **Risk**: lock only on the mapping path (cache hits return first), re-entrant; in a reference cycle a type mapped during a failed type's validation stays registered (acceptable).
- **Effort** S — **Confidence** high.

## A4 — Re-keying registered properties: `@IdField` and Kotlin delegated properties
- **Locations**: `ConfigureProperties.java:30-37`; `kotlin/.../KotlinDelegatedPropertiesDiscovery.kt:10-21`; `EntityModel.java:81-83, 129-147, 171-183, 356`; `PropertyModel.java:218-233`.
- **Current**: conventions change `name`/`mappedName` on a `PropertyModel` already in both maps, which stay keyed by the old values; no re-key API.
  - `@IdField`: `propertyModel.mappedName("_id")` after registration. Reflection (verified): `idProperty=myId`, `mappedName="_id"`, but `getProperty("_id") == null` → `EntityDecoder` skips `_id` on load. A `Mapper.copy()` clone registers under `_id` → clones behave differently from originals. Critter (AOT + runtime) ignores `@IdField` (verified: `idProperty=null`, `mappedName="myId"`). No `@IdField` tests.
  - Kotlin delegated: convention renames `x$delegate` → `x` and calls `addProperty` again → `propertyModelsByName` holds both keys → `properties()` yields the instance twice → `EntityEncoder` writes `x` twice; `getProperties().size()` one too high (used by `ConstructorCreator.getFullConstructor`). Hidden by tests: `TestBase` uses clones, and the copy ctor dedupes (`EntityModel.java:134`). Traced, not run.
- **Proposed**: internal `EntityModel.renameProperty(PropertyModel p, String newName, String newMappedName)` (remove old keys, re-register); use from `ConfigureProperties` and the Kotlin convention; drop the copy-ctor dedupe. Long term (A11): finish configuring properties before registering, once, at end of build. Critter: generate `@IdField` support or reject explicitly. Tests: `@IdField` round trip; delegated property yields a single key on a non-clone mapper.
- **Risk**: @MorphiaInternal; `@IdField` is `@MorphiaExperimental`. **Effort** S–M — **Confidence** high (`@IdField`) / med-high (Kotlin).

## A5 — Two subtype linkers; `getSuperClass()` can return an interface; pending map never drains
- **Locations**: `EntityModel.java:96` (`public EntityModel superClass`), `:381-384`, `:591-606`; `AbstractMapper.java:56, 387-413`; `IndexHelper.java:278`.
- **Current**:
  - Two linkers: `buildHierarchy` (reflective models only, in ctor) eagerly maps every interface transitively incl. superclasses' interfaces and calls `i.addSubtype(this)`; `linkHierarchy` (all mappers + clones) links direct parents only, superclass then interfaces.
  - Last parent wins: `addSubtype` always sets `subtype.superClass = this`. **Verified** both mappers with `C extends B implements Tagged` (all `@Entity`): `getSuperClass()` returns `Tagged`. `IndexHelper.findClassIndexes` follows `getSuperClass()` for inherited `@Indexes` → walks the interface instead of `B`.
  - Graphs differ by origin: reflective originals link transitive implementors directly to `@Entity` interfaces; clones (copy → `register` → `linkHierarchy`) and critter models get only direct links; results differ when an intermediate class isn't mapped.
  - `pendingSubtypesByParentName` records every unmapped parent — `java.lang.Object` for every root entity, JDK interfaces (`Serializable`, `Comparable`). `CopyOnWriteArrayList` buckets → every add copies; `Object` bucket O(n²); never removed → holds every model, rebuilt on every copy.
- **Proposed**: link only in `register`; split `addSubtype` from setting the superclass; set `superClass` only when `parent.getType() == type.getSuperclass()`. Pick one interface semantics (transitive, or direct + recursive `getSubtypes`) for all mappers; remove linking from `buildHierarchy` (keep eager `@Entity` interface mapping only if wanted, explicitly). Don't record pending entries for `Object` or unmappable parents; plain list under existing sync; `superClass` private.
- **Impact**: per registration (startup + every copy); correctness for indexes and polymorphic queries + a little memory/CPU. **Risk**: changes what `getSuperClass()` returns (the fix); transitive linking preserves today's reflective `getSubtypes`.
- **Effort** S — **Confidence** high.

## A6 — Three generic-resolution implementations, three confirmed bugs
- **Locations**: `EntityModel.java:405-417, 634-678`; `TypeData.java:113-145, 147-189` (positional substitution 179-181), `122-128` (wildcards); `PropertyModelGenerator.java:182-243` (keeps only `Class` bindings, 242); `MorphiaMapPropertyCodecProvider.java:44-45` (index 0 = key).
- **Verified by running**:
  1. **Positional substitution**: `class MyMap<V,K> extends HashMap<K,V>`, field `MyMap<String,Integer>` → type params `[String, Integer]` in `HashMap` order → map codec uses key `String` (should be `Integer`), value `Integer` (should be `String`). Cause: `typeArgument = subtypeParams.params.get(index++).type` (by position, not by type-variable name).
  2. **Wildcards**: `List<? super Integer>` → `List<? extends Object>`; `WildcardType.getUpperBounds()` never returns null (`[Object]` for `? super`) so lower bounds are never used.
  3. **Mapper divergence**: `@Entity Base<T> { T value; }`, `Sub extends Base<List<String>>` → reflection resolves `List<String>`, critter runtime `Object` (keeps only `Class` bindings) → different codecs.
- **Proposed**: one internal resolver computing the full `TypeVariable`→`Type` binding along the superclass chain (by name/identity, recursive; handles `ParameterizedType`, `GenericArrayType`, wildcards), cached per class via `ClassValue`; used by the reflective builder, `TypeData.get(Type, bindings)` and `PropertyModelGenerator`. Remove the double copy in `TypeData.Builder.build()`.
- **Impact**: per property at mapping time; mainly correctness + simplicity. **Risk**: `TypeData` changes for reordered generic maps and `? super` (bug fix); critter AOT models need regenerating (happens at build anyway).
- **Effort** M — **Confidence** high.

## A7 — Small per-call work fixable once at mapping time
- `getProperty` double lookup (`EntityModel.java:356`; = D4/B6) → `get` then fallback.
- `shouldSerialize` re-reads `config.storeNulls()`/`ignoreFinals()`/`storeEmpties()` + scans for `@LoadOnly` per property per encode (`MorphiaPropertySerialization.java:36-57`); all fixed at mapping time (`MorphiaConfig` immutable; annotation snapshot taken in ctor) → precompute `storeNulls`, `storeEmpties`, `neverSerialize = (ignoreFinals && isFinal) || loadOnly` (= B11).
- `DiscriminatorLookup.lookup` `containsKey`+`get` (`:84-85`) → single `get`.
- `Plan` `String.matches` compiles the regex per ctor parameter (`ConstructorCreator.java:96`) → static `Pattern`.
- `hasAnnotation` `stream().map().reduce()` doesn't short-circuit (`AbstractMapper.java:443-454`); `Entity`/`ExternalEntity` are `@Inherited` → only interfaces need walking → plain loop.
- **Impact**: tens of ns per property (first three per doc/property; last two startup). **Risk** none. **Effort** S.

## A8 — Validation
- **Locations**: `MappingValidator.java:48-54, 49, 76-96`; `validation/fieldrules/PropertyConstraint.java`; `validation/classrules/MultipleId.java`, `DuplicatedAttributeNames.java`; `EntityModel.java:171-183, 539-548`; `PropertyModel.java:116-121`; `FieldDiscovery.java:144-145`.
- **Current**: new `MappingValidator` + 12 rule objects per validated entity; 8 `PropertyConstraint` passes each copying `getProperties()`; `MultipleId`/`MultipleVersions` stream `getProperties(annotation)`; `DuplicatedAttributeNames` copies `getLoadNames()` per property. `MultipleId` can never fire (every `@Id` gets mapped name `_id` → second throws `MappingException(duplicatedMappedName)` from `UniqueMap.put` in `addProperty`; `TestMapping.multipleIds` passes only because of that). `DuplicatedAttributeNames` can never fire (load names enter via `alternateNames` → `UniqueMap.put`, throws first). `TreeSet` comparator `(o1,o2) -> o1.level > o2.level ? -1 : 1` never returns 0, not antisymmetric — works by accident.
- **Proposed**: static immutable rule list (rules stateless); one pass over `model.properties()` running every property rule; delete/deprecate the two dead rules; better: `UniqueMap` records a violation instead of throwing so all problems report together; replace `TreeSet` with a list sorted by `comparing(Level).reversed()`.
- **Impact**: per entity mapping, a few µs; mostly simplicity. **Risk**: rule classes public → deprecate. **Effort** S.

## A9 — Critter runtime: `PropertyFinder` re-reads classfiles, exception-driven walks
- **Locations**: `CritterGenerator.java:42-57`; `PropertyFinder.java:72-74, 275-295, 335-375, 429-447`; `GenerationUtils.java:118-127`; `NestmateAccessorGenerator.java:52, 80-91`; `PropertyModelGenerator.java:148-157, 190-213`.
- **Current**: `CritterGenerator.generate` builds a new `PropertyFinder` (which builds a new `CritterGenerator`) per entity. Runtime mode: `discoverPropertyMethods` reads every superclass classfile (`getResourceAsStream` + `ClassFile.parse`); FIELDS mode `discoverAllFields` reads them all again; METHODS mode `findSetterInHierarchy` re-reads superclasses per getter; repeated for every subclass entity. `NestmateAccessorGenerator(entity, field)` walks with `getDeclaredField` catching `NoSuchFieldException` to find a declaring class `FieldInfo.declaringClass()` already holds; `findField`/`findDeclaringClass` do the same per inherited property (stack trace per exception).
- **Proposed**: `Map<Class<?>, ClassModel>` cache (`computeIfAbsent`) scoped to one `RuntimeModels` instance or generation call, passed into `PropertyFinder`; use `field.declaringClass()`; cached declared-field map per class instead of exceptions.
- **Impact**: per runtime-generated entity (critter-runtime leg + runtime tier of AOT leg — in practice every embedded type); grows with depth × entities; unmeasured. **Risk** low (key by `Class`, scoped). **Effort** S — **Confidence** med-high. (Moot if C2 is adopted.)

## A10 — Dead & misleading code
- `NoArgCreator` branch in `InstanceCreatorFactoryImpl` (A2).
- `DiscriminatorLookup.packages` never filled → `searchPackages` (`:110-120`) loops over an empty set.
- `PropertyModel.isTransient()` (`:374-378`) never called; checks the property *type's* class modifiers (never TRANSIENT) → always false; critter generates a different, field-based meaning.
- `PropertyModel` copy ctor (`:78-90`) drops `isFinal` (latent: only reader goes through shared `serialization` holding the original).
- `TypeData.nestedTypeParameters` (`:232-247`) referenced only by itself and a commented-out block (344-354); `TypeParameters.forEach` throws `UnsupportedOperationException` (191-194).
- `PropertyCodecRegistryImpl.java:51-52`: `CollectionPropertyCodecProvider` + second `MorphiaCollectionPropertyCodecProvider` unreachable (`MorphiaCodecProvider.java:54-55` puts one first; it never returns null for a Collection). Each `MorphiaCodec` allocates a fresh providers list.
- `IndexHelper.collectIndexes(parentModels)` (`:250-251`) always given `emptyList`.
- `EntityModel` commented-out `getSubtype` (359-366) and stale comment in `build()` (583-585).
- `AbstractMapper.map(String)` ≡ `mapPackage(String)` (315-334).
- **Risk**: @MorphiaInternal except public validation rules. **Effort** S.

## A11 — `EntityModel` builds itself inside its ctor; build-only state; annotation sets differ
- **Locations**: `EntityModel.java:85, 100, 110-119, 209-211, 405-417, 550-686` (`MappingUtil`), `591-632`; `EntityModelGenerator.java:58-85`; `EntityListenerAdapter.java:127-145`; `OnEntityListenerAdapter.java:19`.
- **Current**:
  - Whole reflective build runs in the ctor via inner `MappingUtil`; `EntityModel.this` passed to `mapper.mapEntity`/`addSubtype` and every convention while half-built (e.g. `collectionName` unset).
  - Build-only state stays: `parameterization` read only by `getTypeData` (public; NPEs on critter models and clones when given a type variable); `classes` (`classHierarchy()`) read only by `FieldDiscovery`/`MethodDiscovery`, empty for critter models and clones.
  - Annotations gathered twice in an implicit order: type's annotations added at 593 and again at 612 via `findInterfaces(type)`; each superclass's at 627 and again at 612 via 599; interface recursion uses Lists with no visited set (diamonds revisited); winner depends on `putIfAbsent` order (type + `@Inherited`, type's interfaces, superclasses nearest first, superclasses' interfaces) — undocumented.
  - Mappers collect different annotation sets: critter takes only the superclass chain (no interfaces) and only `dev.morphia.annotations.*` (`EntityModelGenerator.java:63-72`) → `@EntityListeners`/`@Indexes` on an interface reach reflective models only; `getAnnotation` for non-Morphia annotations works only on reflective models.
  - `OnEntityListenerAdapter.listen` walks `getDeclaredMethods` for the whole hierarchy per model (each subclass re-walks superclasses), again on every copy.
- **Proposed**: internal `ReflectiveEntityModelBuilder` holding build-only state (ctor just assigns fields); walk hierarchy once with a visited set; one shared annotation-collection utility for both the reflective builder and `EntityModelGenerator` with documented precedence and identical inclusion rules; `ClassValue` caches for pure per-class data (hierarchy, annotations, lifecycle methods) — subclasses and every copy benefit; deprecate `getTypeData`/`classHierarchy`. (Sharing inherited `FieldDiscovery` results with subclasses not recommended: generic bindings differ per subclass, small gain.)
- **Impact**: per entity mapping (+ per copy for listeners); mainly clarity, small startup. **Risk**: third-party `MorphiaConvention`s may call `getTypeData`/`classHierarchy` → keep deprecated delegates that work during the build.
- **Effort** M — **Confidence** med-high.
