package dev.morphia.rewrite.recipes.test.pipeline;

import dev.morphia.rewrite.recipes.pipeline.PipelineOutRewrite;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class PipelineOutRewriteTest extends MorphiaRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineOutRewrite();
    }

    @Test
    public void testOnlyOut() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.stages.Out;

                        public class TestTheWorld {
                            public void update(Datastore ds) {
                                ds.aggregate(Object.class)
                                  .out(Out.to("collection")
                                    .database("database"));
                            }
                        }""",
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.stages.Out;

                        public class TestTheWorld {
                            public void update(Datastore ds) {
                                ds.aggregate(Object.class)
                                        .pipeline(Out.out("collection")
                                                .database("database"));
                            }
                        }"""));
    }

    @Test
    public void testOutWithOtherStage() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.stages.Out;

                        public class TestTheWorld {
                            public void update(Datastore ds) {
                                ds.aggregate(Object.class)
                                  .count("field")
                                  .out(Out.to("collection")
                                    .database("database"));
                            }
                        }""",
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.stages.Out;

                        public class TestTheWorld {
                            public void update(Datastore ds) {
                                ds.aggregate(Object.class)
                                        .count("field")
                                        .pipeline(Out.out("collection")
                                                .database("database"));
                            }
                        }"""));
    }

    @Test
    public void testUsingAggregationReference() {
        rewriteRun(java(
                """
                        import dev.morphia.aggregation.Aggregation;
                        import dev.morphia.aggregation.stages.Out;

                        public class TestTheWorld {
                            public void update(Aggregation aggregation) {
                                aggregation
                                  .count("field")
                                  .out(Out.to("collection"));
                            }
                        }""",
                """
                        import dev.morphia.aggregation.Aggregation;
                        import dev.morphia.aggregation.stages.Out;

                        public class TestTheWorld {
                            public void update(Aggregation aggregation) {
                                aggregation
                                        .count("field")
                                        .pipeline(Out.out("collection"));
                            }
                        }"""));
    }
}
