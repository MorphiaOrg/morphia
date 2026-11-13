package dev.morphia.rewrite.recipes.test.query;

import dev.morphia.rewrite.recipes.query.QueryExecute;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class QueryExecuteTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new QueryExecute();
    }

    @Test
    public void execute() {
        rewriteRun(java(
                //language=java
                """
                        import java.util.List;

                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;
                        import dev.morphia.query.MorphiaCursor;
                        import dev.morphia.query.Query;

                        public class Executes {
                            public List<Object> execute(Datastore ds, Query<Object> query) {
                                MorphiaCursor<Object> cursor = query.execute(new FindOptions().limit(1));
                                return ds.find(Object.class).execute().toList();
                            }
                        }
                        """,
                //language=java
                """
                        import java.util.List;

                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;
                        import dev.morphia.query.MorphiaCursor;
                        import dev.morphia.query.Query;

                        public class Executes {
                            public List<Object> execute(Datastore ds, Query<Object> query) {
                                MorphiaCursor<Object> cursor = query.iterator(new FindOptions().limit(1));
                                return ds.find(Object.class).iterator().toList();
                            }
                        }
                        """));
    }

    @Test
    public void methodReference() {
        rewriteRun(java(
                //language=java
                """
                        import java.util.function.Supplier;

                        import dev.morphia.query.MorphiaCursor;
                        import dev.morphia.query.Query;

                        public class Executes {
                            public Supplier<MorphiaCursor<Object>> execute(Query<Object> query) {
                                return query::execute;
                            }
                        }
                        """,
                //language=java
                """
                        import java.util.function.Supplier;

                        import dev.morphia.query.MorphiaCursor;
                        import dev.morphia.query.Query;

                        public class Executes {
                            public Supplier<MorphiaCursor<Object>> execute(Query<Object> query) {
                                return query::iterator;
                            }
                        }
                        """));
    }
}
