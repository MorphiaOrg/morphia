package dev.morphia.rewrite.recipes.test;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaParser.Builder;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaDependencies;

public abstract class MorphiaRewriteTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        Builder<? extends JavaParser, ?> builder = JavaParser.fromJavaVersion()
                .classpath(findMorphiaDependencies());
        spec.recipe(getRecipe())
                .parser(builder);
    }

    @NotNull
    protected abstract Recipe getRecipe();
}
