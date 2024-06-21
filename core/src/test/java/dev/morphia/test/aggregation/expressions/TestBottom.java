package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.bottom;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.test.ServerVersion.v52;

public class TestBottom extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/bottom/example1
     * 
     */
    @Test(testName = "Find the Bottom ``Score``")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v52).orderMatters(false),
                (aggregation) -> aggregation.pipeline(match(eq("gameId", "G1")), group(id("$gameId")).field("playerId",
                        bottom(array("$playerId", "$score"), descending("score")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/bottom/example2
     * 
     */
    @Test(testName = "Finding the Bottom ``Score`` Across Multiple Games")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(v52).orderMatters(false),
                (aggregation) -> aggregation.pipeline(group(id("$gameId")).field("playerId",
                        bottom(array("$playerId", "$score"), descending("score")))));
    }

}
