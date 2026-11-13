# Morphia Development Guide

## Code quality
- After making changes, run `./mvnw spotless:apply` to ensure code formatting is consistent.

## Build Commands
- Build with deps: `./mvnw install -pl :morphia-core -am -DskipTests`
- Skip dokka: `-Ddeploy.skip=true`
- Skip invoker tests: `-Dinvoker.skip=true`
- Typical build: `./mvnw install -pl :morphia-core -DskipTests -Ddeploy.skip=true`
- Run specific test: `./mvnw test -pl :morphia-core -Dtest="ClassName#method" -Ddeploy.skip=true`
- Run with critter mapper: `-Dmorphia.mapper=critter` (e.g., `./mvnw test -pl :morphia-core -Dmorphia.mapper=critter -Ddeploy.skip=true`)
- Don't use `-am` with `-Dtest=` — it applies the test filter to all modules causing failures
- When running tests to verify a fix, be sure to run it once with mapper=reflection and once with mapper=critter to ensure both work correctly.
  - run the critter mapper first as it's more likely to have issues and you want to catch those early

## Critter Code Generator
- The `critter-maven` plugin generates models at build time (AOT); runtime generation with the Class-File API (`java.lang.classfile` via the `jdk-classfile-backport` library, package `io.smallrye.classfile`) is the fallback
- Build order for critter-maven: `morphia-core` → `critter-maven`
- `morphia.mapper` config: `reflection` (default) or `critter` — selects `ReflectiveMapper` vs `CritterMapper`

## Code Reviews
- When reviewing code, summarize all concerns and recommended changes in a file so they don't get lost.

## Documentation
- When implementing a new feature or fixing a bug, consult the docs in `~/dev/morphia.dev/morphia-docs` (repo `MorphiaOrg/morphia-docs`) for any updates the change needs there
  - Until the 3.0 release, doc edits go on the `docs-redesign` branch; 3.0 content lives under `content/morphia/3.0/`
- When filing PRs, if a docs PR is needed, make sure the code PR and the docs PR each reference the other so the docs stay in sync

## Git Operations
- Never commit or push code without an explicit request from the user.