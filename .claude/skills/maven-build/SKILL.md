---
name: maven-build
description: Build and test the Morphia project using Maven. Use when compiling, running tests, or building artifacts.
allowed-tools: Bash(mvn:*), Bash(./mvnw:*)
---

# Maven Build Commands

This is a multi-module Maven project. Use `mvn` or `./mvnw` (if available) to build and test.

## Common Commands

- **Compile**: `mvn clean compile`
- **Run tests**: `mvn test`
- **Run specific test**: `mvn test -Dtest=ClassName#methodName`
- **Integration tests**: `mvn clean verify`
- **Full build (skip tests)**: `mvn clean install -DskipTests`
- **Full build**: `mvn clean install`

## Module-Specific Builds

To build or test a specific module:
```
mvn -pl module-name test
mvn -pl module-name -am clean install
```

The `-am` flag builds required dependencies.

## Guidelines

- Prefer `./mvnw` if the wrapper exists in the project root
- Always run tests after making code changes to verify nothing broke
- If tests fail, analyze the error output and suggest fixes
- Use `-DskipTests` only when explicitly requested or for quick compile checks
