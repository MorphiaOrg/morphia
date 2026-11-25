package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.bottom;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lte;

public class TestBottom extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/bottom/findTheBottomScore
     * 
     */
    @Test
    @DisplayName("Find the Bottom ``Score``")
    public void testFindTheBottomScore() {
        checkMinServerVersion("5.2.0");
        testPipeline((aggregation) -> aggregation.pipeline(
                match(eq("gameId", "G1")),
                group(id("$gameId")).field("playerId", bottom(array("$playerId", "$score"), descending("score")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/bottom/findingTheBottomScoreAcrossMultipleGames
     * 
     */
    @Test
    @DisplayName("Finding the Bottom ``Score`` Across Multiple Games")
    public void testFindingTheBottomScoreAcrossMultipleGames() {
        checkMinServerVersion("5.2.0");
        testPipeline((aggregation) -> aggregation.pipeline(
                group(id("$gameId")).field("playerId", bottom(array("$playerId", "$score"), descending("score")))));
    }

    @Test
    @DisplayName("Find the Bottom Rating Across Multiple Years")
    public void testFindTheBottomRatingAcrossMultipleYears() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(exists("imdb.rating"), gte("year", 2010), lte("year", 2012)),
                group(id("$year")).field("lowestRatedMovie", bottom(array("$title", "$imdb.rating"), descending("imdb.rating"))),
                sort().ascending("_id")));
    }

    @Test
    @DisplayName("Find the Bottom Rating in a Genre")
    public void testFindTheBottomRatingInAGenre() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(eq("genres", "Comedy")),
                group(id("Comedy")).field("lowestRatedMovie", bottom(array("$title", "$imdb.rating"), descending("imdb.rating")))));
    }
}
