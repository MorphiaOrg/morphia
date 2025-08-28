# Progressive TestNG to JUnit Migration Plan for Morphia Core

## Project Analysis Summary

**Current State:**

- Framework: TestNG 7.11.0
- Test files: ~400+ Java test files
- Test methods: ~1014 @Test annotations
- Base structure: Hierarchical test base classes (TestBase, MorphiaTestSetup, etc.)
- Dependency injection: Uses TestNG's dependency injection for test setup
- Test containers: MongoDB test containers for integration testing

**Target State:**

- Framework: JUnit 5 (Jupiter)
- Maintain existing test functionality
- Preserve test organization and structure
- Ensure compatibility with existing CI/CD pipelines

## Migration Strategy: Progressive Approach

### Phase 1: Foundation Setup (2-3 days)

#### 1.1 Dependency Management

- Add JUnit 5 dependencies alongside existing TestNG dependencies
- Update parent POM to include JUnit BOM
- Configure Maven Surefire plugin to run both TestNG and JUnit tests

**Required POM Changes:**

```xml
<!-- Add to dependencies -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
<groupId>org.junit.vintage</groupId>
<artifactId>junit-vintage-engine</artifactId>
<scope>test</scope>
</dependency>

        <!-- Update surefire plugin -->
<plugin>
<groupId>org.apache.maven.plugins</groupId>
<artifactId>maven-surefire-plugin</artifactId>
<version>${surefire.version}</version>
<configuration>
    <includes>
        <include>**/*Test.java</include>
        <include>**/Test*.java</include>
    </includes>
</configuration>
</plugin>
```

#### 1.2 Create JUnit Base Infrastructure

- Create `JUnitTestBase` parallel to existing `TestBase`
- Implement JUnit equivalents for common test utilities
- Set up MongoDB test container integration for JUnit

**Key Files to Create:**

- `JUnitTestBase.java` - Base class for JUnit tests
- `JUnitMorphiaTestSetup.java` - JUnit test configuration
- `JUnitMorphiaContainer.java` - Test container setup for JUnit

### Phase 2: Utility Migration (3-4 days)

#### 2.1 Annotation Mapping

Create utility class to handle annotation conversions:

| TestNG                | JUnit 5                       | Notes                  |
|-----------------------|-------------------------------|------------------------|
| `@Test`               | `@Test`                       | Direct mapping         |
| `@BeforeMethod`       | `@BeforeEach`                 | Instance-level setup   |
| `@AfterMethod`        | `@AfterEach`                  | Instance-level cleanup |
| `@BeforeClass`        | `@BeforeAll`                  | Class-level setup      |
| `@AfterClass`         | `@AfterAll`                   | Class-level cleanup    |
| `@BeforeSuite`        | `@BeforeAll` (in suite)       | Suite-level setup      |
| `@AfterSuite`         | `@AfterAll` (in suite)        | Suite-level cleanup    |
| `@DataProvider`       | `@ParameterizedTest` + source | Data-driven tests      |
| `@Test(groups="...")` | `@Tag("...")`                 | Test grouping          |

#### 2.2 Assertion Migration

- Replace TestNG assertions with JUnit assertions
- Create compatibility layer for existing assertion patterns

**Migration Examples:**

```java
// Before (TestNG)
Assert.assertEquals(actual, expected);
Assert.

assertNotNull(object);
Assert.

fail("message");

// After (JUnit 5)
Assertions.

assertEquals(expected, actual);
Assertions.

assertNotNull(object);
Assertions.

fail("message");
```

### Phase 3: Test Categories Migration (1-2 weeks)

#### 3.1 High-Priority Categories (Week 1)

**Order of Migration:**

1. **Unit Tests** (core functionality)
    - `TestCommonTypes.java`
    - `TestMapper.java`
    - `TestEntityModel.java`
    - `TestPropertyModel.java`

2. **Configuration Tests**
    - `TestConfig.java`
    - Config-related tests in `/config` package

3. **Mapping Tests**
    - `/mapping` package tests
    - Essential for core functionality

#### 3.2 Medium-Priority Categories (Week 2)

1. **Query Tests**
    - `TestQuery.java`
    - `FiltersTest.java`
    - Query-related tests in `/query` package

2. **Aggregation Tests**
    - Simple aggregation tests first
    - Complex expression tests later

#### 3.3 Low-Priority Categories (Week 3)

