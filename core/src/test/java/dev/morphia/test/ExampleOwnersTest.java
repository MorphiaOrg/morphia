package dev.morphia.test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.bson.BsonDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every operator example test owns a fixture, and every scenario has its test:
 * <ul>
 * <li>a {@link TemplatedTestBase} {@code @Test} method reads {@code <operator>/<stem>/} for its name, so that folder must
 * hold a {@code scenario.json}, or the legacy {@code action.json} until the test is converted;</li>
 * <li>a {@code scenario.json} under an operator folder names its test: {@code Test<Operator>#test<Stem>}, whose
 * {@code @DisplayName} must equal the scenario's {@code title}.</li>
 * </ul>
 * The methods in {@link #MANUAL} are reviewed tests that own no fixture.
 */
public class ExampleOwnersTest {
    /** The compiled test classes, wherever the build runs from; the module's sources are two levels up. */
    private static final Path CLASSES = classes();
    private static final Path RESOURCES = CLASSES.resolve("../../src/test/resources").normalize();
    private static final List<String> FAMILIES = List.of("aggregation/expressions", "aggregation/stages", "query/filters",
            "query/updates");

    /** Reviewed operator tests that own no fixture, each with the reason. */
    private static final Map<String, String> MANUAL = Map.ofEntries(
            Map.entry("TestBucket#testBucket", "aggregates a mapped entity, not a documented example"),
            Map.entry("TestBucket#testBucketWithBoundariesWithSizeLessThanTwo", "checks Morphia rejects too few boundaries"),
            Map.entry("TestBucket#testBucketWithOptions", "aggregates a mapped entity, not a documented example"),
            Map.entry("TestBucket#testBucketWithUnsortedBoundaries", "checks Morphia rejects unsorted boundaries"),
            Map.entry("TestBucket#testBucketWithoutOptions", "aggregates a mapped entity, not a documented example"),
            Map.entry("TestBucketAuto#testWithGranularity", "aggregates a mapped entity, not a documented example"),
            Map.entry("TestBucketAuto#testWithoutGranularity", "aggregates a mapped entity, not a documented example"),
            Map.entry("TestChangeStream#testChangeStream", "watches a change stream fed from its own data file"),
            Map.entry("TestChangeStream#testChangeStreamOptions", "checks the $changeStream options' encoding"),
            Map.entry("TestLookup#testLookup", "aggregates a mapped entity, not a documented example"),
            Map.entry("TestMerge#keepExisting", "checks the server accepts whenMatched: keepExisting as Morphia writes it"),
            Map.entry("TestPercentile#testExample5", "an empty placeholder for an example the page shows only in $setWindowFields"),
            Map.entry("TestUnwind#testUnwind", "aggregates a mapped entity, not a documented example"));

    @Test
    public void everyExampleTestOwnsAFixture() throws IOException {
        Set<String> problems = new TreeSet<>();
        for (Class<?> type : exampleClasses()) {
            for (Method method : testMethods(type)) {
                if (method.getAnnotation(Test.class) == null) {
                    continue;
                }
                String owner = type.getSimpleName() + "#" + method.getName();
                if (MANUAL.containsKey(owner)) {
                    continue;
                }
                String problem = CurrentTest.problem(method);
                if (problem != null) {
                    problems.add(owner + " " + problem);
                    continue;
                }
                Path folder = folder(type, method);
                if (!Files.exists(folder.resolve("scenario.json")) && !Files.exists(folder.resolve("action.json"))) {
                    problems.add(owner + " has no fixture at " + folder);
                }
            }
        }
        assertEquals(Set.of(), problems);
    }

    @Test
    public void everyScenarioHasItsTest() throws IOException {
        Set<String> problems = new TreeSet<>();
        for (String family : FAMILIES) {
            Path root = existing(RESOURCES.resolve("dev/morphia/test").resolve(family));
            try (Stream<Path> files = Files.walk(root)) {
                for (Path scenario : files.filter(it -> it.getFileName().toString().equals("scenario.json")).toList()) {
                    problems.addAll(owner(family, root.relativize(scenario)));
                }
            }
        }
        assertEquals(Set.of(), problems);
    }

    private static List<String> owner(String family, Path relative) throws IOException {
        List<String> problems = new ArrayList<>();
        if (relative.getNameCount() != 3) {
            problems.add(family + "/" + relative + " is not at <operator>/<stem>/scenario.json");
            return problems;
        }
        String operator = relative.getName(0).toString();
        String stem = relative.getName(1).toString();
        String className = "dev.morphia.test." + family.replace('/', '.') + ".Test" + capitalize(operator);
        String methodName = "test" + capitalize(stem);
        String where = family + "/" + operator + "/" + stem;
        Method method;
        try {
            method = testMethods(Class.forName(className)).stream().filter(it -> it.getName().equals(methodName)).findFirst()
                    .orElseThrow(NoSuchMethodException::new);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            problems.add(where + " has no test " + className + "#" + methodName);
            return problems;
        }
        String problem = CurrentTest.problem(method);
        if (problem != null) {
            problems.add(where + ": " + className + "#" + methodName + " " + problem);
        }
        BsonDocument scenario = BsonDocument.parse(Files.readString(RESOURCES.resolve("dev/morphia/test").resolve(family)
                .resolve(relative), StandardCharsets.UTF_8));
        DisplayName name = method.getAnnotation(DisplayName.class);
        String title = scenario.getString("title").getValue();
        if (name == null || !name.value().equals(title)) {
            problems.add(where + ": @DisplayName " + (name == null ? "is missing" : "\"" + name.value() + "\"")
                    + " but the scenario's title is \"" + title + "\"");
        }
        return problems;
    }

    private static Path folder(Class<?> type, Method method) {
        String name = method.getName().startsWith("test") ? method.getName().substring(4) : method.getName();
        String stem = name.isEmpty() ? name : Character.toLowerCase(name.charAt(0)) + name.substring(1);
        String operator = type.getSimpleName().replace("Test", "");
        operator = Character.toLowerCase(operator.charAt(0)) + operator.substring(1);
        return RESOURCES.resolve(type.getPackageName().replace('.', '/')).resolve(operator).resolve(stem);
    }

    private static List<Class<?>> exampleClasses() throws IOException {
        List<Class<?>> classes = new ArrayList<>();
        for (String family : FAMILIES) {
            Path root = existing(CLASSES.resolve("dev/morphia/test").resolve(family));
            try (Stream<Path> files = Files.list(root)) {
                for (Path file : files.filter(it -> it.toString().endsWith(".class") && !it.toString().contains("$")).toList()) {
                    String name = CLASSES.relativize(file).toString().replace('/', '.').replaceAll("\\.class$", "");
                    try {
                        Class<?> type = Class.forName(name);
                        if (TemplatedTestBase.class.isAssignableFrom(type) && !Modifier.isAbstract(type.getModifiers())) {
                            classes.add(type);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        return classes;
    }

    /**
     * The methods JUnit can run as tests of {@code type}: its own and those it inherits from its example-test
     * superclasses, whatever their visibility, but not the base classes' own.
     */
    private static List<Method> testMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        for (Class<?> current = type; current != null && current != TemplatedTestBase.class
                && TemplatedTestBase.class.isAssignableFrom(current); current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                // An override hides the method it overrides.
                if (seen.add(method.getName() + java.util.Arrays.toString(method.getParameterTypes()))) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    /** A missing root would let both checks pass without looking at anything. */
    private static Path existing(Path root) {
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("No such directory: " + root.toAbsolutePath());
        }
        return root;
    }

    private static Path classes() {
        try {
            return Path.of(ExampleOwnersTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
