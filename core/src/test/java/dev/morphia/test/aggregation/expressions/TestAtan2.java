package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.atan2;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestAtan2 extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/atan2/inverseTangentOfValueInDegrees
     * 
     */
    @Test
    @DisplayName("main :: Inverse Tangent of Value in Degrees")
    public void testInverseTangentOfValueInDegrees() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("angle_a", radiansToDegrees(atan2("$side_b", "$side_a")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/atan2/inverseTangentOfValueInRadians
     * 
     */
    @Test
    @DisplayName("main :: Inverse Tangent of Value in Radians")
    public void testInverseTangentOfValueInRadians() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("angle_a", atan2("$side_b", "$side_a"))));
    }

}
