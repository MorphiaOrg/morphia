package dev.morphia.rewrite.recipes.test.datastore;

import dev.morphia.rewrite.recipes.datastore.DatastoreMergeWriteConcern;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class DatastoreMergeWriteConcernTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new DatastoreMergeWriteConcern();
    }

    @Test
    public void mergeWithWriteConcern() {
        rewriteRun(java(
                //language=java
                """
                        import com.mongodb.WriteConcern;
                        import dev.morphia.Datastore;

                        public class Merges {
                            public void merge(Datastore ds, Object entity, WriteConcern concern) {
                                ds.merge(entity, WriteConcern.MAJORITY);
                                ds.merge(entity, concern);
                            }
                        }
                        """,
                //language=java
                """
                        import com.mongodb.WriteConcern;
                        import dev.morphia.Datastore;
                        import dev.morphia.InsertOneOptions;

                        public class Merges {
                            public void merge(Datastore ds, Object entity, WriteConcern concern) {
                                ds.merge(entity, new InsertOneOptions().writeConcern(WriteConcern.MAJORITY));
                                ds.merge(entity, new InsertOneOptions().writeConcern(concern));
                            }
                        }
                        """));
    }

    @Test
    public void otherMergesAreUnchanged() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.InsertOneOptions;

                        public class Merges {
                            public void merge(Datastore ds, Object entity) {
                                ds.merge(entity);
                                ds.merge(entity, new InsertOneOptions());
                            }
                        }
                        """));
    }
}
