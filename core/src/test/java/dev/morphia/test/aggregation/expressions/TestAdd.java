package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestAdd extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/add/addNumbers
     * 
     */
    @Test
    @DisplayName("Add Numbers")
    public void testAddNumbers() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("item").include("total", add("$price", "$fee"))));
    }

    @Test
    @DisplayName("Add Non-Integer Values to a Date")
    public void testAddNonIntegerValuesToADate() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("item").include("result", add(1.5, 1.6, "$date", 1.5, 1.5))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/add/performAdditionOnADate
     */
    @Test
    @DisplayName("Perform Addition on a Date")
    public void testPerformAdditionOnADate() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("item").include("billing_date", add("$date", 3 * 24 * 60 * 60000))));
    }
}
