package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.anyElementTrue;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestAnyElementTrue extends TemplatedTestBase {

    @Test
    @DisplayName("Check Whether Any Response Is True")
    public void testCheckWhetherAnyResponseIsTrue() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("responses").include("isAnyTrue", anyElementTrue("$responses")).include("_id")));
    }
}
