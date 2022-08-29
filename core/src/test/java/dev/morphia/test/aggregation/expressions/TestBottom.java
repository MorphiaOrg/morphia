package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.WindowExpressions.bottom;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;

public class TestBottom extends AggregationTest {
    @Override
    public String prefix() {
        return "bottom";
    }

    @Test
    public void testBottom() {
        testPipeline(5.2, "bottom", "gamescores", false, false, (aggregation) -> {
            return aggregation
                       .match(eq("gameId", "G1"))
                       .group(group(id(field("gameId")))
                                  .field("playerId", bottom(
                                      array(field("playerId"), field("score")),
                                      descending("score"))));
        });
    }

    @Test
    public void testBottomAcrossGames() {
        testPipeline(5.2, "bottomAcrossGames", "gamescores", false, false, (aggregation) -> {
            return aggregation
                       .group(group(id(field("gameId")))
                                  .field("playerId", bottom(
                                      array(field("playerId"), field("score")),
                                      descending("score"))));
        });
    }

}
