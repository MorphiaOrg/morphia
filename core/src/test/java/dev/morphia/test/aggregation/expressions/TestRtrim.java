package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.rtrim;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestRtrim extends TemplatedTestBase {

    @Test
    @DisplayName("Trim Trailing Whitespace")
    public void testTrimTrailingWhitespace() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("item").include("description", rtrim("$description"))));
    }
}
