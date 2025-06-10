package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.datastore.CreateDatastoreMigration;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("removal")
public class CreateDatastoreMigrationTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new CreateDatastoreMigration();
    }

    @Test
    public void update() {
        rewriteRun(
                //language=java
                java("""
                        import com.mongodb.client.MongoClient;
                        import dev.morphia.Datastore;
                        import dev.morphia.Morphia;
                        import dev.morphia.mapping.MapperOptions;

                        public class UnwrapTest {
                            public void update() {
                                MongoClient client = null;
                                Morphia.createDatastore(client, "benchmarks", MapperOptions.builder()
                                    .discriminatorKey("__type")
                                    .mapSubPackages(true)
                                    .build());
                            }
                        }
                        """,
                        """
                                import com.mongodb.client.MongoClient;
                                import dev.morphia.Datastore;
                                import dev.morphia.Morphia;
                                import dev.morphia.config.MorphiaConfig;

                                import static dev.morphia.Morphia.createDatastore;

                                public class UnwrapTest {
                                    public void update() {
                                        MongoClient client = null;
                                        createDatastore(client,  MorphiaConfig.load()
                                                .discriminatorKey("__type")
                                                .mapSubPackages(true).database("benchmarks"));
                                    }
                                }
                                """));

    }

    @Test
    public void clientAndDbNameOnly() {
        rewriteRun(
                //language=java
                java("""
                        import com.mongodb.client.MongoClient;
                        import dev.morphia.Datastore;
                        import dev.morphia.Morphia;

                        public class UnwrapTest {
                            public void update() {
                                MongoClient client = null;
                                Datastore datastore = Morphia.createDatastore(client, "benchmarks");
                            }
                        }
                        """,
                        """
                                import com.mongodb.client.MongoClient;
                                import dev.morphia.Datastore;
                                import dev.morphia.Morphia;
                                import dev.morphia.config.MorphiaConfig;

                                import static dev.morphia.Morphia.createDatastore;

                                public class UnwrapTest {
                                    public void update() {
                                        MongoClient client = null;
                                        Datastore datastore = createDatastore(client, MorphiaConfig.load().database("benchmarks"));
                                    }
                                }
                                """));
    }

    @Test
    public void clientAndDbNameOnlyStaticImport() {
        rewriteRun(
                //language=java
                java("""
                        import com.mongodb.client.MongoClient;
                        import dev.morphia.Datastore;
                        import static dev.morphia.Morphia.createDatastore;

                        public class UnwrapTest {
                            public void update() {
                                MongoClient client = null;
                                Datastore datastore = createDatastore(client, "benchmarks");
                            }
                        }
                        """,
                        """
                                import com.mongodb.client.MongoClient;
                                import dev.morphia.Datastore;
                                import dev.morphia.config.MorphiaConfig;

                                import static dev.morphia.Morphia.createDatastore;

                                public class UnwrapTest {
                                    public void update() {
                                        MongoClient client = null;
                                        Datastore datastore = createDatastore(client, MorphiaConfig.load().database("benchmarks"));
                                    }
                                }
                                """));

    }

    @Test
    public void usingMorphiaConfigVariable() {
        rewriteRun(
                //language=java
                java("""
                        import com.mongodb.client.MongoClient;
                        import dev.morphia.Datastore;
                        import static dev.morphia.Morphia.createDatastore;
                        import dev.morphia.config.MorphiaConfig;

                        public class UnwrapTest {
                            public void update() {
                                MongoClient client = null;

                                var config = MorphiaConfig.load();

                                Datastore datastore = createDatastore(client, config);
                            }
                        }
                        """));

    }

    @Test
    public void usingMorphiaConfigInline() {
        rewriteRun(
                //language=java
                java("""
                        import com.mongodb.client.MongoClient;
                        import dev.morphia.Datastore;
                        import static dev.morphia.Morphia.createDatastore;
                        import dev.morphia.config.MorphiaConfig;

                        public class UnwrapTest {
                            public void update() {
                                MongoClient client = null;

                                Datastore datastore = createDatastore(client, MorphiaConfig.load());
                            }
                        }
                        """));

    }

}
