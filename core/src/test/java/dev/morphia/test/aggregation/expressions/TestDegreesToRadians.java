package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestDegreesToRadians extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/degreesToRadians/degreesToRadiansExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testDegreesToRadiansExpressionOperator() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("angle_a_rad", degreesToRadians("$angle_a"))
                .field("angle_b_rad", degreesToRadians("$angle_b"))
                .field("angle_c_rad", degreesToRadians("$angle_c"))));
    }

}
