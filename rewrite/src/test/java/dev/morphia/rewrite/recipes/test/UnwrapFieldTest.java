package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.UnwrapFieldExpressions;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class UnwrapFieldTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UnwrapFieldExpressions())
            .parser(JavaParser.fromJavaVersion()
                              .classpath("morphia-core"));
    }

    @Test
    void unwrapsFieldCalls() {
        rewriteRun(
            java(
                """
                    package dev.morphia;

                    import dev.morphia.aggregation.expressions.ComparisonExpressions;
                    import static dev.morphia.aggregation.stages.Projection.project;
                    import dev.morphia.aggregation.Aggregation;
                    
                    public class UnwrapTest {
                        public void something() {
                        }
                        
                       public void update(Aggregation aggregation) {
                            aggregation.pipeline(
                                project()
                                    .suppressId()
                                    .include("item")
                                    .include("qty")
                                    .include("qtyGte250", ComparisonExpressions.gte("$qty", 250)));
                       }
                    }
                """,
                """
                                    package com.yourorg;

                    import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
                    import static dev.morphia.aggregation.stages.Projection.project;
                    import dev.morphia.aggregation.Aggregation;
                    
                    public class UnwrapTest {
                        public void something() {
                        }
                        
                        public void update(Aggregation aggregation) {
                        }
/*
                       public void update(Aggregation aggregation) {
                            aggregation.pipeline(
                                project()
                                    .suppressId()
                                    .include("item")
                                    .include("qty")
                                    .include("qtyGte250", gte("$qty", 250)));
                       }
*/
                    }
                
                """
            )
        );
    }
}