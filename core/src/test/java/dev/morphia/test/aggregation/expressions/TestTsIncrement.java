package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.tsIncrement;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestTsIncrement extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/tsIncrement/example1
     */
    @Test(testName = "Obtain the Incrementing Ordinal from a Timestamp Field")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion("5.1.0"), (aggregation) -> aggregation.pipeline(project()
                .suppressId().include("saleTimestamp").include("saleIncrement", tsIncrement("$saleTimestamp"))));
    }
}
