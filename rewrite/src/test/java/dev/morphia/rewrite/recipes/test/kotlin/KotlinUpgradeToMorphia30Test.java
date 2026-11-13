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

    // MorphiaDatastore is 3.0-only, so this exercises the MorphiaDatastore pattern after DatastoreImpl is renamed
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

    // 3.0's Mapper has no getListeners(), so getInterceptors() is marked rather than renamed
    @Test
    void marksGetInterceptors() {
        rewriteRun(
                //language=kotlin
                kotlin(
                        """
                                import dev.morphia.EntityListener
                                import dev.morphia.mapping.Mapper

                                class Listeners {
                                    fun listeners(mapper: Mapper): List<EntityListener<*>> {
                                        return mapper.getInterceptors()
                                    }
                                }
                                """,
                        """
                                import dev.morphia.EntityListener
                                import dev.morphia.mapping.Mapper

                                class Listeners {
                                    fun listeners(mapper: Mapper): List<EntityListener<*>> {
                                        return /*~~(getInterceptors was removed in 3.0: the global listeners are configured in MorphiaConfig.listeners(), or in morphia.listeners in a config file, so read them from there)~~>*/mapper.getInterceptors()
                                    }
                                }
                                """));
    }

    @Test
    void marksHasInterceptors() {
        rewriteRun(
                //language=kotlin
                kotlin(
                        """
                                import dev.morphia.mapping.Mapper

                                class Listeners {
                                    fun any(mapper: Mapper): Boolean {
                                        return mapper.hasInterceptors()
                                    }
                                }
                                """,
                        """
                                import dev.morphia.mapping.Mapper

                                class Listeners {
                                    fun any(mapper: Mapper): Boolean {
                                        return /*~~(hasInterceptors was removed in 3.0: the global listeners are configured in MorphiaConfig.listeners(), or in morphia.listeners in a config file, so read them from there)~~>*/mapper.hasInterceptors()
                                    }
                                }
                                """));
    }

    @Test
    void embeddedBecomesEntity() {
        rewriteRun(
                //language=kotlin
                kotlin(
                        """
                                import dev.morphia.annotations.Embedded

                                @Embedded(useDiscriminator = false)
                                class Address
                                """,
                        """
                                import dev.morphia.annotations.Entity

                                @Entity(useDiscriminator = false)
                                class Address
                                """));
    }
}
