package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.ArrayExpressions.reduce;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.StringExpressions.concat;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.gt;

public class TestReduce extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                group(id(field("experimentId")))
                        .field("probabilityArr", push(field("probability"))),
                project()
                        .include("description")
                        .include("results", reduce(
                                field("probabilityArr"),
                                value(1),
                                multiply(value("$$value"), value("$$this"))))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(gt("hobbies", array())),
                project()
                        .include("name")
                        .include("bio", reduce(
                                field("hobbies"),
                                value("My hobbies include:"),
                                concat(
                                        value("$$value"),
                                        condition(
                                                eq(value("$$value"), value("My hobbies include:")),
                                                value(" "),
                                                value(", ")),
                                        value("$$this"))))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                project()
                        .include("collapsed", reduce(
                                field("arr"),
                                array(),
                                concatArrays(value("$$value"), value("$$this"))))));
    }

}
