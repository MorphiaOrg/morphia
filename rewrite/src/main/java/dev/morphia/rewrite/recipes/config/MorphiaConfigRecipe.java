package dev.morphia.rewrite.recipes.config;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

public class MorphiaConfigRecipe extends Recipe {
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
