package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.tan;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestTan extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/tan/example1
     * 
     */
    @Test(testName = "main :: Tangent of Value in Degrees")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("side_b", multiply(tan(degreesToRadians("$angle_a")), "$side_a"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/tan/example2
     * 
     */
    @Test(testName = "main :: Tangent of Value in Radians")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("side_b", multiply(tan("$angle_a"), "$side_a"))));
    }

}
