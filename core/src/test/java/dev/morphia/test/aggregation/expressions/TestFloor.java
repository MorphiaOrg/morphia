package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.floor;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestFloor extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/floor/example1
     * 
     */
    @Test
    @DisplayName("main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("value").include("floorValue", floor("$value"))));
    }

}
