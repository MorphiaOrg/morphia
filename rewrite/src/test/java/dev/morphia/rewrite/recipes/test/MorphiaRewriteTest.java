package dev.morphia.rewrite.recipes.test;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaDependencies;
import static org.openrewrite.java.JavaParser.*;

public abstract class MorphiaRewriteTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(getRecipe())
                .parser(fromJavaVersion()
                        .classpath(findMorphiaDependencies()));
    }

    @NotNull
    protected abstract Recipe getRecipe();
}
