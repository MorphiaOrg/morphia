package dev.morphia.rewrite.recipes.test.query.kotlin;

import dev.morphia.rewrite.recipes.query.UpdateExecute;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinUpdateExecuteTest extends KotlinRewriteTest {

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
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .update(set("last_updated", LocalDateTime.now()))
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
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java).filter(eq("_id", ObjectId.get()))
                                    .disableValidation()
                                    .update(set("last_updated", LocalDateTime.now()))
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
                        import dev.morphia.UpdateOptions
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .update(set("last_updated", LocalDateTime.now()))
                                  .execute(UpdateOptions())
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.UpdateOptions
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java).filter(eq("_id", ObjectId.get()))
                                    .disableValidation()
                                    .update(UpdateOptions(), set("last_updated", LocalDateTime.now()))
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
                        import dev.morphia.UpdateOptions
                        import org.bson.types.ObjectId
                        import dev.morphia.query.Update
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doUpdate(update: Update<*>) {
                                val result = update.execute()
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.UpdateOptions
                        import org.bson.types.ObjectId
                        import dev.morphia.query.Update
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doUpdate(update: Update<*>) {
                                val result =update
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
                        import dev.morphia.UpdateOptions
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .update(set("last_updated", LocalDateTime.now()))
                                  .execute(UpdateOptions().upsert(true))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.UpdateOptions
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java).filter(eq("_id", ObjectId.get()))
                                    .disableValidation()
                                    .update(UpdateOptions().upsert(true), set("last_updated", LocalDateTime.now()))
                            }
                        }
                        """));
    }

    @Test
    public void testExecuteWithOptionsPart2() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.UpdateOptions
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java)
                                            .filter(eq("nick", oldNick))
                                            .update(set("nick", newNick))
                                            .execute(UpdateOptions().multi(false))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.UpdateOptions
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class Updates {
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java)
                                    .filter(eq("nick", oldNick))
                                    .update(UpdateOptions().multi(false), set("nick", newNick))
                            }
                        }
                        """));
    }

}