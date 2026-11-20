package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.subtract;
import static dev.morphia.aggregation.expressions.StringExpressions.strLenCP;
import static dev.morphia.aggregation.expressions.StringExpressions.substrCP;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSubstrCP extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/substrCP/singleByteCharacterSet
     * 
     */
    @Test
    @DisplayName("Single-Byte Character Set")
    public void testSingleByteCharacterSet() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("item").include("yearSubstring", substrCP("$quarter", 0, 2))
                        .include("quarterSubtring", substrCP("$quarter", 2, subtract(strLenCP("$quarter"), 2)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/substrCP/singleByteAndMultibyteCharacterSet
     * 
     */
    @Test
    @DisplayName("Single-Byte and Multibyte Character Set")
    public void testSingleByteAndMultibyteCharacterSet() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("name").include("menuCode", substrCP("$name", 0, 3))));
    }

}
