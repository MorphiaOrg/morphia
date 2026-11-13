package dev.morphia.rewrite.recipes.test.pipeline;

import dev.morphia.rewrite.recipes.pipeline.PipelineRewriteRecipes;
import dev.morphia.rewrite.recipes.pipeline.StageFactoryRename;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class StageFactoryRenameTest extends MorphiaRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new StageFactoryRename();
    }

    @Test
    public void testQualifiedCalls() {
        rewriteRun(java(
                """
                        import java.util.List;

                        import dev.morphia.aggregation.stages.*;

                        import static dev.morphia.aggregation.expressions.Expressions.field;
                        import static dev.morphia.query.filters.Filters.eq;

                        public class TestTheWorld {
                            public List<Object> stages() {
                                return List.of(
                                    AddFields.of(),
                                    AutoBucket.of(),
                                    Bucket.of(),
                                    CollectionStats.with(),
                                    CurrentOp.of(),
                                    Facet.of(),
                                    GeoNear.to(new double[] {1, 2}),
                                    GraphLookup.from("collection"),
                                    GraphLookup.from(Object.class),
                                    Group.of(),
                                    Group.of(Group.id("name")),
                                    IndexStats.of(),
                                    Limit.of(10),
                                    Lookup.from("collection"),
                                    Lookup.from(Object.class),
                                    Match.on(eq("name", "value")),
                                    Merge.into("collection"),
                                    Merge.into("database", "collection"),
                                    Merge.into(Object.class),
                                    Out.to("collection"),
                                    Out.to(Object.class),
                                    PlanCacheStats.of(),
                                    Projection.of(),
                                    Redact.on(field("name")),
                                    ReplaceRoot.with(),
                                    ReplaceRoot.with(field("name")),
                                    ReplaceWith.with(),
                                    ReplaceWith.with(field("name")),
                                    Sample.of(10),
                                    Skip.of(10),
                                    Sort.on(),
                                    SortByCount.on(field("name")),
                                    Unset.fields("a", "b"),
                                    Unwind.on("name"));
                            }
                        }""",
                """
                        import java.util.List;

                        import dev.morphia.aggregation.stages.*;

                        import static dev.morphia.aggregation.expressions.Expressions.field;
                        import static dev.morphia.query.filters.Filters.eq;

                        public class TestTheWorld {
                            public List<Object> stages() {
                                return List.of(
                                    AddFields.addFields(),
                                    AutoBucket.autoBucket(),
                                    Bucket.bucket(),
                                    CollectionStats.collStats(),
                                    CurrentOp.currentOp(),
                                    Facet.facet(),
                                    GeoNear.geoNear(new double[] {1, 2}),
                                    GraphLookup.graphLookup("collection"),
                                    GraphLookup.graphLookup(Object.class),
                                    Group.group(),
                                    Group.group(Group.id("name")),
                                    IndexStats.indexStats(),
                                    Limit.limit(10),
                                    Lookup.lookup("collection"),
                                    Lookup.lookup(Object.class),
                                    Match.match(eq("name", "value")),
                                    Merge.merge("collection"),
                                    Merge.merge("database", "collection"),
                                    Merge.merge(Object.class),
                                    Out.out("collection"),
                                    Out.out(Object.class),
                                    PlanCacheStats.planCacheStats(),
                                    Projection.project(),
                                    Redact.redact(field("name")),
                                    ReplaceRoot.replaceRoot(),
                                    ReplaceRoot.replaceRoot(field("name")),
                                    ReplaceWith.replaceWith(),
                                    ReplaceWith.replaceWith(field("name")),
                                    Sample.sample(10),
                                    Skip.skip(10),
                                    Sort.sort(),
                                    SortByCount.sortByCount(field("name")),
                                    Unset.unset("a", "b"),
                                    Unwind.unwind("name"));
                            }
                        }"""));
    }

    @Test
    public void testStaticImports() {
        // ChangeMethodName renames static imports in place, so Group.group stays where Group.of was
        // rather than being re-sorted ahead of Group.id.
        rewriteRun(java(
                """
                        import java.util.List;

                        import dev.morphia.aggregation.stages.Stage;

                        import static dev.morphia.aggregation.stages.Group.id;
                        import static dev.morphia.aggregation.stages.Group.of;
                        import static dev.morphia.aggregation.stages.Limit.of;
                        import static dev.morphia.aggregation.stages.Out.to;
                        import static dev.morphia.aggregation.stages.Sort.on;

                        public class TestTheWorld {
                            public List<Stage> stages() {
                                return List.of(of(id("name")), of(10), on().ascending("name"), to("collection"));
                            }
                        }""",
                """
                        import java.util.List;

                        import dev.morphia.aggregation.stages.Stage;

                        import static dev.morphia.aggregation.stages.Group.id;
                        import static dev.morphia.aggregation.stages.Group.group;
                        import static dev.morphia.aggregation.stages.Limit.limit;
                        import static dev.morphia.aggregation.stages.Out.out;
                        import static dev.morphia.aggregation.stages.Sort.sort;

                        public class TestTheWorld {
                            public List<Stage> stages() {
                                return List.of(group(id("name")), limit(10), sort().ascending("name"), out("collection"));
                            }
                        }"""));
    }

    @Test
    public void testCurrentNamesUntouched() {
        rewriteRun(java(
                """
                        import dev.morphia.aggregation.stages.Group;
                        import dev.morphia.aggregation.stages.Stage;

                        import static dev.morphia.aggregation.stages.Limit.limit;

                        public class TestTheWorld {
                            public Stage[] stages() {
                                return new Stage[] {Group.group(Group.id("name")), limit(10)};
                            }
                        }"""));
    }

    @Test
    public void testUnrelatedFactoriesUntouched() {
        rewriteRun(java(
                """
                        package com.example;

                        public class Group {
                            public static Group of() {
                                return new Group();
                            }
                        }"""),
                java(
                        """
                                import java.util.List;

                                import com.example.Group;

                                public class TestTheWorld {
                                    public List<Object> stages() {
                                        return List.of(Group.of(), List.of());
                                    }
                                }"""));
    }

    @Test
    public void testAfterPipelineRewrite() {
        rewriteRun(spec -> spec.recipes(new PipelineRewriteRecipes(), new StageFactoryRename()),
                java(
                        """
                                import dev.morphia.Datastore;
                                import dev.morphia.aggregation.stages.Group;
                                import dev.morphia.aggregation.stages.Merge;
                                import dev.morphia.aggregation.stages.Out;
                                import dev.morphia.aggregation.stages.Sort;

                                import static dev.morphia.aggregation.stages.Group.id;

                                public class TestTheWorld {
                                    public void update(Datastore ds) {
                                        ds.aggregate(Object.class)
                                            .group(Group.of(id("name")))
                                            .sort(Sort.on().ascending("name"))
                                            .out(Out.to("collection"));
                                        ds.aggregate(Object.class)
                                            .group(Group.of(id("name")))
                                            .merge(Merge.into("collection"));
                                    }
                                }""",
                        """
                                import dev.morphia.Datastore;
                                import dev.morphia.aggregation.stages.Group;
                                import dev.morphia.aggregation.stages.Merge;
                                import dev.morphia.aggregation.stages.Out;
                                import dev.morphia.aggregation.stages.Sort;

                                import static dev.morphia.aggregation.stages.Group.id;

                                public class TestTheWorld {
                                    public void update(Datastore ds) {
                                        ds.aggregate(Object.class)
                                                .pipeline(
                                                        Group.group(id("name")),
                                                        Sort.sort().ascending("name"), Out.out("collection"));
                                        ds.aggregate(Object.class)
                                                .pipeline(
                                                        Group.group(id("name")), Merge.merge("collection"));
                                    }
                                }"""));
    }
}
