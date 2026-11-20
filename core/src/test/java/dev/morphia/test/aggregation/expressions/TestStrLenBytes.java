package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.strLenBytes;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestStrLenBytes extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/strLenBytes/singleByteAndMultibyteCharacterSet
     * 
     */
    @Test
    @DisplayName("Single-Byte and Multibyte Character Set")
    public void testSingleByteAndMultibyteCharacterSet() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("name").include("length", strLenBytes("$name"))));
    }

}
