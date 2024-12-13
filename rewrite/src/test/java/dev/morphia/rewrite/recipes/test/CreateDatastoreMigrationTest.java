package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.CreateDatastoreMigration;
import dev.morphia.rewrite.recipes.MorphiaConfigMigration;
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
              import dev.morphia.config.MorphiaConfig;import dev.morphia.mapping.MapperOptions;
              
              public class UnwrapTest {
                  public void update() {
                      MongoClient client = null;
                      Datastore datastore = Morphia.createDatastore(client, "benchmarks", (MapperOptions) MorphiaConfig.load()
                                                                                                       .applyIndexes(false)
                                                                                                       .applyCaps(true));
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
                        Datastore datastore = Morphia.createDatastore(client, MorphiaConfig.load().database("benchmarks")
                                .applyIndexes(false)
                                .applyCaps(true));
                    }
                }
                """));

    }

}
