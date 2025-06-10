package dev.morphia.rewrite.recipes.datastore;

import java.util.List;

import dev.morphia.rewrite.recipes.config.MorphiaConfigMigration;

import org.openrewrite.Recipe;

public class CreateDatastoreMigration extends Recipe {
    @Override
    public String getDisplayName() {
        return "Update calls to createDatastore";
    }

    @Override
    public String getDescription() {
        return "Migrates calls to createDatastore to the proper form if necessary.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new MorphiaConfigMigration(),
                new CreateDatastoreRecipe());
    }

}