package dev.morphia.rewrite.recipes.test.kotlin;

import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;
import dev.morphia.rewrite.recipes.test.QueryDatastoreUpgradeTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinQueryDatastoreUpgradeTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return QueryDatastoreUpgradeTest.upgradeRecipe();
    }

    @Test
    public void alternateCollectionMergesWithMovedOptions() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import dev.morphia.query.filters.Filters.eq

                        class Executes {
                            fun execute(ds: Datastore, options: FindOptions): Any? {
                                ds.find("other", Any::class.java)
                                    .filter(eq("name", "value"))
                                    .execute(FindOptions().limit(1))
                                    .toList()
                                return ds.find("other", Any::class.java)
                                    .first(options)
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import dev.morphia.query.filters.Filters.eq

                        class Executes {
                            fun execute(ds: Datastore, options: FindOptions): Any? {
                                ds.find(Any::class.java, FindOptions().limit(1).collection("other"))
                                    .filter(eq("name", "value"))
                                    .iterator()
                                    .toList()
                                return ds.find(Any::class.java, FindOptions().collection("other"))
                                    .first(options)
                            }
                        }
                        """));
    }

    @Test
    public void executeWithOptionsMovesToFind() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import dev.morphia.query.filters.Filters.eq

                        class Executes {
                            fun execute(ds: Datastore): List<Any> {
                                return ds.find(Any::class.java)
                                    .filter(eq("name", "value"))
                                    .execute(FindOptions().limit(1))
                                    .toList()
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import dev.morphia.query.filters.Filters.eq

                        class Executes {
                            fun execute(ds: Datastore): List<Any> {
                                return ds.find(Any::class.java, FindOptions().limit(1))
                                    .filter(eq("name", "value"))
                                    .iterator()
                                    .toList()
                            }
                        }
                        """));
    }

    @Test
    public void conflictingOptionsAreLeftForManualMigration() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions

                        class Executes {
                            fun execute(ds: Datastore): Any? {
                                return ds.find(Any::class.java, FindOptions().skip(10))
                                    .first(FindOptions().limit(1))
                            }
                        }
                        """));
    }

    @Test
    public void datastoreOverloads() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import com.mongodb.WriteConcern
                        import dev.morphia.Datastore
                        import dev.morphia.InsertOptions
                        import dev.morphia.query.Query

                        class Overloads {
                            fun update(ds: Datastore, query: Query<Any>, entity: Any) {
                                ds.delete(query)
                                ds.merge(entity, WriteConcern.MAJORITY)
                                ds.save(entity, InsertOptions().writeConcern(WriteConcern.MAJORITY))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import com.mongodb.WriteConcern
                        import dev.morphia.Datastore
                        import dev.morphia.InsertOneOptions
                        import dev.morphia.query.Query

                        class Overloads {
                            fun update(ds: Datastore, query: Query<Any>, entity: Any) {
                                query.delete()
                                ds.merge(entity, InsertOneOptions().writeConcern(WriteConcern.MAJORITY))
                                ds.save(entity, InsertOneOptions().writeConcern(WriteConcern.MAJORITY))
                            }
                        }
                        """));
    }

    @Test
    public void movedCollectionTakesPrecedence() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions

                        class Executes {
                            fun execute(ds: Datastore): Any? {
                                return ds.find("base", Any::class.java)
                                    .first(FindOptions().collection("override"))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions

                        class Executes {
                            fun execute(ds: Datastore): Any? {
                                return ds.find(Any::class.java, FindOptions().collection("override"))
                                    .first()
                            }
                        }
                        """));
    }

    @Test
    public void nullCollectionIsLeftForManualMigration() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions

                        class Executes {
                            fun execute(ds: Datastore): Any? {
                                return ds.find("base", Any::class.java)
                                    .first(FindOptions().collection(null))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions

                        class Executes {
                            fun execute(ds: Datastore): Any? {
                                return ds.find(Any::class.java, FindOptions().collection("base"))
                                    .first(FindOptions().collection(null))
                            }
                        }
                        """));
    }
}
