package dev.morphia.rewrite.recipes.test;

import java.io.File;
import java.nio.file.Path;

import dev.morphia.rewrite.recipes.PipelineRewriteStage2;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaParser.Builder;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.java.Assertions.java;

public class PipelineRewriteStage2Test extends MorphiaRewriteTest {

    private static final String classesFolder;

    static {
        var root = new File(".").getAbsoluteFile();
        while (!new File(root, ".git").exists()) {
            root = root.getParentFile();
        }

        classesFolder = new File(root, "core/target/classes").getAbsolutePath();
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
                                            .pipeline(group(id("author")).field("count", sum(value(1))))
                                            .pipeline(sort().ascending("1"))
                                            .pipeline(sort().ascending("2"))
                                            .pipeline(sort().ascending("3"))
                                            .pipeline(sort().ascending("4"))
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

    @Override
    protected @NotNull String findMorphiaCore() {
        return classesFolder;
    }

    public String[] classpath() {
        return findMongoArtifacts().toArray(new String[0]);
    }

    @Override
    public void defaults(RecipeSpec spec) {
        Builder<? extends JavaParser, ?> builder = JavaParser.fromJavaVersion()
                .addClasspathEntry(Path.of(classesFolder));
        findMongoArtifacts().stream().map(Path::of)
                .forEach(builder::addClasspathEntry);
        spec.recipe(getRecipe())
                .parser(builder);
    }

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineRewriteStage2();
    }

}
