# Morphia Development Guide

## Code quality
- After making changes, run `./mvnw spotless:apply` to ensure code formatting is consistent.

## Build Commands
- `./mvnw` - Use Maven wrapper (requires Maven 4.1.0+)
- `-pl :artifactId` - Target specific module (e.g., `-pl :morphia-core`)
- `-am` - Also build dependencies that need rebuilding
- Build with deps: `./mvnw install -pl :morphia-core -am -DskipTests`
- Skip dokka: `-Ddeploy.skip=true`
- Skip invoker tests: `-Dinvoker.skip=true`
- Typical build: `./mvnw install -pl :morphia-core -DskipTests -Ddeploy.skip=true`
- Run specific test: `./mvnw test -pl :morphia-core -Dtest="ClassName#method" -Ddeploy.skip=true`
- Run with critter mapper: `-Dmorphia.mapper=critter` (e.g., `./mvnw test -pl :morphia-core -Dmorphia.mapper=critter -Ddeploy.skip=true`)
- Don't use `-am` with `-Dtest=` — it applies the test filter to all modules causing failures

## Critter Code Generator
- Critter bytecode generation is integrated into `morphia-core` (under `dev.morphia.critter` package)
- The `critter-maven` plugin generates models at build time (AOT); runtime generation via Gizmo is the fallback
- Build order for critter-maven: `morphia-core` → `critter-maven`
- `morphia.mapper` config: `reflection` (default) or `critter` — selects `ReflectiveMapper` vs `CritterMapper`

## ASM Annotation Parsing Quirks
- Enum annotation values: stored as `Array<String>` - use `(it as Array<String>)[1]` for enum name
- Class annotation values: stored as `org.objectweb.asm.Type` - use `Class.forName((it as Type).className)`
- `MethodNode.signature` can be null (no generics); always check before use
- `MethodNode.desc` is full descriptor `(params)return` - extract return type with `Type.getReturnType()`
- Type parameters (e.g., `TT;`) cannot be converted to Class - use `Object.class` as erasure

## Code Patterns
- `methodCase()` converts getter names like `getName` → `name`
- `titleCase()` converts `name` → `Name` for generated class names

## Code Reviews
- When reviewing code, summarize all concerns and recommended changes in a file so they don't get lost.

## Git Operations
- Never commit or push code without an explicit request from the user.