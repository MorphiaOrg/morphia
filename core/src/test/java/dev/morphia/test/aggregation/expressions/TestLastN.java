package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.lastN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.test.ServerVersion.v52;

public class TestLastN extends TemplatedTestBase {

    @Test(testName = "Find the Last Three Player Scores for a Single Game")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v52).orderMatters(false),
                (aggregation) -> aggregation
                        .pipeline(
                                match(eq("gameId", "G1")),
                                group(id("$gameId"))
                                        .field("lastThreeScores", lastN(
                                                3,
                                                array("$playerId", "$score")))));

    }

    @Test(testName = "Finding the Last Three Player Scores Across Multiple Games")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(v52).orderMatters(false),
                (aggregation) -> aggregation
                        .pipeline(group(id("$gameId"))
                                .field("playerId", lastN(
                                        3,
                                        array("$playerId", "$score")))));

    }

    @Test(testName = "Using ``$sort`` With ``$lastN``")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(v52).orderMatters(false),
                (aggregation) -> aggregation
                        .pipeline(
                                sort().descending("score"),
                                group(id("$gameId"))
                                        .field("playerId", lastN(
                                                3,
                                                array("$playerId", "$score")))));

    }

    @Test(testName = "Computing ``n`` Based on the Group Key for ``$group``")
    public void testExample4() {
        testPipeline(new ActionTestOptions().serverVersion(v52).orderMatters(false),
                (aggregation) -> aggregation
                        .pipeline(group(id().field("gameId", "$gameId"))
                                .field("gamescores", lastN(
                                        condition(ComparisonExpressions.eq("$gameId", "G2"), 1, 3),
                                        "$score"))));
    }

    @Test
    public void testExample5() {
        //  needs db.aggregate() support
    }

}
