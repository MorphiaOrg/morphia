package dev.morphia.rewrite.recipes.test;

import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaDependencies;

public abstract class KotlinRewriteTest extends MorphiaRewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        var builder = KotlinParser.builder()
                .classpath(findMorphiaDependencies());
        spec.recipe(getRecipe())
                .parser(builder);
    }

}
