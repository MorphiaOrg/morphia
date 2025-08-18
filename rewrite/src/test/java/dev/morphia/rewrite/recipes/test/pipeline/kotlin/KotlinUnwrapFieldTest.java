package dev.morphia.rewrite.recipes.test.pipeline.kotlin;

import dev.morphia.rewrite.recipes.pipeline.UnwrapFieldExpressions;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinUnwrapFieldTest extends KotlinRewriteTest {
    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new UnwrapFieldExpressions();
    }

    @Test
    void unwrapsFieldCalls() {
        rewriteRun(
                kotlin(
                        //language=kotlin
                        """
                                    package dev.morphia

                                    import dev.morphia.aggregation.expressions.ComparisonExpressions
                                    import dev.morphia.aggregation.stages.Projection.project
                                    import dev.morphia.aggregation.expressions.Expressions.field
                                    import dev.morphia.aggregation.expressions.Expressions.value
                                    import dev.morphia.aggregation.Aggregation

                                    class UnwrapTest {
                                        fun something() {
                                        }

                                       fun update(aggregation: Aggregation<*>) {
                                            aggregation
                                                .project(project()
                                                    .suppressId()
                                                    .include("item")
                                                    .include("qty")
                                                    .include("qtyGte250", ComparisonExpressions.gte(field("${'$'}qty"), value(250))))
                                       }
                                    }
                                """,
                        //language=kotlin
                        """
                                    package dev.morphia

                                    import dev.morphia.aggregation.expressions.ComparisonExpressions
                                    import dev.morphia.aggregation.stages.Projection.project
                                    import dev.morphia.aggregation.Aggregation

                                    class UnwrapTest {
                                        fun something() {
                                        }

                                       fun update(aggregation: Aggregation<*>) {
                                            aggregation
                                                .project(project()
                                                    .suppressId()
                                                    .include("item")
                                                    .include("qty")
                                                    .include("qtyGte250", ComparisonExpressions.gte("${'$'}qty",250)))
                                       }
                                    }
                                """));
    }

    @Test
    void unwrapsValueCalls() {
        rewriteRun(
                kotlin(
                        //language=kotlin
                        """
                                package dev.morphia

                                import dev.morphia.aggregation.expressions.ComparisonExpressions
                                import dev.morphia.aggregation.stages.Projection.project
                                import dev.morphia.aggregation.expressions.Expressions.field
                                import dev.morphia.aggregation.expressions.Expressions.value
                                import dev.morphia.aggregation.Aggregation
                                import dev.morphia.aggregation.expressions.impls.Expression
                                import dev.morphia.aggregation.expressions.SetExpressions.allElementsTrue
                                import java.util.Arrays.asList

                                class UnwrapTest {
                                    fun update(aggregation: Aggregation<*>) {
                                        assertAndCheckDocShape("{ ${'$'}allElementsTrue: [ [ true, 1, 'someString' ] ] }",
                                            allElementsTrue(value(listOf(value(true), value(1), value("someString")))), true)
                                        assertAndCheckDocShape("{ ${'$'}allElementsTrue: [ [ [ false ] ] ] }",
                                            allElementsTrue(value(listOf(listOf(false)))), true)
                                        assertAndCheckDocShape("{ ${'$'}allElementsTrue: [ [ ] ] }",
                                            allElementsTrue(value(listOf<Any>())), true)
                                        assertAndCheckDocShape("{ ${'$'}allElementsTrue: [ [ null, false, 0 ] ] }",
                                            allElementsTrue(value(asList(null, false, 0))), false)
                                    }

                                    protected fun assertAndCheckDocShape(expectedString: String, value: Expression, expectedValue: Any) {
                                    }
                                }
                                """,
                        //language=kotlin
                        """
                                package dev.morphia

                                import dev.morphia.aggregation.expressions.ComparisonExpressions
                                import dev.morphia.aggregation.stages.Projection.project
                                import dev.morphia.aggregation.Aggregation
                                import dev.morphia.aggregation.expressions.impls.Expression
                                import dev.morphia.aggregation.expressions.SetExpressions.allElementsTrue
                                import java.util.Arrays.asList

                                class UnwrapTest {
                                    fun update(aggregation: Aggregation<*>) {
                                        assertAndCheckDocShape("{ ${'$'}allElementsTrue: [ [ true, 1, 'someString' ] ] }",
                                            allElementsTrue(listOf(true, 1, "someString")), true)
                                        assertAndCheckDocShape("{ ${'$'}allElementsTrue: [ [ [ false ] ] ] }",
                                            allElementsTrue(listOf(listOf(false))), true)
                                        assertAndCheckDocShape("{ ${'$'}allElementsTrue: [ [ ] ] }",
                                            allElementsTrue(listOf<Any>()), true)
                                        assertAndCheckDocShape("{ ${'$'}allElementsTrue: [ [ null, false, 0 ] ] }",
                                            allElementsTrue(asList(null, false, 0)), false)
                                    }

                                    protected fun assertAndCheckDocShape(expectedString: String, value: Expression, expectedValue: Any) {
                                    }
                                }
                                """));
    }
}