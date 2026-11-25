package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.maxN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.filters.Filters.in;

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
        testPipeline((aggregation) -> aggregation.pipeline(match(eq("gameId", "G1")),
                group(id("$gameId")).field("maxThreeScores", maxN(3, array("$score", "$playerId")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/maxN/findingTheMaximumThreeScoresAcrossMultipleGames
     * 
     */
    @Test
    @DisplayName("Finding the Maximum Three Scores Across Multiple Games")
    public void testFindingTheMaximumThreeScoresAcrossMultipleGames() {
        testPipeline((aggregation) -> aggregation
                .pipeline(group(id("$gameId")).field("maxScores", maxN(3, array("$score", "$playerId")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/maxN/computingNBasedOnTheGroupKeyForGroup
     * 
     */
    @Test
    @DisplayName("Computing ``n`` Based on the Group Key for ``$group``")
    public void testComputingNBasedOnTheGroupKeyForGroup() {
        testPipeline((aggregation) -> aggregation.pipeline(group(id().field("gameId", "$gameId")).field("gamescores", maxN(
                condition(ComparisonExpressions.eq("$gameId", "G2"), 1, 3), array("$score", "$playerId")))));
    }

    @Test
    @DisplayName("Find the Three Highest Rated Movies Across Multiple Years")
    public void testFindTheThreeHighestRatedMoviesAcrossMultipleYears() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("year", List.of(1999, 2000, 2001)), exists("imdb.rating")),
                group(id("$year")).field("topThreeRatings", maxN(3, "$imdb.rating")),
                sort().ascending("_id")));
    }

    @Test
    @DisplayName("Find the Three Highest Rated Movies for a Single Year")
    public void testFindTheThreeHighestRatedMoviesForASingleYear() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("year", 1999), exists("imdb.rating")),
                group(id(1999)).field("topThreeRatings", maxN(3, "$imdb.rating"))));
    }

    @Test
    @DisplayName("Set n Based on the Group Key")
    public void testSetNBasedOnTheGroupKey() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("year", List.of(1999, 2000, 2001)), exists("imdb.rating")),
                group(id(document().field("year", "$year"))).field("topRatedMovies",
                        maxN(condition(ComparisonExpressions.eq("$year", 2001), 3, 1), "$imdb.rating")),
                sort().ascending("_id.year")));
    }
}
