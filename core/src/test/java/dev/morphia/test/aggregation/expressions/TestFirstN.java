package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.firstN;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Documents.documents;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.test.ServerVersion.v52;

public class TestFirstN extends TemplatedTestBase {
    @Test(testName = "Find the First Three Player Scores for a Single Game")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v52).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(
                        match(eq("gameId", "G1")),
                        group(id("$gameId"))
                                .field("firstThreeScores", firstN(
                                        3,
                                        array("$playerId", "$score")))));
    }

    @Test(testName = "Finding the First Three Player Scores Across Multiple Games")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(v52).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(
                        group(id("$gameId"))
                                .field("playerId", firstN(
                                        3,
                                        array("$playerId", "$score")))));

    }

    @Test(testName = "Using ``$sort`` With ``$firstN``")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(v52).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(
                        sort().descending("score"),
                        group(id("$gameId"))
                                .field("playerId", firstN(
                                        3,
                                        array("$playerId", "$score")))));

    }

    @Test(testName = "Computing ``n`` Based on the Group Key for ``$group``")
    public void testExample4() {
        testPipeline(new ActionTestOptions().serverVersion(v52).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(
                        group(id()
                                .field("gameId", "$gameId"))
                                .field("gamescores", firstN(
                                        condition(
                                                ComparisonExpressions.eq("$gameId", "G2"),
                                                1,
                                                3),
                                        "$score"))));
    }

    @Test(enabled = false, description = "this needs to run against the db rather than a collection and that requires fixes in the agg code")
    public void testExample5() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(
                        documents(document().field("array", array(10, 20, 30, 40))),
                        project()
                                .include("firstThreeElements", firstN(3, "$array"))

                ));
    }

}
