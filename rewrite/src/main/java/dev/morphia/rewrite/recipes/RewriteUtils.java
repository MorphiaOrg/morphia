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
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.KotlinTemplate;
import org.openrewrite.kotlin.tree.K;
import org.semver4j.Semver;

import io.github.classgraph.ClassGraph;

import static org.openrewrite.java.JavaParser.fromJavaVersion;

/**
 * Utility methods for OpenRewrite recipes used in the Morphia migration tooling.
 */
public class RewriteUtils {
    /** @hidden */
    private RewriteUtils() {
    }

    private static List<URI> runtimeClasspath = new ClassGraph().enableClasspath().disableNestedJarScanning().getClasspathURIs();

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

    /**
     * Creates a matcher for the given method pattern on {@code Datastore}, the 2.x {@code DatastoreImpl}, and
     * {@code MorphiaDatastore} (which {@code DatastoreImpl} is renamed to earlier in the upgrade).
     *
     * @param pattern the method signature pattern
     * @return the matcher
     */
    public static @NotNull MethodMatcher datastoreMethod(String pattern) {
        return new MultiMethodMatcher(
                methodMatcher("dev.morphia.Datastore", pattern),
                methodMatcher("dev.morphia.DatastoreImpl", pattern),
                methodMatcher("dev.morphia.MorphiaDatastore", pattern));
    }

    /**
     * Checks whether the type is one of the named types, ignoring any type parameters.
     *
     * @param type  the type to check
     * @param names the fully-qualified type names to check against
     * @return true if the type matches one of the names
     */
    public static boolean isOfType(JavaType type, String... names) {
        return Arrays.stream(names).anyMatch(name -> TypeUtils.isOfClassType(type, name));
    }

    /**
     * Checks whether evaluating the expression can have no side effects: a literal, a plain name, or a field access on
     * one of those. Moving such an expression relative to other code doesn't change what the code does on its own,
     * though its value can still depend on side effects elsewhere.
     *
     * @param expression the expression to check
     * @return true if evaluating the expression has no side effects
     */
    public static boolean isSideEffectFree(Expression expression) {
        return expression instanceof J.Literal || expression instanceof J.Identifier
                || expression instanceof J.FieldAccess fieldAccess && isSideEffectFree(fieldAccess.getTarget());
    }

    /**
     * Checks whether the cursor is in a Kotlin source file.
     *
     * @param cursor the cursor
     * @return true if the enclosing source file is Kotlin
     */
    public static boolean isKotlin(Cursor cursor) {
        return cursor.firstEnclosing(SourceFile.class) instanceof K.CompilationUnit;
    }

    /**
     * Builds a template against the 3.x classpath, using the Kotlin variant of the code in Kotlin sources.
     *
     * @param cursor     the cursor where the template will be applied
     * @param kotlinCode the template code for Kotlin sources
     * @param javaCode   the template code for Java sources
     * @param imports    the fully-qualified names of the types the template code refers to
     * @return the template
     */
    public static @NotNull JavaTemplate template(Cursor cursor, String kotlinCode, String javaCode, String... imports) {
        JavaTemplate.Builder builder = isKotlin(cursor)
                ? KotlinTemplate.builder(kotlinCode)
                        .parser(KotlinParser.builder()
                                .classpath(runtimeClasspathPaths()))
                : JavaTemplate.builder(javaCode)
                        .javaParser(fromJavaVersion()
                                .classpath(runtimeClasspathPaths()));
        return builder
                .imports(imports)
                .build();
    }
}
