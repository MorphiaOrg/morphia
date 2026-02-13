# Morphia Development Guide

## Build Commands
- `./mvnw` - Use Maven wrapper (requires Maven 4.1.0+)
- `-pl :artifactId` - Target specific module (e.g., `-pl :critter-core`)
- `-am` - Also build dependencies that need rebuilding
- Build with deps: `./mvnw install -pl :morphia-core -am -DskipTests`

## Critter Code Generator
- Changes to `build-plugins` require rebuild before `critter-core` (regenerates `AnnotationNodeExtensions.kt`)
- Integration tests: `./mvnw test -pl :critter-integration-tests`
- Build order: `build-plugins` → `critter-core` → `critter-maven` → `critter-integration-tests`

## ASM Annotation Parsing Quirks
- Enum annotation values: stored as `Array<String>` - use `(it as Array<String>)[1]` for enum name
- Class annotation values: stored as `org.objectweb.asm.Type` - use `Class.forName((it as Type).className)`
- `MethodNode.signature` can be null (no generics); always check before use
- `MethodNode.desc` is full descriptor `(params)return` - extract return type with `Type.getReturnType()`
- Type parameters (e.g., `TT;`) cannot be converted to Class - use `Object.class` as erasure

## Code Patterns
- Kotlin extension functions in `Generators.kt` for ASM Type conversions
- `methodCase()` converts getter names like `getName` → `name`
- `titleCase()` converts `name` → `Name` for generated class names

## Code Reviews
- When reviewing code, summarize all concerns and recommended changes in a file so they don't get lost.