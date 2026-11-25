package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.subtract;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSubtract extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/subtract/subtractNumbers
     * 
     */
    @Test
    @DisplayName("Subtract Numbers")
    public void testSubtractNumbers() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("item").include("total", subtract(add("$price", "$fee"), "$discount"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/subtract/subtractTwoDates
     * 
     */
    @Test
    @DisplayName("Subtract Two Dates")
    public void testSubtractTwoDates() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("item").include("dateDifference", subtract("$$NOW", "$date"))));
    }

}
