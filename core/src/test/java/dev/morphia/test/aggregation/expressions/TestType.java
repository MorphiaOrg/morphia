package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TypeExpressions.type;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestType extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/type/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(project().include("a", type("$a"))));
    }

}
