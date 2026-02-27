# Issue 4179 Evaluation: Integrate critter-core into morphia-core

## Intent
Merge critter's bytecode-generated mapping into morphia-core so users get a faster, reflection-free mapper out of the box, while preserving the legacy mapper as a fallback. The goal is a single artifact with a configuration switch.

## Plan Strengths
- Clear phasing: config option → interface extraction → code migration → cleanup
- Fallback strategy (CritterMapper → ReflectiveMapper) is well-defined
- Test matrix with Maven property for CI coverage of both mappers
- Backward compatibility explicitly called out

## Open Issues

All evaluation issues have been resolved. See Design Decisions table below.

---

# Progress Tracking

Each phase has a tracked sub-issue. When a phase is completed, check its box, close the sub-issue, and **update this issue's title** with the new percentage: `[XX%]` where `XX = (completed_phases / 7) * 100`.

## Directive

The parent issue title (#4179) includes a percentage indicator (e.g., `[0%]`, `[14%]`, `[100%]`). **When a phase or task in a phase is completed**, recompute the percentage for all issues involved and update the issue titles.  Each phase's percentage complete should be a function of the number of tasks in that.  The percent complete on the parent issue should be a function of the completed tasks across all phases.

Update command:
```bash
gh issue edit 4179 --repo MorphiaOrg/morphia --title "[XX%] Integrate critter-core into morphia-core with new Mapper architecture"
```

Also check the corresponding checkbox in the parent issue body.

| Phase | Issue | Tasks | Status |
|---|---|---|---|
| Phase 1: Mapper interface + config | #4184 | 9 | Pending |
| Phase 2: VarHandle accessor generator | #4185 | 6 | Pending |
| Phase 3: Move critter-core into core | #4186 | 8 | Pending |
| Phase 4: CritterMapper implementation | #4187 | 9 | Pending |
| Phase 5: Wire into MorphiaDatastore | #4188 | 5 | Pending |
| Phase 6: Test infrastructure + CI | #4189 | 7 | Pending |
| Phase 7: Cleanup + documentation | #4190 | 9 | Pending |

### Per-phase progress

Each phase sub-issue title also carries a `[XX%]` indicator based on its own task checklist:

```
phase_percentage = (completed_tasks / total_tasks) * 100, rounded to nearest integer
gh issue edit <ISSUE> --repo MorphiaOrg/morphia --title "[XX%] Phase N: ..."
```

When a phase reaches 100%, close the sub-issue and update the parent (#4179) title with:
```
parent_percentage = (completed_phases / 7) * 100, rounded to nearest integer
gh issue edit 4179 --repo MorphiaOrg/morphia --title "[XX%] Integrate critter-core into morphia-core with new Mapper architecture"
```
As tasks are completed or added/removed, update the phase percentage and task count accordingly.
---

# Implementation Plan

## Design Decisions (Resolved)

| Decision | Choice | Rationale |
|---|---|---|
| Generation strategy | **Hybrid** | Check classpath for pre-generated models (critter-maven AOT), fall back to runtime generation (Gizmo), fall back to reflection |
| Runtime accessors | **VarHandle-based** | Generate accessor classes using `VarHandle` for field/method access. Eliminates need for synthetic `__read/__write` methods at runtime. Nearly native performance after JIT. |
| Language | **Keep Kotlin** | Move critter Kotlin code into morphia-core as-is. Add `kotlin-maven-plugin` to core build. |
| Mapper interface scope | **`@MorphiaInternal` only** | Not exposed on `Datastore` or any public API. Strictly internal contract. **Any changes to the interface contract (method signatures, additions, removals) must be explicitly approved by the user before implementation.** |
| Fallback granularity | **Entity-type level** | If any part of generation fails for a type, the entire type falls back to reflection. No mixed models. |
| `copy()` semantics | **Share immutable models** | `CritterEntityModel` is immutable (setters throw). `CritterMapper.copy()` shares model references, creates new `DiscriminatorLookup`. |
| Module removal | **Last step** | Only after full test matrix is green with both mapper values. |
| Dependencies | **Optional + fail-fast** | Gizmo, ASM, ByteBuddy, Roaster marked `<optional>true</optional>` in POM. If user selects CRITTER mapper but deps are missing, throw clear error at startup listing required deps. |

## Phase 1: Extract Mapper Interface and Add Config Option

**Goal:** Make `Mapper` an interface, rename the current implementation to `ReflectiveMapper`, add `mapper()` to `MorphiaConfig`.

### 1.1 Create `MapperType` enum

**New file:** `core/src/main/java/dev/morphia/mapping/MapperType.java`

```java
public enum MapperType { LEGACY, CRITTER }
```

### 1.2 Add `mapper()` to MorphiaConfig

**File:** `core/src/main/java/dev/morphia/config/MorphiaConfig.java`

- Add `MapperType mapper()` with default `LEGACY`
- Add update method `default MorphiaConfig mapper(MapperType value)` following existing pattern

**File:** `core/src/main/java/dev/morphia/config/ManualMorphiaConfig.java`

- Add `MapperType mapper` field
- Copy in constructor, implement getter with `orDefault(mapper, MapperType.LEGACY)`

**File:** `core/src/main/java/dev/morphia/config/MorphiaConfigHelper.java`

- Add `MapperType` to the converter map for properties file serialization

### 1.3 Extract Mapper interface

**File:** `core/src/main/java/dev/morphia/mapping/Mapper.java`

Convert from `public class Mapper` to `public interface Mapper`. The interface is **strictly `@MorphiaInternal`** — it must NOT appear on `Datastore` or any public-facing API. Any changes to the interface contract (adding/removing/modifying method signatures) require **explicit user approval** before implementation.

The interface surface based on actual external usage (30+ files):

```java
@MorphiaInternal
public interface Mapper {
    // Constants (move to interface as static fields)
    String IGNORED_FIELDNAME = ".";
    List<Class<? extends Annotation>> MAPPING_ANNOTATIONS = ...;
    List<Class<? extends Annotation>> LIFECYCLE_ANNOTATIONS = ...;

    // Entity model operations
    @Nullable EntityModel mapEntity(@Nullable Class type);
    EntityModel getEntityModel(Class type);
    Optional<EntityModel> tryGetEntityModel(Class type);
    EntityModel register(EntityModel entityModel);

    // Bulk mapping
    List<EntityModel> map(Class... entityClasses);
    List<EntityModel> map(List<Class<?>> classes);
    void map(String packageName);
    void mapPackage(String packageName);

    // Lookups
    @Nullable <T> Class<T> getClass(Document document);
    Class getClass(String discriminator);
    <T> Class<T> getClassFromCollection(String collection);
    List<EntityModel> getClassesMappedToCollection(String collection);
    List<EntityModel> getMappedEntities();
    boolean isMappable(@Nullable Class type);
    boolean isMapped(Class c);

    // Entity operations
    PropertyModel findIdProperty(Class<?> type);
    @Nullable Object getId(@Nullable Object entity);
    @Nullable WriteConcern getWriteConcern(Class clazz);

    // Config & state
    MorphiaConfig getConfig();
    DiscriminatorLookup getDiscriminatorLookup();
    List<EntityListener<?>> getListeners();
    boolean hasListeners();
    void updateQueryWithDiscriminators(EntityModel model, Document query);

    // Lifecycle
    Mapper copy();

    // Deprecated
    @Deprecated void addInterceptor(EntityListener<?> ei);
}
```

### 1.4 Create `ReflectiveMapper`

**New file:** `core/src/main/java/dev/morphia/mapping/ReflectiveMapper.java`

Move the entire current `Mapper.java` class body here. Rename to `ReflectiveMapper implements Mapper`. All internal logic stays identical.

### 1.5 Extract shared base class `AbstractMapper`

Since both `ReflectiveMapper` and `CritterMapper` (Phase 4) will share entity registration, discriminator management, listener handling, and config access, extract shared logic into:

**New file:** `core/src/main/java/dev/morphia/mapping/AbstractMapper.java`

```java
@MorphiaInternal
public abstract class AbstractMapper implements Mapper {
    protected final Map<String, EntityModel> mappedEntities = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, Set<EntityModel>> mappedEntitiesByCollection = new ConcurrentHashMap<>();
    protected final List<EntityListener<?>> listeners = new ArrayList<>();
    protected final MorphiaConfig config;
    protected final DiscriminatorLookup discriminatorLookup;
    protected final ClassLoader contextClassLoader;

    // Constructor, copy constructor
    // All shared methods: register(), getEntityModel(), tryGetEntityModel(),
    //   getId(), getConfig(), getMappedEntities(), getClass(), isMapped(),
    //   isMappable(), map(), mapPackage(), getClassFromCollection(),
    //   getClassesMappedToCollection(), getDiscriminatorLookup(),
    //   getListeners(), hasListeners(), updateQueryWithDiscriminators(),
    //   getWriteConcern(), findIdProperty(), addInterceptor()
    //   hasAnnotation(), getClasses() (private helpers)

    // Abstract: the only method that differs
    abstract EntityModel mapEntity(@Nullable Class type);
    abstract Mapper copy();
}
```

`ReflectiveMapper` becomes thin:

```java
public class ReflectiveMapper extends AbstractMapper {
    @Override
    public EntityModel mapEntity(Class type) {
        if (isMappable(type)) {
            EntityModel model = mappedEntities.get(type.getName());
            return model != null ? model : register(new EntityModel(this, type));
        }
        return null;
    }

    @Override
    public Mapper copy() { return new ReflectiveMapper(this); }
}
```

### 1.6 Update all callers

All 30+ files referencing `Mapper` already use it by name. Since the interface keeps the same name, **no import changes needed**. The only change is in `MorphiaDatastore`:

**File:** `core/src/main/java/dev/morphia/MorphiaDatastore.java` (line 127)

```java
// Before:
this.mapper = new Mapper(config);

// After:
this.mapper = createMapper(config);

// New private method:
private static Mapper createMapper(MorphiaConfig config) {
    return switch (config.mapper()) {
        case CRITTER -> new CritterMapper(config);  // Phase 4
        case LEGACY -> new ReflectiveMapper(config);
    };
}
```

For now (before Phase 4), default to `ReflectiveMapper` always. Wire `CritterMapper` in Phase 5.

### 1.7 Add Sofia error message

**File:** `core/src/main/resources/sofia.properties`

Add: `invalidMapperConfig=Invalid mapper configuration value: "{0}". Using LEGACY mapper.`

### Verification

```bash
./mvnw clean install -pl :morphia-core -am -DskipTests -Ddeploy.skip=true
./mvnw test -pl :morphia-core -Ddeploy.skip=true
```

All existing tests must pass unchanged (everything still uses `ReflectiveMapper` via `LEGACY` default).

---

## Phase 2: VarHandle Accessor Generator

**Goal:** Create a Gizmo-based generator that produces `PropertyAccessor` implementations using `VarHandle` instead of synthetic `__read/__write` methods.

### 2.1 Create `VarHandleAccessorGenerator`

**New file:** `core/src/main/kotlin/dev/morphia/critter/parser/gizmo/VarHandleAccessorGenerator.kt`

This is a parallel implementation to `PropertyAccessorGenerator.kt` that generates:

```java
// Example generated class for field "name" on Hotel entity:
public class NameAccessor implements PropertyAccessor<String> {
    private final VarHandle varHandle;

    public NameAccessor() {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
            Hotel.class, MethodHandles.lookup());
        this.varHandle = lookup.findVarHandle(Hotel.class, "name", String.class);
    }

    public Object get(Object model) {
        return varHandle.get((Hotel) model);  // cast + VarHandle.get
    }

    public void set(Object model, Object value) {
        varHandle.set((Hotel) model, (String) value);  // cast + VarHandle.set
    }
}
```

Key differences from existing `PropertyAccessorGenerator`:
- Constructor uses `MethodHandles.privateLookupIn()` + `findVarHandle()` instead of being no-arg
- `get()` uses `VarHandle.get()` instead of calling `__readX()`
- `set()` uses `VarHandle.set()` instead of calling `__writeX()`
- Handles primitive types natively (VarHandle supports them directly — no boxing needed for field access, only for the `Object` bridge method)

For method-based properties (getter/setter discovered via `@Property` on methods):
- Use `MethodHandles.Lookup.findVirtual()` to get a `MethodHandle` for the getter/setter
- Store as `MethodHandle` field, invoke in `get()`/`set()`
- This handles computed properties (where getter does more than return a field)

### 2.2 Update `PropertyFinder` to support dual mode

**File:** `critter/core/src/main/kotlin/dev/morphia/critter/parser/PropertyFinder.kt` (will be moved in Phase 3)

Add a `runtimeMode: Boolean` parameter. When `true`:
- Skip the `classLoader.register(entityType.name, fieldAccessors(...))` calls (no entity modification)
- Use `VarHandleAccessorGenerator` instead of `PropertyAccessorGenerator`
- Still generate `PropertyModelGenerator` the same way (metadata doesn't change)

### 2.3 Update `CritterGizmoGenerator`

Add optional `runtimeMode` parameter that propagates to `PropertyFinder`. When `true`, uses VarHandle path.

### Verification

```bash
# Unit test: generate VarHandle accessor, verify field read/write
./mvnw test -pl :morphia-core -Dtest="VarHandleAccessorTest" -Ddeploy.skip=true
```

Write a test that:
1. Creates a simple `@Entity` class with fields of various types (String, int, long, boolean, List, etc.)
2. Generates VarHandle accessor at runtime via Gizmo
3. Loads the generated class from `CritterClassLoader`
4. Calls `get()` / `set()` on instances
5. Verifies correct values including primitives

---

## Phase 3: Move Critter-Core into Morphia-Core

**Goal:** Physically move Kotlin source, configure the build, refactor `Generators` singleton.

### 3.1 Add Kotlin build support to core

**File:** `core/pom.xml`

Add `kotlin-maven-plugin` (must execute **before** `maven-compiler-plugin`):

```xml
<plugin>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-maven-plugin</artifactId>
    <version>${kotlin.version}</version>
    <executions>
        <execution>
            <id>compile</id>
            <phase>compile</phase>
            <goals><goal>compile</goal></goals>
            <configuration>
                <sourceDirs>
                    <sourceDir>${project.basedir}/src/main/kotlin</sourceDir>
                    <sourceDir>${project.basedir}/src/main/java</sourceDir>
                </sourceDirs>
            </configuration>
        </execution>
        <execution>
            <id>test-compile</id>
            <phase>test-compile</phase>
            <goals><goal>test-compile</goal></goals>
            <configuration>
                <sourceDirs>
                    <sourceDir>${project.basedir}/src/test/kotlin</sourceDir>
                    <sourceDir>${project.basedir}/src/test/java</sourceDir>
                </sourceDirs>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Add dependencies. Kotlin stdlib is required (non-optional, since critter Kotlin code is always compiled into morphia-core). Generation-specific deps are `<optional>true</optional>` — only needed when CRITTER mapper is selected:

```xml
<!-- Required: Kotlin runtime (critter code is Kotlin) -->
<dependency>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-stdlib</artifactId>
</dependency>

<!-- Optional: Only needed when mapper=CRITTER -->
<dependency>
    <groupId>io.quarkus.gizmo</groupId>
    <artifactId>gizmo</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>org.ow2.asm</groupId>
    <artifactId>asm-tree</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>org.ow2.asm</groupId>
    <artifactId>asm-util</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>net.bytebuddy</groupId>
    <artifactId>byte-buddy</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>org.jboss.forge.roaster</groupId>
    <artifactId>roaster-jdt</artifactId>
    <optional>true</optional>
</dependency>
```

**Fail-fast check in `CritterMapper` constructor:** Attempt to load a marker class (e.g., `io.quarkus.gizmo.ClassCreator`) and throw a clear `MappingException` if missing, listing the required dependencies.

### 3.2 Move Kotlin source files

Move all files from `critter/core/src/main/kotlin/` to `core/src/main/kotlin/` preserving package structure:

```
dev/morphia/critter/
├── Critter.kt
├── CritterClassLoader.kt
├── conventions/PropertyConvention.kt
├── parser/
│   ├── Generators.kt
│   ├── ExtensionFunctions.kt
│   ├── PropertyFinder.kt
│   ├── java/CritterParser.kt
│   ├── asm/
│   │   ├── AddFieldAccessorMethods.kt
│   │   ├── AddMethodAccessorMethods.kt
│   │   ├── BaseGenerator.kt
│   │   └── ... (all asm/*.kt files)
│   └── gizmo/
│       ├── BaseGizmoGenerator.kt
│       ├── CritterGizmoGenerator.kt
│       ├── GizmoEntityModelGenerator.kt
│       ├── PropertyAccessorGenerator.kt
│       ├── PropertyModelGenerator.kt
│       ├── VarHandleAccessorGenerator.kt  (new from Phase 2)
│       └── ... (all gizmo/*.kt files)
```

Move relevant test files from `critter/core/src/test/kotlin/` to `core/src/test/kotlin/`.

### 3.3 Refactor `Generators` from singleton to instance

**File:** `core/src/main/kotlin/dev/morphia/critter/parser/Generators.kt`

Before (singleton):
```kotlin
object Generators {
    var configFile = MORPHIA_CONFIG_PROPERTIES
    val config: MorphiaConfig by lazy { MorphiaConfig.load(configFile) }
    val mapper: Mapper by lazy { Mapper(config) }
    var convention = MorphiaDefaultsConvention()
    // utility functions
}
```

After (instance-scoped + companion for utilities):
```kotlin
class Generators(val config: MorphiaConfig, val mapper: Mapper) {
    val convention = MorphiaDefaultsConvention()

    companion object {
        // Keep stateless utility functions here (wrap(), Type.isArray(), Type.asClass())
        fun wrap(fieldType: Type): Type { ... }
        fun Type.isArray(): Boolean = ...
        fun Type.asClass(classLoader: ClassLoader = ...): Class<*> = ...
    }
}
```

### 3.4 Update all files that reference `Generators` singleton

Every file that accesses `Generators.config` or `Generators.mapper` must be updated to receive a `Generators` instance. Key files:

- `CritterGizmoGenerator.kt` — add `generators` parameter, pass to `PropertyFinder`
- `PropertyFinder.kt` — already takes `mapper`, add `config` access via it
- `GizmoEntityModelGenerator.kt` — currently accesses `Generators.config` directly (line 87, 94, 105). Pass `config` through constructor or accept `Generators` instance.
- `PropertyModelGenerator.kt` — currently takes `config` in constructor (already good)

### 3.5 Update `CritterClassLoader` constructor

Accept parent classloader as parameter instead of hardcoding. The parent should be the configured classloader so that non-critter-generated classes can be resolved through the normal delegation chain. Build-time generation (critter-maven) produces permanent `.class` files on the classpath; if those are absent, runtime generation produces classes loaded into this classloader. Runtime-generated classes are **not** cached to disk — they are regenerated on each JVM startup.

```kotlin
class CritterClassLoader(parent: ClassLoader = Thread.currentThread().contextClassLoader)
    : ByteArrayClassLoader.ChildFirst(parent, true, mapOf())
```

**Classloader delegation:**
1. When `CritterMapper.tryLoadPregenerated()` calls `Class.forName(name, true, type.getClassLoader())`, it uses the application classloader — finds build-time generated classes on the regular classpath
2. When `CritterMapper.tryRuntimeGeneration()` generates bytecode, it registers classes in `CritterClassLoader` and loads them from there
3. `CritterClassLoader` delegates to its parent (the configured classloader) for all non-critter classes (entity types, Morphia base classes, etc.)

### Verification

```bash
./mvnw clean install -pl :morphia-core -am -DskipTests -Ddeploy.skip=true -Dinvoker.skip=true
```

Must compile successfully with mixed Java + Kotlin sources.

---

## Phase 4: CritterMapper Implementation

**Goal:** Implement the hybrid discovery mapper with three-tier fallback.

### 4.1 Create `CritterMapper`

**New file:** `core/src/main/java/dev/morphia/mapping/CritterMapper.java`

```java
@MorphiaInternal
public class CritterMapper extends AbstractMapper {
    private static final Logger LOG = LoggerFactory.getLogger(CritterMapper.class);
    private final CritterClassLoader critterClassLoader;
    private final Generators generators;
    private final Set<String> fallbackTypes = ConcurrentHashMap.newKeySet();

    public CritterMapper(MorphiaConfig config) {
        super(config);
        checkDependencies();  // Fail-fast if optional deps missing
        this.critterClassLoader = new CritterClassLoader(contextClassLoader);
        this.generators = new Generators(config, this);
    }

    private static void checkDependencies() {
        try {
            Class.forName("io.quarkus.gizmo.ClassCreator");
        } catch (ClassNotFoundException e) {
            throw new MappingException(
                "CritterMapper requires additional dependencies. Add to your POM: "
                + "io.quarkus.gizmo:gizmo, org.ow2.asm:asm-tree, "
                + "net.bytebuddy:byte-buddy");
        }
    }

    // Copy constructor — share immutable critter models
    private CritterMapper(CritterMapper other) {
        super(other);  // copies config, clones discriminator lookup, copies listeners
        this.critterClassLoader = other.critterClassLoader;  // shared (thread-safe)
        this.generators = other.generators;  // shared (stateless after init)
        this.fallbackTypes = other.fallbackTypes;  // shared set
        // mappedEntities already copied by AbstractMapper copy constructor
    }

    @Override
    public Mapper copy() {
        return new CritterMapper(this);
    }

    @Override
    @Nullable
    public EntityModel mapEntity(@Nullable Class type) {
        if (!isMappable(type)) return null;

        EntityModel model = mappedEntities.get(type.getName());
        if (model != null) return model;

        // Three-tier discovery
        model = tryLoadPregenerated(type);
        if (model == null) model = tryRuntimeGeneration(type);
        if (model == null) model = fallbackToReflection(type);

        return model != null ? register(model) : null;
    }

    /** Tier 1: Check classpath for critter-maven pre-generated model */
    @Nullable
    private EntityModel tryLoadPregenerated(Class<?> type) {
        String modelClassName = Critter.critterPackage(type)
            + "." + type.getSimpleName() + "EntityModel";
        try {
            Class<?> modelClass = Class.forName(modelClassName, true, type.getClassLoader());
            Constructor<?> ctor = modelClass.getConstructor(Mapper.class);
            return (EntityModel) ctor.newInstance(this);
        } catch (ClassNotFoundException e) {
            // Expected when critter-maven wasn't used — not an error
            return null;
        } catch (Exception e) {
            LOG.error("Failed to load pre-generated model for {}: {}",
                type.getName(), e.getMessage());
            return null;
        }
    }

    /** Tier 2: Generate at runtime using Gizmo + VarHandle */
    @Nullable
    private EntityModel tryRuntimeGeneration(Class<?> type) {
        try {
            GizmoEntityModelGenerator generator =
                CritterGizmoGenerator.generate(type, critterClassLoader, generators, true);
            String modelClassName = generator.getGeneratedType();
            Class<?> modelClass = critterClassLoader.loadClass(modelClassName);
            Constructor<?> ctor = modelClass.getConstructor(Mapper.class);
            return (EntityModel) ctor.newInstance(this);
        } catch (Exception e) {
            if (fallbackTypes.add(type.getName())) {
                LOG.error("Runtime bytecode generation failed for {}; "
                    + "falling back to reflection: {}",
                    type.getName(), e.getMessage());
            }
            return null;
        }
    }

    /** Tier 3: Fall back to standard reflection-based EntityModel */
    private EntityModel fallbackToReflection(Class<?> type) {
        return new EntityModel(this, type);
    }
}
```

### 4.2 Key design notes

**Thread safety:**
- `mappedEntities` (from `AbstractMapper`) is `ConcurrentHashMap` — `computeIfAbsent` could deadlock with Gizmo classloading. Instead, use the check-then-register pattern already established in `Mapper.mapEntity()` + `register()` which handles races via `mappedEntities.get()` returning existing on collision.
- `CritterClassLoader.register()` must be synchronized (it already is via `ByteArrayClassLoader`).
- `fallbackTypes` uses `ConcurrentHashMap.newKeySet()` for one-time-per-type logging.

**Immutable model sharing in `copy()`:**
- `CritterEntityModel` throws `UnsupportedOperationException` on all setters — safe to share references.
- `AbstractMapper.copy()` can skip cloning for critter models and only clone reflective fallback models.
- `DiscriminatorLookup` is still per-copy (session isolation).

**Pre-generated model detection:**
- Naming convention: `{package}.__morphia.{simpleName.lowercase()}.{SimpleName}EntityModel`
- Example: `dev.morphia.test.models.__morphia.hotel.HotelEntityModel`
- This matches existing `Critter.critterPackage()` + `GizmoEntityModelGenerator` naming.

### Verification

```bash
./mvnw test -pl :morphia-core -Dtest="CritterMapperTest" -Ddeploy.skip=true
```

Tests:
1. Entity with pre-generated model on classpath → loaded from classpath
2. Entity without pre-generated model → runtime generation succeeds
3. Entity where runtime generation fails → reflection fallback
4. Verify fallback logging: error logged once per type, not on every access
5. Concurrent mapping of same type → no duplicate generation
6. `copy()` → shared models, independent discriminator lookup

---

## Phase 5: Wire CritterMapper into MorphiaDatastore

**Goal:** Complete the config-driven mapper selection.

### 5.1 Update `MorphiaDatastore` factory

**File:** `core/src/main/java/dev/morphia/MorphiaDatastore.java`

```java
private static Mapper createMapper(MorphiaConfig config) {
    return switch (config.mapper()) {
        case CRITTER -> new CritterMapper(config);
        case LEGACY -> new ReflectiveMapper(config);
    };
}
```

### 5.2 Update copy constructor

**File:** `core/src/main/java/dev/morphia/MorphiaDatastore.java`

The existing `mapper.copy()` call is already polymorphic (dispatches to `CritterMapper.copy()` or `ReflectiveMapper.copy()`). No change needed beyond what Phase 1 set up.

### 5.3 Validate `importModels()` integration

**File:** `core/src/main/java/dev/morphia/MorphiaDatastore.java`

`importModels()` uses `ServiceLoader<EntityModelImporter>` and calls `mapper.register()`. This already works with both mapper types since `register()` is in `AbstractMapper`. No changes needed.

### Verification

```bash
# Default (LEGACY) — all existing tests pass
./mvnw test -pl :morphia-core -Ddeploy.skip=true

# CRITTER — run a focused subset first
./mvnw test -pl :morphia-core -Dtest="TestMapping,TestEntityModel" -Dmorphia.mapper=critter -Ddeploy.skip=true
```

---

## Phase 6: Test Infrastructure

**Goal:** Enable parameterized testing, Maven property support, CI matrix.

### 6.1 Add Maven property passthrough

**File:** `core/pom.xml` (surefire plugin config)

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <morphia.mapper>${morphia.mapper}</morphia.mapper>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

Default property in `<properties>`:
```xml
<morphia.mapper>legacy</morphia.mapper>
```

### 6.2 Update `MorphiaTestSetup` to read system property

**File:** `core/src/test/java/dev/morphia/test/MorphiaTestSetup.java`

In the default constructor's `buildConfig()` chain, add:

```java
protected static MorphiaConfig buildConfig() {
    String mapperProp = System.getProperty("morphia.mapper", "legacy");
    MapperType mapperType = MapperType.valueOf(mapperProp.toUpperCase());
    return MorphiaConfig.load()
        .database(...)
        .mapper(mapperType);
}
```

This means **all tests** automatically use the mapper specified by `-Dmorphia.mapper=critter` without any individual test changes. This is the simplest approach and gives maximum coverage.

### 6.3 Add DataProvider for tests that need explicit dual-mapper testing

**File:** `core/src/test/java/dev/morphia/test/TestBase.java`

```java
@DataProvider(name = "mapperTypes")
public static Object[][] mapperTypes() {
    return new Object[][] {
        { MapperType.LEGACY },
        { MapperType.CRITTER }
    };
}
```

For specific tests that should always run with both (e.g., `TestMapping`, `TestEntityModel`), annotate with `@Test(dataProvider = "mapperTypes")` and accept `MapperType` parameter.

### 6.4 Update CI workflow

**File:** `.github/workflows/build.yml`

Add `mapper` to the test matrix:

```yaml
strategy:
  matrix:
    mongo: [8.0, 7.0]
    driver: [5.6.4]
    mapper: [legacy, critter]
  # ... existing config
env:
  maven-flags: >-
    -e -Dmongodb=${{ matrix.mongo }}
    -Ddriver.version=${{ matrix.driver }}
    -Dmorphia.mapper=${{ matrix.mapper }}
```

### 6.5 Fold critter-integration-tests into core tests

The `critter-integration-tests` module copies test sources from morphia-core and re-runs them with critter-generated models. With the new architecture, `./mvnw test -Dmorphia.mapper=critter` achieves the same thing. This module becomes redundant.

**Action:** Keep it for now (Phase 7 cleanup). Mark as deprecated in pom.xml comment.

### Verification

```bash
# Both mappers (full suite)
./mvnw test -pl :morphia-core -Dmorphia.mapper=legacy -Ddeploy.skip=true
./mvnw test -pl :morphia-core -Dmorphia.mapper=critter -Ddeploy.skip=true
```

Both must be green before proceeding to Phase 7.

---

## Phase 7: Cleanup and Documentation

**Goal:** Remove redundant modules, update docs, verify published artifacts.

### 7.1 Remove critter-core module

- Delete `critter/core/` directory entirely
- Update `critter/pom.xml`: remove `<subproject>core</subproject>`

### 7.2 Update critter-maven to depend on morphia-core

**File:** `critter/critter-maven/pom.xml`

Replace `critter-core` dependency with `morphia-core`:

```xml
<!-- Remove -->
<dependency>
    <groupId>dev.morphia.morphia</groupId>
    <artifactId>critter-core</artifactId>
</dependency>

<!-- Add (if not already transitive) -->
<dependency>
    <groupId>dev.morphia.morphia</groupId>
    <artifactId>morphia-core</artifactId>
</dependency>
```

### 7.3 Remove critter-integration-tests (or refactor)

If all core tests pass with `-Dmorphia.mapper=critter`, this module is redundant.

- Delete `critter/critter-integration-tests/` or
- Refactor to only test AOT-specific behavior (critter-maven plugin → generated classes → runtime loading)

### 7.4 Update root POM

**File:** `pom.xml`

- Remove `critter-core` from `<dependencyManagement>` if listed
- Update module list if critter-core was listed

### 7.5 Update documentation

**File:** `.claude/CLAUDE.md`

- Update "Critter Code Generator" section: note integration into morphia-core
- Update build commands: remove separate critter-core build steps
- Add `morphia.mapper` config documentation

**File:** `core/src/main/resources/META-INF/morphia-config.properties` (example)

Document the new option:
```properties
# Mapper implementation: legacy (reflection) or critter (bytecode generation)
# morphia.mapper=legacy
```

### 7.6 Verify published artifact

```bash
./mvnw clean install -Ddeploy.skip=true -Dinvoker.skip=true
```

- `morphia-core` JAR contains Kotlin classes under `dev.morphia.critter`
- `morphia-core` POM declares Kotlin/Gizmo/ASM dependencies
- `critter-core` artifact no longer published
- `critter-maven` still published, depends on `morphia-core`

### Verification

```bash
# Full build from scratch
./mvnw clean install -Ddeploy.skip=true

# Full test matrix
./mvnw test -Dmorphia.mapper=legacy -Ddeploy.skip=true
./mvnw test -Dmorphia.mapper=critter -Ddeploy.skip=true
```

---

## Critical Files Reference

| File | Phase | Action |
|---|---|---|
| `core/.../mapping/MapperType.java` | 1 | Create |
| `core/.../config/MorphiaConfig.java` | 1 | Add `mapper()` |
| `core/.../config/ManualMorphiaConfig.java` | 1 | Add `mapper` field |
| `core/.../mapping/Mapper.java` | 1 | Convert to interface |
| `core/.../mapping/AbstractMapper.java` | 1 | Create (shared base) |
| `core/.../mapping/ReflectiveMapper.java` | 1 | Create (current impl) |
| `core/.../MorphiaDatastore.java` | 1, 5 | Factory method |
| `core/.../critter/parser/gizmo/VarHandleAccessorGenerator.kt` | 2 | Create |
| `core/.../critter/parser/PropertyFinder.kt` | 2, 3 | Dual mode + move |
| `core/pom.xml` | 3 | Kotlin plugin + deps |
| `core/src/main/kotlin/dev/morphia/critter/**` | 3 | Move from critter/core |
| `core/.../critter/parser/Generators.kt` | 3 | Refactor to instance |
| `core/.../mapping/CritterMapper.java` | 4 | Create |
| `core/src/test/.../TestBase.java` | 6 | DataProvider |
| `core/src/test/.../MorphiaTestSetup.java` | 6 | System property |
| `.github/workflows/build.yml` | 6 | Matrix extension |
| `critter/core/` | 7 | Delete |
| `critter/critter-maven/pom.xml` | 7 | Update deps |

## Risk Mitigation

| Risk | Mitigation |
|---|---|
| VarHandle access fails for private fields in other modules (JPMS) | Use `MethodHandles.privateLookupIn()` which works when the calling module has access. If entity is in an unexported package, fall back to reflection. Add `--add-opens` guidance for modular apps. |
| Kotlin compilation slows core build | Measure build time delta. If >30s increase, consider incremental compilation or splitting compilation phases. |
| Gizmo bytecode incompatible with newer JVMs | Gizmo targets the classfile version of the running JVM. Pin minimum Java 17+. |
| CritterClassLoader leaks memory | Tie lifecycle to `CritterMapper` instance. When `MorphiaDatastore` is closed/GC'd, the classloader and generated classes become eligible for GC. |
| Pre-generated models from critter-maven use synthetic `__read/__write` but entity class on classpath doesn't have them (build skipped plugin) | Pre-generated models only exist when critter-maven runs. If the plugin ran, the modified entity class files are also present. This is inherently consistent. |
