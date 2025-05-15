package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.pipeline.PipelineMergeRewrite;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class PipelineMergeRewriteTest extends MorphiaRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineMergeRewrite();
    }

    @Test
    public void testOnlyMerge() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.stages.Merge;

                        public class TestTheWorld {
                            public void update(Datastore ds) {
                                ds.aggregate(Object.class)
                                  .merge(Merge.into("database", "collection"));
                            }
                        }""",
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.stages.Merge;

                        public class TestTheWorld {
                            public void update(Datastore ds) {
                                ds.aggregate(Object.class)
                                  .pipeline(Merge.merge("database", "collection"));
                            }
                        }"""));
    }

    @Test
    public void testMergeWithOtherStage() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.stages.Merge;

                        public class TestTheWorld {
                            public void update(Datastore ds) {
                                ds.aggregate(Object.class)
                                  .count("field")
                                  .merge(Merge.into("database", "collection"));
                            }
                        }""",
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.stages.Merge;

                        public class TestTheWorld {
                            public void update(Datastore ds) {
                                ds.aggregate(Object.class)
                                  .count("field")
                                  .pipeline(Merge.merge("database", "collection"));
                            }
                        }"""));
    }

    @Test
    public void testUsingAggregationReference() {
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
                        import dev.morphia.aggregation.stages.Merge;

                        public class TestTheWorld {
                            public void update(Aggregation aggregation) {
                                aggregation
                                  .count("field")
                                  .pipeline(Merge.merge("database", "collection"));
                            }
                        }"""));
    }
}
