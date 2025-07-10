package dev.morphia.rewrite.recipes.test.pipeline.kotlin;

import java.nio.file.Path;
import java.util.Set;

import dev.morphia.rewrite.recipes.RewriteUtils;
import dev.morphia.rewrite.recipes.pipeline.PipelineRewrite;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.KotlinParser.Builder;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinPipelineRewriteTest extends MorphiaRewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        Builder builder = KotlinParser.builder()
                .classpath(Set.of(RewriteUtils.findMorphiaCore()));
        findMongoDependencies().stream()
                .map(Path::of)
                .forEach(builder::addClasspathEntry);
        spec.recipe(getRecipe())
                .parser(builder);
    }

    @Override
    @NotNull
    protected Recipe getRecipe() {
        return new PipelineRewrite();
    }

    @Test
    void unwrapStageMethods() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.ComparisonExpressions
                        import dev.morphia.aggregation.expressions.AccumulatorExpressions.sum
                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.Count
                        import dev.morphia.aggregation.stages.Group.group
                        import dev.morphia.aggregation.stages.Group.id
                        import dev.morphia.aggregation.stages.Projection.project
                        import dev.morphia.aggregation.expressions.Expressions.field
                        import dev.morphia.aggregation.expressions.Expressions.value
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.aggregation.Aggregation
                        import dev.morphia.query.MorphiaCursor
                        import org.bson.Document

                        class UnwrapTest {
                            fun update(aggregation: Aggregation<*>): MorphiaCursor<Document> {
                                return aggregation
                                    .match(eq("author", "Sanderson"))
                                    .group(group(id("author")).field("count", sum(value(1))))
                                    .set(set().field("_id", literal("2019Q1")))
                                    .sort(sort().ascending("1"))
                                    .sort(sort().ascending("2"))
                                    .sort(sort().ascending("3"))
                                    .sort(sort().ascending("4"))
                                    .count("beans")
                                    .execute(Document::class.java)
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.aggregation.expressions.ComparisonExpressions
                        import dev.morphia.aggregation.expressions.AccumulatorExpressions.sum
                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.Count
                        import dev.morphia.aggregation.stages.Count.count
                        import dev.morphia.aggregation.stages.Group.group
                        import dev.morphia.aggregation.stages.Group.id
                        import dev.morphia.aggregation.stages.Match
                        import dev.morphia.aggregation.stages.Match.match
                        import dev.morphia.aggregation.stages.Projection.project
                        import dev.morphia.aggregation.expressions.Expressions.field
                        import dev.morphia.aggregation.expressions.Expressions.value
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.aggregation.Aggregation
                        import dev.morphia.query.MorphiaCursor
                        import org.bson.Document

                        class UnwrapTest {
                            fun update(aggregation: Aggregation<*>): MorphiaCursor<Document> {
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
                                    .execute(Document::class.java)
                            }
                        }
                        """));
    }

    @Test
    public void testUnionWith() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                          import dev.morphia.Datastore
                          import dev.morphia.aggregation.expressions.Expressions.literal
                          import dev.morphia.aggregation.stages.AddFields.addFields
                          import dev.morphia.aggregation.stages.Set.set
                          import dev.morphia.aggregation.stages.Sort.sort
                          import dev.morphia.aggregation.stages.UnionWith
                          import org.bson.Document

                          class UnwrapSet {
                            fun test(ds: Datastore) {
                                ds.aggregate("sales2019q1")
                                  .set(set().field("_id", literal("2019Q1")))
                                  .unionWith("sales2019q2", addFields().field("_id", literal("2019Q2")))
                                  .unionWith("sales2019q3", addFields().field("_id", literal("2019Q3")))
                                  .unionWith("sales2019q4", addFields().field("_id", literal("2019Q4")))
                                  .sort(sort().ascending("_id", "store", "item"))
                                  .execute(Document::class.java)
                                  .toList()
                            }
                          }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.expressions.Expressions.literal
                        import dev.morphia.aggregation.stages.AddFields.addFields
                        import dev.morphia.aggregation.stages.Set.set
                        import dev.morphia.aggregation.stages.Sort.sort
                        import dev.morphia.aggregation.stages.UnionWith
                        import dev.morphia.aggregation.stages.UnionWith.unionWith
                        import org.bson.Document

                        class UnwrapSet {
                          fun test(ds: Datastore) {
                              ds.aggregate("sales2019q1")
                                    .pipeline(
                                        set().field("_id", literal("2019Q1")),
                                        unionWith("sales2019q2", addFields().field("_id", literal("2019Q2"))),
                                        unionWith("sales2019q3", addFields().field("_id", literal("2019Q3"))),
                                        unionWith("sales2019q4", addFields().field("_id", literal("2019Q4"))),
                                        sort().ascending("_id", "store", "item"))
                                .execute(Document::class.java)
                                .toList()
                          }
                        }
                        """));
    }

    @Test
    public void testTheWorld() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.AggregationOptions
                        import dev.morphia.aggregation.expressions.Expressions
                        import dev.morphia.aggregation.stages.AddFields
                        import dev.morphia.aggregation.stages.AutoBucket
                        import dev.morphia.aggregation.stages.Bucket
                        import dev.morphia.aggregation.stages.ChangeStream
                        import dev.morphia.aggregation.stages.CollectionStats
                        import dev.morphia.aggregation.stages.CurrentOp
                        import dev.morphia.aggregation.stages.Densify
                        import dev.morphia.aggregation.stages.Densify.Range
                        import dev.morphia.aggregation.stages.Documents
                        import dev.morphia.aggregation.stages.Expressions
                        import dev.morphia.aggregation.stages.Facet
                        import dev.morphia.aggregation.stages.Fill
                        import dev.morphia.aggregation.stages.GeoNear
                        import dev.morphia.aggregation.stages.GeoNear.geoNear
                        import dev.morphia.aggregation.stages.GraphLookup
                        import dev.morphia.aggregation.stages.Group
                        import dev.morphia.aggregation.stages.Lookup
                        import dev.morphia.aggregation.stages.Merge
                        import dev.morphia.aggregation.stages.Out
                        import dev.morphia.aggregation.stages.Projection
                        import dev.morphia.aggregation.stages.Redact
                        import dev.morphia.aggregation.stages.ReplaceRoot
                        import dev.morphia.aggregation.stages.ReplaceWith
                        import dev.morphia.aggregation.stages.Set
                        import dev.morphia.aggregation.stages.Sort
                        import dev.morphia.aggregation.stages.SortByCount
                        import dev.morphia.aggregation.stages.Unset
                        import dev.morphia.aggregation.stages.Unwind
                        import dev.morphia.annotations.Field
                        import dev.morphia.query.filters.Filters
                        import dev.morphia.aggregation.expressions.Expressions.literal

                        class TestTheWorld {
                            fun update(ds: Datastore) {
                                ds.aggregate(Object::class.java)
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
                                    .geoNear(geoNear(arrayOf(2.0, 42.0).toDoubleArray()))
                                    .graphLookup(GraphLookup.graphLookup("from"))
                                    .group(Group.group())
                                    .indexStats()
                                    .limit(12)
                                    .lookup(Lookup.lookup())
                                    .match(Filters.eq("field", "value"))
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
                                    .unionWith(Field::class.java, Sort.sort())
                                    .unionWith("UnionWithName", Projection.project())
                                    .unset(Unset.unset("unset"))
                                    .unwind(Unwind.unwind("unwind"))
                                    .execute(Object::class.java)
                                    .tryNext()
                            }
                        }""",
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.aggregation.AggregationOptions
                        import dev.morphia.aggregation.expressions.Expressions
                        import dev.morphia.aggregation.stages.*
                        import dev.morphia.aggregation.stages.Count.count
                        import dev.morphia.aggregation.stages.Densify.Range
                        import dev.morphia.aggregation.stages.Documents
                        import dev.morphia.aggregation.stages.Expressions
                        import dev.morphia.aggregation.stages.Facet
                        import dev.morphia.aggregation.stages.Fill
                        import dev.morphia.aggregation.stages.GeoNear
                        import dev.morphia.aggregation.stages.GeoNear.geoNear
                        import dev.morphia.aggregation.stages.GraphLookup
                        import dev.morphia.aggregation.stages.Group
                        import dev.morphia.aggregation.stages.IndexStats.indexStats
                        import dev.morphia.aggregation.stages.Limit.limit
                        import dev.morphia.aggregation.stages.Lookup
                        import dev.morphia.aggregation.stages.Match.match
                        import dev.morphia.aggregation.stages.Merge
                        import dev.morphia.aggregation.stages.Out
                        import dev.morphia.aggregation.stages.PlanCacheStats.planCacheStats
                        import dev.morphia.aggregation.stages.Projection
                        import dev.morphia.aggregation.stages.Redact
                        import dev.morphia.aggregation.stages.ReplaceRoot
                        import dev.morphia.aggregation.stages.ReplaceWith
                        import dev.morphia.aggregation.stages.Sample.sample
                        import dev.morphia.aggregation.stages.Set
                        import dev.morphia.aggregation.stages.Skip.skip
                        import dev.morphia.aggregation.stages.Sort
                        import dev.morphia.aggregation.stages.SortByCount
                        import dev.morphia.aggregation.stages.SortByCount.sortByCount
                        import dev.morphia.aggregation.stages.UnionWith.unionWith
                        import dev.morphia.aggregation.stages.Unset
                        import dev.morphia.aggregation.stages.Unwind
                        import dev.morphia.annotations.Field
                        import dev.morphia.query.filters.Filters
                        import dev.morphia.aggregation.expressions.Expressions.literal

                        class TestTheWorld {
                            fun update(ds: Datastore) {
                                ds.aggregate(Object::class.java)
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
                                            geoNear(arrayOf(2.0, 42.0).toDoubleArray()),
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
                                            unionWith(Field::class.java, Sort.sort()),
                                            unionWith("UnionWithName", Projection.project()),
                                            Unset.unset("unset"),
                                            Unwind.unwind("unwind"))
                                    .execute(Object::class.java)
                                    .tryNext()
                            }
                        }"""));
    }

}
