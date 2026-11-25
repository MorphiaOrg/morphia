package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.tanh;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestTanh extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/tanh/hyperbolicTangentInDegrees
     * 
     */
    @Test
    @DisplayName("main :: Hyperbolic Tangent in Degrees")
    public void testHyperbolicTangentInDegrees() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("tanh_output", tanh(degreesToRadians("$angle")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/tanh/hyperbolicTangentInRadians
     * 
     */
    @Test
    @DisplayName("main :: Hyperbolic Tangent in Radians")
    public void testHyperbolicTangentInRadians() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("tanh_output", tanh("$angle"))));
    }

}
