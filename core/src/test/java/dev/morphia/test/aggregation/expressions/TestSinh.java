package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.sinh;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestSinh extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/sinh/example1
     * 
     */
    @Test(testName = "main :: Hyperbolic Sine of Value in Degrees")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation
                .pipeline(addFields().field("sinh_output", sinh(degreesToRadians("$angle")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sinh/example2
     * 
     */
    @Test(testName = "main :: Hyperbolic Sine of Value in Radians")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(addFields().field("sinh_output", sinh("$angle"))));
    }

}
