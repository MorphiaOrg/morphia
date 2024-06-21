package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.divide;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.asin;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestAsin extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/asin/example1
     * 
     */
    @Test(testName = "main :: Inverse Sine of Value in Degrees")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(addFields().field("angle_a", radiansToDegrees(asin(divide("$side_a", "$hypotenuse"))))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/asin/example2
     * 
     */
    @Test(testName = "main :: Inverse Sine of Value in Radians")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(addFields().field("angle_a", asin(divide("$side_a", "$hypotenuse")))));
    }

}
