package dev.morphia.rewrite.recipes.test.pipeline.kotlin;

import dev.morphia.rewrite.recipes.pipeline.PipelineRewriteRecipes;
import dev.morphia.rewrite.recipes.pipeline.StageFactoryRename;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinStageFactoryRenameTest extends KotlinRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new StageFactoryRename();
    }

    @Test
    public void testQualifiedCalls() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.aggregation.stages.Group
                        import dev.morphia.aggregation.stages.Limit
                        import dev.morphia.aggregation.stages.Lookup
                        import dev.morphia.aggregation.stages.Match
                        import dev.morphia.aggregation.stages.Merge
                        import dev.morphia.aggregation.stages.Out
                        import dev.morphia.aggregation.stages.Sort
                        import dev.morphia.aggregation.stages.Unset
                        import dev.morphia.query.filters.Filters.eq

                        class TestTheWorld {
                            fun stages(): List<Any> {
                                return listOf(
                                    Group.of(Group.id("name")),
                                    Limit.of(10),
                                    Lookup.from(Any::class.java),
                                    Match.on(eq("name", "value")),
                                    Merge.into("collection"),
                                    Out.to("collection"),
                                    Sort.on().ascending("name"),
                                    Unset.fields("a", "b"))
                            }
                        }""",
                """
                        import dev.morphia.aggregation.stages.Group
                        import dev.morphia.aggregation.stages.Limit
                        import dev.morphia.aggregation.stages.Lookup
                        import dev.morphia.aggregation.stages.Match
                        import dev.morphia.aggregation.stages.Merge
                        import dev.morphia.aggregation.stages.Out
                        import dev.morphia.aggregation.stages.Sort
                        import dev.morphia.aggregation.stages.Unset
                        import dev.morphia.query.filters.Filters.eq

                        class TestTheWorld {
                            fun stages(): List<Any> {
                                return listOf(
                                    Group.group(Group.id("name")),
                                    Limit.limit(10),
                                    Lookup.lookup(Any::class.java),
                                    Match.match(eq("name", "value")),
                                    Merge.merge("collection"),
                                    Out.out("collection"),
                                    Sort.sort().ascending("name"),
                                    Unset.unset("a", "b"))
                            }
                        }"""));
    }

    @Test
    public void testStaticImports() {
        // Out.to is called from a top-level function: inside a class body, a bare to("collection") resolves to
        // kotlin's Pair-building `to` extension on the implicit `this`, not the imported Out.to.
        rewriteRun(kotlin(
                """
                        import dev.morphia.aggregation.stages.Group.id
                        import dev.morphia.aggregation.stages.Group.of
                        import dev.morphia.aggregation.stages.Out.to
                        import dev.morphia.aggregation.stages.Sort.on

                        class TestTheWorld {
                            fun stages(): List<Any> {
                                return listOf(of(id("name")), on().ascending("name"))
                            }
                        }

                        fun outStage(): Any = to("collection")""",
                """
                        import dev.morphia.aggregation.stages.Group.id
                        import dev.morphia.aggregation.stages.Group.group
                        import dev.morphia.aggregation.stages.Out.out
                        import dev.morphia.aggregation.stages.Sort.sort

                        class TestTheWorld {
                            fun stages(): List<Any> {
                                return listOf(group(id("name")), sort().ascending("name"))
                            }
                        }

                        fun outStage(): Any = out("collection")"""));
    }

    @Test
    public void testCurrentNamesUntouched() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.aggregation.stages.Group
                        import dev.morphia.aggregation.stages.Limit.limit

                        class TestTheWorld {
                            fun stages(): List<Any> {
                                return listOf(Group.group(Group.id("name")), limit(10))
                            }
                        }"""));
    }

    @Test
    public void testAfterPipelineRewrite() {
        // The unused Out.out and Merge.merge imports come from PipelineOutRewrite and PipelineMergeRewrite,
        // matching KotlinPipelineOutRewriteTest.
        rewriteRun(spec -> spec.recipes(new PipelineRewriteRecipes(), new StageFactoryRename()),
                kotlin(
                        """
                                import dev.morphia.Datastore
                                import dev.morphia.aggregation.stages.Group
                                import dev.morphia.aggregation.stages.Group.id
                                import dev.morphia.aggregation.stages.Merge
                                import dev.morphia.aggregation.stages.Out
                                import dev.morphia.aggregation.stages.Sort

                                class TestTheWorld {
                                    fun update(ds: Datastore) {
                                        ds.aggregate(Object::class.java)
                                            .group(Group.of(id("name")))
                                            .sort(Sort.on().ascending("name"))
                                            .out(Out.to("collection"))
                                        ds.aggregate(Object::class.java)
                                            .group(Group.of(id("name")))
                                            .merge(Merge.into("collection"))
                                    }
                                }""",
                        """
                                import dev.morphia.Datastore
                                import dev.morphia.aggregation.stages.Group
                                import dev.morphia.aggregation.stages.Group.id
                                import dev.morphia.aggregation.stages.Merge
                                import dev.morphia.aggregation.stages.Merge.merge
                                import dev.morphia.aggregation.stages.Out
                                import dev.morphia.aggregation.stages.Out.out
                                import dev.morphia.aggregation.stages.Sort

                                class TestTheWorld {
                                    fun update(ds: Datastore) {
                                        ds.aggregate(Object::class.java)
                                            .pipeline(
                                                Group.group(id("name")),
                                                Sort.sort().ascending("name"), Out.out("collection"))
                                        ds.aggregate(Object::class.java)
                                            .pipeline(
                                                Group.group(id("name")), Merge.merge("collection"))
                                    }
                                }"""));
    }
}
