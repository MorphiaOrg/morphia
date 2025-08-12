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

public class RewriteUtils {
    private static List<URI> runtimeClasspath = new ClassGraph().disableNestedJarScanning().getClasspathURIs();

    private static LinkedHashSet<Path> dependencies = new LinkedHashSet<Path>();

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

    @NotNull
    protected static List<String> findMongoDependencies() {
        return runtimeClasspath.stream()
                .filter(uri -> uri.toString().contains("mongodb")
                        || uri.toString().contains("bson"))
                .map(uri -> new File(uri).getAbsolutePath())
                .collect(ArrayList::new, List::add, List::addAll);
    }

    public static @NotNull MethodMatcher methodMatcher(String type, String pattern) {
        return new MethodMatcher(type + " " + pattern);
    }
}
