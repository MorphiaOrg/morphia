# Code Review: Method-Based Property Accessor Support

**Branch:** critter-mojos
**Date:** 2026-01-28
**Status:** RESOLVED (2026-01-29)
**Files Changed:**
- `critter/core/src/main/kotlin/dev/morphia/critter/parser/Generators.kt`
- `critter/core/src/main/kotlin/dev/morphia/critter/parser/PropertyFinder.kt`
- `critter/core/src/main/kotlin/dev/morphia/critter/parser/asm/AddMethodAccessorMethods.kt` (new)
- `critter/core/src/main/kotlin/dev/morphia/critter/parser/gizmo/CritterGizmoGenerator.kt`
- `critter/core/src/main/kotlin/dev/morphia/critter/parser/gizmo/PropertyAccessorGenerator.kt`
- `critter/core/src/main/kotlin/dev/morphia/critter/parser/gizmo/PropertyModelGenerator.kt`

## Summary

These changes extend Critter to support method-based property accessors (getter/setter patterns) in addition to the existing field-based accessors. The implementation correctly mirrors the field-based approach while adapting for method-based access.

## Concerns

### 1. Wide Type Handling in `AddMethodAccessorMethods.kt`

**Location:** Lines 73 and 103

**Issue:** The `visitMaxs` calls may be insufficient for wide types (`long`, `double`).

```kotlin
// Line 73 (writer method)
mv.visitMaxs(2, 2)

// Line 103 (reader method)
mv.visitMaxs(1, 1)
```

**Details:**
- For `long` and `double` types, parameters and return values occupy 2 slots on the stack/locals
- In `writer()`: when the parameter is a wide type, slot 1 holds the value but it spans into slot 2, so `visitMaxs(3, 3)` may be needed
- In `reader()`: when returning a wide type, the stack needs 2 slots, so `visitMaxs(2, 1)` may be needed

**Note:** `AddFieldAccessorMethods.kt` uses the same values (2,2 and 1,1), so this may work if ASM's `COMPUTE_FRAMES` or `COMPUTE_MAXS` is being used. Verify with a test using `long` or `double` properties.

**Recommended Action:**
- Add integration test with entity containing `long` and `double` getter/setter properties
- If tests fail, update `visitMaxs` calls to handle wide types:
  ```kotlin
  // writer
  val stackSize = if (propertyType.size == 2) 3 else 2
  val localsSize = if (propertyType.size == 2) 3 else 2
  mv.visitMaxs(stackSize, localsSize)

  // reader
  mv.visitMaxs(returnType.size, 1)
  ```

### 2. Missing Setter Validation

**Location:** `AddMethodAccessorMethods.kt`, line 42

**Issue:** The code assumes a setter always exists for every getter.

```kotlin
val setterName = "set${propertyName.titleCase()}"
```

**Details:**
- Read-only properties (getter without setter) will cause the generated `__writeXxx` method to call a non-existent setter
- This will cause `NoSuchMethodError` at runtime when attempting to write to the property

**Recommended Action:**
- Before generating the writer method, verify the setter exists on the entity class
- Option A: Skip writer generation for read-only properties
- Option B: Generate a writer that throws `UnsupportedOperationException`
- The methods list passed to the constructor could be filtered to only include getters that have matching setters

```kotlin
// Example validation in emit()
methods.forEach { method ->
    val propertyName = method.name.methodCase()
    val returnType = Type.getReturnType(method.desc)
    reader(propertyName, returnType, method.name)

    // Only generate writer if setter exists
    val setterName = "set${propertyName.titleCase()}"
    val hasSetterMethod = entity.methods.any {
        it.name == setterName && it.parameterCount == 1
    }
    if (hasSetterMethod) {
        writer(propertyName, returnType)
    }
}
```

### 3. Code Duplication Between Accessor Generators

**Location:** `AddMethodAccessorMethods.kt` and `AddFieldAccessorMethods.kt`

**Issue:** These two classes share significant structural similarity:
- Same `init` block for loading the class
- Same `emit()` structure
- Similar `reader()` and `writer()` method patterns

**Recommended Action (Low Priority):**
Consider extracting common functionality:
- Extract shared `init` logic to `BaseGenerator`
- Create a common interface or abstract methods for `reader`/`writer` generation
- Use template method pattern where base class calls abstract `emitRead`/`emitWrite`

This is a refactoring opportunity, not a bug. Address when convenient.

## Positive Changes

The following fixes are correct and well-implemented:

1. **`Generators.kt`**: Added `Type.VOID_TYPE -> Void.TYPE` mapping for setter return types

2. **`PropertyFinder.kt`**: Properly implemented the TODO branch for method-based property discovery

3. **`PropertyAccessorGenerator.kt` (lines 31-32)**:
   - Fixed `propertyName` to use `methodCase()` for converting `getName` to `name`
   - Fixed `propertyType` to use `method.desc` instead of `method.signature` (signature can be null)

4. **`PropertyModelGenerator.kt`**:
   - Line 65: Added null-safe handling for `method.signature` when getting type arguments
   - Line 68: Added null-safe handling for `method.visibleAnnotations`
   - Lines 88-98: Correctly extracts return type from method signature/descriptor

## Testing Recommendations

1. Add integration tests for entities with:
   - Method-based properties (getter/setter pattern only, no backing fields)
   - `long` and `double` property types
   - Read-only properties (getter without setter)
   - Properties with generic return types
   - Properties without annotations

2. Verify the generated bytecode is valid using a bytecode verifier or by loading and exercising the generated classes

## Resolution (2026-01-29)

### Concern #1: Wide Type Handling - NOT AN ISSUE
`BaseGenerator` uses `COMPUTE_MAXS or COMPUTE_FRAMES`, so ASM automatically computes correct stack/locals sizes. The hardcoded `visitMaxs` values are ignored.

### Concern #2: Missing Setter Validation - FIXED
**Changes made:**
- Modified `AddMethodAccessorMethods.kt` to check if a setter exists before generating the writer method
- For read-only properties (getter without setter), the generated `__write` method now throws `UnsupportedOperationException` with the message "Property 'X' is read-only"
- This allows the `PropertyAccessor` interface to be satisfied while preventing writes to read-only properties

### Concern #3: Code Duplication - DEFERRED
Low priority refactoring opportunity, not addressed in this fix.

### Additional Fix: Getter Name Conversion
**Issue discovered:** The `methodCase()` function was being used incorrectly for getter method names. It only lowercases the first letter, but for getter methods like `getId`, the code needs to strip the "get" prefix and then lowercase.

**Changes made:**
- Added new `getterToPropertyName()` function in `ExtensionFunctions.kt` that properly converts getter names:
  - `"getName"` → `"name"`
  - `"isActive"` → `"active"`
  - `"getX"` → `"x"`
- Updated `AddMethodAccessorMethods.kt`, `PropertyAccessorGenerator.kt`, and `PropertyModelGenerator.kt` to use `getterToPropertyName()` instead of `methodCase()` for method-based properties

### Tests Added
- `TestGizmoGeneration.testMethodBasedAccessors()` - Unit test verifying:
  - Generation of `__read` methods for all annotated getter methods
  - Generation of `__write` methods for properties with setters
  - `UnsupportedOperationException` thrown when writing to read-only properties
- `MethodExample.java` - Test entity with long/double types and a read-only property

### Integration Tests (Pending Infrastructure Fix)
Integration tests were written in `TestMapping.java`:
- `testMethodMappingWideTypes()` - Tests CRUD with long/double types
- `testMethodMappingReadOnlyProperty()` - Tests read-only property behavior

These tests require the integration test infrastructure issues to be resolved before they can run.
