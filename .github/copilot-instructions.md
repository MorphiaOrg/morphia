# Morphia – Copilot Coding-Agent Instructions

## Project overview

Morphia is a Java ODM (Object-Document Mapper) for MongoDB.
It maps annotated Java (and Kotlin) POJOs to/from BSON documents.
The current development branch targets **Java 17**, **Maven 4**, and the **MongoDB Java Driver 5.x**.

---

## Repository layout

| Directory | Purpose |
|-----------|---------|
| `core/` | Main library – mapping, query, aggregation, config |
| `kotlin/` | Kotlin extensions for Morphia |
| `critter/` | Annotation-processor / compile-time query-criteria generator |
| `validation/` | Bean-Validation integration |
| `rewrite/` | OpenRewrite recipes for migrating user code |
| `build-plugins/` | Custom Maven plug-ins used during the build |
| `util/` | Shared test utilities and internal helpers |
| `examples/` | Runnable usage examples (also serve as integration tests) |
| `docs/` | Antora-based documentation sources |
| `audits/` | Code-audit tooling (enabled via the `audits` Maven profile) |
| `config/` | Shared IDE formatter / FindBugs / RevAPI config files |
| `.github/` | CI workflows, issue templates, JBang helper scripts |

---

## Toolchain

- **Java 17** (Temurin) — enforced in `.sdkmanrc` and `pom.xml`
- **Maven 4** (wrapper at `./mvnw`) — use the wrapper, not a system Maven
- **Kotlin** — compiled alongside Java in the `kotlin/` subproject
- **TestNG** — test framework (not JUnit)
- **Testcontainers** — spins up a real `mongo:<major>` Docker container for tests
- **SmallRye Config** — drives `MorphiaConfig` via `morphia-config.properties` on the classpath

---

## Building

```bash
# Compile everything, skip tests
./mvnw install -DskipTests

# Compile and run all tests (requires Docker for Testcontainers)
./mvnw verify

# Build a single subproject
./mvnw install -DskipTests -pl core

# Run tests against a specific MongoDB version (default is 8.0.0)
./mvnw surefire:test -Dmongodb=7.0.0

# Run tests against a specific driver version
./mvnw surefire:test -Ddriver.version=5.3.0
```

Use `test-all.sh` for an interactive matrix test across multiple server / driver combinations.

---

## Testing

### Framework and conventions

- **TestNG** is used; no JUnit.
- Tests that need a running MongoDB instance extend `TestBase` (in `core/src/test/java/dev/morphia/test/`).
- `MorphiaTestSetup` (parent of `TestBase`) starts a Testcontainers `MongoDBContainer` in `@BeforeSuite`; tests share a single container per suite run.
- Pass `-Dmongodb=local` to point at a local `mongod` on `27017` instead of a container.
- The test database name is `morphia_test` (constant `TestBase.TEST_DB_NAME`).

### Running tests

```bash
# All tests in a module
./mvnw test -pl core

# A single test class
./mvnw test -pl core -Dtest=TestDatastore

# A single test method
./mvnw test -pl core -Dtest=TestDatastore#testSave

# Skip tests but build everything
./mvnw install -DskipTests
```

### Test configuration

Create `src/test/resources/morphia-config.properties` (or supply a `MorphiaConfig` programmatically) to override defaults in tests.

---

## Key source-code entry points

| Class | Role |
|-------|------|
| `dev.morphia.Morphia` | Factory – call `Morphia.createDatastore(mongoClient)` or `Morphia.createDatastore(mongoClient, config)` |
| `dev.morphia.Datastore` | Primary API for CRUD, queries, and aggregations |
| `dev.morphia.MorphiaDatastore` | Concrete `Datastore` implementation |
| `dev.morphia.config.MorphiaConfig` | SmallRye Config-backed configuration interface (prefix `morphia`) |
| `dev.morphia.mapping.Mapper` | Maps Java types ↔ BSON documents |
| `dev.morphia.annotations.*` | `@Entity`, `@Id`, `@Property`, `@Indexed`, `@Version`, etc. |

