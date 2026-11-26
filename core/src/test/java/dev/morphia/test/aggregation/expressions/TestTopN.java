package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.topN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
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
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.in;

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
        testPipeline((aggregation) -> aggregation.pipeline(
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
        testPipeline((aggregation) -> aggregation.pipeline(
                group(id("$gameId")).field("playerId", topN(3, array("$playerId", "$score"), descending("score")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/topN/computingNBasedOnTheGroupKeyForGroup
     * 
     */
    @Test
    @DisplayName("Computing ``n`` Based on the Group Key for ``$group``")
    public void testComputingNBasedOnTheGroupKeyForGroup() {
        testPipeline((aggregation) -> aggregation.pipeline(group(id(document("gameId", "$gameId"))).field("gamescores", topN(
                condition(ComparisonExpressions.eq("$gameId", "G2"), 1, 3), "$score", descending("score")))));
    }

    @Test
    @DisplayName("Find the Highest-Rated Movies for Each Genre")
    public void testFindTheHighestRatedMoviesForEachGenre() {
        testPipeline((aggregation) -> aggregation.pipeline(
                unwind("$genres"),
                match(gt("imdb.rating", 0)),
                group(id("$genres")).field("topRatedMovies",
                        topN(3, array("$title", "$imdb.rating"), descending("imdb.rating"), ascending("title"))),
                sort().ascending("_id"),
                limit(5)));
    }

    @Test
    @DisplayName("Find the Three Highest-Rated Movies in a Single Genre")
    public void testFindTheThreeHighestRatedMoviesInASingleGenre() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("genres", "Short"), gt("imdb.rating", 0)),
                group(id("Short")).field("topRatedMovies",
                        topN(3, array("$title", "$imdb.rating"), descending("imdb.rating"), ascending("title")))));
    }

    @Test
    @DisplayName("Set n Dynamically Based on the Group Key")
    public void testSetNDynamicallyBasedOnTheGroupKey() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("rated", List.of("G", "PG", "PG-13", "R")), gt("imdb.rating", 0)),
                group(id(document().field("rated", "$rated"))).field("movies",
                        topN(condition(ComparisonExpressions.eq("$rated", "PG"), 3, 1),
                                document().field("title", "$title").field("rating", "$imdb.rating"), descending("imdb.rating"))),
                sort().ascending("_id.rated")));
    }
}
