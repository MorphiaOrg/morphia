package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.acosh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestAcosh extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/acosh/inverseHyperbolicCosineInDegrees
     * 
     */
    @Test
    @DisplayName("main :: Inverse Hyperbolic Cosine in Degrees")
    public void testInverseHyperbolicCosineInDegrees() {
        testPipeline(aggregation -> aggregation
                .pipeline(addFields().field("y-coordinate", radiansToDegrees(acosh("$x-coordinate")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/acosh/inverseHyperbolicCosineInRadians
     * 
     */
    @Test
    @DisplayName("main :: Inverse Hyperbolic Cosine in Radians")
    public void testInverseHyperbolicCosineInRadians() {
        testPipeline(aggregation -> aggregation.pipeline(addFields().field("y-coordinate", acosh("$x-coordinate"))));

    }
}
