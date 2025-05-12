package dev.morphia.rewrite.recipes;

import java.util.List;

import dev.morphia.rewrite.recipes.pipeline.PipelineExecuteRewrite;
import dev.morphia.rewrite.recipes.pipeline.PipelineRewrite;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;

public class PipelineRewriteRecipes extends Recipe {
    public static final String AGGREGATION = "dev.morphia.aggregation.Aggregation";

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new PipelineExecuteRewrite(),
                new PipelineRewrite());
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Aggregation pipeline rewrite recipes";
    }

    @Override
    public @NotNull String getDescription() {
        return "Rewrites an aggregation from using stage-named methods to using pipeline(Stage...).";
    }

}
