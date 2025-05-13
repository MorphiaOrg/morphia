package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.pipeline.PipelineRewrite;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.java.Assertions.java;

public class PipelineRewriteTest extends MorphiaRewriteTest {

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineRewrite();
    }

    @Override
    public void defaults(RecipeSpec spec) {
        super.defaults(spec);
        spec.expectedCyclesThatMakeChanges(2);
    }

    @Test
    void unwrapStageMethods() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.aggregation.expressions.ComparisonExpressions;

                        import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.Group.group;
                        import static dev.morphia.aggregation.stages.Group.id;
                        import static dev.morphia.aggregation.stages.Projection.project;
                        import static dev.morphia.aggregation.expressions.Expressions.field;
                        import static dev.morphia.aggregation.expressions.Expressions.value;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.query.filters.Filters.eq;

                        import dev.morphia.aggregation.Aggregation;
                        import dev.morphia.query.MorphiaCursor;
                        import org.bson.Document;

                        public class UnwrapTest {
                            public MorphiaCursor<Document> update(Aggregation<?> aggregation) {
                                return aggregation
                                    .match(eq("author", "Sanderson"))
                                    .group(group(id("author")).field("count", sum(value(1))))
                                    .set(set().field("_id", literal("2019Q1")))
                                    .sort(sort().ascending("1"))
                                    .sort(sort().ascending("2"))
                                    .sort(sort().ascending("3"))
                                    .sort(sort().ascending("4"))
                                    .count("beans")
                                    .execute(Document.class);
                            }
                        }""",
                //language=java
                """
                        import dev.morphia.aggregation.expressions.ComparisonExpressions;
                        import dev.morphia.aggregation.stages.Count;
                        import dev.morphia.aggregation.stages.Match;

                        import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.Count.count;
                        import static dev.morphia.aggregation.stages.Group.group;
                        import static dev.morphia.aggregation.stages.Group.id;
                        import static dev.morphia.aggregation.stages.Match.match;
                        import static dev.morphia.aggregation.stages.Projection.project;
                        import static dev.morphia.aggregation.expressions.Expressions.field;
                        import static dev.morphia.aggregation.expressions.Expressions.value;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.query.filters.Filters.eq;

                        import dev.morphia.aggregation.Aggregation;
                        import dev.morphia.query.MorphiaCursor;
                        import org.bson.Document;

                        public class UnwrapTest {
                            public MorphiaCursor<Document> update(Aggregation<?> aggregation) {
                                return aggregation
                                        .pipeline(
                                                match(eq("author", "Sanderson")),
                                                group(id("author")).field("count", sum(value(1))),
                                                set().field("_id", literal("2019Q1")),
                                                sort().ascending("1"),
                                                sort().ascending("2"),
                                                sort().ascending("3"),
                                                sort().ascending("4"),
                                                count("beans"))
                                    .execute(Document.class);
                            }
                        }"""));
    }

    @Test
    public void testUnionWith() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import org.bson.Document;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.AddFields.addFields;
                        import static dev.morphia.aggregation.stages.Set.set;
                        import static dev.morphia.aggregation.stages.Sort.sort;
                        import static dev.morphia.aggregation.stages.UnionWith;

                        public class UnwrapSet {
                            public void test(Datastore ds) {
                                ds.aggregate("sales2019q1")
                                  .set(set().field("_id", literal("2019Q1")))
                                  .unionWith("sales2019q2", addFields().field("_id", literal("2019Q2")))
                                  .unionWith("sales2019q3", addFields().field("_id", literal("2019Q3")))
                                  .unionWith("sales2019q4", addFields().field("_id", literal("2019Q4")))
                                  .sort(sort().ascending("_id", "store", "item"))
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
                                ds.aggregate("sales2019q1")
                                          .pipeline(
                                                  set().field("_id", literal("2019Q1")),
                                                  unionWith("sales2019q2", addFields().field("_id", literal("2019Q2"))),
                                                  unionWith("sales2019q3", addFields().field("_id", literal("2019Q3"))),
                                                  unionWith("sales2019q4", addFields().field("_id", literal("2019Q4"))),
                                                  sort().ascending("_id", "store", "item"))
                                  .execute(Document.class)
                                  .toList();
                            }
                        }
                        """));
    }

    @Test
    public void testTheWorld() {
        rewriteRun(java(
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.AggregationOptions;
                        import dev.morphia.aggregation.expressions.Expressions;
                        import dev.morphia.aggregation.stages.AddFields;
                        import dev.morphia.aggregation.stages.AutoBucket;
                        import dev.morphia.aggregation.stages.Bucket;
                        import dev.morphia.aggregation.stages.ChangeStream;
                        import dev.morphia.aggregation.stages.CollectionStats;
                        import dev.morphia.aggregation.stages.CurrentOp;
                        import dev.morphia.aggregation.stages.Densify;
                        import dev.morphia.aggregation.stages.Densify.Range;
                        import dev.morphia.aggregation.stages.Documents;
                        import dev.morphia.aggregation.stages.Expressions;
                        import dev.morphia.aggregation.stages.Facet;
                        import dev.morphia.aggregation.stages.Fill;
                        import dev.morphia.aggregation.stages.GeoNear;
                        import dev.morphia.aggregation.stages.GraphLookup;
                        import dev.morphia.aggregation.stages.Group;
                        import dev.morphia.aggregation.stages.Lookup;
                        import dev.morphia.aggregation.stages.Merge;
                        import dev.morphia.aggregation.stages.Out;
                        import dev.morphia.aggregation.stages.Projection;
                        import dev.morphia.aggregation.stages.Redact;
                        import dev.morphia.aggregation.stages.ReplaceRoot;
                        import dev.morphia.aggregation.stages.ReplaceWith;
                        import dev.morphia.aggregation.stages.Set;
                        import dev.morphia.aggregation.stages.Sort;
                        import dev.morphia.aggregation.stages.SortByCount;
                        import dev.morphia.aggregation.stages.Unset;
                        import dev.morphia.aggregation.stages.Unwind;
                        import dev.morphia.annotations.Field;
                        import dev.morphia.query.filters.Filters;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;

                        public class TestTheWorld {
                            public void update(Datastore ds) {
                                ds.aggregate(Object.class)
                                  .addFields(AddFields.addFields())
                                  .autoBucket(AutoBucket.autoBucket())
                                  .bucket(Bucket.bucket())
                                  .changeStream(ChangeStream.changeStream())
                                  .collStats(CollectionStats.collStats())
                                  .count("field")
                                  .currentOp(CurrentOp.currentOp())
                                  .densify(Densify.densify("field", Range.full(42)))
                                  .documents(Expressions.document())
                                  .facet(Facet.facet())
                                  .fill(Fill.fill())
                                  .geoNear(GeoNear.geoNear(new double[]{42, 42}))
                                  .graphLookup(GraphLookup.graphLookup("from"))
                                  .group(Group.group())
                                  .indexStats()
                                  .limit(12)
                                  .lookup(Lookup.lookup())
                                  .match(Filters.eq("field", "value"))
                                  .merge(Merge.into(""))
                                  .out(Out.to(""))
                                  .planCacheStats()
                                  .project(Projection.project())
                                  .redact(Redact.redact(literal("redact")))
                                  .replaceRoot(ReplaceRoot.replaceRoot())
                                  .replaceWith(ReplaceWith.replaceWith())
                                  .sample(18)
                                  .set(AddFields.addFields())
                                  .set(Set.set())
                                  .skip(70)
                                  .sort(Sort.sort())
                                  .sortByCount(literal("sortByCount"))
                                  .unionWith(Field.class, Sort.sort())
                                  .unionWith("UnionWithName", Projection.project())
                                  .unset(Unset.unset("unset"))
                                  .unwind(Unwind.unwind("unwind"))
                                  .execute(Object.class)
                                  .tryNext();
                            }
                        }""",
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.aggregation.AggregationOptions;
                        import dev.morphia.aggregation.expressions.Expressions;
                        import dev.morphia.aggregation.stages.*;
                        import dev.morphia.aggregation.stages.Densify.Range;
                        import dev.morphia.aggregation.stages.Documents;
                        import dev.morphia.aggregation.stages.Expressions;
                        import dev.morphia.aggregation.stages.Facet;
                        import dev.morphia.aggregation.stages.Fill;
                        import dev.morphia.aggregation.stages.GeoNear;
                        import dev.morphia.aggregation.stages.GraphLookup;
                        import dev.morphia.aggregation.stages.Group;
                        import dev.morphia.aggregation.stages.Lookup;
                        import dev.morphia.aggregation.stages.Merge;
                        import dev.morphia.aggregation.stages.Out;
                        import dev.morphia.aggregation.stages.Projection;
                        import dev.morphia.aggregation.stages.Redact;
                        import dev.morphia.aggregation.stages.ReplaceRoot;
                        import dev.morphia.aggregation.stages.ReplaceWith;
                        import dev.morphia.aggregation.stages.Set;
                        import dev.morphia.aggregation.stages.Sort;
                        import dev.morphia.aggregation.stages.SortByCount;
                        import dev.morphia.aggregation.stages.Unset;
                        import dev.morphia.aggregation.stages.Unwind;
                        import dev.morphia.annotations.Field;
                        import dev.morphia.query.filters.Filters;

                        import static dev.morphia.aggregation.expressions.Expressions.literal;
                        import static dev.morphia.aggregation.stages.Count.count;
                        import static dev.morphia.aggregation.stages.IndexStats.indexStats;
                        import static dev.morphia.aggregation.stages.Limit.limit;
                        import static dev.morphia.aggregation.stages.Match.match;
                        import static dev.morphia.aggregation.stages.PlanCacheStats.planCacheStats;
                        import static dev.morphia.aggregation.stages.Sample.sample;
                        import static dev.morphia.aggregation.stages.Skip.skip;
                        import static dev.morphia.aggregation.stages.SortByCount.sortByCount;
                        import static dev.morphia.aggregation.stages.UnionWith.unionWith;

                        public class TestTheWorld {
                            public void update(Datastore ds) {
                                ds.aggregate(Object.class)
                                          .pipeline(
                                                  AddFields.addFields(),
                                                  AutoBucket.autoBucket(),
                                                  Bucket.bucket(),
                                                  ChangeStream.changeStream(),
                                                  CollectionStats.collStats(),
                                                  count("field"),
                                                  CurrentOp.currentOp(),
                                                  Densify.densify("field", Range.full(42)),
                                                  Expressions.document(),
                                                  Facet.facet(),
                                                  Fill.fill(),
                                                  GeoNear.geoNear(new double[]{42, 42}),
                                                  GraphLookup.graphLookup("from"),
                                                  Group.group(),
                                                  indexStats(),
                                                  limit(12),
                                                  Lookup.lookup(),
                                                  match(Filters.eq("field", "value")),
                                                  planCacheStats(),
                                                  Projection.project(),
                                                  Redact.redact(literal("redact")),
                                                  ReplaceRoot.replaceRoot(),
                                                  ReplaceWith.replaceWith(),
                                                  sample(18),
                                                  AddFields.addFields(),
                                                  Set.set(),
                                                  skip(70),
                                                  Sort.sort(),
                                                  sortByCount(literal("sortByCount")),
                                                  unionWith(Field.class, Sort.sort()),
                                                  unionWith("UnionWithName", Projection.project()),
                                                  Unset.unset("unset"),
                                                  Unwind.unwind("unwind"))
                                  .execute(Object.class)
                                  .tryNext();
                            }
                        }"""));
    }
}
