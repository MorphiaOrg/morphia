package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.bottom;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.test.ServerVersion.v52;

public class TestBottom extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(v52, false, false, (aggregation) -> aggregation
                .pipeline(
                        match(eq("gameId", "G1")),
                        group(id("$gameId"))
                                .field("playerId", bottom(
                                        array("$playerId", "$score"),
                                        descending("score")))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                group(id("$gameId"))
                        .field("playerId",
                                bottom(array("$playerId", "$score"),
                                        descending("score")))));
    }

}
