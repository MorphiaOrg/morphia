package dev.morphia.rewrite.recipes.test;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/**
 * Covers the moved and renamed expression helpers, nested expression types, and annotations declared directly in
 * {@code dev.morphia.UpgradeToMorphia30}.
 */
public class ExpressionAndTypeMovesTest extends MorphiaRewriteTest {
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
    void stdDevStaticImports() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.aggregation.expressions.impls.Expression;

                        import static dev.morphia.aggregation.expressions.AccumulatorExpressions.stdDevPop;
                        import static dev.morphia.aggregation.expressions.AccumulatorExpressions.stdDevSamp;

                        public class Upgrades {
                            public Expression pop(Expression a) {
                                return stdDevPop(a);
                            }

                            public Expression samp(Expression a, Expression b) {
                                return stdDevSamp(a, b);
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.aggregation.expressions.impls.Expression;

                        import static dev.morphia.aggregation.expressions.WindowExpressions.stdDevPop;
                        import static dev.morphia.aggregation.expressions.WindowExpressions.stdDevSamp;

                        public class Upgrades {
                            public Expression pop(Expression a) {
                                return stdDevPop(a);
                            }

                            public Expression samp(Expression a, Expression b) {
                                return stdDevSamp(a, b);
                            }
                        }
                        """));
    }

    @Test
    void stdDevQualified() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.aggregation.expressions.AccumulatorExpressions;
                        import dev.morphia.aggregation.expressions.impls.Expression;

                        public class Upgrades {
                            public Expression pop(Expression a) {
                                return AccumulatorExpressions.stdDevPop(a);
                            }

                            public Expression samp(Expression a, Expression b) {
                                return AccumulatorExpressions.stdDevSamp(a, b);
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.aggregation.expressions.WindowExpressions;
                        import dev.morphia.aggregation.expressions.impls.Expression;

                        public class Upgrades {
                            public Expression pop(Expression a) {
                                return WindowExpressions.stdDevPop(a);
                            }

                            public Expression samp(Expression a, Expression b) {
                                return WindowExpressions.stdDevSamp(a, b);
                            }
                        }
                        """));
    }

    @Test
    void substr() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.aggregation.expressions.StringExpressions;
                        import dev.morphia.aggregation.expressions.impls.Expression;

                        import static dev.morphia.aggregation.expressions.StringExpressions.substr;

                        public class Upgrades {
                            public Expression imported(Expression a) {
                                return substr(a, 0, 3);
                            }

                            public Expression qualified(Expression a) {
                                return StringExpressions.substr(a, 1, 2);
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.aggregation.expressions.StringExpressions;
                        import dev.morphia.aggregation.expressions.impls.Expression;

                        import static dev.morphia.aggregation.expressions.StringExpressions.substrBytes;

                        public class Upgrades {
                            public Expression imported(Expression a) {
                                return substrBytes(a, 0, 3);
                            }

                            public Expression qualified(Expression a) {
                                return StringExpressions.substrBytes(a, 1, 2);
                            }
                        }
                        """));
    }

    @Test
    void logicalExpressionImported() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.aggregation.expressions.BooleanExpressions.LogicalExpression;
                        import dev.morphia.aggregation.expressions.impls.Expression;

                        import static dev.morphia.aggregation.expressions.BooleanExpressions.and;

                        public class Upgrades {
                            public LogicalExpression both(Expression a, Expression b) {
                                LogicalExpression expression = and(a);
                                return expression.add(b);
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.aggregation.expressions.impls.LogicalExpression;
                        import dev.morphia.aggregation.expressions.impls.Expression;

                        import static dev.morphia.aggregation.expressions.BooleanExpressions.and;

                        public class Upgrades {
                            public LogicalExpression both(Expression a, Expression b) {
                                LogicalExpression expression = and(a);
                                return expression.add(b);
                            }
                        }
                        """));
    }

    @Test
    void logicalExpressionQualified() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.aggregation.expressions.BooleanExpressions;
                        import dev.morphia.aggregation.expressions.impls.Expression;

                        public class Upgrades {
                            public BooleanExpressions.LogicalExpression either(Expression a, Expression b) {
                                return BooleanExpressions.or(a, b);
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.aggregation.expressions.BooleanExpressions;
                        import dev.morphia.aggregation.expressions.impls.Expression;
                        import dev.morphia.aggregation.expressions.impls.LogicalExpression;

                        public class Upgrades {
                            public LogicalExpression either(Expression a, Expression b) {
                                return BooleanExpressions.or(a, b);
                            }
                        }
                        """));
    }

    @Test
    void mergeObjects() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.aggregation.expressions.ObjectExpressions.MergeObjects;
                        import dev.morphia.aggregation.expressions.impls.Expression;

                        import static dev.morphia.aggregation.expressions.ObjectExpressions.mergeObjects;

                        public class Upgrades {
                            public MergeObjects merge(Expression a) {
                                MergeObjects merge = mergeObjects();
                                return merge.add(a);
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.aggregation.expressions.impls.MergeObjects;
                        import dev.morphia.aggregation.expressions.impls.Expression;

                        import static dev.morphia.aggregation.expressions.ObjectExpressions.mergeObjects;

                        public class Upgrades {
                            public MergeObjects merge(Expression a) {
                                MergeObjects merge = mergeObjects();
                                return merge.add(a);
                            }
                        }
                        """));
    }

    @Test
    void expressionCodec() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.aggregation.codecs.ExpressionCodec;

                        public class Upgrades {
                            private ExpressionCodec<?> codec;
                        }
                        """,
                //language=java
                """
                        import dev.morphia.mapping.codec.expressions.ExpressionCodec;

                        public class Upgrades {
                            private ExpressionCodec<?> codec;
                        }
                        """));
    }

    @Test
    void notSaved() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Entity;
                        import dev.morphia.annotations.NotSaved;

                        @Entity
                        public class Upgrades {
                            @NotSaved
                            private String computed;
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;
                        import dev.morphia.annotations.LoadOnly;

                        @Entity
                        public class Upgrades {
                            @LoadOnly
                            private String computed;
                        }
                        """));
    }

    @Test
    void experimentalConstructor() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Entity;
                        import dev.morphia.annotations.experimental.Constructor;

                        @Entity
                        public class Upgrades {
                            private final String name;

                            @Constructor
                            public Upgrades(String name) {
                                this.name = name;
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        @Entity
                        public class Upgrades {
                            private final String name;

                            public Upgrades(String name) {
                                this.name = name;
                            }
                        }
                        """));
    }

    @Test
    void customRecipesAreRegistered() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.FindAndModifyOptions;
                        import dev.morphia.query.Meta;

                        public class Upgrades {
                            public FindAndModifyOptions options() {
                                return new FindAndModifyOptions().returnNew(true);
                            }

                            public String field() {
                                return new Meta("textScore", "score").getField();
                            }
                        }
                        """,
                //language=java
                """
                        import com.mongodb.client.model.ReturnDocument;
                        import dev.morphia.ModifyOptions;
                        import dev.morphia.query.Meta;

                        public class Upgrades {
                            public ModifyOptions options() {
                                return new ModifyOptions().returnDocument(ReturnDocument.AFTER);
                            }

                            public String field() {
                                return Meta.textScore("score").field();
                            }
                        }
                        """));
    }
}
