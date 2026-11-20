package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.reverseArray;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestReverseArray extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/reverseArray/reverseArrayExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testReverseArrayExpressionOperator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("name").include("reverseFavorites", reverseArray("$favorites"))));
    }

}
