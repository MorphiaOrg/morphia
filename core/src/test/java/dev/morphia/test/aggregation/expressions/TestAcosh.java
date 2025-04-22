package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.acosh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestAcosh extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/acosh/example1
     * 
     */
    @Test(testName = "main :: Inverse Hyperbolic Cosine in Degrees")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion("0.0.0"), aggregation -> aggregation
                .pipeline(addFields().field("y-coordinate", radiansToDegrees(acosh("$x-coordinate")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/acosh/example2
     * 
     */
    @Test(testName = "main :: Inverse Hyperbolic Cosine in Radians")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion("0.0.0"),
                aggregation -> aggregation.pipeline(addFields().field("y-coordinate", acosh("$x-coordinate"))));

    }
}
