package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.asinh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestAsinh extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/asinh/inverseHyperbolicSineInDegrees
     * 
     */
    @Test
    @DisplayName("main :: Inverse Hyperbolic Sine in Degrees")
    public void testInverseHyperbolicSineInDegrees() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("y-coordinate", radiansToDegrees(asinh("$x-coordinate")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/asinh/inverseHyperbolicSineInRadians
     * 
     */
    @Test
    @DisplayName("main :: Inverse Hyperbolic Sine in Radians")
    public void testInverseHyperbolicSineInRadians() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("y-coordinate", asinh("$x-coordinate"))));
    }

}
