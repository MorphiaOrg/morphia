package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.tan;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestTan extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/tan/tangentOfValueInRadians
     * 
     */
    @Test
    @DisplayName("main :: Tangent of Value in Radians")
    public void testTangentOfValueInRadians() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("side_b", multiply(tan("$angle_a"), "$side_a"))));
    }

}
