package dev.morphia.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static java.lang.Character.toLowerCase;
import static java.util.Arrays.stream;

/**
 * The accepted method/fixture ownership for the operator example tests. Coverage and resource lookup use these explicit
 * bindings instead of inferring them from method names, so a renamed test cannot silently stop being checked and a manual
 * test cannot load a fixture by accident. The descriptor is produced and validated by the audits module.
 */
public final class ExampleBindings {
    public static final String DESCRIPTOR = "/dev/morphia/test/example-bindings.json";

    /**
     * The operator packages under binding control. Pinned here so dropping a package from the descriptor cannot switch its
     * checks off.
     */
    public static final Set<String> PACKAGES = Set.of(
            "dev.morphia.test.aggregation.expressions",
            "dev.morphia.test.aggregation.stages",
            "dev.morphia.test.query.filters",
            "dev.morphia.test.query.updates");

    /**
     * Files an unbound, marker-only fixture directory may hold; anything executable needs a bound test.
     */
    private static final Set<String> MARKERS = Set.of("name", "lock", "lockd");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private static Index index;

    private ExampleBindings() {
    }

    public record Descriptor(int schemaVersion, List<String> packages, List<TestClass> classes) {
    }

    public record TestClass(String type, String resources, List<Binding> bindings, List<Manual> manual,
            List<Unbound> unbound) {
    }

    public record Binding(String method, String resource) {
    }

    public record Manual(String method, String reason) {
    }

    public record Unbound(String resource, String reason) {
    }

    record Index(Set<String> packages, Map<String, TestClass> classes) {
    }

    public static synchronized Descriptor descriptor() {
        try (InputStream stream = ExampleBindings.class.getResourceAsStream(DESCRIPTOR)) {
            if (stream == null) {
                throw new IllegalStateException("Missing example binding descriptor " + DESCRIPTOR);
            }
            return MAPPER.readValue(stream, Descriptor.class);
        } catch (IOException e) {
            throw new IllegalStateException("Invalid example binding descriptor " + DESCRIPTOR, e);
        }
    }

    private static synchronized Index index() {
        if (index == null) {
            index = validate(descriptor());
        }
        return index;
    }

    static Index validate(Descriptor descriptor) {
        require(descriptor.schemaVersion() == 1, "Unsupported example binding schema " + descriptor.schemaVersion());
        require(descriptor.packages() != null && !descriptor.packages().isEmpty(), "No example binding packages");
        require(descriptor.classes() != null && !descriptor.classes().isEmpty(), "No example binding classes");
        var packages = new HashSet<String>();
        descriptor.packages().forEach(name -> require(name != null && packages.add(name), "Duplicate package " + name));
        require(packages.equals(PACKAGES), "Example binding packages must be exactly " + new TreeSet<>(PACKAGES));
        var classes = new LinkedHashMap<String, TestClass>();
        for (TestClass type : descriptor.classes()) {
            require(type != null && type.type() != null && type.resources() != null && type.bindings() != null
                    && type.manual() != null && type.unbound() != null, "Incomplete binding entry " + type);
            require(classes.put(type.type(), type) == null, "Duplicate binding class " + type.type());
            int dot = type.type().lastIndexOf('.');
            require(dot > 0 && packages.contains(type.type().substring(0, dot)),
                    "Binding class outside the declared packages: " + type.type());
            require(type.resources().equals(resources(type.type())),
                    "Resource directory %s does not match %s".formatted(type.resources(), type.type()));
            var methods = new HashSet<String>();
            var fixtures = new HashSet<String>();
            type.bindings().forEach(binding -> {
                require(binding != null && binding.method() != null && binding.resource() != null,
                        "Incomplete binding in " + type.type());
                require(methods.add(binding.method()), "Duplicate method owner %s#%s".formatted(type.type(), binding.method()));
                require(fixtures.add(binding.resource().toLowerCase(Locale.ROOT)),
                        "Duplicate fixture owner %s/%s".formatted(type.resources(), binding.resource()));
                require(isLeaf(binding.resource()), "Invalid fixture name " + binding.resource());
            });
            type.manual().forEach(manual -> {
                require(manual != null && manual.method() != null && hasText(manual.reason()),
                        "Manual methods need a reason in " + type.type());
                require(methods.add(manual.method()), "Duplicate method owner %s#%s".formatted(type.type(), manual.method()));
            });
            type.unbound().forEach(unbound -> {
                require(unbound != null && unbound.resource() != null && hasText(unbound.reason()),
                        "Unbound fixtures need a reason in " + type.type());
                require(isLeaf(unbound.resource()), "Invalid fixture name " + unbound.resource());
                require(fixtures.add(unbound.resource().toLowerCase(Locale.ROOT)),
                        "Duplicate fixture owner %s/%s".formatted(type.resources(), unbound.resource()));
            });
        }
        var folders = new HashSet<String>();
        classes.values().forEach(type -> require(folders.add(type.resources().toLowerCase(Locale.ROOT)),
                "Duplicate resource directory owner " + type.resources()));
        return new Index(Set.copyOf(packages), Map.copyOf(classes));
    }

    /**
     * @return the accepted entry for an in-scope test class, or null for templated tests outside the operator packages
     */
    public static TestClass lookup(Class<?> type) {
        var index = index();
        TestClass entry = index.classes().get(type.getName());
        require(entry != null || !index.packages().contains(type.getPackageName()),
                "No example bindings for " + type.getName());
        return entry;
    }

