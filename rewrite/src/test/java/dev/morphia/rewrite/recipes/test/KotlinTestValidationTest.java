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

import static org.junit.jupiter.api.Assertions.*;

public class KotlinTestValidationTest {

    private static final String QUERY_PACKAGE = "dev.morphia.rewrite.recipes.test.query";
    private static final String PIPELINE_PACKAGE = "dev.morphia.rewrite.recipes.test.pipeline";

    @DisplayName("Checking for Kotlin variants of Java tests")
    @ParameterizedTest(name = "{0}.{2}()")
    @MethodSource("generateTestData")
    public void validateKotlinTestMethodExists(String javaClassName, String kotlinClassName,
            String javaMethodName, String kotlinMethodName) {
        // First check both class names are not null
        if (javaClassName == null || kotlinClassName == null) {
            fail(String.format("Missing Kotlin test class. Expected to find Kotlin class: Kotlin" + javaClassName));
        }

        // Both class names are present, now check method names
        if (javaMethodName == null || kotlinMethodName == null) {
            fail(String.format("Missing Kotlin test method '%s' in class %s. Java class %s has this method.",
                    javaMethodName, kotlinClassName, javaClassName));
        }

        // If we get here, both class and method exist - test passes
        assertNotNull(javaClassName, "Java class name should not be null");
        assertNotNull(kotlinClassName, "Kotlin class name should not be null");
        assertNotNull(javaMethodName, "Java method name should not be null");
        assertNotNull(kotlinMethodName, "Kotlin method name should not be null");
    }

    public static Stream<Object[]> generateTestData() {
        List<Object[]> testData = new ArrayList<>();

        // Generate test data for query tests
        Map<String, Set<String>> queryJavaTests = getJavaTestMethods(QUERY_PACKAGE);
        Map<String, Set<String>> queryKotlinTests = getKotlinTestMethods(QUERY_PACKAGE + ".kotlin");
        testData.addAll(generateTestDataForPackage(queryJavaTests, queryKotlinTests));

        // Generate test data for pipeline tests
        Map<String, Set<String>> pipelineJavaTests = getJavaTestMethods(PIPELINE_PACKAGE);
        Map<String, Set<String>> pipelineKotlinTests = getKotlinTestMethods(PIPELINE_PACKAGE + ".kotlin");
        testData.addAll(generateTestDataForPackage(pipelineJavaTests, pipelineKotlinTests));

        return testData.stream();
    }

    private static List<Object[]> generateTestDataForPackage(Map<String, Set<String>> javaTests,
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
                        kotlinMethodName // null if method doesn't exist
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
        List<Class<?>> testClasses = getTestClasses(packageName);

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