package dev.morphia.rewrite.recipes.test.kotlin;

import java.util.List;

import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

/**
 * Covers the moved and renamed expression helpers, nested expression types, and annotations declared directly in
 * {@code dev.morphia.UpgradeToMorphia30}.
 */
public class KotlinExpressionAndTypeMovesTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        // rewrite-maven isn't on the test classpath so the maven recipes in the list can't load. Run everything else.
        Recipe upgrade = RewriteTest.fromRuntimeClasspath("dev.morphia.UpgradeToMorphia30");
        List<Recipe> recipes = upgrade.getRecipeList().stream()
                .filter(recipe -> recipe.validate().isValid())
                .toList();
        return new CompositeRecipe(recipes);
    }

    @Test
    void stdDevQualified() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.AccumulatorExpressions
                        import dev.morphia.aggregation.expressions.impls.Expression

                        class Upgrades {
                            fun pop(a: Expression): Expression {
                                return AccumulatorExpressions.stdDevPop(a)
                            }

                            fun samp(a: Expression, b: Expression): Expression {
                                return AccumulatorExpressions.stdDevSamp(a, b)
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.WindowExpressions
                        import dev.morphia.aggregation.expressions.impls.Expression

                        class Upgrades {
                            fun pop(a: Expression): Expression {
                                return WindowExpressions.stdDevPop(a)
                            }

                            fun samp(a: Expression, b: Expression): Expression {
                                return WindowExpressions.stdDevSamp(a, b)
                            }
                        }
                        """));
    }

    @Test
    void substr() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.StringExpressions
                        import dev.morphia.aggregation.expressions.impls.Expression

                        class Upgrades {
                            fun qualified(a: Expression): Expression {
                                return StringExpressions.substr(a, 1, 2)
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.StringExpressions
                        import dev.morphia.aggregation.expressions.impls.Expression

                        class Upgrades {
                            fun qualified(a: Expression): Expression {
                                return StringExpressions.substrBytes(a, 1, 2)
                            }
                        }
                        """));
    }

    @Test
    void logicalExpressionImported() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.BooleanExpressions
                        import dev.morphia.aggregation.expressions.BooleanExpressions.LogicalExpression
                        import dev.morphia.aggregation.expressions.impls.Expression

                        class Upgrades {
                            fun both(a: Expression, b: Expression): LogicalExpression {
                                val expression: LogicalExpression = BooleanExpressions.and(a)
                                return expression.add(b)
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.BooleanExpressions
                        import dev.morphia.aggregation.expressions.impls.Expression
                        import dev.morphia.aggregation.expressions.impls.LogicalExpression

                        class Upgrades {
                            fun both(a: Expression, b: Expression): LogicalExpression {
                                val expression: LogicalExpression = BooleanExpressions.and(a)
                                return expression.add(b)
                            }
                        }
                        """));
    }

    @Test
    void mergeObjects() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.ObjectExpressions
                        import dev.morphia.aggregation.expressions.ObjectExpressions.MergeObjects
                        import dev.morphia.aggregation.expressions.impls.Expression

                        class Upgrades {
                            fun merge(a: Expression): MergeObjects {
                                val merge: MergeObjects = ObjectExpressions.mergeObjects()
                                return merge.add(a)
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.ObjectExpressions
                        import dev.morphia.aggregation.expressions.impls.Expression
                        import dev.morphia.aggregation.expressions.impls.MergeObjects

                        class Upgrades {
                            fun merge(a: Expression): MergeObjects {
                                val merge: MergeObjects = ObjectExpressions.mergeObjects()
                                return merge.add(a)
                            }
                        }
                        """));
    }

    @Test
    void notSaved() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity
                        import dev.morphia.annotations.NotSaved

                        @Entity
                        class Upgrades {
                            @NotSaved
                            var computed: String? = null
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity
                        import dev.morphia.annotations.LoadOnly

                        @Entity
                        class Upgrades {
                            @LoadOnly
                            var computed: String? = null
                        }
                        """));
    }

    @Test
    void experimentalConstructor() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity
                        import dev.morphia.annotations.experimental.Constructor

                        @Entity
                        class Upgrades @Constructor constructor(val name: String)
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        @Entity
                        class Upgrades constructor(val name: String)
                        """));
    }

    @Test
    void stdDevStaticImports() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.AccumulatorExpressions.stdDevPop
                        import dev.morphia.aggregation.expressions.AccumulatorExpressions.stdDevSamp
                        import dev.morphia.aggregation.expressions.impls.Expression

                        class Upgrades {
                            fun pop(a: Expression): Expression {
                                return stdDevPop(a)
                            }

                            fun samp(a: Expression, b: Expression): Expression {
                                return stdDevSamp(a, b)
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.WindowExpressions.stdDevPop
                        import dev.morphia.aggregation.expressions.WindowExpressions.stdDevSamp
                        import dev.morphia.aggregation.expressions.impls.Expression

                        class Upgrades {
                            fun pop(a: Expression): Expression {
                                return stdDevPop(a)
                            }

                            fun samp(a: Expression, b: Expression): Expression {
                                return stdDevSamp(a, b)
                            }
                        }
                        """));
    }

    @Test
    void logicalExpressionQualified() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.BooleanExpressions
                        import dev.morphia.aggregation.expressions.impls.Expression

                        class Upgrades {
                            fun either(a: Expression, b: Expression): BooleanExpressions.LogicalExpression {
                                return BooleanExpressions.or(a, b)
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.BooleanExpressions
                        import dev.morphia.aggregation.expressions.impls.Expression
                        import dev.morphia.aggregation.expressions.impls.LogicalExpression

                        class Upgrades {
                            fun either(a: Expression, b: Expression): LogicalExpression {
                                return BooleanExpressions.or(a, b)
                            }
                        }
                        """));
    }

    @Test
    void expressionCodec() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.aggregation.codecs.ExpressionCodec

                        class Upgrades {
                            var codec: ExpressionCodec<*>? = null
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.mapping.codec.expressions.ExpressionCodec

                        class Upgrades {
                            var codec: ExpressionCodec<*>? = null
                        }
                        """));
    }

    @Test
    void customRecipesAreRegistered() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.FindAndModifyOptions
                        import dev.morphia.query.Meta

                        class Upgrades {
                            fun options(): FindAndModifyOptions {
                                return FindAndModifyOptions().returnNew(true)
                            }

                            fun field(): String {
                                return Meta("textScore", "score").getField()
                            }
                        }
                        """,
                //language=kotlin
                """
                        import com.mongodb.client.model.ReturnDocument
                        import dev.morphia.ModifyOptions
                        import dev.morphia.query.Meta

                        class Upgrades {
                            fun options(): ModifyOptions {
                                return ModifyOptions().returnDocument(ReturnDocument.AFTER)
                            }

                            fun field(): String {
                                return Meta.textScore("score").field()
                            }
                        }
                        """));
    }
}
