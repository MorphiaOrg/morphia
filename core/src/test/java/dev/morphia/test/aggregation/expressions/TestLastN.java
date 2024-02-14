package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.aggregation.AggregationTest;

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

public class TestLastN extends AggregationTest {

    @Test
    public void testExample1() {
        testPipeline(v52, false, false, (aggregation) -> aggregation
                .pipeline(
                        match(eq("gameId", "G1")),
                        group(id("$gameId"))
                                .field("lastThreeScores", lastN(
                                        3,
                                        array("$playerId", "$score")))));

    }

    @Test
    public void testExample2() {
        testPipeline(v52, false, false, (aggregation) -> aggregation
                .pipeline(group(id("$gameId"))
                        .field("playerId", lastN(
                                3,
                                array("$playerId", "$score")))));

    }

    @Test
    public void testExample3() {
        testPipeline(v52, false, false, (aggregation) -> aggregation
                .pipeline(
                        sort().descending("score"),
                        group(id("$gameId"))
                                .field("playerId", lastN(
                                        3,
                                        array("$playerId", "$score")))));

    }

    @Test
    public void testExample4() {
        testPipeline(v52, false, false, (aggregation) -> aggregation
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
