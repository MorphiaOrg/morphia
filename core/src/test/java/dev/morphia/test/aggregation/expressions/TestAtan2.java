package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.atan2;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestAtan2 extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/atan2/example1
     * 
     */
    @Test(testName = "main :: Inverse Tangent of Value in Degrees")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("angle_a", radiansToDegrees(atan2("$side_b", "$side_a")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/atan2/example2
     * 
     */
    @Test(testName = "main :: Inverse Tangent of Value in Radians")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("angle_a", atan2("$side_b", "$side_a"))));
    }

}
