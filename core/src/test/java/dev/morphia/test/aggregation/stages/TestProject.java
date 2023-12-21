package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.expressions.SystemVariables;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.StringExpressions.substrBytes;
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
                        .exclude("lastModified")));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .exclude("author.first")
                        .exclude("lastModified")));
    }

    @Test
    public void testExample5() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("title")
                        .include("author.first")
                        .include("author.last")
                        .include("author.middle", condition(
                                eq(value(""), field("author.middle")),
                                SystemVariables.REMOVE,
                                field("author.middle")))));
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
                        .include("title")
                        .include("isbn", document()
                                .field("prefix", substrBytes(field("isbn"), value(0), value(3)))
                                .field("group", substrBytes(field("isbn"), value(3), value(2)))
                                .field("publisher", substrBytes(field("isbn"), value(5), value(4)))
                                .field("title", substrBytes(field("isbn"), value(9), value(3)))
                                .field("checkDigit", substrBytes(field("isbn"), value(12), value(1))))
                        .include("lastName", field("author.last"))
                        .include("copiesSold", field("copies"))));
    }

    @Test
    public void testExample8() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("myArray", array(field("x"), field("y")))));
    }

    @Test
    public void testExample9() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("x", field("name"))));
    }

}
