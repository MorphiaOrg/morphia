package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.*;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.MathExpressions.trunc;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestMap extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("adjustedGrades",
                                map(field("quizzes"),
                                        add(
                                                value("$$grade"),
                                                value(2)))
                                        .as("grade"))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("city", field("city"))
                        .include("integerValues",
                                map(
                                        field("distances"),
                                        trunc(value("$$decimalValue")))
                                        .as("decimalValue"))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("tempsF",
                                map(
                                        field("tempsC"),
                                        add(
                                                multiply(value("$$tempInCelsius"), value(1.8)),
                                                value(32)))
                                        .as("tempInCelsius"))));
    }
}
