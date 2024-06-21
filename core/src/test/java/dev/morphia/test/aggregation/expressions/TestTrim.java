package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.trim;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestTrim extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/trim/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().orderMatters(false), (aggregation) -> aggregation
                .pipeline(project().include("item").include("description", trim("$description"))));
    }

}
