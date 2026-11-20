package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.maxN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;

public class TestMaxN extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.2.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/maxN/findTheMaximumThreeScoresForASingleGame
     * 
     */
    @Test
    @DisplayName("Find the Maximum Three ``Scores`` for a Single Game")
    public void testFindTheMaximumThreeScoresForASingleGame() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(match(eq("gameId", "G1")),
                        group(id("$gameId")).field("maxThreeScores", maxN(3, array("$score", "$playerId")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/maxN/findingTheMaximumThreeScoresAcrossMultipleGames
     * 
     */
    @Test
    @DisplayName("Finding the Maximum Three Scores Across Multiple Games")
    public void testFindingTheMaximumThreeScoresAcrossMultipleGames() {
        testPipeline(new ActionTestOptions().orderMatters(false), (aggregation) -> aggregation
                .pipeline(group(id("$gameId")).field("maxScores", maxN(3, array("$score", "$playerId")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/maxN/computingNBasedOnTheGroupKeyForGroup
     * 
     */
    @Test
    @DisplayName("Computing ``n`` Based on the Group Key for ``$group``")
    public void testComputingNBasedOnTheGroupKeyForGroup() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(group(id().field("gameId", "$gameId")).field("gamescores", maxN(
                        condition(ComparisonExpressions.eq("$gameId", "G2"), 1, 3), array("$score", "$playerId")))));
    }
}
