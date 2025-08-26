package dev.morphia.rewrite.recipes.test.query.kotlin;

import dev.morphia.rewrite.recipes.query.QueryFindOptions;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinQueryIteratorOptionsTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new QueryFindOptions();
    }

    @Test
    public void testWithTryNext() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import org.bson.types.ObjectId
                        import dev.morphia.query.filters.Filters.eq

                        class Updates {
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java)
                                    .filter(eq("_id", ObjectId.get()))
                                    .iterator(FindOptions().limit(1))
                                    .tryNext()
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import org.bson.types.ObjectId
                        import dev.morphia.query.filters.Filters.eq

                        class Updates {
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java, FindOptions().limit(1))
                                    .filter(eq("_id", ObjectId.get()))
                                    .iterator()
                                    .tryNext()
                            }
                        }
                        """));

    }

    @Test
    public void testWithIteratorNoFilter() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import org.bson.types.ObjectId
                        import dev.morphia.query.filters.Filters.eq

                        class Updates {
                            lateinit var datastore: Datastore
                            fun getDs(): Datastore {
                                return datastore
                            }

                            fun doUpdate(ds: Datastore) {
                                getDs().find(Any::class.java)
                                    .iterator(FindOptions().limit(1))
                                    .next()
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import org.bson.types.ObjectId
                        import dev.morphia.query.filters.Filters.eq

                        class Updates {
                            lateinit var datastore: Datastore
                            fun getDs(): Datastore {
                                return datastore
                            }

                            fun doUpdate(ds: Datastore) {
                                getDs().find(Any::class.java, FindOptions().limit(1))
                                    .iterator()
                                    .next()
                            }
                        }
                        """));

    }

    @Test
    public void testWithIteratorWithOptions() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import org.bson.types.ObjectId
                        import dev.morphia.query.Sort.descending
                        import dev.morphia.query.filters.Filters.eq

                        class Updates {
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java)
                                    .filter(eq("_id", ObjectId.get()))
                                    .iterator(FindOptions().limit(1).sort(descending("changeDate")))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import org.bson.types.ObjectId
                        import dev.morphia.query.Sort.descending
                        import dev.morphia.query.filters.Filters.eq

                        class Updates {
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java, FindOptions().limit(1).sort(descending("changeDate")))
                                    .filter(eq("_id", ObjectId.get()))
                                    .iterator()
                            }
                        }
                        """));

    }

    @Test
    public void testWithIteratorNoOptions() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import org.bson.types.ObjectId
                        import dev.morphia.query.filters.Filters.eq

                        class Updates {
                            fun doUpdate(ds: Datastore) {
                                ds.find(Any::class.java)
                                    .filter(eq("_id", ObjectId.get()))
                                    .iterator()
                            }
                        }
                        """));

    }

}
