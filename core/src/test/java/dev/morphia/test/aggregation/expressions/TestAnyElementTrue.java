package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.anyElementTrue;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestAnyElementTrue extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/anyElementTrue/example1
     * 
     */
    @Test
    @DisplayName("main")
    public void testExample1() {
        testPipeline(aggregation -> aggregation.pipeline(
                project().suppressId().include("responses").include("isAnyTrue", anyElementTrue("$responses"))));

    }
}
