package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.sinh;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestSinh extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/sinh/example1
     * 
     */
    @Test
    @DisplayName("main :: Hyperbolic Sine of Value in Degrees")
    public void testExample1() {
        testPipeline(new ActionTestOptions().removeIds(true), (aggregation) -> aggregation
                .pipeline(addFields().field("sinh_output", sinh(degreesToRadians("$angle")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sinh/example2
     * 
     */
    @Test
    @DisplayName("main :: Hyperbolic Sine of Value in Radians")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("sinh_output", sinh("$angle"))));
    }

}
