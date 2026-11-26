package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.strcasecmp;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestStrcasecmp extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/strcasecmp/strcasecmpExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testStrcasecmpExpressionOperator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("item").include("comparisonResult", strcasecmp("$quarter", "13q4"))));
    }

}
