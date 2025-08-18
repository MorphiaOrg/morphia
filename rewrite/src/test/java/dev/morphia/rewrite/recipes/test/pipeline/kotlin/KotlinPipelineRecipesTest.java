package dev.morphia.rewrite.recipes.test.pipeline.kotlin;

import dev.morphia.rewrite.recipes.PipelineRewriteRecipes;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinPipelineRecipesTest extends KotlinRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineRewriteRecipes();
    }

    @Test
    public void existingPipelineCall() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.aggregation.Aggregation
                        import dev.morphia.aggregation.stages.Merge

                        class TestTheWorld {
                            fun update(aggregation: Aggregation<*>) {
                                aggregation
                                  .count("field")
                                  .merge(Merge.into("database", "collection"))
                            }
                        }""",
                """
                        import dev.morphia.aggregation.Aggregation
                        import dev.morphia.aggregation.stages.Count
                        import dev.morphia.aggregation.stages.Count.count
                        import dev.morphia.aggregation.stages.Merge
                        import dev.morphia.aggregation.stages.Merge.merge

                        class TestTheWorld {
                            fun update(aggregation: Aggregation<*>) {
                                aggregation
                                    .pipeline(
                                        count("field"), Merge.merge("database", "collection"))
                            }
                        }"""));
    }

    @Test
    public void testUnionWith() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith
                        import org.bson.Document

                        class UnwrapSet {
                            fun test(ds: Datastore) {
                                ds.aggregate("sales2019q1")
                                  .set(set().field("_id", literal("2019Q1")))
                                  .unionWith("sales2019q2", addFields().field("_id", literal("2019Q2")))
                                  .unionWith("sales2019q3", addFields().field("_id", literal("2019Q3")))
                                  .unionWith("sales2019q4", addFields().field("_id", literal("2019Q4")))
                                  .sort(sort().ascending("_id", "store", "item"))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.AggregationOptions
                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith
                        import dev.morphia.aggregation.stages.UnionWith.unionWith
                        import org.bson.Document

                        class UnwrapSet {
                            fun test(ds: Datastore) {
                                ds.aggregate(Document::class.java,Document::class.java, AggregationOptions().collection("sales2019q1"))
                                      .pipeline(
                                          set().field("_id", literal("2019Q1")),
                                          unionWith("sales2019q2", addFields().field("_id", literal("2019Q2"))),
                                          unionWith("sales2019q3", addFields().field("_id", literal("2019Q3"))),
                                          unionWith("sales2019q4", addFields().field("_id", literal("2019Q4"))),
                                          sort().ascending("_id", "store", "item"))
                                  .iterator()
                                  .toList()
                            }
                        }
                        """));
    }

    @Test
    public void removeExecuteWithClassDefinedSource() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.Datastore
                        import org.bson.Document

                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith

                        class RewriteExecute {
                            fun test(ds: Datastore) {
                                val list: List<Document> = ds.aggregate(String::class.java)
                                  .set(set().field("_id", literal("2019Q1")))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore
                        import org.bson.Document

                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith

                        class RewriteExecute {
                            fun test(ds: Datastore) {
                                val list: List<Document> = ds.aggregate(String::class.java,Document::class.java)
                                      .pipeline(
                                          set().field("_id", literal("2019Q1")))
                                  .iterator()
                                  .toList()
                            }
                        }
                        """));
    }

    @Test
    public void removeExecuteWithClassDefinedSourceOnDatastoreImpl() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.DatastoreImpl
                        import org.bson.Document

                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith

                        class RewriteExecute {
                            fun test(ds: DatastoreImpl) {
                                val list: List<Document> = ds.aggregate(String::class.java)
                                  .set(set().field("_id", literal("2019Q1")))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                        }
                        """,
                """
                        import dev.morphia.DatastoreImpl
                        import org.bson.Document

                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith

                        class RewriteExecute {
                            fun test(ds: DatastoreImpl) {
                                val list: List<Document> = ds.aggregate(String::class.java,Document::class.java)
                                      .pipeline(
                                          set().field("_id", literal("2019Q1")))
                                  .iterator()
                                  .toList()
                            }
                        }
                        """));
    }

    @Test
    public void removeExecuteWithOptions() {
        rewriteRun(kotlin(
                """
                        import com.mongodb.ReadConcern
                        import dev.morphia.Datastore
                        import org.bson.Document
                        import dev.morphia.aggregation.AggregationOptions

                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith

                        class RewriteExecute {
                            fun test(ds: Datastore) {
                                ds.aggregate(String::class.java)
                                  .set(set().field("_id", literal("2019Q1")))
                                  .execute(Document::class.java, AggregationOptions()
                                         .readConcern(ReadConcern.LOCAL)
                                         .hint("hint"))
                                  .toList()
                            }
                        }
                        """,
                """
                        import com.mongodb.ReadConcern
                        import dev.morphia.Datastore
                        import org.bson.Document
                        import dev.morphia.aggregation.AggregationOptions

                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith

                        class RewriteExecute {
                            fun test(ds: Datastore) {
                                ds.aggregate(String::class.java,Document::class.java, AggregationOptions()
                                         .readConcern(ReadConcern.LOCAL)
                                         .hint("hint"))
                                      .pipeline(
                                          set().field("_id", literal("2019Q1")))
                                  .iterator()
                                  .toList()
                            }
                        }
                        """));
    }

    @Test
    public void removeExecuteWithOptionsAlternateCollection() {
        rewriteRun(kotlin(
                """
                        import com.mongodb.ReadConcern
                        import dev.morphia.Datastore
                        import org.bson.Document
                        import dev.morphia.aggregation.AggregationOptions

                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith

                        class RewriteExecute {
                            fun test(ds: Datastore) {
                                ds.aggregate("sales2019q1")
                                      .set(set().field("_id", literal("2019Q1")))
                                  .execute(Document::class.java, AggregationOptions()
                                         .readConcern(ReadConcern.LOCAL)
                                         .hint("hint"))
                                  .toList()
                            }
                        }
                        """,
                """
                        import com.mongodb.ReadConcern
                        import dev.morphia.Datastore
                        import org.bson.Document
                        import dev.morphia.aggregation.AggregationOptions

                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith

                        class RewriteExecute {
                            fun test(ds: Datastore) {
                                ds.aggregate(Document::class.java,Document::class.java,  AggregationOptions().collection("sales2019q1")
                                         .readConcern(ReadConcern.LOCAL)
                                         .hint("hint"))
                                      .pipeline(
                                          set().field("_id", literal("2019Q1")))
                                  .iterator()
                                  .toList()
                            }
                        }
                        """));
    }

}