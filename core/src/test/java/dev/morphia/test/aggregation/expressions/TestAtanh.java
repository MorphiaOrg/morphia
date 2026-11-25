package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.atanh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestAtanh extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/atanh/inverseHyperbolicTangentInDegrees
     * 
     */
    @Test
    @DisplayName("main :: Inverse Hyperbolic Tangent in Degrees")
    public void testInverseHyperbolicTangentInDegrees() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("y-coordinate", radiansToDegrees(atanh("$x-coordinate")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/atanh/inverseHyperbolicTangentInRadians
     * 
     */
    @Test
    @DisplayName("main :: Inverse Hyperbolic Tangent in Radians")
    public void testInverseHyperbolicTangentInRadians() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("y-coordinate", atanh("$x-coordinate"))));
    }

}
