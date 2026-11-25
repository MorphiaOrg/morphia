package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.cmp;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestCmp extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/cmp/cmpExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testCmpExpressionOperator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().suppressId().include("item").include("qty").include("cmpTo250", cmp("$qty", 250))));
    }

}
