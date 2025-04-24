package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.topN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;

public class TestTopN extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/topN/example1
     * 
     */
    @Test(testName = "Find the Three Highest ``Scores``")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion("5.2.0").orderMatters(false),
                (aggregation) -> aggregation.pipeline(match(eq("gameId", "G1")), group(id("$gameId")).field("playerId",
                        topN(3, array("$playerId", "$score"), descending("score")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/topN/example2
     * 
     */
    @Test(testName = "Finding the Three Highest Score Documents Across Multiple Games")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion("5.2.0").orderMatters(false),
                (aggregation) -> aggregation.group(group(id("$gameId")).field("playerId",
                        topN(3, array("$playerId", "$score"), descending("score")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/topN/example3
     * 
     */
    @Test(testName = "Computing ``n`` Based on the Group Key for ``$group``")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion("5.2.0").orderMatters(false),
                (aggregation) -> aggregation.group(group(id(document("gameId", "$gameId"))).field("gamescores", topN(
                        condition(ComparisonExpressions.eq("$gameId", "G2"), 1, 3), "$score", descending("score")))));
    }
}
