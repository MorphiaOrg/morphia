package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.ne;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestNe extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/ne/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(
                        project().suppressId().include("item").include("qty").include("qtyNe250", ne("$qty", 250))));
    }

}
