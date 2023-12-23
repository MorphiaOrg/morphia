package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.PipelineRewrite;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.testng.annotations.Test;

import static org.openrewrite.java.Assertions.java;

public class PipelineRewriteTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PipelineRewrite());
    }

    @Test
    void addsHelloToFooBar() {
        rewriteRun(
                java(
                        """
                                    package com.yourorg;

                                    class FooBar {
                                    }
                                """,
                        """
                                    package com.yourorg;

                                    class FooBar {
                                        public String hello() {
                                            return "Hello from com.yourorg.FooBar!";
                                        }
                                    }
                                """));
    }

    @Test
    void doesNotChangeExistingHello() {
        rewriteRun(
                java(
                        """
                                    package com.yourorg;

                                    class FooBar {
                                        public String hello() { return ""; }
                                    }
                                """));
    }

    @Test
    void doesNotChangeOtherClasses() {
        rewriteRun(
                java(
                        """
                                    package com.yourorg;

                                    class Bash {
                                    }
                                """));
    }
}