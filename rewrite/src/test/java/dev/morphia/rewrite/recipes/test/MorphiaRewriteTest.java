package dev.morphia.rewrite.recipes.test;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaParser.Builder;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import io.github.classgraph.ClassGraph;

public abstract class MorphiaRewriteTest implements RewriteTest {
    protected List<URI> runtimeClasspath = new ClassGraph().disableNestedJarScanning().getClasspathURIs();

    @Override
    public void defaults(RecipeSpec spec) {
        Builder<? extends JavaParser, ?> builder = JavaParser.fromJavaVersion()
                .addClasspathEntry(Path.of(findMorphiaCore()));
        findMongoDependencies().stream()
                .map(Path::of)
                .forEach(builder::addClasspathEntry);
        spec.recipe(getRecipe())
                .parser(builder);
    }

    @NotNull
    protected List<String> findMongoDependencies() {
        List<String> classpath = runtimeClasspath.stream()
                .filter(uri -> uri.toString().contains("mongodb") || uri.toString().contains("bson"))
                .map(uri -> new File(uri).getAbsolutePath())
                .collect(ArrayList::new, List::add, List::addAll);
        return classpath;
    }

    protected @NotNull String findMorphiaCore() {
        var core = runtimeClasspath.stream()
                .filter(uri -> uri.toString().contains("morphia-core"))
                .map(uri -> new File(uri).getAbsolutePath())
                .findFirst().orElseThrow().toString();

        return core;
    }

    @NotNull
    protected abstract Recipe getRecipe();
}
