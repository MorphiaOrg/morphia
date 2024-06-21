package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.concat;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestConcat extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/concat/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("itemDescription", concat("$item", " - ", "$description"))));
    }

}
