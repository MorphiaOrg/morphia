package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.topN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;

public class TestTopN extends AggregationTest {
    @Test
    public void testSingleGame() {
        testPipeline("5.2.0", "singleGame", false, false, (aggregation) -> aggregation
                .match(eq("gameId", "G1"))
                .group(group(id(field("gameId")))
                        .field("playerId", topN(
                                value(3),
                                array(field("playerId"), field("score")),
                                descending("score")))));
    }

    @Test
    public void testAcrossGames() {
        testPipeline("5.2.0", "acrossGames", false, false, (aggregation) -> aggregation
                .group(group(id(field("gameId")))
                        .field("playerId", topN(
                                value(3),
                                array(field("playerId"), field("score")),
                                descending("score")))));
    }

    @Test
    public void testComputedN() {
        testPipeline("5.2.0", "computedN", false, false, (aggregation) -> aggregation
                .group(group(id(document("gameId", field("gameId"))))
                        .field("gamescores", topN(
                                condition(
                                        eq(field("gameId"), value("G2")),
                                        value(1),
                                        value(3)),
                                field("score"),
                                descending("score")))));
    }
}
