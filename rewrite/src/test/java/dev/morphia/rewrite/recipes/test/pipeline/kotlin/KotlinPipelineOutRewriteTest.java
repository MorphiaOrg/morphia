package dev.morphia.rewrite.recipes.test.pipeline.kotlin;

import dev.morphia.rewrite.recipes.pipeline.PipelineOutRewrite;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinPipelineOutRewriteTest extends KotlinRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineOutRewrite();
    }

    @Test
    public void testOnlyOut() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.stages.Out

                        class TestTheWorld {
                            fun update(ds: Datastore) {
                                ds.aggregate(Object::class.java)
                                  .out(Out.to("collection")
                                    .database("database"))
                            }
                        }""",
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.stages.Out
                        import dev.morphia.aggregation.stages.Out.out

                        class TestTheWorld {
                            fun update(ds: Datastore) {
                                ds.aggregate(Object::class.java)
                                    .pipeline(Out.out("collection")
                                        .database("database"))
                            }
                        }"""));
    }

    @Test
    public void testOutWithOtherStage() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.stages.Out

                        class TestTheWorld {
                            fun update(ds: Datastore) {
                                ds.aggregate(Object::class.java)
                                  .count("field")
                                  .out(Out.to("collection")
                                    .database("database"))
                            }
                        }""",
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.stages.Out
                        import dev.morphia.aggregation.stages.Out.out

                        class TestTheWorld {
                            fun update(ds: Datastore) {
                                ds.aggregate(Object::class.java)
                                    .count("field")
                                    .pipeline(Out.out("collection")
                                        .database("database"))
                            }
                        }"""));
    }

    @Test
    public void testUsingAggregationReference() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.aggregation.Aggregation
                        import dev.morphia.aggregation.stages.Out

                        class TestTheWorld {
                            fun update(aggregation: Aggregation<*>) {
                                aggregation
                                  .count("field")
                                  .out(Out.to("collection"))
                            }
                        }""",
                """
                        import dev.morphia.aggregation.Aggregation
                        import dev.morphia.aggregation.stages.Out
                        import dev.morphia.aggregation.stages.Out.out

                        class TestTheWorld {
                            fun update(aggregation: Aggregation<*>) {
                                aggregation
                                    .count("field")
                                    .pipeline(Out.out("collection"))
                            }
                        }"""));
    }
}