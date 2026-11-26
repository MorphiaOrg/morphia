package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.cosh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestCosh extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/cosh/hyperbolicCosineInDegrees
     * 
     */
    @Test
    @DisplayName("main :: Hyperbolic Cosine in Degrees")
    public void testHyperbolicCosineInDegrees() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("cosh_output", cosh(degreesToRadians("$angle")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/cosh/hyperbolicCosineInRadians
     * 
     */
    @Test
    @DisplayName("main :: Hyperbolic Cosine in Radians")
    public void testHyperbolicCosineInRadians() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("cosh_output", cosh("$angle"))));
    }

}
