package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.sin;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestSin extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/sin/sineOfValueInDegrees
     * 
     */
    @Test
    @DisplayName("main :: Sine of Value in Degrees")
    public void testSineOfValueInDegrees() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("side_b", multiply(sin(degreesToRadians("$angle_a")), "$hypotenuse"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sin/sineOfValueInRadians
     * 
     */
    @Test
    @DisplayName("main :: Sine of Value in Radians")
    public void testSineOfValueInRadians() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("side_b", multiply(sin("$angle_a"), "$hypotenuse"))));
    }

}
