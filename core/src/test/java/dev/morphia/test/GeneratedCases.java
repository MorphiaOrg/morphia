package dev.morphia.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.platform.commons.annotation.Testable;

/**
 * The manifest of the generated operator-case namespace ({@code dev.morphia.test.generated} and
 * {@code generated/fixtures}). It lists every case and every file the namespace owns, so that a deleted or unlisted
 * test, scenario or file is caught offline, in both directions. Every file under the generated source and resource
 * roots must be listed, except the manifest itself; the runner's self-test scenarios
 * ({@code generated/fixtures/runner/}) and the golden vectors are listed as protected files, not cases.
 */
public final class GeneratedCases {
    public static final String MANIFEST = "/dev/morphia/test/generated/manifest.json";
    private static final String SOURCES = "src/test/java/dev/morphia/test/generated";
    private static final String RESOURCES = "src/test/resources/dev/morphia/test/generated";
    private static final String FIXTURES = RESOURCES + "/fixtures";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    private GeneratedCases() {
    }

    /**
     * @param schemaVersion the manifest schema
     * @param cases         every case the namespace runs
     * @param files         every file the namespace owns, with its SHA-256
     */
    public record Manifest(int schemaVersion, List<Case> cases, List<OwnedFile> files) {
    }

    /**
     * @param name     the qualified example name
     * @param kind     how the case was made: {@code MIGRATED} (a legacy case moved verbatim) or {@code GENERATED}
     * @param scenario the absolute classpath resource of its scenario
     * @param test     the owning test, as {@code <type>#<method>}
     */
    public record Case(String name, String kind, String scenario, String test) {
    }

    /**
     * @param path   the path relative to the core module
     * @param sha256 its digest
     * @param owner  {@code GENERATED} (rewritten by generation) or {@code PROTECTED} (reviewed source that generation must
     *               not touch)
     */
    public record OwnedFile(String path, String sha256, String owner) {
    }

    public static Manifest manifest() {
        try (InputStream stream = GeneratedCases.class.getResourceAsStream(MANIFEST)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + MANIFEST);
            }
            return MAPPER.readValue(stream, Manifest.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + MANIFEST, e);
        }
    }

    /** Checks the namespace against the manifest in both directions. */
    public static void verifyAll() {
        verify(manifest(), TemplatedTestBase.rootToCore(""));
    }

    static void verify(Manifest manifest, File core) {
        List<String> problems = new ArrayList<>();
        if (manifest.schemaVersion() != 1) {
            problems.add("Unsupported manifest schema " + manifest.schemaVersion());
        }
        Set<String> names = new HashSet<>();
        Set<String> tests = new HashSet<>();
        Set<String> scenarios = new HashSet<>();
        for (Case each : manifest.cases()) {
            if (!names.add(each.name())) {
                problems.add("Case listed more than once: " + each.name());
            }
            if (!Set.of("MIGRATED", "GENERATED").contains(each.kind())) {
                problems.add(each.name() + ": unknown kind " + each.kind());
            }
            tests.add(each.test());
            scenarios.add(each.scenario());
            String[] parts = each.test().split("#", 2);
            try {
                Class<?> type = Class.forName(parts[0]);
                if (parts.length != 2 || !isTest(type, parts[1])) {
                    problems.add(each.name() + ": no @Test " + each.test());
                }
            } catch (ClassNotFoundException e) {
                problems.add(each.name() + ": missing test class " + parts[0]);
            }
            try {
                String declared = OperatorCase.of(each.scenario()).scenario().getString("name").getValue();
                if (!declared.equals(each.name())) {
                    problems.add(each.name() + ": its scenario is named " + declared);
                }
            } catch (RuntimeException e) {
                problems.add(each.name() + ": " + e.getMessage());
            }
        }
        Set<String> owned = new TreeSet<>();
        for (OwnedFile file : manifest.files()) {
            owned.add(file.path());
            if (!Set.of("GENERATED", "PROTECTED").contains(file.owner())) {
                problems.add(file.path() + ": unknown owner " + file.owner());
            }
            Path path = core.toPath().resolve(file.path());
            if (!Files.isRegularFile(path)) {
                problems.add("Missing owned file " + file.path());
            } else if (!sha256(path).equals(file.sha256())) {
                problems.add("Owned file changed: " + file.path());
            }
        }
        for (String scenario : scenarios) {
            if (!owned.contains("src/test/resources" + scenario)) {
                problems.add("Scenario is not an owned file: " + scenario);
            }
        }
        walk(core, SOURCES).forEach(path -> {
            if (!owned.contains(path)) {
                problems.add("Unlisted generated source " + path);
            }
            if (!path.endsWith(".java")) {
                return;
            }
            String type = path.substring("src/test/java/".length(), path.length() - ".java".length()).replace('/', '.');
            try {
                Class<?> loaded = Class.forName(type);
                for (Class<?> nested : loaded.getDeclaredClasses()) {
                    problems.add("Nested class in generated source: " + nested.getName());
                }
                for (String method : testMethods(loaded)) {
                    if (!tests.contains(type + "#" + method)) {
                        problems.add("Test is not a listed case: " + type + "#" + method);
                    }
                }
            } catch (ClassNotFoundException e) {
                problems.add("Uncompiled generated source " + type);
            }
        });
        walk(core, RESOURCES).filter(path -> !path.equals("src/test/resources" + MANIFEST)).forEach(path -> {
            if (!owned.contains(path)) {
                problems.add("Unlisted generated resource " + path);
            }
            if (path.startsWith(FIXTURES + "/") && !path.startsWith(FIXTURES + "/runner/")
                    && !scenarios.contains(path.substring("src/test/resources".length()))) {
                problems.add("Scenario is not a listed case: " + path);
            }
        });
        if (!problems.isEmpty()) {
            throw new IllegalStateException(String.join("\n", problems));
        }
    }

    /** Any JUnit test kind: {@code @Test}, {@code @ParameterizedTest}, {@code @RepeatedTest}, factories, templates. */
    private static boolean isTestable(Method method) {
        return Arrays.stream(method.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().isAnnotationPresent(Testable.class));
    }

    private static boolean isTest(Class<?> type, String name) {
        return testMethods(type).contains(name);
    }

    /**
     * The names of every test method JUnit would run on [type]: declared at any visibility, inherited, and of any test
     * kind. A method is taken from the most specific class that declares it, so an override without a test annotation
     * hides an inherited test, as in JUnit.
     */
    private static Set<String> testMethods(Class<?> type) {
        Set<String> seen = new HashSet<>();
        Set<String> tests = new TreeSet<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                String signature = method.getName() + Arrays.toString(method.getParameterTypes());
                if (!method.isSynthetic() && seen.add(signature) && isTestable(method)) {
                    tests.add(method.getName());
                }
            }
        }
        return tests;
    }

    private static Stream<String> walk(File core, String root) {
        Path base = core.toPath();
        Path start = base.resolve(root);
        if (!Files.isDirectory(start)) {
            return Stream.empty();
        }
        try (Stream<Path> paths = Files.walk(start)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> base.relativize(path).toString().replace(File.separatorChar, '/'))
                    .sorted()
                    .toList()
                    .stream();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256(Path path) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
