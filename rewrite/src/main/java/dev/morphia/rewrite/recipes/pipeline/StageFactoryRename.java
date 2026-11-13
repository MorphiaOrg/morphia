package dev.morphia.rewrite.recipes.pipeline;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeMethodName;

/**
 * An OpenRewrite recipe that renames the 2.x aggregation stage factories removed in 3.0 (e.g. {@code Group.of()},
 * {@code Match.on(..)}, {@code Out.to(..)}) to their named replacements (e.g. {@code Group.group()},
 * {@code Match.match(..)}, {@code Out.out(..)}). Each mapping is a pure rename: the replacement takes the same
 * arguments. Static imports of the old names are updated as well.
 * <p>
 * This must run after {@link PipelineRewriteRecipes}: {@link PipelineMergeRewrite} looks for {@code Merge.into(..)}
 * to find the stage it moves into {@code pipeline()}.
 */
public class StageFactoryRename extends Recipe {
    private static final String STAGES = "dev.morphia.aggregation.stages.";

    /**
     * The renames, keyed by "{@code Type oldName(..)}" relative to {@code dev.morphia.aggregation.stages}, with the
     * new method name as the value.
     */
    static final Map<String, String> RENAMES = Map.ofEntries(
            Map.entry("AddFields of(..)", "addFields"),
            Map.entry("AutoBucket of(..)", "autoBucket"),
            Map.entry("Bucket of(..)", "bucket"),
            Map.entry("CollectionStats with(..)", "collStats"),
            Map.entry("CurrentOp of(..)", "currentOp"),
            Map.entry("Facet of(..)", "facet"),
            Map.entry("GeoNear to(..)", "geoNear"),
            Map.entry("GraphLookup from(..)", "graphLookup"),
            Map.entry("Group of(..)", "group"),
            Map.entry("IndexStats of(..)", "indexStats"),
            Map.entry("Limit of(..)", "limit"),
            Map.entry("Lookup from(..)", "lookup"),
            Map.entry("Match on(..)", "match"),
            Map.entry("Merge into(..)", "merge"),
            Map.entry("Out to(..)", "out"),
            Map.entry("PlanCacheStats of(..)", "planCacheStats"),
            Map.entry("Projection of(..)", "project"),
            Map.entry("Redact on(..)", "redact"),
            Map.entry("ReplaceRoot with(..)", "replaceRoot"),
            Map.entry("ReplaceWith with(..)", "replaceWith"),
            Map.entry("Sample of(..)", "sample"),
            Map.entry("Skip of(..)", "skip"),
            Map.entry("Sort on(..)", "sort"),
            Map.entry("SortByCount on(..)", "sortByCount"),
            Map.entry("Unset fields(..)", "unset"),
            Map.entry("Unwind on(..)", "unwind"));

    /** Creates a new instance. */
    public StageFactoryRename() {
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Aggregation stage factory rename";
    }

    @Override
    public @NotNull String getDescription() {
        return "Renames the 2.x aggregation stage factories removed in 3.0 (e.g. `Group.of()`, `Match.on(..)`, `Out.to(..)`) "
                + "to their named replacements (e.g. `Group.group()`, `Match.match(..)`, `Out.out(..)`).";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return RENAMES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> (Recipe) new ChangeMethodName(STAGES + e.getKey(), e.getValue(), false, true))
                .toList();
    }
}
