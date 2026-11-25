package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.bitAnd;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestBitAnd extends TemplatedTestBase {

    @Test
    @DisplayName("Bitwise AND with a Long and Integer")
    public void testBitwiseAndWithALongAndInteger() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("result", bitAnd("$a", 63L))));
    }

    @Test
    @DisplayName("Bitwise AND with Two Integers")
    public void testBitwiseAndWithTwoIntegers() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("result", bitAnd("$a", "$b"))));
    }
}
