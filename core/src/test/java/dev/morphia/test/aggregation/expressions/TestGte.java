package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestGte extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/gte/gteExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testGteExpressionOperator() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().suppressId().include("item").include("qty").include("qtyGte250", gte("$qty", 250))));
    }

}
