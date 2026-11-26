package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.trim;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestTrim extends TemplatedTestBase {

    @Test
    @DisplayName("Trim Surrounding Whitespace")
    public void testTrimSurroundingWhitespace() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("item").include("description", trim("$description"))));
    }
}
