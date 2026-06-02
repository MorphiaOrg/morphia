package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.indexOfArray;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIndexOfArray extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/indexOfArray/example1
     * 
     */
    @Test
    @DisplayName("main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("index", indexOfArray("$items", 2))));
    }

}
