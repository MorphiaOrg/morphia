package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.topN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;

public class TestTopN extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.2.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/topN/findTheThreeHighestScores
     * 
     */
    @Test
    @DisplayName("Find the Three Highest ``Scores``")
    public void testFindTheThreeHighestScores() {
        testPipeline(new ActionTestOptions().orderMatters(false), (aggregation) -> aggregation.pipeline(
                match(eq("gameId", "G1")),
                group(id("$gameId")).field("playerId", topN(3, array("$playerId", "$score"), descending("score")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/topN/findingTheThreeHighestScoreDocumentsAcrossMultipleGames
     * 
     */
    @Test
    @DisplayName("Finding the Three Highest Score Documents Across Multiple Games")
    public void testFindingTheThreeHighestScoreDocumentsAcrossMultipleGames() {
        testPipeline(new ActionTestOptions().orderMatters(false), (aggregation) -> aggregation.pipeline(
                group(id("$gameId")).field("playerId", topN(3, array("$playerId", "$score"), descending("score")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/topN/computingNBasedOnTheGroupKeyForGroup
     * 
     */
    @Test
    @DisplayName("Computing ``n`` Based on the Group Key for ``$group``")
    public void testComputingNBasedOnTheGroupKeyForGroup() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(group(id(document("gameId", "$gameId"))).field("gamescores", topN(
                        condition(ComparisonExpressions.eq("$gameId", "G2"), 1, 3), "$score", descending("score")))));
    }
}
