package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.setIsSubset;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSetIsSubset extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/setIsSubset/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(project().suppressId().include("flowerFieldA")
                        .include("flowerFieldB").include("AisSubset", setIsSubset("$flowerFieldA", "$flowerFieldB"))));
    }

}
