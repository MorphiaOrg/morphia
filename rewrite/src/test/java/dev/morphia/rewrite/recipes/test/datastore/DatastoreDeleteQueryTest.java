package dev.morphia.rewrite.recipes.test.datastore;

import dev.morphia.rewrite.recipes.datastore.DatastoreDeleteQuery;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class DatastoreDeleteQueryTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new DatastoreDeleteQuery();
    }

    @Test
    public void deleteQuery() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.Query;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Deletes {
                            public void delete(Datastore ds, Query<Object> query) {
                                ds.delete(query);
                                ds.delete(ds.find(Object.class).filter(eq("name", "value")));
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.Query;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Deletes {
                            public void delete(Datastore ds, Query<Object> query) {
                                query.delete();
                                ds.find(Object.class).filter(eq("name", "value")).delete();
                            }
                        }
                        """));
    }

    @Test
    public void deleteQueryWithOptions() {
        rewriteRun(java(
                //language=java
                """
                        import com.mongodb.client.result.DeleteResult;
                        import dev.morphia.DeleteOptions;
                        import dev.morphia.Datastore;
                        import dev.morphia.query.Query;

                        public class Deletes {
                            public DeleteResult delete(Datastore ds, Query<Object> query) {
                                return ds.delete(query, new DeleteOptions().multi(true));
                            }
                        }
                        """,
                //language=java
                """
                        import com.mongodb.client.result.DeleteResult;
                        import dev.morphia.DeleteOptions;
                        import dev.morphia.Datastore;
                        import dev.morphia.query.Query;

                        public class Deletes {
                            public DeleteResult delete(Datastore ds, Query<Object> query) {
                                return query.delete(new DeleteOptions().multi(true));
                            }
                        }
                        """));
    }

    @Test
    public void deleteEntityIsUnchanged() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.DeleteOptions;
                        import dev.morphia.Datastore;

                        public class Deletes {
                            public void delete(Datastore ds, Object entity) {
                                ds.delete(entity);
                                ds.delete(entity, new DeleteOptions());
                            }
                        }
                        """));
    }
}
