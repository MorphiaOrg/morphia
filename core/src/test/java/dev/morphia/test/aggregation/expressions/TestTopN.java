package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.aggregation.AggregationTest;

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
import static dev.morphia.test.ServerVersion.v52;

public class TestTopN extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(v52, false, false, (aggregation) -> aggregation.pipeline(
                match(eq("gameId", "G1")),
                group(id("$gameId"))
                        .field("playerId", topN(3, array("$playerId", "$score"),
                                descending("score")))));
    }

    @Test
    public void testExample2() {
        testPipeline(v52, false, false, (aggregation) -> {
            return aggregation
                    .group(group(id("$gameId"))
                            .field("playerId", topN(
                                    3,
                                    array("$playerId", "$score"),
                                    descending("score"))));
        });
    }

    @Test
    public void testExample3() {
        testPipeline(v52, false, false, (aggregation) -> {
            return aggregation
                    .group(group(id(document("gameId", "$gameId")))
                            .field("gamescores", topN(
                                    condition(
                                            ComparisonExpressions.eq("$gameId", "G2"),
                                            1,
                                            3),
                                    "$score",
                                    descending("score"))));
        });
    }
}
