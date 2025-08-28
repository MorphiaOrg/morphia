package dev.morphia.rewrite.recipes.test;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

public class KotlinTestValidationTest {

    private static final String BASE_TEST_PACKAGE = "dev.morphia.rewrite.recipes.test";

    private static final String QUERY_PACKAGE = BASE_TEST_PACKAGE + ".query";
    private static final String PIPELINE_PACKAGE = BASE_TEST_PACKAGE + ".pipeline";

    @DisplayName("Checking for Kotlin variants of Java tests")
    @ParameterizedTest(name = "{0}.{2}()")
    @MethodSource("generateTestData")
    public void validateKotlinTestMethodExists(String javaClassName, String kotlinClassName,
            String javaMethodName, String kotlinMethodName, String packageName) {
        // First check both class names are not null
        Map<String, Set<String>> kotlinTestMethods = getKotlinTestMethods(packageName);
        if (javaClassName == null || kotlinClassName == null) {
            var joiner = new StringJoiner(", ");
            kotlinTestMethods.keySet().forEach(joiner::add);
            fail(format("Missing Kotlin test class. Expected to find Kotlin class: Kotlin%s.  For package '%s.kotlin', found %s"
                    .formatted(javaClassName, packageName, joiner)));
        }

        // Both class names are present, now check method names
        if (javaMethodName == null || kotlinMethodName == null) {
            var joiner = new StringJoiner(", ");
            kotlinTestMethods.get(javaClassName)
                    .forEach(joiner::add);
            fail(format("Missing Kotlin test method '%s' in class %s. Java class %s has this method.  Methods found: %s",
                    javaMethodName, kotlinClassName, javaClassName, joiner));
        }

        // If we get here, both class and method exist - test passes
        assertNotNull(javaClassName, "Java class name should not be null");
        assertNotNull(kotlinClassName, "Kotlin class name should not be null");
        assertNotNull(javaMethodName, "Java method name should not be null");
        assertNotNull(kotlinMethodName, "Kotlin method name should not be null");
    }

    private static Stream<Object[]> generateTestData() {
        return List.of(BASE_TEST_PACKAGE, QUERY_PACKAGE, PIPELINE_PACKAGE)
                .stream().flatMap(pkg -> generateTestDataForPackage(pkg,
                        getJavaTestMethods(pkg),
                        getKotlinTestMethods(pkg))
                        .stream())
                .toList()
                .stream();
    }

    private static List<Object[]> generateTestDataForPackage(String pkg, Map<String, Set<String>> javaTests,
            Map<String, Set<String>> kotlinTests) {
        List<Object[]> testData = new ArrayList<>();

        // Iterate over Java test classes and their methods
        for (String javaClassName : javaTests.keySet()) {
            String kotlinClassName = "Kotlin" + javaClassName;

            // Check if Kotlin class exists
            Set<String> kotlinMethods = kotlinTests.get(javaClassName); // Note: using javaClassName as key

            for (String javaMethodName : javaTests.get(javaClassName)) {
                String kotlinMethodName = null;

                // If Kotlin class exists, check if method exists
                if (kotlinMethods != null && kotlinMethods.contains(javaMethodName)) {
                    kotlinMethodName = javaMethodName; // Same method name
                }

                // Add test data entry
                testData.add(new Object[] {
                        javaClassName,
                        kotlinMethods != null ? kotlinClassName : null, // null if class doesn't exist
                        javaMethodName,
                        kotlinMethodName, // null if method doesn't exist
                        pkg,
                });
            }
        }

        return testData;
    }

    private static Map<String, Set<String>> getJavaTestMethods(String packageName) {
        Map<String, Set<String>> testMethods = new HashMap<>();

        // Get all Java test classes in the package
        List<Class<?>> testClasses = getTestClasses(packageName);

        for (Class<?> testClass : testClasses) {
            String className = testClass.getSimpleName();
            Set<String> methods = Arrays.stream(testClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(org.junit.jupiter.api.Test.class))
                    .map(Method::getName)
                    .collect(Collectors.toSet());

            if (!methods.isEmpty()) {
                testMethods.put(className, methods);
            }
        }

        return testMethods;
    }

    private static Map<String, Set<String>> getKotlinTestMethods(String packageName) {
        Map<String, Set<String>> testMethods = new HashMap<>();

        // Get all Kotlin test classes in the package
        List<Class<?>> testClasses = getTestClasses(packageName + ".kotlin");

        for (Class<?> testClass : testClasses) {
            String className = testClass.getSimpleName();
            // Remove "Kotlin" prefix to match with Java class name
            String javaClassName = className.startsWith("Kotlin") ? className.substring(6) : className;

            Set<String> methods = Arrays.stream(testClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(org.junit.jupiter.api.Test.class))
                    .map(Method::getName)
                    .collect(Collectors.toSet());

            if (!methods.isEmpty()) {
                testMethods.put(javaClassName, methods);
            }
        }

        return testMethods;
    }

    private static List<Class<?>> getTestClasses(String packageName) {
        List<Class<?>> classes = new ArrayList<>();

        // Get the package path
        String packagePath = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        URL resource = classLoader.getResource(packagePath);
        if (resource == null) {
            return classes;
        }

        File directory = new File(resource.getFile());
        if (!directory.exists()) {
            return classes;
        }

        // Find all .class files
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".class"));
        if (files == null) {
            return classes;
        }

        for (File file : files) {
            String className = file.getName().substring(0, file.getName().length() - 6); // Remove .class
            String fullClassName = packageName + "." + className;

            try {
                Class<?> clazz = Class.forName(fullClassName);
                // Only include test classes (those ending with "Test")
                if (className.endsWith("Test")) {
                    classes.add(clazz);
                }
            } catch (ClassNotFoundException e) {
                // Skip classes that can't be loaded
            }
        }

        return classes;
    }
}