package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.abs;
import static dev.morphia.aggregation.expressions.MathExpressions.subtract;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAbs extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/abs/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ANY).removeIds(false).orderMatters(true),
                aggregation -> aggregation
                        .pipeline(project().include("delta", abs(subtract("$startTemp", "$endTemp")))));

    }
}
