package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.top;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.test.ServerVersion.v52;

public class TestTop extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/top/example1
     * 
     */
    @Test(testName = "Find the Top ``Score``")
    public void testExample1() {
        testPipeline(v52, false, false, (aggregation) -> aggregation.pipeline(match(eq("gameId", "G1")),
                group(id("$gameId")).field("playerId", top(array("$playerId", "$score"), descending("score")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/top/example2
     * 
     */
    @Test(testName = "Find the Top ``Score`` Across Multiple Games")
    public void testExample2() {
        testPipeline(v52, false, false, (aggregation) -> aggregation.pipeline(
                group(id("$gameId")).field("playerId", top(array("$playerId", "$score"), descending("score")))));
    }

}
