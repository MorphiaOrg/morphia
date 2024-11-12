package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.UnwrapFieldExpressions;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class UnwrapFieldTest extends MorphiaRewriteTest {

    /*
     * @Override
     * public String[] classpath() {
     * List<URI> runtimeClasspath = new ClassGraph().disableNestedJarScanning().getClasspathURIs();
     * List<String> classpath = runtimeClasspath.stream()
     * .filter(uri -> {
     * String string = uri.toString();
     * return string.contains("mongodb") || string.contains("bson");
     * })
     * .map(uri -> {
     * return new File(uri).getName().replaceAll("-[0-9].*", "");
     * })
     * .collect(ArrayList::new, List::add, List::addAll);
     * var core = runtimeClasspath.stream()
     * .filter(uri -> {
     * String string = uri.toString();
     * return string.contains("morphia") && string.contains("core");
     * })
     * .findFirst().orElseThrow().toString();
     * 
     * final String artifact = core.contains("morphia-core") ? "morphia-core" : "morphia/core";
     * 
     * classpath.add(artifact);
     * return classpath.toArray(new String[0]);
     * }
     */

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