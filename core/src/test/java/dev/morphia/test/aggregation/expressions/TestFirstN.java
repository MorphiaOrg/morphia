package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.firstN;
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

public class TestFirstN extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.2.0");
    }

    @Test
    @DisplayName("Find the First Three Player Scores for a Single Game")
    public void testFindTheFirstThreePlayerScoresForASingleGame() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(eq("gameId", "G1")),
                group(id("$gameId"))
                        .field("firstThreeScores", firstN(
                                3,
                                array("$playerId", "$score")))));
    }

    @Test
    @DisplayName("Finding the First Three Player Scores Across Multiple Games")
    public void testFindingTheFirstThreePlayerScoresAcrossMultipleGames() {
        testPipeline((aggregation) -> aggregation.pipeline(
                group(id("$gameId"))
                        .field("playerId", firstN(
                                3,
                                array("$playerId", "$score")))));

    }

    @Test
    @DisplayName("Using ``$sort`` With ``$firstN``")
    public void testUsingSortWithFirstN() {
        testPipeline((aggregation) -> aggregation.pipeline(
                sort().descending("score"),
                group(id("$gameId"))
                        .field("playerId", firstN(
                                3,
                                array("$playerId", "$score")))));

    }

    @Test
    @DisplayName("Computing ``n`` Based on the Group Key for ``$group``")
    public void testComputingNBasedOnTheGroupKeyForGroup() {
        testPipeline((aggregation) -> aggregation.pipeline(
                group(id()
                        .field("gameId", "$gameId"))
                        .field("gamescores", firstN(
                                condition(
                                        ComparisonExpressions.eq("$gameId", "G2"),
                                        1,
                                        3),
                                "$score"))));
    }

    @Test
    @DisplayName("Find the First Three Movies Across Multiple Genres")
    public void testFindTheFirstThreeMoviesAcrossMultipleGenres() {
        testPipeline((aggregation) -> aggregation.pipeline(
                unwind("$genres"),
                sort().ascending("year"),
                group(id("$genres")).field("movies", firstN(3, array("$title", "$year"))),
                sort().ascending("_id"),
                limit(5)));
    }

    @Test
    @DisplayName("Find the First Three Movies for a Single Genre")
    public void testFindTheFirstThreeMoviesForASingleGenre() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("genres", "Horror")),
                sort().ascending("year"),
                group(id("Horror")).field("firstThreeMovies", firstN(3, array("$title", "$year")))));
    }

    @Test
    @DisplayName("Set n Based on the Group Key")
    public void testSetNBasedOnTheGroupKey() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("rated", List.of("G", "PG", "PG-13", "R"))),
                sort().ascending("title"),
                group(id(document().field("rated", "$rated"))).field("movies",
                        firstN(condition(ComparisonExpressions.eq("$rated", "PG"), 3, 1), "$title"))));
    }
}
