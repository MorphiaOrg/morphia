package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.divide;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.asin;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestAsin extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/asin/inverseSineOfValueInDegrees
     * 
     */
    @Test
    @DisplayName("main :: Inverse Sine of Value in Degrees")
    public void testInverseSineOfValueInDegrees() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("angle_a", radiansToDegrees(asin(divide("$side_a", "$hypotenuse"))))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/asin/inverseSineOfValueInRadians
     * 
     */
    @Test
    @DisplayName("main :: Inverse Sine of Value in Radians")
    public void testInverseSineOfValueInRadians() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("angle_a", asin(divide("$side_a", "$hypotenuse")))));
    }

}
