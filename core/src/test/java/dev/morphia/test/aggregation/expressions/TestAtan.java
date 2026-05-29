package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.divide;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.atan;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestAtan extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/atan/example1
     * 
     */
    @Test
    @DisplayName("main :: Inverse Tangent of Value in Degrees")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("angle_a", radiansToDegrees(atan(divide("$side_b", "$side_a"))))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/atan/example2
     * 
     */
    @Test
    @DisplayName("main :: Inverse Tangent of Value in Radians")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("angle_a", atan(divide("$side_b", "$side_a")))));
    }

}
