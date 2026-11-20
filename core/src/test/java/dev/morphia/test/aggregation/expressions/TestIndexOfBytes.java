package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.indexOfBytes;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIndexOfBytes extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/indexOfBytes/indexOfBytesExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testIndexOfBytesExpressionOperator() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(project().include("byteLocation", indexOfBytes("$item", "foo"))));
    }

}
