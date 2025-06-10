package dev.morphia.rewrite.recipes.config;

import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MorphiaConfigMigration extends Recipe {
    private static final Logger LOG = LoggerFactory.getLogger(MorphiaConfigMigration.class);

    private static final String OLD_TYPE = "dev.morphia.mapping.MapperOptions";

    @Override
    public String getDisplayName() {
        return "Migrate MapperOptions to MorphiaConfig";
    }

    @Override
    public String getDescription() {
        return "Converts uses of dev.morphia.mapping.MapperOptions to dev.morphia.config.MorphiaConfig.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new MorphiaConfigRecipe(),
                new ChangeType("dev.morphia.mapping.MapperOptions",
                        "dev.morphia.config.MorphiaConfig", true));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MorphiaConfigMigrationVisitor();
    }

}