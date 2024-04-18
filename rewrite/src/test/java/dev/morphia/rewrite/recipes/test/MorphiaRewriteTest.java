package dev.morphia.rewrite.recipes.test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import io.github.classgraph.ClassGraph;

public abstract class MorphiaRewriteTest implements RewriteTest {
    public static final String ARTIFACT;

    public static final List<String> classpath;

    static {
        List<URI> runtimeClasspath = new ClassGraph().disableNestedJarScanning().getClasspathURIs();
        classpath = runtimeClasspath.stream()
                .filter(uri -> {
                    String string = uri.toString();
                    return string.contains("mongodb") || string.contains("bson");
                })
                .map(uri -> {
                    return new File(uri).getName().replaceAll("-[0-9].*", "");
                })
                .collect(ArrayList::new, List::add, List::addAll);
        var core = runtimeClasspath.stream()
                .filter(uri -> {
                    String string = uri.toString();
                    return string.contains("morphia") && string.contains("core");
                })
                .findFirst().orElseThrow().toString();
        if (core.contains("morphia-core")) {
            ARTIFACT = "morphia-core";
        } else {
            ARTIFACT = "morphia/core";
        }

        classpath.add(ARTIFACT);
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(getRecipe())
                .parser(JavaParser.fromJavaVersion()
                        .classpath(classpath.toArray(new String[0])));
    }

    @NotNull
    protected abstract Recipe getRecipe();
}
