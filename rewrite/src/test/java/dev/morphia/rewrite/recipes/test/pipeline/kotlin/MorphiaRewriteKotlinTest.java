package dev.morphia.rewrite.recipes.test.pipeline.kotlin;

import java.nio.file.Path;
import java.util.Set;

import dev.morphia.rewrite.recipes.RewriteUtils;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.KotlinParser.Builder;
import org.openrewrite.test.RecipeSpec;

public abstract class MorphiaRewriteKotlinTest extends MorphiaRewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        Builder builder = KotlinParser.builder()
                .classpath(Set.of(RewriteUtils.findMorphiaCore()));
        findMongoDependencies().stream()
                .map(Path::of)
                .forEach(builder::addClasspathEntry);
        spec.recipe(getRecipe())
                .parser(builder);
    }
}
