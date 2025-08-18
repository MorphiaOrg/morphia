package dev.morphia.rewrite.recipes.test.pipeline.kotlin;

import dev.morphia.rewrite.recipes.pipeline.PipelineExecuteRewrite;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinPipelineExecuteRewriteTest extends KotlinRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineExecuteRewrite();
    }

    @Test
    public void removeExecuteWithNamedCollection() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.Datastore
                        import org.bson.Document

                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith

                        class RewriteExecute {
                            fun test(ds: Datastore) {
                                ds.aggregate("sales2019q1")
                                  .set(set().field("_id", literal("2019Q1")))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore
                        import org.bson.Document

                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith

                        class RewriteExecute {
                            fun test(ds: Datastore) {
                                ds.aggregate("sales2019q1",Document::class.java)
                                  .set(set().field("_id", literal("2019Q1")))
                                  .iterator()
                                  .toList()
                            }
                        }
                        """));
    }

    @Test
    public void removeExecuteWithClassDefinedSource() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.Datastore
                        import org.bson.Document

                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith

                        class RewriteExecute {
                            fun test(ds: Datastore) {
                                ds.aggregate(String::class.java)
                                  .set(set().field("_id", literal("2019Q1")))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                        }
                        """,
                """
                        import dev.morphia.Datastore
                        import org.bson.Document

                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith

                        class RewriteExecute {
                            fun test(ds: Datastore) {
                                ds.aggregate(String::class.java,Document::class.java)
                                  .set(set().field("_id", literal("2019Q1")))
                                  .iterator()
                                  .toList()
                            }
                        }
                        """));
    }

}