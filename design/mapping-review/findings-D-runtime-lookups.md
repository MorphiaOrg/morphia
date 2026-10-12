# Findings D — Runtime mapper lookups & model data structures

(Agent D report, saved by the coordinator because the agent's own file write was blocked.)
HEAD 76697be. Read-only; claims from source read + grep-traced callers. Costs are estimates, not measured.
Coordinator spot-checked D1 (MorphiaDatastore.java:356-365), D2 (PathTarget.java:166), D4 (EntityModel.java:355-357): confirmed.

## Ranking

| ID | Title | Category | Freq | Effort | Conf |
|---|---|---|---|---|---|
| D1 | Every `startSession()`/`withTransaction()` deep-copies the Mapper and rebuilds the whole codec registry | speed-hot-path | per session/txn | M–L | high (exists) / medium (size) |
| D2 | `PathTarget`: regex compiled per segment + avoidable lookups; `UpdateOperator.field()` uncached; `Projection.knownFields` resolves every property | speed-hot-path | per filter/update op/sort key/projection field | S | high |
| D3 | `isMappable()` negatives never cached | speed-hot-path | per path / ref value / Document query | S | high |
| D4 | `EntityModel.getProperty()` always does 2 hash lookups | speed-hot-path | per decoded field per doc | S | high |
| D5 | Deep equals/hashCode that are de-facto identity; `getSubtypes()` rebuilt + re-hashed every call | speed / simplicity | per polymorphic query, path miss, registration | S | high |
| D6 | `getClassFromCollection()` copies set, WARN-logs every call, picks arbitrary class | speed / correctness | per DBRef fetch, per `find(String)` | S–M | high |
| D7 | Non-atomic map+register in default (reflective) mapper | thread-safety | first concurrent use of a type | S | high |
| D8 | `MorphiaCodecProvider.codecs` is a plain HashMap | thread-safety | first use per type per datastore | S | high |
| D9 | Lifecycle dispatch allocs per call; duplicate adapter class | allocation | per entity per event | S | high |
| D10 | `Filter.getValue()` reference path reads back the untranslated field name (likely bug) | correctness | per reference filter | S | medium-high |
| D11 | Repeated lookups in `MorphiaDatastore` (save = 3), Optional chains, `getCollection` rebuilds, refresh builds a codec | allocation | per op | S–M | high |
| D12 | `DiscriminatorLookup`: double lookup, misses never cached, dead code, wrong discriminator key in places | speed/dead/correctness | per polymorphic decode | S | high |
| D13 | Model registry keyed by class *name*; `@ExternalEntity` stand-ins & anonymous subclasses mishandled | correctness | per lookup / edge cases | M | medium |
| D14 | `updateQueryWithDiscriminators` is query logic in Mapper, rebuilt per query | clarity | per query | S | high |
| D15 | Startup: ServiceLoader per EntityModel, wrong classloader | speed-startup | per mapped type | S | high |
| D16 | Mapper interface surface & dead code | dead-code/clarity | — | S–M | high |

## D1 — Every session copies the Mapper and rebuilds the codec registry
- `MorphiaDatastore.java:687-697` (`startSession`) and `:707-711` (`doTransaction`) → `new SessionDatastore` → `SessionDatastore.java:59-63` `super(datastore)` → copy ctor `MorphiaDatastore.java:356-365`: `this.mapper = datastore.mapper.copy(); codecRegistry = buildRegistry(...)`.
- **Reflective copy** (`AbstractMapper:114-129`, `EntityModel(EntityModel)` `EntityModel:121-169`), per entity: copies annotations; rebuilds every PropertyModel and dedupes through a LinkedHashSet (deep hashCode); new `InstanceCreatorFactoryImpl` (constructor-selection reflection reruns on first decode); `initializeListeners()` → `OnEntityListenerAdapter.listen` (`getDeclaredMethods` over hierarchy, 4 `isAnnotationPresent` per method); `register()` → `DiscriminatorLookup.addModel`, `CopyOnWriteArraySet.add` with deep `equals`, `linkHierarchy`.
- **Critter copy** (`CritterMapper:138-160`): `getConstructor(Mapper.class).newInstance(this)` per model (reruns generated ctor + `initializeListeners`); fresh `Conversions` (~50 registrations).
- **buildRegistry**: ~140 codec objects (filters 20, expressions ~55, aggregation ~35, update ops ~11, types ~17); `new MorphiaCodecProvider` → `ServiceLoader.load(MorphiaPropertyCodecProvider)` (`MorphiaCodecProvider:57`, classpath scan); new driver registry has an empty cache → every entity `MorphiaCodec` rebuilt on first use in the session.
- **Root cause**: datastore-bound codecs are cached on the shared model. `PropertyModel.codec` (`PropertyModel:70, 316-322`) is set by `MorphiaCodec.specializePropertyCodecs` (`MorphiaCodec:199-210`); `specializeCodec(ds)` only builds when none cached; `ReferenceCodec` is bound to one datastore → each session needs its own PropertyModels → whole-mapper copy. Isolation asserted by `TestDatastore.testDatastoreClones`, `TestCritterMapper.testSessionDatastoreCopyPattern`, `TestMapper.testMapperCopying`.
- **Side effects of the same coupling**: `MorphiaDatastore(client, config, Mapper)` with a mapper already used by another datastore silently reuses the first datastore's codecs (references resolve against the wrong DB). Copy ctor skips `importModels()` → sessions lose `EntityModelImporter` codec providers (latent; no impls in repo). `CritterMapper` javadoc `:99-112` says models are shared; code at `:144-159` creates new ones.
- **Fix (staged)**: (1) move the per-property codec off `PropertyModel` into `MorphiaCodec` (`Codec<?>[]` indexed by property ordinal, or `IdentityHashMap`); encoders/decoders read it from there (overlaps codec lane). (2) Session shares the mapper (`this.mapper = datastore.mapper`). (3) Optionally share the stateless filter/expression/update/aggregation providers; ServiceLoader once per mapper. Interim cheap win: copy `other.listeners` in the EntityModel copy ctor instead of rescanning.
- **Impact**: O(entities × (properties + methods)) + ~140 allocations + ServiceLoader scan + cold codec caches per transaction. Estimate 0.5–5 ms CPU for 50–200 entities (≈ a local txn round trip). Measurable offline: time `new MorphiaDatastore(ds)` with `Fixtures.offlineClient()`.
- **Risk**: all `@MorphiaInternal`, but isolation tests must change. No other session-specific state found in models besides `PropertyModel.codec`.

## D2 — PathTarget hot path
- `PathTarget.java:166`: `if ("$".equals(segment) || (segment.startsWith("$[") && segment.endsWith("]")) || segment.matches("[0-9]+"))` — for a normal segment the first two are false so `String.matches` compiles the pattern every time.
- `resolveProperty` (`:196-215`): `getProperty` 2 lookups (D4); on miss `context.getSubtypes()` (new HashSet + deep hashing, D5) then recurse per subtype which calls `getSubtypes` again (O(S²)). If no subtype matches, `context` is left at the last subtype tried → with validation off, the next segment resolves against the wrong model. Then `mapper.tryGetEntityModel(model.getNormalizedType())` per segment including terminal String/Integer (uncached negative walk, D3) — duplicated with the successful recursive call that already set `context`.
- Class ctors (`:88-103`) call `isMappable` then `getEntityModel`.
- `UpdateOperator.field()` (`UpdateOperator.java:97-102`) builds a new PathTarget per call; `Operations.versionUpdate` (`Operations.java:66-74`) calls it per `$inc`, the codec calls it again.
- `Projection.knownFields` (`Projection.java:135-143`) does a PathTarget + `getEntityModel(clazz)` per property just to return `mf.getMappedName()`. Aside: `knownFields` omits the discriminator key, unlike `project()`.
- **Fix**: hand-written check (`"$"` / `$[...]` / non-empty all ASCII `'0'..'9'`); iterate cached flat subtype list (D5) without recursion and restore `context` on no match; optional per-property nested-model memo per mapper; cache `field()` (invalidate in `model`/`validate`/`datastore` setters); `knownFields` → just mapped names; class ctors → `tryGetEntityModel(type).orElse(null)`. Later: bounded per-model cache of resolved paths (successes only; skip numeric/positional/map-key segments since `scores.<userId>` is unbounded).
- **Impact**: ~0.5–1 µs per segment (regex) + 0.2–0.5 µs per terminal scalar segment, per filter/sort/update/projection field. S / high.

## D3 — `isMappable` negatives never cached
- `AbstractMapper:280-288`, `hasAnnotation` `:443-454`: a miss walks every superclass + super-interface (no visited set); `Arrays.stream(...).reduce` doesn't short-circuit; 2 `getAnnotation` per class + a stream per level. `@Entity`/`@ExternalEntity` are `@Inherited` (`annotations/.../Entity.java:14`) so the superclass walk is redundant; only interfaces need walking.
- Runtime miss callers: `PathTarget` (terminal segment of almost every path; Document roots), `MorphiaQuery.getQueryDocument:368` (every query on `Document.class`), `ReferenceCodec.encodeId:280-286` + `AbstractMapper.getId` (two misses per raw id value), `FindOptions.apply:143`.
- **Fix**: static `ClassValue<Boolean> MAPPABLE` (own annotations + iterative interface walk; MorphiaProxy→superclass rule inside). Annotations are immutable per class; Boolean value → no loader leak; thread-safe. S / high.

## D4 — `getProperty` always does 2 lookups
- `EntityModel.java:354-357`: `propertyModelsByMappedName.getOrDefault(name, propertyModelsByName.get(name))` — default arg evaluated eagerly.
- Callers: `EntityDecoder.java:89` (per field per document — hottest), `PathTarget:199`, `OperationTarget:57-60`.
- **Fix**: `PropertyModel p = byMapped.get(name); return p != null ? p : byName.get(name);` — same semantics (no null values). S / high.

## D5 — equals/hashCode & getSubtypes
- `EntityModel.equals` (`:463-488`) is effectively identity (compares `creatorFactory`, a new `InstanceCreatorFactoryImpl` per model with identity equality) but only after comparing annotation maps (reflective `Annotation.equals`) and both property maps (deep). `hashCode` hashes all annotations reflectively and every PropertyModel twice (+ load names, TypeData recursively).
- `PropertyModel` (`:174-178`, `:295-310`): `hashCode` includes lazily-set `codec` → hash changes after first use (contract violation); hashes `annotationMap.values()` (identity of a view) which `equals` doesn't compare; `equals` compares accessor by identity.
- `getSubtypes()` (`:373-379`): new HashSet + stream per level, recursive; a depth-d model is re-hashed d times. Every `CopyOnWriteArraySet.add` (`subtypes`, `mappedEntitiesByCollection`) runs the deep `equals`.
- `addSubtype` (`:381-384`) sets `subtype.superClass = this` for interface parents too; `linkHierarchy` does superclass then interfaces so the last interface wins → `class C extends P implements MappedI` gives `C.getSuperClass()` == interface model. `IndexHelper.findClassIndexes:278` walks `getSuperClass()` for inherited indexes. `superClass` is a public mutable field.
- **Fix**: delete both equals/hashCode overrides → identity sets; cache flattened subtype list, invalidate up the parent chain in `addSubtype`; only set `superClass` for class parents; make it private. Tests use `assertNotSame`/`getType()`. S / high.

## D6 — `getClassFromCollection`
- `AbstractMapper:165-186`: copies the set every call; with >1 class per collection (any hierarchy) `LOG.warn` with `MessageFormat` + stream join; returns `get(0)`.
- Callers: `ReferenceCodec.fetchSingle:375-378` (DBRefs), `ReferenceCodec.queryCollection:410` via `datastore.find(collection)` per reference fetch, `MorphiaDatastore.find(String)` (`:887-890`).
- `get(0)` = first registered; package-scan order comes from a HashSet (`AbstractMapper.getClasses:417`) → nondeterministic. For `find("shapes")` without `_id` filter, the discriminator `$in` comes from a possibly-subclass model → silently drops parent/sibling docs. Reference fetches filter on `_id` (skips discriminators) so they only pay warn + allocs.
- **Fix**: `rootModelByCollection` at registration (model whose superclass model isn't in the same collection); warn once at registration only for unrelated roots; make `getClassesMappedToCollection` private. S–M / high.

## D7 — Non-atomic registration (default mapper)
- `ReflectiveMapper.mapEntity` (`:68-76`) unsynchronized; `AbstractMapper.register` (`:360-378`) get-then-put. Two threads lazily mapping the same type → both build, both pass, second `put` overwrites; `discriminatorLookup.addModel` throws "duplicate discriminators" for the loser, or two models in use. `linkHierarchy` race: child can record its parent as pending after the parent drained the list → lost link. `CritterMapper.mapEntity` is `synchronized` for exactly this (`:168-171`).
- **Fix**: synchronize the slow path in `AbstractMapper` (template `mapEntity` + `register`); fast path via `mappedEntities.get` stays lock-free. S / high.

## D8 — MorphiaCodecProvider HashMap
- `MorphiaCodecProvider:38, 66-81`: `codecs` HashMap read/written in `get()`; driver registry doesn't serialize provider calls on miss → concurrent puts can corrupt; duplicate codecs per type. Condition does 3 lookups (`isMapped || isMappable`, then `getEntityModel`).
- **Fix**: ConcurrentHashMap + single `tryGetEntityModel`; get/putIfAbsent (not computeIfAbsent — codec construction can recursively request other entity codecs). Review `AggregationCodecProvider` lazy `codecs == null` init (`:64-70`) similarly. S.

## D9 — Lifecycle dispatch
- `EntityModel:197-207, 435-461, 522-537`; `EntityListenerAdapter:56-79`; `UntypedEntityListenerAdapter:28-37`; `OnEntityListenerAdapter:19-23`.
- `LOG.debug(Sofia.callingInterceptorMethod(...))` formats eagerly (`MessageFormat.format` + `listener.toString()`) per entity per event per global listener even with DEBUG off. Global listeners force LifecycleEncoder/Decoder onto every type.
- `collectArgs` clones `getParameterTypes()` + ArrayList + `toArray()` per invocation.
- `hasLifecycle` uses a stream; called 4× in `MorphiaCodecProvider`, 4× in `ConstructorCreator`.
- `UntypedEntityListenerAdapter.invoke(..., Datastore)` is an overload, not an override → never called; class behaves exactly like `EntityListenerAdapter`; the `isAssignableFrom` branch in `initializeListeners` is pointless.
- **Fix**: guard with `isDebugEnabled()`; precompute per-method argument-kind array → fill an `Object[]`; lifecycle bitmask computed once + early exit; delete `UntypedEntityListenerAdapter`; optionally event enum dispatch. S.

## D10 — `Filter.getValue` reference path (likely bug)
- `Filter.java:158-172`, `OperationTarget.java:48-75`: `this.value = ((Document) operationTarget.encode(datastore)).get(field);` — `encode()` returns `new Document(target.translatedPath(), mappedValue)` but `get(field)` uses the *untranslated* name. `PropertyHandler` is implemented only by `ReferenceCodec`.
- Breaks when mapped name ≠ Java name: `@Reference("r")` (`FieldDiscovery.discoverMappedName` uses `reference.value()`) or non-identity `propertyNaming` (snake_case) → `eq("parentRef", entity)` becomes `{parent_ref: null}`. Existing tests use same-name refs only.
- Also builds a throwaway Document and re-looks-up the property.
- **Fix**: if codec is `PropertyHandler h`, `value = h.encode(value)` directly; drop `OperationTarget` (deprecated for removal; `Filter` is the only caller). Add tests for `@Reference("r")` and snake_case. Medium-high (static trace, not executed).

## D11 — Repeated lookups in MorphiaDatastore
- `save(entity)`: `getCollection` (`:406`) → `save(collection, …)` (`:719`) → `updateVersioning` (`:826`) = 3 model lookups. `save(List)` (`:581-616`): `getEntityModel` + `getId` per entity, then 3 more each.
- `getEntityModel`/`getId` (`AbstractMapper:224-241`) allocate Optionals + capturing lambdas.
- `getCollection` builds a new driver `MongoCollection` (namespace validation) + `WriteConcern.valueOf` from the annotation proxy per query/save/refresh.
- `delete(entity)` (`:372-375`) re-applies `mapper.getWriteConcern` (redundant; `getCollection` already did).
- `refresh` builds a new `MorphiaCodec` per call (`:755-763`).
- **Fix**: internal nullable `findEntityModel(Class)`; resolve the model once per op and pass it down; precompute write concern on EntityModel; cache `MongoCollection` per Class per datastore; drop `Mapper.getWriteConcern`; cache refresh decoder per type. S–M; low per call, removes clutter.

## D12 — DiscriminatorLookup
- `DiscriminatorLookup:42-43, 83-120`: `containsKey` then `get`; misses not cached — a stored discriminator not matching the current one (e.g. className-written data, config now simpleName) does `Class.forName` per document; unknown value throws/catches ClassNotFoundException every time. `packages` never populated → `searchPackages` dead.
- `AbstractMapper.getClass(Document)` (`:151-158`, used by `ReferenceCodec.processId:150-154`) uses config `discriminatorKey`, ignoring per-entity `@Entity(discriminatorKey=…)`. `Projection.project:161-163` uses the config key + annotation's `useDiscriminator()` rather than the model's.
- **Fix**: single get; separate cache for `Class.forName` results only; delete `packages`; use the model's key/flags wherever a model is available. S.

## D13 — Registry keyed by class name
- `AbstractMapper:50, 212, 293, 362-366, 400-411`: `mappedEntities` and `pendingSubtypesByParentName` keyed by `Class.getName()` → same-name classes in different loaders share a model.
- `@ExternalEntity` models registered under the target class name; lookup by the stand-in misses → `mapEntity` builds a full new EntityModel (conventions, ServiceLoader) just for `register` to return the existing one — every time. Critter mode: also `Class.forName` + CNFE.
- Anonymous/synthetic subclasses (`new User(){{…}}`) pass `isMappable` via inherited `@Entity`; EntityModel ctor only rejects non-static member classes → mapped as own entity (empty collection name unless explicit, discriminator `pkg.User$1` or `""`, registered as subtype, included in polymorphic `$in`). Only MorphiaProxy is normalized.
- **Fix**: key by `Class<?>`; register ExternalEntity under stand-in and target; normalize via `ClassValue<Class<?>>` (proxy → superclass; anonymous/synthetic/hidden → nearest annotated superclass); decide explicitly whether anonymous subclasses persist as parent or are rejected. M / medium.

## D14 — `updateQueryWithDiscriminators`
- `AbstractMapper:336-352`, called from `MorphiaQuery.getQueryDocument:353-380` on every find/count/delete/update/modify: `isMappable` + `getEntityModel`, reads annotation proxy `useDiscriminator()` (model has the flag), new list (+ `getSubtypes` when polymorphic). Query-building logic in `Mapper`.
- **Fix**: resolve the model once in the MorphiaQuery ctor (already calls `getCollection`); move logic to MorphiaQuery or `EntityModel.discriminatorValues(polymorphic)`, cached with D5's subtype cache; if a cached list goes into a user-visible Document make it immutable or copy. S.

## D15 — Startup ServiceLoader
- `EntityModel:559-569`: `ServiceLoader.load(MorphiaConvention.class)` per EntityModel construction, thread context loader not the mapper's `classLoader`. `new MappingUtil(mapper)` is a side-effect-only ctor. `register` builds a new `MappingValidator` each time.
- **Fix**: load conventions once per mapper with the mapper's loader (same for `MorphiaPropertyCodecProvider`); one validator per mapper; explicit `build()` instead of side-effect ctor. Impact 0.1–1 ms per scan on large classpaths, per mapped type. Risk: loader change needs a decision for containers relying on TCCL.

## D16 — Interface surface & dead code
- Delete (0 callers): `Mapper.findIdProperty`; `MorphiaDatastore.hasLifecycle(EntityModel, Class)` (`:902-906`); static `ReferenceCodec.encodeId(Mapper, Object, EntityModel)` (`:106-127`, reads `@Reference` off an EntityModel where it's always null); `PropertyModel.getDocumentValue`/`loadFromDocument` (`:184-186`, `:430-443`); `PropertyModel.isTransient()` (`:374-378`, checks the *type's* modifiers; critter emits an override); commented-out blocks `EntityModel:359-366, 583-585`; `DiscriminatorLookup.packages`; `UntypedEntityListenerAdapter`.
- Simplify: `getClassesMappedToCollection` → private; `map(String)`/`mapPackage(String)` duplicate → keep one; `getWriteConcern` → delete (D11); `isMapped` → only `MorphiaCodecProvider` uses it; `updateQueryWithDiscriminators` → move (D14); `getEntityModel`/`tryGetEntityModel`/`mapEntity`/`map(...)`/`register` = five overlapping "give me a model" entry points → one nullable lookup + one throwing wrapper; `PropertyModel.getLoadNames()` copies per call → read-only view; `EntityModel.superClass` → private.
- Deprecated `addInterceptor` (forRemoval since 2.4.0) still drives the D9 global-listener path → plan removal. Fix stale `CritterMapper` copy-ctor javadoc.

## Runtime needs if EntityModel went away
(min info → dynamic lookup vs fixed per-type action)
- **PathTarget** (filters, updates, sort, projection, `$meta`/`$slice`, index field strings): per type, Java name & mapped name → {mappedName, nested normalized type, isMap, isReference}, plus same for transitive subtypes. **Dynamic, by user path string, per segment.**
- **Filter.getValue (reference filters)**: the property's reference encoder (target id + collection). Dynamic via path resolution, then fixed per property.
- **MorphiaQuery**: collection, write concern, useDiscriminator, discriminatorKey, own + transitive subtype discriminators. Fixed per type (invalidated when a subtype maps later).
- **MorphiaDatastore save/insert/replace/merge/delete/refresh**: collection, write concern, id getter/setter (+ id generation rule), version getter/setter, shard keys as (mappedName, getter). Fixed per-type actions dispatched by runtime `entity.getClass()`.
- **Mapper.getId** (references, delete, merge): id getter, proxy-aware. Dynamic by runtime Class → fixed getter.
- **find(String) / DBRef fetch**: collection name → root entity class. Dynamic by collection name.
- **IndexHelper** (startup only): `@Indexes`/`@Indexed`/`@Text` with mapped names + superclass chain. Fixed per type + dynamic path translation at startup.
- **ReferenceCodec / BaseReferenceCodec**: per property `@Reference` flags, normalized target type & its collection, container kind; per referenced value id getter + collection by runtime class; per DBRef collection → class. Fixed per property + dynamic by runtime Class and by collection name.
- **Lazy proxies**: property's declared type + proxy → superclass unwrap rule. Fixed per property type.
- **Lifecycle**: per type event mask, ordered invokers per event (entity methods + `@EntityListeners`), global listener list, "needs no-arg ctor" flag. Fixed per type.
- **Aggregation stages** (`$lookup`, `$graphLookup`, `$out`, `$merge`, `$unionWith`): collection name only. Fixed per type. `$match` uses PathTarget; expression field paths are *not* translated today.
- **Discriminator decode** (EntityDecoder, LifecycleDecoder, ObjectCodec, ReferenceCodec.processId): useDiscriminator + key per type; discriminator → Class. Dynamic by discriminator string.
- **MorphiaCodecProvider**: Class → entity? + codec. Dynamic by Class, memoizable.
- **MappingValidator**: full metadata, startup only.

**Takeaway**: outside codecs there are only four dynamic needs — (1) user path → mapped path + nested type with subtype fallback, (2) discriminator → Class, (3) collection → root class, (4) runtime Class → per-type descriptor. Everything else could be a small immutable per-type record (collection, write concern, id/version/shard-key accessors, discriminator list, lifecycle invokers).

## Suggested measurements (offline client in `benchmarks/`, no server)
1. Time `new MorphiaDatastore(ds)` (D1), reflection vs critter.
2. Time `PathTarget.translatedPath()` and `MorphiaQuery.toDocument()` with 3 filters (D2–D4).
3. Existing `CodecBenchmark.decode` should show D4 directly.
