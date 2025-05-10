package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.firstN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.eq;

public class TestFirstN extends AggregationTest {
    @Test
    public void testComputedN() {
        testPipeline("5.2.0", "computedN", false, false, (aggregation) -> aggregation
                .group(group(id()
                        .field("gameId", field("gameId")))
                        .field("gamescores", firstN(
                                condition(
                                        ComparisonExpressions.eq(field("gameId"), value("G2")),
                                        value(1),
                                        value(3)),
                                field("score")))));
    }

    @Test
    public void testSingleGame() {
        testPipeline("5.2.0", "singleGame", false, false, (aggregation) -> aggregation
                .match(eq("gameId", "G1"))
                .group(group(id(field("gameId")))
                        .field("firstThreeScores", firstN(
                                value(3),
                                array(field("playerId"), field("score"))))));

    }

    @Test
    public void testAcrossGames() {
        testPipeline("5.2.0", "acrossGames", false, false, (aggregation) -> aggregation
                .group(group(id("$gameId"))
                        .field("playerId", firstN(
                                value(3),
                                array(field("playerId"), field("score"))))));

    }

    @Test
    public void testSortedScores() {
        testPipeline("5.2.0", "sortedScores", false, false, (aggregation) -> aggregation
                .sort(sort()
                        .descending("score"))
                .group(group(id("$gameId"))
                        .field("playerId", firstN(
                                value(3),
                                array(field("playerId"), field("score"))))));

    }
}
