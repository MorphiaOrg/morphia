package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.aggregation.AggregationTest;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.firstN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.eq;

public class TestFirstN extends AggregationTest {
    @Test
    public void testComputedN() {
        testPipeline(5.2, "computedN", "gamescores", false, false, (aggregation) -> {
            return aggregation
                       .group(group(id()
                                        .field("gameId", field("gameId")))
                                  .field("gamescores", firstN(
                                      condition(
                                          ComparisonExpressions.eq(field("gameId"), value("G2")),
                                          value(1),
                                          value(3)),
                                      field("score"))));
        });
    }

    @Test
    public void testFirst3Scores() {
        testPipeline(5.2, "first3scores", "gamescores", false, false, (aggregation) -> {
            return aggregation
                       .match(eq("gameId", "G1"))
                       .group(group(id(field("gameId")))
                                  .field("firstThreeScores", firstN(
                                      value(3),
                                      array(field("playerId"), field("score")))));
        });

    }

    @Test
    public void testFirst3ScoresAcrossGames() {
        testPipeline(5.2, "first3scoresAcrossGames", "gamescores", false, false, (aggregation) -> {
            return aggregation
                       .group(group(id("$gameId"))
                                  .field("playerId", firstN(
                                      value(3),
                                      array(field("playerId"), field("score")))));
        });

    }

    @Test
    @Ignore("needs #2014 to be complete")
    public void testNullAndMissing() {
        testPipeline(5.2, "nullAndMissing", "gamescores", false, false, (aggregation) -> {
            return aggregation.documents(
                                  document("playerId", value("PlayerA"))
                                      .field("gameId", value("G1"))
                                      .field("score", value(1)),
                                  document("playerId", value("PlayerB"))
                                      .field("gameId", value("G1"))
                                      .field("score", value(2)),
                                  document("playerId", value("PlayerC"))
                                      .field("gameId", value("G1"))
                                      .field("score", value(3)),
                                  document("playerId", value("PlayerD"))
                                      .field("gameId", value("G1")),
                                  document("playerId", value("PlayerE"))
                                      .field("gameId", value("G1"))
                                      .field("score", value(null)))
                              .group(group(id("$gameId"))
                                         .field("firstFiveScores", firstN(value(5), field("score"))));
        });
    }

    @Test
    public void testSortedFirst3Scores() {
        testPipeline(5.2, "sortedFirst3scores", "gamescores", false, false, (aggregation) -> {
            return aggregation
                       .sort(sort()
                                 .descending("score"))
                       .group(group(id("$gameId"))
                                  .field("playerId", firstN(
                                      value(3),
                                      array(field("playerId"), field("score")))));
        });

    }
}
