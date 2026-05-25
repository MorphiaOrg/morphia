package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.replaceOne;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestReplaceOne extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/replaceOne/example1
     * 
     */
    @Test
    @DisplayName("main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("item", replaceOne("$item", "blue paint", "red paint"))));
    }

}
