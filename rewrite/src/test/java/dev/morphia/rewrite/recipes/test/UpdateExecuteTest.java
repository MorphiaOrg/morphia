package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.UpdateExecute;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

class UpdateExecuteTest extends MorphiaRewriteTest {

    @NotNull
    @Override
    protected Recipe getRecipe() {
        return new UpdateExecute();
    }

    @Test
    public void testBasicExecute() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class Updates {
                            public void doUpdate(Datastore ds) {
                                ds.find(Object.class).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .update(set("last_updated", LocalDateTime.now()))
                                  .execute();
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class Updates {
                            public void doUpdate(Datastore ds) {
                                ds.find(Object.class).filter(eq("_id", ObjectId.get()))
                                        .disableValidation()
                                        .update(set("last_updated", LocalDateTime.now()));
                            }
                        }
                        """));
    }

    @Test
    public void testExecuteWithDefaultOptions() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.UpdateOptions;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class Updates {
                            public void doUpdate(Datastore ds) {
                                ds.find(Object.class).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .update(set("last_updated", LocalDateTime.now()))
                                  .execute(new UpdateOptions());
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.UpdateOptions;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class Updates {
                            public void doUpdate(Datastore ds) {
                                ds.find(Object.class).filter(eq("_id", ObjectId.get()))
                                        .disableValidation()
                                        .update(new UpdateOptions(), set("last_updated", LocalDateTime.now()));
                            }
                        }
                        """));
    }

    @Test
    public void testExecuteWithOptions() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.UpdateOptions;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class Updates {
                            public void doUpdate(Datastore ds) {
                                ds.find(Object.class).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .update(set("last_updated", LocalDateTime.now()))
                                  .execute(new UpdateOptions().upsert(true));
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.UpdateOptions;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class Updates {
                            public void doUpdate(Datastore ds) {
                                ds.find(Object.class).filter(eq("_id", ObjectId.get()))
                                        .disableValidation()
                                        .update(new UpdateOptions().upsert(true), set("last_updated", LocalDateTime.now()));
                            }
                        }
                        """));
    }

}