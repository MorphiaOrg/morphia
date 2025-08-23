package dev.morphia.rewrite.recipes.test.pipeline;

import dev.morphia.rewrite.recipes.pipeline.PipelineRewriteRecipes;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class PipelineRecipesTest extends MorphiaRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineRewriteRecipes();
    }

    @Test
    public void existingPipelineCall() {
        rewriteRun(java(
                """
                        import dev.morphia.aggregation.Aggregation;
                        import dev.morphia.aggregation.stages.Merge;

                        public class TestTheWorld {
                            public void update(Aggregation aggregation) {
                                aggregation
                                  .count("field")
                                  .merge(Merge.into("database", "collection"));
                            }
                        }""",
                """
                        import dev.morphia.aggregation.Aggregation;
                        import dev.morphia.aggregation.stages.Count;
                        import dev.morphia.aggregation.stages.Merge;

                        import static dev.morphia.aggregation.stages.Count.count;

                        public class TestTheWorld {
                            public void update(Aggregation aggregation) {
                                aggregation
                                        .pipeline(
                                                count("field"), Merge.merge("database", "collection"));
                            }
                        }"""));
    }

    @Test
    public void testUnionWith() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;

                        public class UnwrapSet {
                            public void test(Datastore ds) {
                                ds.aggregate("sales2019q1")
                                  .set(set().field("_id", literal("2019Q1")))
                                  .unionWith("sales2019q2", addFields().field("_id", literal("2019Q2")))
                                  .unionWith("sales2019q3", addFields().field("_id", literal("2019Q3")))
                                  .unionWith("sales2019q4", addFields().field("_id", literal("2019Q4")))
                                  .sort(sort().ascending("_id", "store", "item"))
                                  .execute(Document.class)
                                  .toList();
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.AggregationOptions;
                        import dev.morphia.aggregation.stages.UnionWith;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;
                        import static dev.morphia.aggregation.stages.UnionWith.unionWith;

                        public class UnwrapSet {
                            public void test(Datastore ds) {
                                ds.aggregate(Document.class,Document.class, new AggregationOptions().collection("sales2019q1"))
                                          .pipeline(
                                                  set().field("_id", literal("2019Q1")),
                                                  unionWith("sales2019q2", addFields().field("_id", literal("2019Q2"))),
                                                  unionWith("sales2019q3", addFields().field("_id", literal("2019Q3"))),
                                                  unionWith("sales2019q4", addFields().field("_id", literal("2019Q4"))),
                                                  sort().ascending("_id", "store", "item"))
                                  .iterator()
                                  .toList();
                            }
                        }
                        """));
    }

    @Test
    public void removeExecuteWithClassDefinedSource() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import java.util.List;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;

                        public class RewriteExecute {
                            public void test(Datastore ds) {
                                List<Document> list = ds.aggregate(String.class)
                                  .set(set().field("_id", literal("2019Q1")))
                                  .execute(Document.class)
                                  .toList();
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore;
                        import java.util.List;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;

                        public class RewriteExecute {
                            public void test(Datastore ds) {
                                List<Document> list = ds.aggregate(String.class,Document.class)
                                          .pipeline(
                                                  set().field("_id", literal("2019Q1")))
                                  .iterator()
                                  .toList();
                            }
                        }
                        """));
    }

    @Test
    public void removeExecuteWithClassDefinedSourceOnDatastoreImpl() {
        rewriteRun(java(
                """
                        import dev.morphia.DatastoreImpl;
                        import java.util.List;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;

                        public class RewriteExecute {
                            public void test(DatastoreImpl ds) {
                                List<Document> list = ds.aggregate(String.class)
                                  .set(set().field("_id", literal("2019Q1")))
                                  .execute(Document.class)
                                  .toList();
                            }
                        }
                        """,
                """
                        import dev.morphia.DatastoreImpl;
                        import java.util.List;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;

                        public class RewriteExecute {
                            public void test(DatastoreImpl ds) {
                                List<Document> list = ds.aggregate(String.class,Document.class)
                                          .pipeline(
                                                  set().field("_id", literal("2019Q1")))
                                  .iterator()
                                  .toList();
                            }
                        }
                        """));
    }

    @Test
    public void removeExecuteWithOptions() {
        rewriteRun(java(
                """
                        import com.mongodb.ReadConcern;
                        import dev.morphia.Datastore;
                        import org.bson.Document;
                        import dev.morphia.aggregation.AggregationOptions;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;

                        public class RewriteExecute {
                            public void test(Datastore ds) {
                                ds.aggregate(String.class)
                                  .set(set().field("_id", literal("2019Q1")))
                                  .execute(Document.class, new AggregationOptions()
                                         .readConcern(ReadConcern.LOCAL)
                                         .hint("hint"))
                                  .toList();
                            }
                        }
                        """,
                """
                        import com.mongodb.ReadConcern;
                        import dev.morphia.Datastore;
                        import org.bson.Document;
                        import dev.morphia.aggregation.AggregationOptions;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;

                        public class RewriteExecute {
                            public void test(Datastore ds) {
                                ds.aggregate(String.class,Document.class, new AggregationOptions()
                                         .readConcern(ReadConcern.LOCAL)
                                         .hint("hint"))
                                          .pipeline(
                                                  set().field("_id", literal("2019Q1")))
                                  .iterator()
                                  .toList();
                            }
                        }
                        """));
    }

    @Test
    public void removeExecuteWithOptionsAlternateCollection() {
        rewriteRun(java(
                """
                        import com.mongodb.ReadConcern;
                        import dev.morphia.Datastore;
                        import org.bson.Document;
                        import dev.morphia.aggregation.AggregationOptions;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;

                        public class RewriteExecute {
                            public void test(Datastore ds) {
                                ds.aggregate("sales2019q1")
                                  .set(set().field("_id", literal("2019Q1")))
                                  .execute(Document.class, new AggregationOptions()
                                         .readConcern(ReadConcern.LOCAL)
                                         .hint("hint"))
                                  .toList();
                            }
                        }
                        """,
                """
                        import com.mongodb.ReadConcern;
                        import dev.morphia.Datastore;
                        import org.bson.Document;
                        import dev.morphia.aggregation.AggregationOptions;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;

                        public class RewriteExecute {
                            public void test(Datastore ds) {
                                ds.aggregate(Document.class,Document.class,  new AggregationOptions().collection("sales2019q1")
                                         .readConcern(ReadConcern.LOCAL)
                                         .hint("hint"))
                                          .pipeline(
                                                  set().field("_id", literal("2019Q1")))
                                  .iterator()
                                  .toList();
                            }
                        }
                        """));
    }

}
