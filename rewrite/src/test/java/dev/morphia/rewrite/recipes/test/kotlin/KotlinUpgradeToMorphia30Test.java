package dev.morphia.rewrite.recipes.test.kotlin;

import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;

import static org.openrewrite.kotlin.Assertions.kotlin;

/**
 * Runs the declarative {@code dev.morphia.UpgradeToMorphia30} recipe from rewrite.yml.
 */
public class KotlinUpgradeToMorphia30Test extends KotlinRewriteTest {
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
                //language=kotlin
                kotlin(
                        """
                                import dev.morphia.Datastore

                                class CapsTest {
                                    fun update(datastore: Datastore) {
                                        datastore.ensureCaps()
                                        datastore.ensureIndexes()
                                    }
                                }
                                """,
                        """
                                import dev.morphia.Datastore

                                class CapsTest {
                                    fun update(datastore: Datastore) {
                                    }
                                }
                                """));
    }

    @Test
    void removeEnsureCapsOnDatastoreImpl() {
        rewriteRun(
                //language=kotlin
                kotlin(
                        """
                                import dev.morphia.DatastoreImpl

                                class CapsTest {
                                    fun update(datastore: DatastoreImpl) {
                                        datastore.ensureCaps()
                                    }
                                }
                                """,
                        """
                                import dev.morphia.MorphiaDatastore

                                class CapsTest {
                                    fun update(datastore: MorphiaDatastore) {
                                    }
                                }
                                """));
    }
}
