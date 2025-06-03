package dev.morphia.rewrite.recipes.test.pipeline;

import dev.morphia.rewrite.recipes.PipelineRewriteRecipes;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class PipelineRecipesTest extends MorphiaRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineRewriteRecipes();
    }

    @Test
    public void existingPipelineCall() {
        rewriteRun(java(
                """
                        import dev.morphia.aggregation.Aggregation;
                        import dev.morphia.aggregation.stages.Merge;

                        public class TestTheWorld {
                            public void update(Aggregation aggregation) {
                                aggregation
                                  .count("field")
                                  .merge(Merge.into("database", "collection"));
                            }
                        }""",
                """
                        import dev.morphia.aggregation.Aggregation;
                        import dev.morphia.aggregation.stages.Count;
                        import dev.morphia.aggregation.stages.Merge;

                        import static dev.morphia.aggregation.stages.Count.count;

                        public class TestTheWorld {
                            public void update(Aggregation aggregation) {
                                aggregation
                                        .pipeline(
                                                count("field"), Merge.merge("database", "collection"));
                            }
                        }"""));
    }

}
