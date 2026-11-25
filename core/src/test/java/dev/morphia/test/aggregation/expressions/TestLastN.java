package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.query.Type;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.lastN;
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
import static dev.morphia.query.filters.Filters.regex;
import static dev.morphia.query.filters.Filters.type;

public class TestLastN extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.2.0");
    }

    @Test
    @DisplayName("Find the Last Three Player Scores for a Single Game")
    public void testFindTheLastThreePlayerScoresForASingleGame() {
        testPipeline((aggregation) -> aggregation
                .pipeline(
                        match(eq("gameId", "G1")),
                        group(id("$gameId"))
                                .field("lastThreeScores", lastN(
                                        3,
                                        array("$playerId", "$score")))));

    }

    @Test
    @DisplayName("Finding the Last Three Player Scores Across Multiple Games")
    public void testFindingTheLastThreePlayerScoresAcrossMultipleGames() {
        testPipeline((aggregation) -> aggregation
                .pipeline(group(id("$gameId"))
                        .field("playerId", lastN(
                                3,
                                array("$playerId", "$score")))));

    }

    @Test
    @DisplayName("Using ``$sort`` With ``$lastN``")
    public void testUsingSortWithLastN() {
        testPipeline((aggregation) -> aggregation
                .pipeline(
                        sort().descending("score"),
                        group(id("$gameId"))
                                .field("playerId", lastN(
                                        3,
                                        array("$playerId", "$score")))));

    }

    @Test
    @DisplayName("Computing ``n`` Based on the Group Key for ``$group``")
    public void testComputingNBasedOnTheGroupKeyForGroup() {
        testPipeline((aggregation) -> aggregation
                .pipeline(group(id().field("gameId", "$gameId"))
                        .field("gamescores", lastN(
                                condition(ComparisonExpressions.eq("$gameId", "G2"), 1, 3),
                                "$score"))));
    }

    @Test
    @DisplayName("Find the Last Three Movies Across Multiple Genres")
    public void testFindTheLastThreeMoviesAcrossMultipleGenres() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(type("year", Type.INTEGER_32_BIT)),
                unwind("$genres"),
                sort().ascending("year").ascending("title"),
                group(id("$genres")).field("movies", lastN(3, array("$title", "$year"))),
                sort().ascending("_id"),
                limit(5)));
    }

    @Test
    @DisplayName("Find the Last Three Movies for a Single Genre")
    public void testFindTheLastThreeMoviesForASingleGenre() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("genres", "Horror"), type("year", Type.INTEGER_32_BIT)),
                sort().ascending("year").ascending("title"),
                group(id("Horror")).field("lastThreeMovies", lastN(3, array("$title", "$year")))));
    }

    @Test
    @DisplayName("Set n Based on the Group Key")
    public void testSetNBasedOnTheGroupKey() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("rated", List.of("G", "PG", "PG-13", "R")), type("year", Type.INTEGER_32_BIT), regex("title", "^[A-Za-z0-9]")),
                sort().ascending("title"),
                group(id(document().field("rated", "$rated"))).field("movies",
                        lastN(condition(ComparisonExpressions.eq("$rated", "PG"), 3, 1), "$title")),
                sort().ascending("_id.rated")));
    }
}
