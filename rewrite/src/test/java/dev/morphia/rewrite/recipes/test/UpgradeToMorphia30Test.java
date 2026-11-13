package dev.morphia.rewrite.recipes.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;

import static org.openrewrite.java.Assertions.java;

/**
 * Runs the declarative {@code dev.morphia.UpgradeToMorphia30} recipe from rewrite.yml.
 */
public class UpgradeToMorphia30Test extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return Environment.builder()
                .scanRuntimeClasspath("dev.morphia")
                .build()
                .activateRecipes("dev.morphia.UpgradeToMorphia30");
    }

    @Test
    void removeEnsureCapsOnDatastore() {
        rewriteRun(
                //language=java
                java(
                        """
                                import dev.morphia.Datastore;

                                public class CapsTest {
                                    public void update(Datastore datastore) {
                                        datastore.ensureCaps();
                                        datastore.ensureIndexes();
                                    }
                                }
                                """,
                        """
                                import dev.morphia.Datastore;

                                public class CapsTest {
                                    public void update(Datastore datastore) {
                                    }
                                }
                                """));
    }

    // MorphiaDatastore is 3.0-only, so this exercises the MorphiaDatastore pattern after DatastoreImpl is renamed
    @Test
    void removeEnsureCapsOnDatastoreImpl() {
        rewriteRun(
                //language=java
                java(
                        """
                                import dev.morphia.DatastoreImpl;

                                public class CapsTest {
                                    public void update(DatastoreImpl datastore) {
                                        datastore.ensureCaps();
                                    }
                                }
                                """,
                        """
                                import dev.morphia.MorphiaDatastore;

                                public class CapsTest {
                                    public void update(MorphiaDatastore datastore) {
                                    }
                                }
                                """));
    }
}
