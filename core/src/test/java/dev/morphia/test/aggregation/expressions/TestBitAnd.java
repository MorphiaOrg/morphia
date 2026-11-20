package dev.morphia.test.aggregation.expressions;

import java.io.FileNotFoundException;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.bitAnd;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestBitAnd extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/expressions/bitAnd/bitwiseAndWithTwoIntegers
     */
    @Test
    @DisplayName("Bitwise ``AND`` with Two Integers ")
    public void testBitwiseAndWithTwoIntegers() {
        testPipeline(new ActionTestOptions().serverVersion("7.0.0"),
                aggregation -> aggregation.pipeline(project().include("result", bitAnd("$a", "$b"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/bitAnd/bitwiseAndWithALongAndInteger
     *
     */
    @Test
    @DisplayName("Bitwise ``AND`` with a Long and Integer ")
    public void testBitwiseAndWithALongAndInteger() throws FileNotFoundException {
        testPipeline(new ActionTestOptions().serverVersion("7.0.0"),
                aggregation -> aggregation.pipeline(project().include("result", bitAnd("$a", 63L))));
    }

}
