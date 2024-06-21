package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.maxN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.test.ServerVersion.v52;

public class TestMaxN extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/maxN/example1
     * 
     */
    @Test(testName = "Find the Maximum Three ``Scores`` for a Single Game")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v52).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(match(eq("gameId", "G1")),
                        group(id("$gameId")).field("maxThreeScores", maxN(3, array("$score", "$playerId")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/maxN/example2
     * 
     */
    @Test(testName = "Finding the Maximum Three Scores Across Multiple Games")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(v52).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation
                        .pipeline(group(id("$gameId")).field("maxScores", maxN(3, array("$score", "$playerId")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/maxN/example3
     * 
     */
    @Test(testName = "Computing ``n`` Based on the Group Key for ``$group``")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(v52).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(group(id().field("gameId", "$gameId")).field("gamescores", maxN(
                        condition(ComparisonExpressions.eq("$gameId", "G2"), 1, 3), array("$score", "$playerId")))));
    }
}
