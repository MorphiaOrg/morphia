package dev.morphia.rewrite.recipes;

import org.openrewrite.Recipe;

public class PipelineRewrite extends Recipe {
    @Override
    public String getDisplayName() {
        return "Aggregation pipeline rewrite";
    }

    @Override
    public String getDescription() {
        return "Rewrites an aggregation from using stage-named methods to using pipeline(Stage...)";
    }
}
