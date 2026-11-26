package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.minN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.in;

public class TestMinN extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.2.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/minN/findTheMinimumThreeScoresForASingleGame
     * 
     */
    @Test
    @DisplayName("Find the Minimum Three ``Scores`` for a Single Game")
    public void testFindTheMinimumThreeScoresForASingleGame() {
        testPipeline((aggregation) -> aggregation.pipeline(match(eq("gameId", "G1")),
                group(id("$gameId")).field("minScores", minN(3, array("$score", "$playerId")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/minN/findingTheMinimumThreeDocumentsAcrossMultipleGames
     * 
     */
    @Test
    @DisplayName("Finding the Minimum Three Documents Across Multiple Games")
    public void testFindingTheMinimumThreeDocumentsAcrossMultipleGames() {
        testPipeline((aggregation) -> aggregation
                .pipeline(group(id("$gameId")).field("minScores", minN(3, array("$score", "$playerId")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/minN/computingNBasedOnTheGroupKeyForGroup
     * 
     */
    @Test
    @DisplayName("Computing ``n`` Based on the Group Key for ``$group``")
    public void testComputingNBasedOnTheGroupKeyForGroup() {
        testPipeline((aggregation) -> aggregation.pipeline(group(id().field("gameId", "$gameId")).field("gamescores", minN(
                condition(ComparisonExpressions.eq("$gameId", "G2"), 1, 3), array("$score", "$playerId")))));
    }

    @Test
    @DisplayName("Find the Minimum Three Ratings Across Multiple Genres")
    public void testFindTheMinimumThreeRatingsAcrossMultipleGenres() {
        testPipeline((aggregation) -> aggregation.pipeline(
                unwind("$genres"),
                group(id("$genres")).field("minThreeRatings", minN(3, "$imdb.rating")),
                sort().ascending("_id"),
                limit(5)));
    }

    @Test
    @DisplayName("Find the Minimum Three Ratings for a Single Genre")
    public void testFindTheMinimumThreeRatingsForASingleGenre() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("genres", "Horror")),
                group(id("Horror")).field("minThreeRatings", minN(3, "$imdb.rating"))));
    }

    @Test
    @DisplayName("Set n Dynamically Based on the Group Key")
    public void testSetNDynamicallyBasedOnTheGroupKey() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("rated", List.of("G", "PG", "PG-13", "R"))),
                group(id(document().field("rated", "$rated"))).field("minMovies",
                        minN(condition(ComparisonExpressions.eq("$rated", "R"), 3, 1), "$imdb.rating"))));
    }
}
