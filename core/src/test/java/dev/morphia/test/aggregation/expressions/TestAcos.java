package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.divide;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.acos;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAcos extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/acos/example1
     * 
     */
    @Test(testName = "main :: Inverse Cosine of Value in Degrees")
    public void testExample1() {
        testPipeline(ANY, false, true, aggregation -> aggregation
                .pipeline(addFields().field("angle_a", radiansToDegrees(acos(divide("$side_b", "$hypotenuse"))))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/acos/example2
     * 
     */
    @Test(testName = "main :: Inverse Cosine of Value in Radians")
    public void testExample2() {
        testPipeline(ANY, false, true, aggregation -> aggregation
                .pipeline(addFields().field("angle_a", acos(divide("$side_b", "$hypotenuse")))));

    }
}
