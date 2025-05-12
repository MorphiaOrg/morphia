package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.pipeline.PipelineExecuteRewrite;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class PipelineExecuteRewriteTest extends MorphiaRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineExecuteRewrite();
    }

    @Test
    public void removeExecute() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;

                        public class RewriteExecute {
                            public void test(Datastore ds) {
                                ds.aggregate("sales2019q1")
                                  .set(set().field("_id", literal("2019Q1")))
                                  .execute(Document.class)
                                  .toList();
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.stages.UnionWith;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;
                        import static dev.morphia.aggregation.stages.UnionWith.unionWith;

                        public class UnwrapSet {
                            public void test(Datastore ds) {
                                ds.aggregate("sales2019q1", Document.class)
                                  .set(set().field("_id", literal("2019Q1")))
                                  .toList();
                            }
                        }
                        """));
    }

}
