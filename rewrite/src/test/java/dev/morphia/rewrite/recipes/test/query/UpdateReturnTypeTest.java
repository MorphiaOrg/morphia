package dev.morphia.rewrite.recipes.test.query;

import dev.morphia.rewrite.recipes.query.UpdateReturnType;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

class UpdateReturnTypeTest extends MorphiaRewriteTest {

    @NotNull
    @Override
    protected Recipe getRecipe() {
        return new UpdateReturnType();
    }

    @Test
    public void testAssignmentUpdates() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.Update;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class ReturnTypes {
                            public void example(Datastore ds) {
                                Update<String> result = ds.find(String.class).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .update(set("last_updated", LocalDateTime.now()))
                                  .execute();
                            }
                        }
                        """,
                //language=java
                """
                        import com.mongodb.client.result.UpdateResult;
                        import dev.morphia.Datastore;
                        import dev.morphia.query.Update;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class ReturnTypes {
                            public void example(Datastore ds) {
                                UpdateResult result = ds.find(String.class).filter(eq("_id", ObjectId.get()))
                                        .disableValidation()
                                        .update(set("last_updated", LocalDateTime.now()))
                                        .execute();
                            }
                        }
                        """));
    }
}