    /**
     * @return the fixture explicitly bound to this test method, or null when its class is outside the binding scope
     */
    public static String resource(Method method) {
        TestClass entry = lookup(method.getDeclaringClass());
        if (entry == null) {
            return null;
        }
        return entry.bindings().stream()
                .filter(binding -> binding.method().equals(method.getName()))
                .map(Binding::resource)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "%s#%s does not own an example fixture".formatted(method.getDeclaringClass().getName(), method.getName())));
    }

    /**
     * Checks the class's declared tests and physical fixture directories against its bindings in both directions.
     */
    public static void verify(Class<?> type) {
        TestClass entry = lookup(type);
        if (entry != null) {
            verify(type, entry);
        }
    }

    private static void verify(Class<?> type, TestClass entry) {
        require(type.getSuperclass() == TemplatedTestBase.class,
                type.getName() + " must extend TemplatedTestBase directly so test ownership is explicit");
        var declared = new TreeSet<String>();
        stream(type.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Test.class))
                .forEach(method -> declared.add(method.getName()));
        var owned = new TreeSet<String>();
        entry.bindings().forEach(binding -> owned.add(binding.method()));
        entry.manual().forEach(manual -> owned.add(manual.method()));
        compare("test methods of " + type.getName(), owned, declared);
        entry.bindings().forEach(binding -> {
            String derived = derivedResource(binding.method());
            require(binding.resource().equals(derived),
                    "%s#%s must round-trip to fixture %s, not %s".formatted(type.getName(), binding.method(), derived,
                            binding.resource()));
        });

        var expected = new TreeSet<String>();
        entry.bindings().forEach(binding -> expected.add(binding.resource()));
        entry.unbound().forEach(unbound -> expected.add(unbound.resource()));
        File folder = TemplatedTestBase.rootToCore("src/test/resources/" + entry.resources());
        require(folder.isDirectory(), "Missing fixture directory " + folder);
        var physical = new TreeSet<String>();
        stream(Objects.requireNonNull(folder.listFiles()))
                .filter(File::isDirectory)
                .forEach(file -> physical.add(file.getName()));
        compare("fixtures of " + type.getName(), expected, physical);
        entry.unbound().forEach(unbound -> {
            var files = Objects.requireNonNull(new File(folder, unbound.resource()).list());
            require(MARKERS.containsAll(List.of(files)),
                    "Unbound fixture %s/%s holds more than markers: %s".formatted(entry.resources(), unbound.resource(),
                            new TreeSet<>(List.of(files))));
        });
    }

    /**
     * Checks every in-scope class and fixture directory, including classes and operators missing from the descriptor.
     */
    public static void verifyAll() {
        var index = index();
        var problems = new ArrayList<String>();
        index.classes().values().forEach(entry -> {
            try {
                verify(Class.forName(entry.type()), entry);
            } catch (ClassNotFoundException e) {
                problems.add("Missing bound test class " + entry.type());
            } catch (IllegalStateException e) {
                problems.add(e.getMessage());
            }
        });
        var owners = new HashMap<String, TestClass>();
        index.classes().values().forEach(entry -> owners.put(entry.resources(), entry));
        for (String name : new TreeSet<>(index.packages())) {
            String path = name.replace('.', '/');
            File sources = TemplatedTestBase.rootToCore("src/test/java/" + path);
            stream(Objects.requireNonNull(sources.listFiles(), "Missing test sources " + sources))
                    .map(File::getName)
                    .filter(file -> file.endsWith(".java"))
                    .sorted()
                    .forEach(file -> {
                        String type = name + "." + file.substring(0, file.length() - 5);
                        try {
                            Class<?> loaded = Class.forName(type);
                            if (TemplatedTestBase.class.isAssignableFrom(loaded)
                                    && !Modifier.isAbstract(loaded.getModifiers())
                                    && !index.classes().containsKey(type)) {
                                problems.add("No example bindings for " + type);
                            }
                        } catch (ClassNotFoundException e) {
                            problems.add("Uncompiled test source " + type);
                        }
                    });
            File resources = TemplatedTestBase.rootToCore("src/test/resources/" + path);
            stream(Objects.requireNonNull(resources.listFiles(), "Missing test resources " + resources))
                    .filter(File::isDirectory)
                    .sorted()
                    .forEach(operator -> {
                        String folder = path + "/" + operator.getName();
                        if (owners.containsKey(folder)) {
                            return;
                        }
                        if (!new File(operator, "ignored").isFile()) {
                            problems.add("No test class owns " + folder);
                        }
                        stream(Objects.requireNonNull(operator.listFiles()))
                                .filter(File::isDirectory)
                                .forEach(example -> problems.add("No test owns fixture " + folder + "/" + example.getName()));
                    });
        }
        require(problems.isEmpty(), String.join("\n", problems));
    }

    static String resources(String type) {
        int dot = type.lastIndexOf('.');
        String simple = type.substring(dot + 1).replace("Test", "");
        return type.substring(0, dot).replace('.', '/') + "/" + toLowerCase(simple.charAt(0)) + simple.substring(1);
    }

    static String derivedResource(String method) {
        require(method.startsWith("test") && method.length() > 4, "Fixture-owning methods must start with test: " + method);
        String name = method.substring(4);
        return name.substring(0, 1).toLowerCase(Locale.ROOT) + name.substring(1);
    }

    private static void compare(String what, Set<String> expected, Set<String> actual) {
        var missing = new TreeSet<>(expected);
        missing.removeAll(actual);
        var unexpected = new TreeSet<>(actual);
        unexpected.removeAll(expected);
        require(missing.isEmpty() && unexpected.isEmpty(),
                "Bindings for %s differ: missing %s, unbound %s".formatted(what, missing, unexpected));
    }

    private static boolean isLeaf(String name) {
        return !name.isEmpty() && name.indexOf('/') < 0 && name.indexOf('\\') < 0 && !name.equals(".") && !name.equals("..");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
