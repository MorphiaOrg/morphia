package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.test.aggregation.AggregationTest;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.WindowExpressions.bottomN;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;

public class TestBottomN extends AggregationTest {
    @Override
    public String prefix() {
        return "testBottomN";
    }

    @Test
    public void testThreeLowestScores() {
        testPipeline(5.2, "threeLowestScores", "gamescores", false, (aggregation) -> {
            return aggregation
                       .match(eq("gameId", "G1"))
                       .group(group(id(field("gameId")))
                                  .field("playerId", bottomN(
                                      value(3),
                                      array(field("playerId"), field("score")),
                                      descending("score"))));
        });
    }

    @Test
    public void testThreeLowestScoresAcrossGames() {
        testPipeline(5.2, "threeLowestScoresAcrossGames", "gamescores", false, (aggregation) -> {
            return aggregation
                       .group(group(id(field("gameId")))
                                  .field("playerId", bottomN(
                                      value(3),
                                      array(field("playerId"), field("score")),
                                      descending("score"))));
        });
    }

    @Test
    public void testComputedN() {
        testPipeline(5.2, "computedN", "gamescores", false, (aggregation) -> {
            return aggregation
                       .group(group(id(document("gameId", field("gameId"))))
                                  .field("gamescores", bottomN(
                                      condition(
                                          eq(field("gameId"), value("G2")),
                                          value(1),
                                          value(3)),
                                      field("score"),
                                      descending("score"))));
        });

    }

}
