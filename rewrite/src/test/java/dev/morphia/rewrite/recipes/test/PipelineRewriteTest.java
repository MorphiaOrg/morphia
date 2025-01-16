package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.PipelineRewrite;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class PipelineRewriteTest extends MorphiaRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineRewrite();
    }

    @Test
    void unwrapStageMethods() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.aggregation.expressions.ComparisonExpressions;

                        import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
                        import static dev.morphia.aggregation.stages.Group.group;
                        import static dev.morphia.aggregation.stages.Group.id;
                        import static dev.morphia.aggregation.stages.Projection.project;
                        import static dev.morphia.aggregation.expressions.Expressions.field;
                        import static dev.morphia.aggregation.expressions.Expressions.value;
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

                        import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
                        import static dev.morphia.aggregation.stages.Group.group;
                        import static dev.morphia.aggregation.stages.Group.id;
                        import static dev.morphia.aggregation.stages.Match.match;
                        import static dev.morphia.aggregation.stages.Projection.project;
                        import static dev.morphia.aggregation.expressions.Expressions.field;
                        import static dev.morphia.aggregation.expressions.Expressions.value;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.query.filters.Filters.eq;

                        import dev.morphia.aggregation.Aggregation;
                        import dev.morphia.query.MorphiaCursor;
                        import org.bson.Document;

                        public class UnwrapTest {
                            public MorphiaCursor<Document> update(Aggregation<?> aggregation) {
                                return#
                                        aggregation
                                                .pipeline(
                                                        match(eq("author", "Sanderson")),
                                                        group(id("author")).field("count", sum(value(1))),
                                                        sort().ascending("1"),
                                                        sort().ascending("2"),
                                                        sort().ascending("3"),
                                                        sort().ascending("4"))
                                    .execute(Document.class);
                            }
                        }""".replace('#', ' ')));
    }

    @Test
    public void testWhitespace() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.aggregation.Aggregation;

                        import static dev.morphia.aggregation.expressions.AccumulatorExpressions.top;
                        import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
                        import static dev.morphia.aggregation.expressions.Expressions.field;
                        import static dev.morphia.aggregation.stages.Group.group;
                        import static dev.morphia.aggregation.stages.Group.id;
                        import static dev.morphia.query.Sort.descending;
                        import static dev.morphia.query.filters.Filters.eq;

                        public class UnwrapTest {
                            Aggregation<?> aggregation;
                            public Aggregation<?> testWhitespace() {
                                return aggregation
                                           .match(eq("gameId", "G1"))
                                           .group(group(id(field("gameId")))
                                                      .field("playerId", top(
                                                          array(field("playerId"), field("score")),
                                                          descending("score"))));
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.aggregation.Aggregation;

                        import static dev.morphia.aggregation.expressions.AccumulatorExpressions.top;
                        import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
                        import static dev.morphia.aggregation.expressions.Expressions.field;
                        import static dev.morphia.aggregation.stages.Group.group;
                        import static dev.morphia.aggregation.stages.Group.id;
                        import static dev.morphia.aggregation.stages.Match.match;
                        import static dev.morphia.query.Sort.descending;
                        import static dev.morphia.query.filters.Filters.eq;

                        public class UnwrapTest {
                            Aggregation<?> aggregation;
                            public Aggregation<?> testWhitespace() {
                                return
                                        aggregation
                                                .pipeline(
                                                        match(eq("gameId", "G1")),
                                                        group(id(field("gameId")))
                                                                .field("playerId", top(
                                                                        array(field("playerId"), field("score")),
                                                                        descending("score"))));
                            }
                        }"""));
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
                        import dev.morphia.query.MorphiaCursor;
                        import dev.morphia.query.filters.Filters;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.stages.Match.match;

                        public class UnwrapTest {
                          public MorphiaCursor<Document> update(Datastore ds) {
                              Object e2 =#
                                              ds.aggregate(Object.class)
                                                      .pipeline(
                                                              match(Filters.eq("reference", "ec")))
                                      .execute(Object.class)
                                      .tryNext();
                          }
                        }
                        """.replace('#', ' ')));
    }
}
