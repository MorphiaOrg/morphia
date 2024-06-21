package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.atanh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestAtanh extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/atanh/example1
     * 
     */
    @Test(testName = "main :: Inverse Hyperbolic Tangent in Degrees")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(addFields().field("y-coordinate", radiansToDegrees(atanh("$x-coordinate")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/atanh/example2
     * 
     */
    @Test(testName = "main :: Inverse Hyperbolic Tangent in Radians")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(addFields().field("y-coordinate", atanh("$x-coordinate"))));
    }

}
