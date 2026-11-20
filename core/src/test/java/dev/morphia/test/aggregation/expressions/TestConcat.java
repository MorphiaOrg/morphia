package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.concat;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestConcat extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/concat/concatExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testConcatExpressionOperator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("itemDescription", concat("$item", " - ", "$description"))));
    }

}
