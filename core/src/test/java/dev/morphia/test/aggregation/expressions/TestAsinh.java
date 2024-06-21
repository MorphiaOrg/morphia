package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.asinh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestAsinh extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/asinh/example1
     * 
     */
    @Test(testName = "main :: Inverse Hyperbolic Sine in Degrees")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("y-coordinate", radiansToDegrees(asinh("$x-coordinate")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/asinh/example2
     * 
     */
    @Test(testName = "main :: Inverse Hyperbolic Sine in Radians")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("y-coordinate", asinh("$x-coordinate"))));
    }

}
