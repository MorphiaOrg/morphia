package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.FindOptions;
import dev.morphia.query.Meta;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.meta;
import static dev.morphia.aggregation.expressions.MetadataKeyword.*;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.text;

public class TestMeta extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/meta/example1
     * 
     */
    @Test(testName = "``$meta: \"textScore\"`` :: Aggregation")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false,
                (aggregation) -> aggregation.pipeline(match(text("cake")), group(id(meta())).field("count", sum(1))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/meta/example2
     * 
     */
    @Test(testName = "``$meta: \"textScore\"`` :: Find and Project")
    public void testExample2() {
        testQuery(
                new ActionTestOptions().orderMatters(false)
                        .findOptions(new FindOptions().projection().project(Meta.textScore("score"))),
                (query) -> query.filter(text("cake")));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/meta/example3
     */
    @Test(testName = "``$meta: \"indexKey\"`` :: Aggregation")
    public void testExample3() {
        testPipeline(ServerVersion.ANY, true, true, aggregation -> aggregation.pipeline(match(eq("type", "apparel")),
                addFields().field("idxKey", meta(INDEXKEY))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/meta/example4
     */
    @Test(testName = "``$meta: \"indexKey\"`` :: Find and Project")
    public void testExample4() {
        testQuery(
                new ActionTestOptions().orderMatters(false).removeIds(true)
                        .findOptions(new FindOptions().projection().project(Meta.indexKey("idxKey"))),
                (query) -> query.filter(eq("type", "apparel")));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/meta/example5
     */
    @Test(testName = "``$meta: \"indexKey\"`` :: Aggregation [1]")
    public void testExample5() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(match(gte("price", 10)),
                addFields().field("idxKey", meta(INDEXKEY))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/meta/example6
     */
    @Test(testName = "``$meta: \"indexKey\"`` :: Find and Project [1]")
    public void testExample6() {
        testQuery(
                new ActionTestOptions().orderMatters(false).removeIds(true)
                        .findOptions(new FindOptions().projection().project(Meta.indexKey("idxKey"))),
                (query) -> query.filter(gte("price", 10)));
    }

}
