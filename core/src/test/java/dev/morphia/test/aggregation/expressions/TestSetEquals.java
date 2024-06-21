package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.setEquals;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSetEquals extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/setEquals/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(project().suppressId().include("cakes").include("cupcakes")
                        .include("sameFlavors", setEquals("$cakes", "$cupcakes"))));
    }

}
