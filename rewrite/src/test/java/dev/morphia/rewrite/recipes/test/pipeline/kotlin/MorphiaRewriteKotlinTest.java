package dev.morphia.rewrite.recipes.test.pipeline.kotlin;

import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.KotlinParser.Builder;
import org.openrewrite.test.RecipeSpec;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaDependencies;

public abstract class MorphiaRewriteKotlinTest extends MorphiaRewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        Builder builder = KotlinParser.builder()
                .classpath(findMorphiaDependencies());
        spec.recipe(getRecipe())
                .parser(builder);
    }
}
