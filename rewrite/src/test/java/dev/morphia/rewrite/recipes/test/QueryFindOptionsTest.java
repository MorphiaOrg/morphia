package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.QueryIteratorOptions;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class QueryFindOptionsTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new QueryIteratorOptions();
    }

    @Test
    public void testWithTryNext() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;
                        import org.bson.types.ObjectId;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Updates {
                            public void doUpdate(Datastore ds) {
                                ds.find(Object.class)
                                  .filter(eq("_id", ObjectId.get()))
                                  .iterator(new FindOptions().limit(1))
                                  .tryNext();
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;
                        import org.bson.types.ObjectId;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Updates {
                            public void doUpdate(Datastore ds) {
                                ds.find(Object.class, new FindOptions().limit(1))
                                        .filter(eq("_id", ObjectId.get()))
                                        .iterator()
                                        .tryNext();
                            }
                        }
                        """));

    }

    @Test
    public void testWithIteratorNoFilter() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;
                        import org.bson.types.ObjectId;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Updates {
                            Datastore datastore;
                            public Datastore getDs() {
                                return datastore;
                            }

                            public void doUpdate(Datastore ds) {
                                getDs().find(Object.class)
                                    .iterator(new FindOptions().limit(1))
                                    .next();
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;
                        import org.bson.types.ObjectId;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Updates {
                            Datastore datastore;
                            public Datastore getDs() {
                                return datastore;
                            }

                            public void doUpdate(Datastore ds) {
                                getDs().find(Object.class, new FindOptions().limit(1))
                                        .iterator()
                                        .next();
                            }
                        }
                        """));

    }

    @Test
    public void testWithIteratorWithOptions() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;
                        import org.bson.types.ObjectId;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Updates {
                            public void doUpdate(Datastore ds) {
                                ds.find(Object.class)
                                  .filter(eq("_id", ObjectId.get()))
                                  .iterator(new FindOptions().limit(1));
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;
                        import org.bson.types.ObjectId;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Updates {
                            public void doUpdate(Datastore ds) {
                                ds.find(Object.class, new FindOptions().limit(1))
                                        .filter(eq("_id", ObjectId.get()))
                                        .iterator();
                            }
                        }
                        """));

    }

    @Test
    public void testWithIteratorNoOptions() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;
                        import org.bson.types.ObjectId;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Updates {
                            public void doUpdate(Datastore ds) {
                                ds.find(Object.class)
                                  .filter(eq("_id", ObjectId.get()))
                                  .iterator();
                            }
                        }
                        """));

    }

}
