package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.sinh;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestSinh extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/sinh/hyperbolicSineOfValueInDegrees
     * 
     */
    @Test
    @DisplayName("main :: Hyperbolic Sine of Value in Degrees")
    public void testHyperbolicSineOfValueInDegrees() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("sinh_output", sinh(degreesToRadians("$angle")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sinh/hyperbolicSineOfValueInRadians
     * 
     */
    @Test
    @DisplayName("main :: Hyperbolic Sine of Value in Radians")
    public void testHyperbolicSineOfValueInRadians() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("sinh_output", sinh("$angle"))));
    }

}
