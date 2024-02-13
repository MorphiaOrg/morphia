package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.divide;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.acos;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAcos extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ANY, false, true, aggregation -> aggregation
                .pipeline(addFields()
                        .field("angle_a",
                                radiansToDegrees(
                                        acos(
                                                divide(
                                                        "$side_b",
                                                        "$hypotenuse"))))));

    }

    @Test
    public void testExample2() {
        testPipeline(ANY, false, true, aggregation -> aggregation
                .pipeline(addFields()
                        .field("angle_a",
                                acos(
                                        divide(
                                                "$side_b",
                                                "$hypotenuse")))));

    }
}
