package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.*;
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
                                map("$quizzes",
                                        add(
                                                "$$grade",
                                                2))
                                        .as("grade"))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("city", "$city")
                        .include("integerValues",
                                map(
                                        "$distances",
                                        trunc("$$decimalValue"))
                                        .as("decimalValue"))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("tempsF",
                                map(
                                        "$tempsC",
                                        add(
                                                multiply("$$tempInCelsius", 1.8),
                                                32))
                                        .as("tempInCelsius"))));
    }
}
