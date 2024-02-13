package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.SystemVariables.REMOVE;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestProject extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("title")
                        .include("author")));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("title")
                        .include("author")));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .exclude("author.first")
                        .exclude("lastModified")));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("title")
                        .include("author.first")
                        .include("author.last")
                        .include("author.middle", condition(
                                eq("", "$author.middle"), REMOVE, "$author.middle"))));
    }

    @Test
    public void testExample5() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("stop.title")));
    }

    @Test
    public void testExample6() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("stop.title")));
    }

    @Test
    public void testExample7() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("myArray", array("$x", "$y"))));
    }

    @Test
    public void testExample8() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("x", "$name")));
    }

    @Test
    public void testExample9() {
        // unsupported multiple examples here
        /*
         * testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
         * project()
         * .suppressId()
         * .include("x", "$name")));
         */
    }

}
