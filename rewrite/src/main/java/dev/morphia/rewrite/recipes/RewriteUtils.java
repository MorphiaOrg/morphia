package dev.morphia.rewrite.recipes;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.java.MethodMatcher;
import org.semver4j.Semver;

import io.github.classgraph.ClassGraph;

/**
 * Utility methods for OpenRewrite recipes used in the Morphia migration tooling.
 */
public class RewriteUtils {
    /** @hidden */
    private RewriteUtils() {
    }

    private static List<URI> runtimeClasspath = new ClassGraph().disableNestedJarScanning().getClasspathURIs();

    private static LinkedHashSet<Path> dependencies = new LinkedHashSet<Path>();

    /**
     * Locates the most recent pre-3.0 morphia-core JAR from the local Maven repository along with MongoDB driver dependencies.
     *
     * @return the resolved set of dependency paths
     */
    public static Set<Path> findMorphiaDependencies() {
        if (dependencies.isEmpty()) {
            var repo = new File(System.getProperty("user.home"), ".m2/repository/dev/morphia/morphia/morphia-core");
            Path morphiaCore = Arrays.stream(repo.listFiles((dir, name) -> name.matches("\\d+\\.\\d+\\.\\d+.*")))
                    .map(File::getName)
                    .map(Semver::parse)
                    .sorted(Comparator.reverseOrder())
                    .filter(v -> v.isLowerThan("3.0.0-SNAPSHOT"))
                    .map(v -> new File(repo, "%s/morphia-core-%s.jar".formatted(v, v)))
                    .filter(File::exists)
                    .map(File::toPath)
                    .findFirst().orElseThrow();

            dependencies.add(morphiaCore);
            dependencies.addAll(findMongoDependencies().stream()
                    .map(Path::of)
                    .toList());
        }

        return dependencies;
    }

    /**
     * Returns the absolute paths of MongoDB and BSON library JARs found on the runtime classpath.
     *
     * @return a list of absolute path strings for MongoDB-related dependencies
     */
    @NotNull
    protected static List<String> findMongoDependencies() {
        return runtimeClasspath.stream()
                .filter(uri -> uri.toString().contains("mongodb")
                        || uri.toString().contains("bson"))
                .map(uri -> new File(uri).getAbsolutePath())
                .collect(ArrayList::new, List::add, List::addAll);
    }

    /**
     * Creates a {@link MethodMatcher} for the given fully-qualified type and method pattern.
     *
     * @param type    the fully-qualified type name
     * @param pattern the method signature pattern
     * @return a MethodMatcher for the combined pattern
     */
    public static @NotNull MethodMatcher methodMatcher(String type, String pattern) {
        return new MethodMatcher(type + " " + pattern);
    }

    /**
     * Returns the runtime classpath as paths, suitable for templates generating 3.x API code.
     * This includes the current project's classes (3.x morphia-core) unlike findMorphiaDependencies()
     * which only includes 2.x versions.
     *
     * @return the runtime classpath as a set of paths
     */
    public static Set<Path> runtimeClasspathPaths() {
        return runtimeClasspath.stream()
                .map(uri -> Path.of(uri))
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }
}
