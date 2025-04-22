package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.UpdateExecute;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class ModifyExecuteTest extends MorphiaRewriteTest {

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
                            public void doModify(Datastore ds) {
                                ds.find(Object.class).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .modify(set("last_updated", LocalDateTime.now()))
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
                            public void doModify(Datastore ds) {
                                ds.find(Object.class).filter(eq("_id", ObjectId.get()))
                                        .disableValidation()
                                        .modify(set("last_updated", LocalDateTime.now()));
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
                        import dev.morphia.ModifyOptions;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class Updates {
                            public void doModify(Datastore ds) {
                                ds.find(Object.class).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .modify(set("last_updated", LocalDateTime.now()))
                                  .execute(new ModifyOptions());
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.ModifyOptions;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class Updates {
                            public void doModify(Datastore ds) {
                                ds.find(Object.class).filter(eq("_id", ObjectId.get()))
                                        .disableValidation()
                                        .modify(new ModifyOptions(), set("last_updated", LocalDateTime.now()));
                            }
                        }
                        """));
    }

    @Test
    public void testEmptyExecute() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.ModifyOptions;
                        import org.bson.types.ObjectId;
                        import dev.morphia.query.Modify;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class Updates {
                            public void doModify(Modify<?> modify) {
                                var result = modify.execute();
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.ModifyOptions;
                        import org.bson.types.ObjectId;
                        import dev.morphia.query.Modify;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class Updates {
                            public void doModify(Modify<?> modify) {
                                var result =modify;
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
                        import dev.morphia.ModifyOptions;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class Updates {
                            public void doModify(Datastore ds) {
                                ds.find(Object.class).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .modify(set("last_updated", LocalDateTime.now()))
                                  .execute(new ModifyOptions().upsert(true));
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.ModifyOptions;
                        import org.bson.types.ObjectId;

                        import java.time.LocalDateTime;

                        import static dev.morphia.query.filters.Filters.eq;
                        import static dev.morphia.query.updates.UpdateOperators.set;

                        public class Updates {
                            public void doModify(Datastore ds) {
                                ds.find(Object.class).filter(eq("_id", ObjectId.get()))
                                        .disableValidation()
                                        .modify(new ModifyOptions().upsert(true), set("last_updated", LocalDateTime.now()));
                            }
                        }
                        """));
    }

}