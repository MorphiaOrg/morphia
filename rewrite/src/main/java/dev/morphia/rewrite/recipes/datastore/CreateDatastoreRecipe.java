package dev.morphia.rewrite.recipes.datastore;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

class CreateDatastoreRecipe extends Recipe {
    @Override
    public @DisplayName String getDisplayName() {
        return "Update calls to createDatastore";
    }

    @Override
    public @Description String getDescription() {
        return "Migrates calls to createDatastore to the proper form if necessary.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new CreateDatastoreMigrationVisitor();
    }
}
