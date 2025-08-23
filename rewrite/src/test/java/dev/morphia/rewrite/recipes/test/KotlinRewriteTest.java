package dev.morphia.rewrite.recipes.test;

import org.openrewrite.test.RecipeSpec;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaDependencies;
import static org.openrewrite.kotlin.KotlinParser.*;

public abstract class KotlinRewriteTest extends MorphiaRewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(getRecipe())
                .parser(builder()
                        .classpath(findMorphiaDependencies()));
    }

}
