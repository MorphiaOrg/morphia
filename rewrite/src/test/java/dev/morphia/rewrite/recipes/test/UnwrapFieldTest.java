package dev.morphia.rewrite.recipes.test;

import java.net.URI;
import java.util.List;

import dev.morphia.rewrite.recipes.UnwrapFieldExpressions;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import io.github.classgraph.ClassGraph;

import static org.openrewrite.java.Assertions.java;

public class UnwrapFieldTest implements RewriteTest {
    private static final String ARTIFACT;
    static {
        List<URI> runtimeClasspath = new ClassGraph().disableNestedJarScanning().getClasspathURIs();
        var core = runtimeClasspath.stream()
                .filter(uri -> {
                    String string = uri.toString();
                    return string.contains("morphia") && string.contains("core");
                })
                .findFirst().orElseThrow().toString();
        if (core.contains("morphia-core")) {
            ARTIFACT = "morphia-core";
        } else {
            ARTIFACT = "morphia/core";

        }
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UnwrapFieldExpressions())
                .parser(JavaParser.fromJavaVersion()
                        .classpath(ARTIFACT));
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