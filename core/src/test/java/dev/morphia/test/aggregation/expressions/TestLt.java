package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.lt;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestLt extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/lt/ltExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testLtExpressionOperator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().suppressId().include("item").include("qty").include("qtyLt250", lt("$qty", 250))));
    }

}
