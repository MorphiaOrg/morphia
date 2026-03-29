package dev.morphia.rewrite.recipes.config;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

/**
 * An OpenRewrite recipe that rewrites {@code MapperOptions.Builder} usages to the {@code MorphiaConfig} API.
 */
public class MorphiaConfigRecipe extends Recipe {
    /** Creates a new instance. */
    public MorphiaConfigRecipe() {
    }

    @Override
    public @DisplayName String getDisplayName() {
        return "Migrate MapperOptions Builder to MorphiaConfig";
    }

    @Override
    public @Description String getDescription() {
        return "Converts uses of dev.morphia.mapping.MapperOptions.Builder to dev.morphia.config.MorphiaConfig.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MorphiaConfigMigrationVisitor();
    }
}
