package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.allElementsTrue;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestAllElementsTrue extends TemplatedTestBase {

    @Test
    @DisplayName("Check Whether Every Response Is True")
    public void testCheckWhetherEveryResponseIsTrue() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("responses").include("isAllTrue", allElementsTrue("$responses")).suppressId()));
    }
}
