package dev.morphia.rewrite.recipes.test.query;

import dev.morphia.rewrite.recipes.query.QueryFindOptions;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class FirstWithOptionsTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new QueryFindOptions();
    }

    @Test
    public void testWithFirstNoFilter() {
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
                                    .first(new FindOptions().limit(1));
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
                                        .first();
                            }
                        }
                        """));

    }

    @Test
    public void testWithFirstWithOptions() {
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
                                  .first(new FindOptions().limit(1));
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
                                        .first();
                            }
                        }
                        """));

    }

    @Test
    public void testWithFirstNoOptions() {
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
                                  .first();
                            }
                        }
                        """));

    }
}
