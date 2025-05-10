package dev.morphia.rewrite.recipes;

import java.util.List;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.Count;
import dev.morphia.aggregation.stages.Limit;
import dev.morphia.aggregation.stages.Match;
import dev.morphia.aggregation.stages.Skip;
import dev.morphia.aggregation.stages.SortByCount;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.aggregation.stages.UnionWith;
import dev.morphia.query.filters.Filter;
import dev.morphia.rewrite.recipes.pipeline.PipelineArgWrap;
import dev.morphia.rewrite.recipes.pipeline.PipelineRollup;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;

public class PipelineRewriteRecipes extends Recipe {
    static final String AGGREGATION = "dev.morphia.aggregation.Aggregation";

    private static final String STAGE = Stage.class.getName();

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new PipelineArgWrap(Count.class, "count", "String"),
                new PipelineArgWrap(Limit.class, "limit", "long"),
                new PipelineArgWrap(Match.class, "match", "dev.morphia.query.filters.Filter[]", Filter.class),
                new PipelineArgWrap(Skip.class, "skip", "long"),
                new PipelineArgWrap(SortByCount.class, "sortByCount", Expression.class.getName()),
                new PipelineArgWrap(UnionWith.class, "unionWith",
                        "Class,dev.morphia.aggregation.stages.Stage,dev.morphia.aggregation.stages.Stage[]"),
                new PipelineArgWrap(UnionWith.class, "unionWith",
                        "String,dev.morphia.aggregation.stages.Stage,dev.morphia.aggregation.stages.Stage[]"),
                new PipelineRollup());
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
