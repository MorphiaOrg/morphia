package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.bitOr;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestBitOr extends TemplatedTestBase {

    @Test
    @DisplayName("Bitwise OR with Two Integers")
    public void testBitwiseOrWithTwoIntegers() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("result", bitOr("$a", "$b"))));
    }
}
