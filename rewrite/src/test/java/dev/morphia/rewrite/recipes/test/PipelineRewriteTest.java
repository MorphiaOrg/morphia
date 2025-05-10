package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.PipelineRewrite;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.java.Assertions.java;

public class PipelineRewriteTest extends MorphiaRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineRewrite();
    }

    @Override
    public void defaults(RecipeSpec spec) {
        super.defaults(spec);
        spec.expectedCyclesThatMakeChanges(2);
    }

    @Test
    void unwrapStageMethods() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.aggregation.expressions.ComparisonExpressions;

                        import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.Group.group;
                        import static dev.morphia.aggregation.stages.Group.id;
                        import static dev.morphia.aggregation.stages.Projection.project;
                        import static dev.morphia.aggregation.expressions.Expressions.field;
                        import static dev.morphia.aggregation.expressions.Expressions.value;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.query.filters.Filters.eq;

                        import dev.morphia.aggregation.Aggregation;
                        import dev.morphia.query.MorphiaCursor;
                        import org.bson.Document;

                        public class UnwrapTest {
                            public MorphiaCursor<Document> update(Aggregation<?> aggregation) {
                                return aggregation
                                    .match(eq("author", "Sanderson"))
                                    .group(group(id("author")).field("count", sum(value(1))))
                                    .set(set().field("_id", literal("2019Q1")))
                                    .sort(sort().ascending("1"))
                                    .sort(sort().ascending("2"))
                                    .sort(sort().ascending("3"))
                                    .sort(sort().ascending("4"))
                                    .execute(Document.class);
                            }
                        }""",
                //language=java
                """
                        import dev.morphia.aggregation.expressions.ComparisonExpressions;
                        import dev.morphia.aggregation.stages.Match;

                        import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.Group.group;
                        import static dev.morphia.aggregation.stages.Group.id;
                        import static dev.morphia.aggregation.stages.Match.match;
                        import static dev.morphia.aggregation.stages.Projection.project;
                        import static dev.morphia.aggregation.expressions.Expressions.field;
                        import static dev.morphia.aggregation.expressions.Expressions.value;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.query.filters.Filters.eq;

                        import dev.morphia.aggregation.Aggregation;
                        import dev.morphia.query.MorphiaCursor;
                        import org.bson.Document;

                        public class UnwrapTest {
                            public MorphiaCursor<Document> update(Aggregation<?> aggregation) {
                                return aggregation
                                        .pipeline(
                                                match(eq("author", "Sanderson")),
                                                group(id("author")).field("count", sum(value(1))),
                                                set().field("_id", literal("2019Q1")),
                                                sort().ascending("1"),
                                                sort().ascending("2"),
                                                sort().ascending("3"),
                                                sort().ascending("4"))
                                    .execute(Document.class);
                            }
                        }""".replace('#', ' ')));
    }

    @Test
    public void testMatch() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.MorphiaCursor;
                        import dev.morphia.query.filters.Filters;
                        import org.bson.Document;

                        public class UnwrapTest {
                          public MorphiaCursor<Document> update(Datastore ds) {
                              Object e2 = ds.aggregate(Object.class)
                                      .match(Filters.eq("reference", "ec"))
                                      .execute(Object.class)
                                      .tryNext();
                          }
                        }
                        """,
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.stages.Match;
                        import dev.morphia.query.MorphiaCursor;
                        import dev.morphia.query.filters.Filters;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.stages.Match.match;

                        public class UnwrapTest {
                          public MorphiaCursor<Document> update(Datastore ds) {
                              Object e2 = ds.aggregate(Object.class)
                                              .pipeline(
                                                      match(Filters.eq("reference", "ec")))
                                      .execute(Object.class)
                                      .tryNext();
                          }
                        }
                        """));
    }

    @Test
    public void testSet() {
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
                                ds.aggregate("sales2019q1")
                                          .pipeline(
                                                  set().field("_id", literal("2019Q1")),
                                                  unionWith("sales2019q2", addFields().field("_id", literal("2019Q2"))),
                                                  unionWith("sales2019q3", addFields().field("_id", literal("2019Q3"))),
                                                  unionWith("sales2019q4", addFields().field("_id", literal("2019Q4"))),
                                                  sort().ascending("_id", "store", "item"))
                                  .execute(Document.class)
                                  .toList();
                            }
                        }
                        """));
    }

    @Test
    public void testSkip() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.MorphiaCursor;
                        import dev.morphia.query.filters.Filters;
                        import org.bson.Document;

                        public class UnwrapTest {
                          public MorphiaCursor<Document> update(Datastore ds) {
                              Object e2 = ds.aggregate(Object.class)
                                      .skip(42)
                                      .execute(Object.class)
                                      .tryNext();
                          }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.stages.Skip;
                        import dev.morphia.query.MorphiaCursor;
                        import dev.morphia.query.filters.Filters;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.stages.Skip.skip;

                        public class UnwrapTest {
                          public MorphiaCursor<Document> update(Datastore ds) {
                              Object e2 = ds.aggregate(Object.class)
                                              .pipeline(
                                                      skip(42))
                                      .execute(Object.class)
                                      .tryNext();
                          }
                        }
                        """.replace('#', ' ')));
    }

    @Test
    public void testSortByCount() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.MorphiaCursor;
                        import dev.morphia.query.filters.Filters;
                        import org.bson.Document;
                        import static dev.morphia.aggregation.expressions.Expressions.field;

                        public class UnwrapTest {
                          public MorphiaCursor<Document> update(Datastore ds) {
                              Object e2 = ds.aggregate(Object.class)
                                      .sortByCount(field("docs"))
                                      .execute(Object.class)
                                      .tryNext();
                          }
                        }
                        """,
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.stages.SortByCount;
                        import dev.morphia.query.MorphiaCursor;
                        import dev.morphia.query.filters.Filters;
                        import org.bson.Document;
                        import static dev.morphia.aggregation.expressions.Expressions.field;
                        import static dev.morphia.aggregation.stages.SortByCount.sortByCount;

                        public class UnwrapTest {
                          public MorphiaCursor<Document> update(Datastore ds) {
                              Object e2 = ds.aggregate(Object.class)
                                              .pipeline(
                                                      sortByCount(field("docs")))
                                      .execute(Object.class)
                                      .tryNext();
                          }
                        }
                        """.replace('#', ' ')));
    }
}
