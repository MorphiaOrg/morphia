package dev.morphia.rewrite.recipes.test.pipeline.kotlin;

import dev.morphia.rewrite.recipes.pipeline.PipelineMergeRewrite;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinPipelineMergeRewriteTest extends KotlinRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineMergeRewrite();
    }

    @Test
    public void testOnlyMerge() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.stages.Merge

                        class TestTheWorld {
                            fun update(ds: Datastore) {
                                ds.aggregate(Object::class.java)
                                  .merge(Merge.into("database", "collection"))
                            }
                        }""",
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.stages.Merge
                        import dev.morphia.aggregation.stages.Merge.merge

                        class TestTheWorld {
                            fun update(ds: Datastore) {
                                ds.aggregate(Object::class.java)
                                    .pipeline(Merge.merge("database", "collection"))
                            }
                        }"""));
    }

    @Test
    public void testComplexMerge() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.Datastore

                        import com.mongodb.client.model.MergeOptions.WhenMatched.REPLACE
                        import com.mongodb.client.model.MergeOptions.WhenNotMatched.INSERT
                        import dev.morphia.aggregation.stages.Merge.into

                        class TestTheWorld {
                            fun update(ds: Datastore) {
                                ds.aggregate(Object::class.java)
                                  .merge(into("budgets")
                                       .on("_id")
                                       .whenMatched(REPLACE)
                                       .whenNotMatched(INSERT))
                            }
                        }""",
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.stages.Merge.merge
                        import com.mongodb.client.model.MergeOptions.WhenMatched.REPLACE
                        import com.mongodb.client.model.MergeOptions.WhenNotMatched.INSERT

                        class TestTheWorld {
                            fun update(ds: Datastore) {
                                ds.aggregate(Object::class.java)
                                    .pipeline(merge("budgets")
                                        .on("_id")
                                        .whenMatched(REPLACE)
                                        .whenNotMatched(INSERT))
                            }
                        }"""));
    }

    @Test
    public void testMergeWithOtherStage() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.stages.Merge

                        class TestTheWorld {
                            fun update(ds: Datastore) {
                                ds.aggregate(Object::class.java)
                                  .count("field")
                                  .merge(Merge.into("database", "collection"))
                            }
                        }""",
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.stages.Merge
                        import dev.morphia.aggregation.stages.Merge.merge

                        class TestTheWorld {
                            fun update(ds: Datastore) {
                                ds.aggregate(Object::class.java)
                                    .count("field")
                                    .pipeline(Merge.merge("database", "collection"))
                            }
                        }"""));
    }

    @Test
    public void testUsingAggregationReference() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.aggregation.Aggregation
                        import dev.morphia.aggregation.stages.Merge

                        class TestTheWorld {
                            fun update(aggregation: Aggregation<*>) {
                                aggregation
                                  .count("field")
                                  .merge(Merge.into("database", "collection"))
                            }
                        }""",
                """
                        import dev.morphia.aggregation.Aggregation
                        import dev.morphia.aggregation.stages.Merge
                        import dev.morphia.aggregation.stages.Merge.merge

                        class TestTheWorld {
                            fun update(aggregation: Aggregation<*>) {
                                aggregation
                                    .count("field")
                                    .pipeline(Merge.merge("database", "collection"))
                            }
                        }"""));
    }
}