package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.replaceAll;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestReplaceAll extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/replaceAll/replaceAllExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testReplaceAllExpressionOperator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("item", replaceAll("$item", "blue paint", "red paint"))));
    }

}
