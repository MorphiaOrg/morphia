package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestEq extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/eq/eqExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testEqExpressionOperator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().suppressId().include("item").include("qty").include("qtyEq250", eq("$qty", 250))));
    }

}
