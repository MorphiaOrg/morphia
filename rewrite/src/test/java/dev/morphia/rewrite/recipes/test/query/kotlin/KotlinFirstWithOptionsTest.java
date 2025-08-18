package dev.morphia.rewrite.recipes.test.query.kotlin;

import dev.morphia.rewrite.recipes.query.QueryFindOptions;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinFirstWithOptionsTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new QueryFindOptions();
    }

    @Test
    public void testWithFirstNoFilter() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import org.bson.types.ObjectId

                        import dev.morphia.query.filters.Filters.eq

                        class Updates {
                            var datastore: Datastore? = null
                            fun getDs(): Datastore? {
                                return datastore
                            }

                            fun doUpdate(ds: Datastore) {
                                getDs()?.find(Any::class.java)
                                    ?.first(FindOptions().limit(1))
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
                            var datastore: Datastore? = null
                            fun getDs(): Datastore? {
                                return datastore
                            }

                            fun doUpdate(ds: Datastore) {
                                getDs()?.find(Any::class.java, FindOptions().limit(1))
                                    ?.first()
                            }
                        }
                        """));

    }

    @Test
    public void testWithFirstWithOptions() {
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
                                  .first(FindOptions().limit(1))
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
                                    .first()
                            }
                        }
                        """));

    }

    @Test
    public void testWithFirstNoOptions() {
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
                                  .first()
                            }
                        }
                        """));

    }
}