package dev.morphia.rewrite.recipes.test.pipeline.kotlin;

import dev.morphia.rewrite.recipes.PipelineRewriteRecipes;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinDatastoreAggregateStringTest extends MorphiaRewriteKotlinTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineRewriteRecipes();
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
                                ds.aggregate(Document::class.java, AggregationOptions().collection("myCollection"))
                                  .match(eq("field", "value"))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                        }
                        """));
    }

    @Test
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
                                val myCollection = "myCollection"
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
                                val myCollection = "myCollection"
                                ds.aggregate(Document::class.java, AggregationOptions().collection(myCollection))
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
                        import dev.morphia.aggregation.stages.Match
                        import org.bson.Document

                        import dev.morphia.aggregation.stages.Match.match
                        import dev.morphia.query.filters.Filters.eq

                        class DatastoreAggregateStringTest {
                            lateinit var ds: Datastore
                            fun getDs(): Datastore {
                                return ds
                            }

                            fun test() {
                                getDs().aggregate(Document::class.java,Document::class.java, AggregationOptions().collection("myCollection"))
                                      .pipeline(
                                          match(eq("field", "value")))
                                  .iterator()
                                  .toList()
                            }
                        }
                        """));
    }
}