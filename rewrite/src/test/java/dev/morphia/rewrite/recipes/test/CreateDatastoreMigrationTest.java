package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.CreateDatastoreMigration;

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
                java(
                        """
                                import com.mongodb.client.MongoClient;
                                import dev.morphia.Datastore;
                                import dev.morphia.Morphia;
                                import dev.morphia.mapping.MapperOptions;

                                public class UnwrapTest {
                                    public void update() {
                                        MongoClient client = null;
                                        Datastore datastore = Morphia.createDatastore(client, "benchmarks", MapperOptions.builder()
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

                                public class UnwrapTest {
                                    public void update() {
                                        MongoClient client = null;
                                        Datastore datastore = Morphia.createDatastore(client,  MorphiaConfig.load().database("benchmarks")
                                                .discriminatorKey("__type")
                                                .mapSubPackages(true));
                                    }
                                }
                                """));

    }

    //    @Test
    public void variableArgument() {
        rewriteRun(
                //language=java
                java(
                        """

                                import com.mongodb.client.MongoClient;
                                      import dev.morphia.Datastore;
                                      import dev.morphia.Morphia;
                                      import dev.morphia.mapping.MapperOptions;

                                      public class UnwrapTest {
                                          public void update() {
                                              MongoClient client = null;
                                              MapperOptions options = null;
                                              Datastore datastore = Morphia.createDatastore(client, "benchmarks",  options);
                                          }
                                      }
                                      """,
                        """
                                import com.mongodb.client.MongoClient;
                                      import dev.morphia.Datastore;
                                      import dev.morphia.Morphia;
                                import dev.morphia.config.MorphiaConfig;
                                import dev.morphia.mapping.MapperOptions;

                                public class UnwrapTest {
                                          public void update() {
                                              MongoClient client = null;
                                              MapperOptions options = null;
                                              Datastore datastore = Morphia.createDatastore(client, options.database("benchmarks"));
                                          }
                                      }
                                """));

    }

}
