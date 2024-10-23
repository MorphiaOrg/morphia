package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.ArrayExpressions.isArray;
import static dev.morphia.aggregation.expressions.BooleanExpressions.and;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIsArray extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/isArray/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("items", condition(and(isArray("$instock"), isArray("$ordered")),
                        concatArrays("$instock", "$ordered"), "One or more fields is not an array."))));
    }

}
