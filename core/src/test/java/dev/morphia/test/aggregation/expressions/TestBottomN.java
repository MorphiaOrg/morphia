package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.bottomN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.filters.Filters.in;

public class TestBottomN extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.2.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/bottomN/findTheThreeLowestScores
     * 
     */
    @Test
    @DisplayName("Find the Three Lowest ``Scores``")
    public void testFindTheThreeLowestScores() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("gameId", "G1")),
                group(id("$gameId")).field("playerId", bottomN(3, array("$playerId", "$score"), descending("score")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/bottomN/findingTheThreeLowestScoreDocumentsAcrossMultipleGames
     * 
     */
    @Test
    @DisplayName("Finding the Three Lowest Score Documents Across Multiple Games")
    public void testFindingTheThreeLowestScoreDocumentsAcrossMultipleGames() {
        testPipeline((aggregation) -> aggregation.pipeline(
                group(id("$gameId")).field("playerId", bottomN(3, array("$playerId", "$score"), descending("score")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/bottomN/computingNBasedOnTheGroupKeyForGroup
     * 
     */
    @Test
    @DisplayName("Computing ``n`` Based on the Group Key for ``$group``")
    public void testComputingNBasedOnTheGroupKeyForGroup() {
        testPipeline((aggregation) -> aggregation.pipeline(group(id(document("gameId", "$gameId"))).field("gamescores",
                bottomN(condition(eq("$gameId", "G2"), 1, 3), "$score", descending("score")))));

    }

    @Test
    @DisplayName("Find the Lowest Rated Movies for Each Genre")
    public void testFindTheLowestRatedMoviesForEachGenre() {
        testPipeline((aggregation) -> aggregation.pipeline(
                unwind("$genres"),
                match(exists("imdb.rating")),
                group(id("$genres")).field("lowestRatedMovie",
                        bottomN(3, array("$title", "$imdb.rating"), descending("imdb.rating"), ascending("title"))),
                sort().ascending("_id"),
                limit(5)));
    }

    @Test
    @DisplayName("Find the Three Lowest Rated Movies in a Single Genre")
    public void testFindTheThreeLowestRatedMoviesInASingleGenre() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("genres", "Short"), exists("imdb.rating")),
                group(id("Short")).field("lowestRatedMovies",
                        bottomN(3, array("$title", "$imdb.rating"), descending("imdb.rating"), ascending("title")))));
    }

    @Test
    @DisplayName("Set n Dynamically Based on the Group Key")
    public void testSetNDynamicallyBasedOnTheGroupKey() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("rated", List.of("G", "PG", "PG-13", "R")), exists("imdb.rating")),
                group(id(document().field("rated", "$rated"))).field("movies",
                        bottomN(condition(ComparisonExpressions.eq("$rated", "PG"), 3, 1),
                                document().field("title", "$title").field("rating", "$imdb.rating"), descending("imdb.rating"))),
                sort().ascending("_id.rated")));
    }
}
