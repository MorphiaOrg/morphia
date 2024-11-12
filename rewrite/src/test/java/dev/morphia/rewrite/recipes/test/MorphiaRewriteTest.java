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
    protected List<URI> runtimeClasspath = new ClassGraph().disableNestedJarScanning().getClasspathURIs();

    public String[] classpath() {
        List<String> classpath = findMongoArtifacts();

        classpath.add(findMorphiaCore());
        return classpath.toArray(new String[0]);
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(getRecipe())
                .parser(JavaParser.fromJavaVersion()
                        .classpath(classpath()));
    }

    @NotNull
    protected List<String> findMongoArtifacts() {
        List<String> classpath = runtimeClasspath.stream()
                .filter(uri -> uri.toString().contains("mongodb") || uri.toString().contains("bson"))
                .map(uri -> new File(uri).getAbsolutePath()/* .getName().replaceAll("-[0-9].*", "") */)
                .collect(ArrayList::new, List::add, List::addAll);
        return classpath;
    }

    @NotNull
    protected abstract String findMorphiaCore();

    @NotNull
    protected abstract Recipe getRecipe();
}
