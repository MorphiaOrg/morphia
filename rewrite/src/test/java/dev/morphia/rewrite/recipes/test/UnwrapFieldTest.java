package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.UnwrapFieldExpressions;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class UnwrapFieldTest extends MorphiaRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new UnwrapFieldExpressions();
    }

    @Test
    void unwrapsFieldCalls() {
        rewriteRun(
                java(
                        """
                                    package dev.morphia;

                                    import dev.morphia.aggregation.expressions.ComparisonExpressions;
                                    import static dev.morphia.aggregation.stages.Projection.project;
                                    import static dev.morphia.aggregation.expressions.Expressions.field;
                                    import static dev.morphia.aggregation.expressions.Expressions.value;
                                    import dev.morphia.aggregation.Aggregation;

                                    public class UnwrapTest {
                                        public void something() {
                                        }

                                       public void update(Aggregation<?> aggregation) {
                                            aggregation
                                                .project(project()
                                                    .suppressId()
                                                    .include("item")
                                                    .include("qty")
                                                    .include("qtyGte250", ComparisonExpressions.gte(field("$qty"), value(250))));
                                       }
                                    }
                                """,
                        """
                                    package dev.morphia;

                                    import dev.morphia.aggregation.expressions.ComparisonExpressions;
                                    import static dev.morphia.aggregation.stages.Projection.project;
                                    import static dev.morphia.aggregation.expressions.Expressions.field;
                                    import static dev.morphia.aggregation.expressions.Expressions.value;
                                    import dev.morphia.aggregation.Aggregation;

                                    public class UnwrapTest {
                                        public void something() {
                                        }

                                       public void update(Aggregation<?> aggregation) {
                                            aggregation
                                                .project(project()
                                                    .suppressId()
                                                    .include("item")
                                                    .include("qty")
                                                    .include("qtyGte250", ComparisonExpressions.gte("$qty",250)));
                                       }
                                    }
                                """));
    }
}