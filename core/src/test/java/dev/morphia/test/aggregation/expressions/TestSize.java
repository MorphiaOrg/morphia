package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.isArray;
import static dev.morphia.aggregation.expressions.ArrayExpressions.size;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSize extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/size/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("item").include("numberOfColors",
                condition(isArray("$colors"), size("$colors"), "NA"))));
    }

}
