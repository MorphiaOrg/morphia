package dev.morphia.rewrite.recipes.test.query.kotlin;

import dev.morphia.rewrite.recipes.query.UpdateExecute;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinModifyExecuteTest extends KotlinRewriteTest {

    @NotNull
    @Override
    protected Recipe getRecipe() {
        return new UpdateExecute();
    }

    @Test
    public void testBasicExecute() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doModify(ds: Datastore) {
                                ds.find(Any::class.java).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .modify(set("last_updated", LocalDateTime.now()))
                                  .execute()
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doModify(ds: Datastore) {
                                ds.find(Any::class.java).filter(eq("_id", ObjectId.get()))
                                    .disableValidation()
                                    .modify(set("last_updated", LocalDateTime.now()))
                            }
                        }
                        """));
    }

    @Test
    public void testExecuteWithDefaultOptions() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.ModifyOptions
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doModify(ds: Datastore) {
                                ds.find(Any::class.java).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .modify(set("last_updated", LocalDateTime.now()))
                                  .execute(ModifyOptions())
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.ModifyOptions
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doModify(ds: Datastore) {
                                ds.find(Any::class.java).filter(eq("_id", ObjectId.get()))
                                    .disableValidation()
                                    .modify(ModifyOptions(), set("last_updated", LocalDateTime.now()))
                            }
                        }
                        """));
    }

    @Test
    public void testEmptyExecute() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.ModifyOptions
                        import org.bson.types.ObjectId
                        import dev.morphia.query.Modify
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doModify(modify: Modify<*>) {
                                val result = modify.execute()
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.ModifyOptions
                        import org.bson.types.ObjectId
                        import dev.morphia.query.Modify
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doModify(modify: Modify<*>) {
                                val result =modify
                            }
                        }
                        """));
    }

    @Test
    public void testExecuteWithOptions() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.ModifyOptions
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doModify(ds: Datastore) {
                                ds.find(Any::class.java).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .modify(set("last_updated", LocalDateTime.now()))
                                  .execute(ModifyOptions().upsert(true))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.ModifyOptions
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doModify(ds: Datastore) {
                                ds.find(Any::class.java).filter(eq("_id", ObjectId.get()))
                                    .disableValidation()
                                    .modify(ModifyOptions().upsert(true), set("last_updated", LocalDateTime.now()))
                            }
                        }
                        """));
    }

}