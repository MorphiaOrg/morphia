package dev.morphia.rewrite.recipes.test.datastore;

import dev.morphia.rewrite.recipes.datastore.DatastoreSaveInsertOptions;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class DatastoreSaveInsertOptionsTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new DatastoreSaveInsertOptions();
    }

    @Test
    public void saveEntityWithInsertOptions() {
        rewriteRun(java(
                //language=java
                """
                        import com.mongodb.WriteConcern;
                        import dev.morphia.Datastore;
                        import dev.morphia.InsertOptions;

                        public class Saves {
                            public void save(Datastore ds, Object entity) {
                                ds.save(entity, new InsertOptions().writeConcern(WriteConcern.MAJORITY));
                                ds.save(entity, new InsertOptions().bypassDocumentValidation(true).ordered(false));
                            }
                        }
                        """,
                //language=java
                """
                        import com.mongodb.WriteConcern;
                        import dev.morphia.Datastore;
                        import dev.morphia.InsertOneOptions;

                        public class Saves {
                            public void save(Datastore ds, Object entity) {
                                ds.save(entity, new InsertOneOptions().writeConcern(WriteConcern.MAJORITY));
                                ds.save(entity, new InsertOneOptions().bypassDocumentValidation(true));
                            }
                        }
                        """));
    }

    @Test
    public void saveListWithInsertOptions() {
        rewriteRun(java(
                //language=java
                """
                        import java.util.List;

                        import com.mongodb.WriteConcern;
                        import dev.morphia.Datastore;
                        import dev.morphia.InsertOptions;

                        public class Saves {
                            public List<Object> save(Datastore ds, List<Object> entities) {
                                return ds.save(entities, new InsertOptions().writeConcern(WriteConcern.MAJORITY).ordered(false));
                            }
                        }
                        """,
                //language=java
                """
                        import java.util.List;

                        import com.mongodb.WriteConcern;
                        import dev.morphia.Datastore;
                        import dev.morphia.InsertManyOptions;

                        public class Saves {
                            public List<Object> save(Datastore ds, List<Object> entities) {
                                return ds.save(entities, new InsertManyOptions().writeConcern(WriteConcern.MAJORITY).ordered(false));
                            }
                        }
                        """));
    }

    @Test
    public void saveCollection() {
        rewriteRun(java(
                //language=java
                """
                        import java.util.Set;

                        import dev.morphia.Datastore;
                        import dev.morphia.InsertOptions;

                        public class Saves {
                            public void save(Datastore ds, Set<Object> entities) {
                                ds.save(entities);
                                ds.save(entities, new InsertOptions().ordered(false));
                            }
                        }
                        """,
                //language=java
                """
                        import java.util.ArrayList;
                        import java.util.Set;

                        import dev.morphia.Datastore;
                        import dev.morphia.InsertManyOptions;

                        public class Saves {
                            public void save(Datastore ds, Set<Object> entities) {
                                ds.save(new ArrayList<>(entities));
                                ds.save(new ArrayList<>(entities), new InsertManyOptions().ordered(false));
                            }
                        }
                        """));
    }

    @Test
    public void unconvertibleSavesAreUnchanged() {
        rewriteRun(java(
                //language=java
                """
                        import java.util.List;

                        import dev.morphia.Datastore;
                        import dev.morphia.InsertOneOptions;
                        import dev.morphia.InsertOptions;

                        public class Saves {
                            public void save(Datastore ds, Object entity, Iterable<Object> entities, List<Object> list, InsertOptions options) {
                                ds.save(entity);
                                ds.save(entity, new InsertOneOptions());
                                ds.save(list);
                                ds.save(entities);
                                ds.save(entity, options);
                            }
                        }
                        """));
    }
}
