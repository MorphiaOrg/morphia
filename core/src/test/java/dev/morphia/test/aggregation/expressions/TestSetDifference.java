package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.setDifference;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSetDifference extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/setDifference/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(project().suppressId().include("flowerFieldA")
                        .include("flowerFieldB").include("inBOnly", setDifference("$flowerFieldB", "$flowerFieldA"))));
    }

}
