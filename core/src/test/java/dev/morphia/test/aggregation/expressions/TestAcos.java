package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.divide;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.acos;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestAcos extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/acos/example1
     * 
     */
    @Test
    @DisplayName("main :: Inverse Cosine of Value in Degrees")
    public void testExample1() {
        testPipeline(aggregation -> aggregation
                .pipeline(addFields().field("angle_a", radiansToDegrees(acos(divide("$side_b", "$hypotenuse"))))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/acos/example2
     * 
     */
    @Test
    @DisplayName("main :: Inverse Cosine of Value in Radians")
    public void testExample2() {
        testPipeline(aggregation -> aggregation
                .pipeline(addFields().field("angle_a", acos(divide("$side_b", "$hypotenuse")))));

    }
}
