package dev.morphia.rewrite.recipes.test.datastore.kotlin;

import dev.morphia.rewrite.recipes.datastore.DatastoreSaveInsertOptions;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinDatastoreSaveInsertOptionsTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new DatastoreSaveInsertOptions();
    }

    @Test
    public void saveEntityWithInsertOptions() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import com.mongodb.WriteConcern
                        import dev.morphia.Datastore
                        import dev.morphia.InsertOptions

                        class Saves {
                            fun save(ds: Datastore, entity: Any) {
                                ds.save(entity, InsertOptions().writeConcern(WriteConcern.MAJORITY))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import com.mongodb.WriteConcern
                        import dev.morphia.Datastore
                        import dev.morphia.InsertOneOptions

                        class Saves {
                            fun save(ds: Datastore, entity: Any) {
                                ds.save(entity, InsertOneOptions().writeConcern(WriteConcern.MAJORITY))
                            }
                        }
                        """));
    }

    @Test
    public void saveIterable() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.InsertOptions

                        class Saves {
                            fun save(ds: Datastore, entities: Iterable<Any>) {
                                ds.save(entities)
                                ds.save(entities, InsertOptions().ordered(false))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.InsertManyOptions

                        class Saves {
                            fun save(ds: Datastore, entities: Iterable<Any>) {
                                ds.save(entities.toMutableList())
                                ds.save(entities.toMutableList(), InsertManyOptions().ordered(false))
                            }
                        }
                        """));
    }

    @Test
    public void saveListWithInsertOptions() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import com.mongodb.WriteConcern
                        import dev.morphia.Datastore
                        import dev.morphia.InsertOptions

                        class Saves {
                            fun save(ds: Datastore, entities: List<Any>): List<Any> {
                                return ds.save(entities, InsertOptions().writeConcern(WriteConcern.MAJORITY).ordered(false))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import com.mongodb.WriteConcern
                        import dev.morphia.Datastore
                        import dev.morphia.InsertManyOptions

                        class Saves {
                            fun save(ds: Datastore, entities: List<Any>): List<Any> {
                                return ds.save(entities.toMutableList(), InsertManyOptions().writeConcern(WriteConcern.MAJORITY).ordered(false))
                            }
                        }
                        """));
    }

    @Test
    public void saveCollection() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.InsertOptions

                        class Saves {
                            fun save(ds: Datastore, entities: Set<Any>) {
                                ds.save(entities)
                                ds.save(entities, InsertOptions().ordered(false))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.InsertManyOptions

                        class Saves {
                            fun save(ds: Datastore, entities: Set<Any>) {
                                ds.save(entities.toMutableList())
                                ds.save(entities.toMutableList(), InsertManyOptions().ordered(false))
                            }
                        }
                        """));
    }

    @Test
    public void unconvertibleSavesAreUnchanged() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.InsertOneOptions
                        import dev.morphia.InsertOptions

                        class Saves {
                            fun save(ds: Datastore, entity: Any, list: List<Any>, options: InsertOptions) {
                                ds.save(entity, InsertOptions().ordered(isOrdered()))
                                ds.save(list, InsertOptions().ordered(list.isEmpty()))
                                ds.save(entity)
                                ds.save(entity, InsertOneOptions())
                                ds.save(list)
                                ds.save(entity, options)
                            }

                            private fun isOrdered(): Boolean {
                                return false
                            }
                        }
                        """));
    }
}
