package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.reverseArray;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestReverseArray extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/reverseArray/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("name").include("reverseFavorites", reverseArray("$favorites"))));
    }

}
