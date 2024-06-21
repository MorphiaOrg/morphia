package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestGt extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/gt/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().suppressId().include("item").include("qty").include("qtyGt250", gt("$qty", 250))));
    }

}
