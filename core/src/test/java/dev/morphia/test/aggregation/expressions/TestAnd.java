package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.BooleanExpressions.and;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.lt;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAnd extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/and/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ANY), aggregation -> aggregation.pipeline(
                project().include("item").include("qty").include("result", and(gt("$qty", 100), lt("$qty", 250)))));

    }
}
