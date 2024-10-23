package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Projection;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.allElementsTrue;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAllElementsTrue extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/allElementsTrue/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ANY).orderMatters(false),
                aggregation -> aggregation.pipeline(Projection.project().suppressId().include("responses")
                        .include("isAllTrue", allElementsTrue("$responses"))));

    }
}
