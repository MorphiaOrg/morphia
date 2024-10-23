package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.bitXor;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.v63;

public class TestBitXor extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/expressions/bitXor/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v63),
                aggregation -> aggregation.pipeline(project().include("result", bitXor("$a", "$b"))));
    }
}
