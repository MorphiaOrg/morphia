package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.cosh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestCosh extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/cosh/example1
     * 
     */
    @Test(testName = "main :: Hyperbolic Cosine in Degrees")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation
                .pipeline(addFields().field("cosh_output", cosh(degreesToRadians("$angle")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/cosh/example2
     * 
     */
    @Test(testName = "main :: Hyperbolic Cosine in Radians")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, true, true,
                (aggregation) -> aggregation.pipeline(addFields().field("cosh_output", cosh("$angle"))));
    }

}
