package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.anyElementTrue;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAnyElementTrue extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/anyElementTrue/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ANY), aggregation -> aggregation.pipeline(
                project().suppressId().include("responses").include("isAnyTrue", anyElementTrue("$responses"))));

    }
}
