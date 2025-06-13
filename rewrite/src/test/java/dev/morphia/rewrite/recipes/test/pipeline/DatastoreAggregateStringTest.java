package dev.morphia.rewrite.recipes.test.pipeline;

import dev.morphia.rewrite.recipes.pipeline.AlternateAggregationCollection;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class DatastoreAggregateStringTest extends MorphiaRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new AlternateAggregationCollection();
    }

    @Test
    public void referenceCall() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.stages.Match.match;
                        import static dev.morphia.query.filters.Filters.eq;

                        public class DatastoreAggregateStringTest {
                            public void test(Datastore ds) {
                                ds.aggregate("myCollection")
                                  .match(eq("field", "value"))
                                  .execute(Document.class)
                                  .toList();
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.AggregationOptions;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.stages.Match.match;
                        import static dev.morphia.query.filters.Filters.eq;

                        public class DatastoreAggregateStringTest {
                            public void test(Datastore ds) {
                                ds.aggregate(Document.class, new AggregationOptions().collection("myCollection"))
                                  .match(eq("field", "value"))
                                  .execute(Document.class)
                                  .toList();
                            }
                        }
                        """));
    }

    @Test
    public void nameVariable() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.AggregationOptions;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.stages.Match.match;
                        import static dev.morphia.query.filters.Filters.eq;

                        public class DatastoreAggregateStringTest {
                            public void test(Datastore ds) {
                                String myCollection = "myCollection";
                                ds.aggregate(myCollection)
                                  .match(eq("field", "value"))
                                  .execute(Document.class)
                                  .toList();
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.AggregationOptions;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.stages.Match.match;
                        import static dev.morphia.query.filters.Filters.eq;

                        public class DatastoreAggregateStringTest {
                            public void test(Datastore ds) {
                                String myCollection = "myCollection";
                                ds.aggregate(Document.class, new AggregationOptions().collection(myCollection))
                                  .match(eq("field", "value"))
                                  .execute(Document.class)
                                  .toList();
                            }
                        }
                        """));
    }

    @Test
    public void methodCall() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.AggregationOptions;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.stages.Match.match;
                        import static dev.morphia.query.filters.Filters.eq;

                        public class DatastoreAggregateStringTest {
                            Datastore ds;
                            Datastore getDs() {
                                return ds;
                            }

                            public void test() {
                                getDs().aggregate("myCollection")
                                  .match(eq("field", "value"))
                                  .execute(Document.class)
                                  .toList();
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.AggregationOptions;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.stages.Match.match;
                        import static dev.morphia.query.filters.Filters.eq;

                        public class DatastoreAggregateStringTest {
                            Datastore ds;
                            Datastore getDs() {
                                return ds;
                            }

                            public void test() {
                                getDs().aggregate(Document.class, new AggregationOptions().collection("myCollection"))
                                  .match(eq("field", "value"))
                                  .execute(Document.class)
                                  .toList();
                            }
                        }
                        """));
    }
}