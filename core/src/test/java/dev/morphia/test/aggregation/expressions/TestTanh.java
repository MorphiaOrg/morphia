package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.tanh;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestTanh extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/tanh/example1
     * 
     */
    @Test(testName = "main :: Hyperbolic Tangent in Degrees")
    public void testExample1() {
        testPipeline(new ActionTestOptions().removeIds(true), (aggregation) -> aggregation
                .pipeline(addFields().field("tanh_output", tanh(degreesToRadians("$angle")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/tanh/example2
     * 
     */
    @Test(testName = "main :: Hyperbolic Tangent in Radians")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("tanh_output", tanh("$angle"))));
    }

}
