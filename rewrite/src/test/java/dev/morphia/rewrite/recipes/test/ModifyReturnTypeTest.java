package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.ModifyReturnType;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

class ModifyReturnTypeTest extends MorphiaRewriteTest {

    @NotNull
    @Override
    protected Recipe getRecipe() {
        return new ModifyReturnType();
    }

    @Test
    public void testAssignmentUpdates() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.Modify;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class ReturnTypes {
                            public void example(Datastore ds) {
                                Modify<String> result = ds.find(String.class).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .modify(set("last_updated", LocalDateTime.now()));
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.Modify;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class ReturnTypes {
                            public void example(Datastore ds) {
                                String result = ds.find(String.class).filter(eq("_id", ObjectId.get()))
                                        .disableValidation()
                                        .modify(set("last_updated", LocalDateTime.now()));
                            }
                        }
                        """));
    }
}