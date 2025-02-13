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

    @Test
    void unwrapsValueCalls() {
        rewriteRun(
                java(
                        """
                                package dev.morphia;

                                import dev.morphia.aggregation.expressions.ComparisonExpressions;
                                import static dev.morphia.aggregation.stages.Projection.project;
                                import static dev.morphia.aggregation.expressions.Expressions.field;
                                import static dev.morphia.aggregation.expressions.Expressions.value;
                                import dev.morphia.aggregation.Aggregation;
                                import java.util.List;
                                import dev.morphia.aggregation.expressions.impls.Expression;
                                import static dev.morphia.aggregation.expressions.SetExpressions.allElementsTrue;
                                import static java.util.Arrays.asList;

                                public class UnwrapTest {
                                    public void update(Aggregation<?> aggregation) {
                                        assertAndCheckDocShape("{ $allElementsTrue: [ [ true, 1, 'someString' ] ] }",
                                            allElementsTrue(value(List.of(value(true), value(1), value("someString")))), true);
                                        assertAndCheckDocShape("{ $allElementsTrue: [ [ [ false ] ] ] }",
                                            allElementsTrue(value(List.of(List.of(false)))), true);
                                        assertAndCheckDocShape("{ $allElementsTrue: [ [ ] ] }",
                                            allElementsTrue(value(List.of())), true);
                                        assertAndCheckDocShape("{ $allElementsTrue: [ [ null, false, 0 ] ] }",
                                            allElementsTrue(value(asList(null, false, 0))), false);
                                    }

                                    protected void assertAndCheckDocShape(String expectedString, Expression value, Object expectedValue) {
                                    }
                                }
                                """,
                        """
                                package dev.morphia;

                                import dev.morphia.aggregation.expressions.ComparisonExpressions;
                                import static dev.morphia.aggregation.stages.Projection.project;
                                import dev.morphia.aggregation.Aggregation;
                                import java.util.List;
                                import dev.morphia.aggregation.expressions.impls.Expression;
                                import static dev.morphia.aggregation.expressions.SetExpressions.allElementsTrue;
                                import static java.util.Arrays.asList;

                                public class UnwrapTest {
                                    public void update(Aggregation<?> aggregation) {
                                        assertAndCheckDocShape("{ $allElementsTrue: [ [ true, 1, 'someString' ] ] }",
                                            allElementsTrue(List.of(true, 1, "someString")), true);
                                        assertAndCheckDocShape("{ $allElementsTrue: [ [ [ false ] ] ] }",
                                            allElementsTrue(List.of(List.of(false))), true);
                                        assertAndCheckDocShape("{ $allElementsTrue: [ [ ] ] }",
                                            allElementsTrue(List.of()), true);
                                        assertAndCheckDocShape("{ $allElementsTrue: [ [ null, false, 0 ] ] }",
                                            allElementsTrue(asList(null, false, 0)), false);
                                    }

                                    protected void assertAndCheckDocShape(String expectedString, Expression value, Object expectedValue) {
                                    }
                                }
                                """));
    }
}