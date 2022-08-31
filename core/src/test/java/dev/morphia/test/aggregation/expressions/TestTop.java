package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.top;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;

public class TestTop extends AggregationTest {
    @Test
    public void testSingleGame() {
        testPipeline(5.2, "singleGame", false, false, (aggregation) -> {
            return aggregation
                       .match(eq("gameId", "G1"))
                       .group(group(id(field("gameId")))
                                  .field("playerId", top(
                                      array(field("playerId"), field("score")),
                                      descending("score"))));
        });
    }

    @Test
    public void testAcrossGames() {
        testPipeline(5.2, "acrossGames", false, false, (aggregation) -> {
            return aggregation
                       .group(group(id(field("gameId")))
                                  .field("playerId", top(
                                      array(field("playerId"), field("score")),
                                      descending("score"))));
        });
    }

}
