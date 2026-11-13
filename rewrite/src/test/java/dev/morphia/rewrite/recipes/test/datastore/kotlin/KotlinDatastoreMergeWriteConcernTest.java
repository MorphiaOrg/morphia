package dev.morphia.rewrite.recipes.test.datastore.kotlin;

import dev.morphia.rewrite.recipes.datastore.DatastoreMergeWriteConcern;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinDatastoreMergeWriteConcernTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new DatastoreMergeWriteConcern();
    }

    @Test
    public void mergeWithWriteConcern() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import com.mongodb.WriteConcern
                        import dev.morphia.Datastore

                        class Merges {
                            fun merge(ds: Datastore, entity: Any, concern: WriteConcern) {
                                ds.merge(entity, WriteConcern.MAJORITY)
                                ds.merge(entity, concern)
                            }
                        }
                        """,
                //language=kotlin
                """
                        import com.mongodb.WriteConcern
                        import dev.morphia.Datastore
                        import dev.morphia.InsertOneOptions

                        class Merges {
                            fun merge(ds: Datastore, entity: Any, concern: WriteConcern) {
                                ds.merge(entity, InsertOneOptions().writeConcern(WriteConcern.MAJORITY))
                                ds.merge(entity, InsertOneOptions().writeConcern(concern))
                            }
                        }
                        """));
    }

    @Test
    public void otherMergesAreUnchanged() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.InsertOneOptions

                        class Merges {
                            fun merge(ds: Datastore, entity: Any) {
                                ds.merge(entity)
                                ds.merge(entity, InsertOneOptions())
                            }
                        }
                        """));
    }

    @Test
    public void expressionBodiesAreUnchanged() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import com.mongodb.WriteConcern
                        import dev.morphia.Datastore

                        class Merges {
                            fun merge(ds: Datastore, entity: Any, concern: WriteConcern) = ds.merge(entity, concern)

                            fun mergeUnit(ds: Datastore, entity: Any, concern: WriteConcern): Unit = ds.merge(entity, concern)
                        }
                        """));
    }
}