---

## Annotations

Entities must be annotated with `@Entity`.
The identifier field must be annotated with `@Id`.
Additional mapping annotations live in `dev.morphia.annotations`.

Example:

```java
@Entity("users")
public class User {
    @Id ObjectId id;
    String name;
    int age;
}
```

---

## Configuration

Morphia reads `morphia-config.properties` from the classpath (via SmallRye Config).
All keys are prefixed with `morphia.`.
Common properties:

| Key | Default | Description |
|-----|---------|-------------|
| `morphia.database` | *(required)* | MongoDB database name |
| `morphia.collection-naming` | `identity` | Collection naming strategy |
| `morphia.property-naming` | `identity` | Property / field naming strategy |
| `morphia.discriminator` | `className` | Discriminator function for polymorphism |
| `morphia.auto-import-models` | `true` | Auto-discover `@Entity` classes |

Programmatic configuration uses the builder returned by `MorphiaConfig`:

```java
MorphiaConfig config = MorphiaConfig.load(); // from classpath
// or build manually with ManualMorphiaConfig
```

---

## CI / GitHub Actions

Workflows live in `.github/workflows/`:

| File | Trigger | What it does |
|------|---------|-------------|
| `pull-request.yml` | PR opened/updated | Code audits + full build |
| `build.yml` | Push to non-dependabot branches / tags | Matrix build across MongoDB and driver versions; release |
| `audits.yml` | Scheduled / manual | Extra code-quality audits |
| `claude.yml` | `@claude` comment | Invokes Claude AI assistant |

The build matrix is generated dynamically by JBang scripts in `.github/` (`BuildMatrix.java`, `DriverVersions.java`, `DriverSnapshot.java`).

The `pull-request.yml` workflow runs on every PR and is the gating check:
1. **CodeAudits** job runs first.
2. **Build** job runs after, reusing the compiled artifacts.

---

## Code style

- Formatter config: `config/eclipse-format.xml`
- Import ordering: `config/eclipse.importorder`
- Use the IntelliJ / Eclipse formatter before committing.
- `@MorphiaInternal` marks APIs that are not part of the public contract.
- `@MorphiaExperimental` marks APIs that may change before stabilising.

---

## Common errors and workarounds

| Error | Cause | Fix |
|-------|-------|-----|
| `Cannot connect to MongoDB` in tests | Docker not running / Testcontainers can't pull image | Start Docker daemon; or use `-Dmongodb=local` with a local `mongod` |
| `No MorphiaConfig found` | Missing `morphia-config.properties` on classpath | Add the file to `src/test/resources/`, or call `Morphia.createDatastore(client, new ManualMorphiaConfig())` |
| `MappingException: No usable constructor` | Entity class has no no-arg constructor accessible to Morphia | Add a `protected` or `public` no-arg constructor |
| `ClassCastException` with proxies | Lazy-loading proxy and `instanceof` check clash | Check `MorphiaInternals.proxyClassesPresent()` before using proxies, or disable lazy loading |
| Tests skipped with `SkipException` | MongoDB version below minimum for a feature | Pass a higher `-Dmongodb=` version |
| Build fails on `revapi` compatibility check | Public API change without a documented justification | Add a `revapi.json` entry or suppress with `@SuppressWarnings` as appropriate; see `config/revapi.json` |

---

## Useful Maven flags

| Flag | Effect |
|------|--------|
| `-DskipTests` | Skip all tests |
| `-Dmongodb=8.0.0` | MongoDB version for Testcontainers |
| `-Ddriver.version=5.6.0` | MongoDB Java driver version |
| `-pl core` | Build only the `core` module |
| `-am` | Also build upstream dependencies |
| `-T 1C` | Parallel build (1 thread per core) |
| `-e` | Show full exception stack traces |
| `-Paudits` | Activate the `audits` Maven profile |
