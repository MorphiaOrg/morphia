package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.sin;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestSin extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/sin/example1
     * 
     */
    @Test(testName = "main :: Sine of Value in Degrees")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("side_b", multiply(sin(degreesToRadians("$angle_a")), "$hypotenuse"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sin/example2
     * 
     */
    @Test(testName = "main :: Sine of Value in Radians")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("side_b", multiply(sin("$angle_a"), "$hypotenuse"))));
    }

}
