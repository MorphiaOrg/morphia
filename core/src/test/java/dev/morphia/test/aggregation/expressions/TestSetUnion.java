package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.setUnion;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSetUnion extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/setUnion/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(project().suppressId().include("flowerFieldA")
                        .include("flowerFieldB").include("allValues", setUnion("$flowerFieldA", "$flowerFieldB"))));
    }

}
