package dev.morphia.rewrite.recipes.test.pipeline.kotlin;

import dev.morphia.rewrite.recipes.pipeline.AlternateAggregationCollection;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinDatastoreAggregateStringTest extends MorphiaRewriteKotlinTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new AlternateAggregationCollection();
    }

    @Test
    public void referenceCall() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.Datastore
                        import org.bson.Document

                        import dev.morphia.aggregation.stages.Match.match
                        import dev.morphia.query.filters.Filters.eq

                        class DatastoreAggregateStringTest {
                            fun test(ds: Datastore) {
                                ds.aggregate("myCollection")
                                  .match(eq("field", "value"))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.AggregationOptions
                        import org.bson.Document

                        import dev.morphia.aggregation.stages.Match.match
                        import dev.morphia.query.filters.Filters.eq

                        class DatastoreAggregateStringTest {
                            fun test(ds: Datastore) {
                                ds.aggregate( AggregationOptions().collection("myCollection"))
                                  .match(eq("field", "value"))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                        }
                        """));
    }

    @Test
    @Disabled
    public void nameVariable() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.AggregationOptions
                        import org.bson.Document

                        import dev.morphia.aggregation.stages.Match.match
                        import dev.morphia.query.filters.Filters.eq

                        class DatastoreAggregateStringTest {
                            fun test(ds: Datastore) {
                                val myCollection: String = "myCollection"
                                ds.aggregate(myCollection)
                                  .match(eq("field", "value"))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.AggregationOptions
                        import org.bson.Document

                        import dev.morphia.aggregation.stages.Match.match
                        import dev.morphia.query.filters.Filters.eq

                        class DatastoreAggregateStringTest {
                            fun test(ds: Datastore) {
                                val myCollection: String = "myCollection"
                                ds.aggregate( AggregationOptions().collection(myCollection))
                                  .match(eq("field", "value"))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                        }
                        """));
    }

    @Test
    public void methodCall() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.Datastore
                        import org.bson.Document

                        import dev.morphia.aggregation.stages.Match.match
                        import dev.morphia.query.filters.Filters.eq

                        class DatastoreAggregateStringTest {
                            lateinit var ds: Datastore
                            fun getDs(): Datastore {
                                return ds
                            }

                            fun test() {
                                getDs().aggregate("myCollection")
                                  .match(eq("field", "value"))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.AggregationOptions
                        import org.bson.Document

                        import dev.morphia.aggregation.stages.Match.match
                        import dev.morphia.query.filters.Filters.eq

                        class DatastoreAggregateStringTest {
                            lateinit var ds: Datastore
                            fun getDs(): Datastore {
                                return ds
                            }

                            fun test() {
                                getDs().aggregate( AggregationOptions().collection("myCollection"))
                                  .match(eq("field", "value"))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                        }
                        """));
    }
}