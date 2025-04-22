package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.minN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;

public class TestMinN extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/expressions/minN/example1
     * 
     */
    @Test(testName = "Find the Minimum Three ``Scores`` for a Single Game")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion("5.2.0").orderMatters(false),
                (aggregation) -> aggregation.pipeline(match(eq("gameId", "G1")),
                        group(id("$gameId")).field("minScores", minN(3, array("$score", "$playerId")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/minN/example2
     * 
     */
    @Test(testName = "Finding the Minimum Three Documents Across Multiple Games")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion("5.2.0").orderMatters(false), (aggregation) -> aggregation
                .pipeline(group(id("$gameId")).field("minScores", minN(3, array("$score", "$playerId")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/minN/example3
     * 
     */
    @Test(testName = "Computing ``n`` Based on the Group Key for ``$group``")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion("5.2.0").orderMatters(false),
                (aggregation) -> aggregation.pipeline(group(id().field("gameId", "$gameId")).field("gamescores", minN(
                        condition(ComparisonExpressions.eq("$gameId", "G2"), 1, 3), array("$score", "$playerId")))));
    }
}
