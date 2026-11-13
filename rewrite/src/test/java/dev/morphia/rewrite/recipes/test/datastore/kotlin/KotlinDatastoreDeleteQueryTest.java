package dev.morphia.rewrite.recipes.test.datastore.kotlin;

import dev.morphia.rewrite.recipes.datastore.DatastoreDeleteQuery;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinDatastoreDeleteQueryTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new DatastoreDeleteQuery();
    }

    @Test
    public void deleteQuery() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.Query
                        import dev.morphia.query.filters.Filters.eq

                        class Deletes {
                            fun delete(ds: Datastore, query: Query<Any>) {
                                ds.delete(query)
                                ds.delete<Any>(query)
                                ds.delete(ds.find(Any::class.java).filter(eq("name", "value")))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.Query
                        import dev.morphia.query.filters.Filters.eq

                        class Deletes {
                            fun delete(ds: Datastore, query: Query<Any>) {
                                query.delete()
                                query.delete()
                                ds.find(Any::class.java).filter(eq("name", "value")).delete()
                            }
                        }
                        """));
    }

    @Test
    public void deleteQueryWithOptions() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.DeleteOptions
                        import dev.morphia.Datastore
                        import dev.morphia.query.Query

                        class Deletes {
                            fun delete(ds: Datastore, query: Query<Any>) {
                                ds.delete(query, DeleteOptions().multi(true))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.DeleteOptions
                        import dev.morphia.Datastore
                        import dev.morphia.query.Query

                        class Deletes {
                            fun delete(ds: Datastore, query: Query<Any>) {
                                query.delete(DeleteOptions().multi(true))
                            }
                        }
                        """));
    }

    @Test
    public void deleteEntityIsUnchanged() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.DeleteOptions
                        import dev.morphia.Datastore
                        import dev.morphia.query.Query

                        class Deletes {
                            fun delete(ds: Datastore, entity: Any) {
                                ds.delete(entity)
                                ds.delete(entity, DeleteOptions())
                            }

                            fun safeCall(ds: Datastore?, query: Query<Any>) {
                                ds?.delete(query)
                            }
                        }
                        """));
    }
}
