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
        rewriteRun(
                //language=java
                java(
                        """
                                import dev.morphia.aggregation.expressions.ComparisonExpressions;

                                import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
                                import static dev.morphia.aggregation.stages.Group.group;
                                import static dev.morphia.aggregation.stages.Group.id;
                                import static dev.morphia.aggregation.stages.Projection.project;
                                import static dev.morphia.aggregation.expressions.Expressions.field;
                                import static dev.morphia.aggregation.expressions.Expressions.value;
                                import static dev.morphia.aggregation.stages.Sort.sort;

                                import dev.morphia.aggregation.Aggregation;
                                import org.bson.Document;

                                public class UnwrapTest {
                                    public void update(Aggregation<?> aggregation) {
                                        aggregation
                                            .group(group(id("author")).field("count", sum(value(1))))
                                            .sort(sort().ascending("1"))
                                            .sort(sort().ascending("2"))
                                            .sort(sort().ascending("3"))
                                            .sort(sort().ascending("4"))
                                            .execute(Document.class);
                                    }
                                }
                                """,
                        """
                                import dev.morphia.aggregation.expressions.ComparisonExpressions;

                                import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
                                import static dev.morphia.aggregation.stages.Group.group;
                                import static dev.morphia.aggregation.stages.Group.id;
                                import static dev.morphia.aggregation.stages.Projection.project;
                                import static dev.morphia.aggregation.expressions.Expressions.field;
                                import static dev.morphia.aggregation.expressions.Expressions.value;
                                import static dev.morphia.aggregation.stages.Sort.sort;

                                import dev.morphia.aggregation.Aggregation;
                                import org.bson.Document;

                                public class UnwrapTest {
                                    public void update(Aggregation<?> aggregation) {
                                        aggregation
                                            .pipeline(group(id("author")).field("count", sum(value(1))),sort().ascending("1"),sort().ascending("2"),sort().ascending("3"),sort().ascending("4"))
                                            .execute(Document.class);
                                    }
                                }
                                """));
    }
}
