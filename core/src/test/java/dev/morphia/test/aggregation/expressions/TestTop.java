package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.top;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.in;

public class TestTop extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.2.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/top/findTheTopScore
     * 
     */
    @Test
    @DisplayName("Find the Top ``Score``")
    public void testFindTheTopScore() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(eq("gameId", "G1")),
                group(id("$gameId")).field("playerId", top(array("$playerId", "$score"), descending("score")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/top/findTheTopScoreAcrossMultipleGames
     * 
     */
    @Test
    @DisplayName("Find the Top ``Score`` Across Multiple Games")
    public void testFindTheTopScoreAcrossMultipleGames() {
        testPipeline((aggregation) -> aggregation.pipeline(
                group(id("$gameId")).field("playerId", top(array("$playerId", "$score"), descending("score")))));
    }

    @Test
    @DisplayName("Find the Top Rating Across Multiple Ratings")
    public void testFindTheTopRatingAcrossMultipleRatings() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("rated", List.of("G", "PG", "PG-13", "R")), gt("imdb.rating", 0)),
                group(id("$rated")).field("highestRatedMovie", top(array("$title", "$imdb.rating"), descending("imdb.rating"))),
                sort().ascending("_id")));
    }

    @Test
    @DisplayName("Find the Top Rating in a Genre")
    public void testFindTheTopRatingInAGenre() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(eq("genres", "Comedy"), gt("imdb.rating", 0)),
                group(id("Comedy")).field("highestRatedMovie", top(array("$title", "$imdb.rating"), descending("imdb.rating")))));
    }
}