1. **Integration Tests**
    - Tests requiring complex setup
    - Multi-container tests

2. **Performance Tests**
    - Benchmark-style tests
    - Load testing scenarios

### Phase 4: Advanced Features Migration (1 week)

#### 4.1 Parameterized Tests

Convert TestNG `@DataProvider` to JUnit `@ParameterizedTest`:

```java
// Before (TestNG)
@DataProvider
public Object[][] testData() {
    return new Object[][]{
            {"input1", "expected1"},
            {"input2", "expected2"}
    };
}

@Test(dataProvider = "testData")
public void testMethod(String input, String expected) {
    // test logic
}

// After (JUnit 5)
@ParameterizedTest
@MethodSource("testData")
public void testMethod(String input, String expected) {
    // test logic
}

private static Stream<Arguments> testData() {
    return Stream.of(
            Arguments.of("input1", "expected1"),
            Arguments.of("input2", "expected2")
    );
}
```

#### 4.2 Test Lifecycle Management

- Migrate complex test setup/teardown logic
- Handle resource management (MongoDB containers)
- Convert TestNG groups to JUnit tags

### Phase 5: Cleanup and Optimization (3-5 days)

#### 5.1 Remove TestNG Dependencies

- Remove TestNG dependencies from POM
- Clean up unused imports and annotations
- Update CI/CD configurations

#### 5.2 Documentation Updates

- Update README with new test execution instructions
- Document JUnit-specific testing patterns
- Create migration guide for contributors

## Implementation Guidelines

### File Naming Convention

- Keep existing test file names
- Maintain package structure
- Use consistent naming for new utility classes

### Migration Checklist per Test File

- [ ] Replace TestNG imports with JUnit imports
- [ ] Convert annotations (@Test, @BeforeMethod, etc.)
- [ ] Update assertions (TestNG → JUnit)
- [ ] Migrate parameterized tests if applicable
- [ ] Update test lifecycle methods
- [ ] Verify test execution and results
- [ ] Update any test-specific configurations

### Quality Assurance

- Run both TestNG and JUnit tests during migration
- Maintain code coverage levels
- Ensure all tests pass with same functionality
- Performance benchmarking to ensure no regression

## Risk Mitigation

### Low-Risk Items

- Simple unit tests with basic assertions
- Tests without complex setup/teardown
- Standalone test methods

### Medium-Risk Items

- Tests using TestNG groups extensively
- Parameterized tests with complex data providers
- Tests with intricate lifecycle management

### High-Risk Items

- Integration tests with MongoDB containers
- Tests depending on TestNG-specific features
- Custom test listeners and reporters

## Timeline Estimation

| Phase                      | Duration      | Effort            | Dependencies |
|----------------------------|---------------|-------------------|--------------|
| Phase 1: Foundation        | 2-3 days      | 16-24 hours       | None         |
| Phase 2: Utilities         | 3-4 days      | 24-32 hours       | Phase 1      |
| Phase 3: Test Migration    | 2-3 weeks     | 80-120 hours      | Phase 1-2    |
| Phase 4: Advanced Features | 1 week        | 40 hours          | Phase 3      |
| Phase 5: Cleanup           | 3-5 days      | 24-40 hours       | Phase 4      |
| **Total**                  | **6-8 weeks** | **184-256 hours** | -            |

## Success Criteria

- [ ] All existing test functionality preserved
- [ ] No reduction in test coverage
- [ ] All tests execute successfully with JUnit 5
- [ ] CI/CD pipelines updated and functional
- [ ] Documentation updated
- [ ] Team trained on JUnit 5 patterns
- [ ] TestNG dependencies completely removed

## Rollback Plan

If critical issues arise during migration:

1. Keep TestNG dependencies in place until full migration
2. Use feature flags to switch between frameworks
3. Maintain parallel test execution capability
4. Document rollback procedures for each phase

## Post-Migration Benefits

- **Modern Testing Framework**: Access to JUnit 5's advanced features
- **Better IDE Integration**: Enhanced support in modern IDEs
- **Community Support**: Larger ecosystem and community
- **Performance**: Potential performance improvements
- **Maintainability**: Cleaner, more modern test code

## Conclusion

This progressive migration plan minimizes risk while ensuring comprehensive migration from TestNG to JUnit 5. The phased
approach allows for continuous validation and provides opportunities to address issues early in the process. The
estimated timeline provides buffer for unexpected complications while maintaining development velocity.