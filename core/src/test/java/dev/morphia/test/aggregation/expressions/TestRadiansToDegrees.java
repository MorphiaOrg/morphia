package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestRadiansToDegrees extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/radiansToDegrees/radiansToDegreesExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testRadiansToDegreesExpressionOperator() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("angle_a_deg", radiansToDegrees("$angle_a"))
                .field("angle_b_deg", radiansToDegrees("$angle_b"))
                .field("angle_c_deg", radiansToDegrees("$angle_c"))));
    }

}
