package dev.morphia.rewrite.recipes.test.kotlin;

import dev.morphia.rewrite.recipes.datastore.CreateDatastoreMigration;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("removal")
public class KotlinCreateDatastoreMigrationTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new CreateDatastoreMigration();
    }

    @Test
    public void update() {
        rewriteRun(
                //language=kotlin
                kotlin("""
                        import com.mongodb.client.MongoClient
                        import dev.morphia.Datastore
                        import dev.morphia.Morphia
                        import dev.morphia.mapping.MapperOptions

                        class UnwrapTest {
                            fun update() {
                                val client: MongoClient? = null
                                Morphia.createDatastore(client, "benchmarks", MapperOptions.builder()
                                    .discriminatorKey("__type")
                                    .mapSubPackages(true)
                                    .build())
                            }
                        }
                        """,
                        """
                                import com.mongodb.client.MongoClient
                                import dev.morphia.Datastore
                                import dev.morphia.Morphia
                                import dev.morphia.Morphia.createDatastore
                                import dev.morphia.config.MorphiaConfig

                                class UnwrapTest {
                                    fun update() {
                                        val client: MongoClient? = null
                                        createDatastore(client,  MorphiaConfig.load()
                                            .discriminatorKey("__type")
                                            .mapSubPackages(true).database("benchmarks"))
                                    }
                                }
                                """,
                        //language=kotlin
                        spec -> spec.afterRecipe(cu -> {
                        })));

    }

    @Test
    public void clientAndDbNameOnly() {
        rewriteRun(
                //language=kotlin
                kotlin("""
                        import com.mongodb.client.MongoClient
                        import dev.morphia.Datastore
                        import dev.morphia.Morphia

                        class UnwrapTest {
                            fun update() {
                                val client: MongoClient? = null
                                val datastore: Datastore = Morphia.createDatastore(client, "benchmarks")
                            }
                        }
                        """,
                        """
                                import com.mongodb.client.MongoClient
                                import dev.morphia.Datastore
                                import dev.morphia.Morphia
                                import dev.morphia.Morphia.createDatastore
                                import dev.morphia.config.MorphiaConfig

                                class UnwrapTest {
                                    fun update() {
                                        val client: MongoClient? = null
                                        val datastore: Datastore = createDatastore(client, MorphiaConfig.load().database("benchmarks"))
                                    }
                                }
                                """,
                        //language=kotlin
                        spec -> spec.afterRecipe(cu -> {
                        })));
    }

    @Test
    public void clientAndDbNameOnlyStaticImport() {
        rewriteRun(
                //language=kotlin
                kotlin("""
                        import com.mongodb.client.MongoClient
                        import dev.morphia.Datastore
                        import dev.morphia.Morphia.createDatastore

                        class UnwrapTest {
                            fun update() {
                                val client: MongoClient? = null
                                val datastore: Datastore = createDatastore(client, "benchmarks")
                            }
                        }
                        """,
                        """
                                import com.mongodb.client.MongoClient
                                import dev.morphia.Datastore
                                import dev.morphia.Morphia.createDatastore
                                import dev.morphia.config.MorphiaConfig

                                class UnwrapTest {
                                    fun update() {
                                        val client: MongoClient? = null
                                        val datastore: Datastore = createDatastore(client, MorphiaConfig.load().database("benchmarks"))
                                    }
                                }
                                """,
                        //language=kotlin
                        spec -> spec.afterRecipe(cu -> {
                        })));

    }

    @Test
    public void usingMapperOptionsVariable() {
        rewriteRun(
                //language=kotlin
                kotlin("""
                        import com.mongodb.client.MongoClient
                        import dev.morphia.Datastore
                        import dev.morphia.Morphia.createDatastore
                        import dev.morphia.mapping.MapperOptions

                        class UnwrapTest {
                            fun update() {
                                val client: MongoClient? = null
                                val config = MapperOptions.builder().build()
                                val datastore: Datastore = createDatastore(client, "testing", config)
                            }
                        }
                        """,
                        """
                                import com.mongodb.client.MongoClient
                                import dev.morphia.Datastore
                                import dev.morphia.Morphia.createDatastore
                                import dev.morphia.config.MorphiaConfig

                                class UnwrapTest {
                                    fun update() {
                                        val client: MongoClient? = null
                                        val config = MorphiaConfig.load()
                                        val datastore: Datastore = createDatastore(client,  config.database("testing"))
                                    }
                                }
                                """,
                        //language=kotlin
                        spec -> spec.afterRecipe(cu -> {
                        })));

    }

    @Test
    public void usingMorphiaConfigVariable() {
        rewriteRun(
                //language=kotlin
                kotlin("""
                        import com.mongodb.client.MongoClient
                        import dev.morphia.Datastore
                        import dev.morphia.Morphia.createDatastore
                        import dev.morphia.config.MorphiaConfig

                        class UnwrapTest {
                            fun update() {
                                val client: MongoClient? = null

                                val config = MorphiaConfig.load()

                                val datastore: Datastore = createDatastore(client, config)
                            }
                        }
                        """,
                        //language=kotlin
                        spec -> spec.afterRecipe(cu -> {
                        })));

    }

    @Test
    public void usingMorphiaConfigInline() {
        rewriteRun(
                //language=kotlin
                kotlin("""
                        import com.mongodb.client.MongoClient
                        import dev.morphia.Datastore
                        import dev.morphia.Morphia.createDatastore
                        import dev.morphia.config.MorphiaConfig

                        class UnwrapTest {
                            fun update() {
                                val client: MongoClient? = null

                                val datastore: Datastore = createDatastore(client, MorphiaConfig.load())
                            }
                        }
                        """,
                        //language=kotlin
                        spec -> spec.afterRecipe(cu -> {
                        })));

    }

}