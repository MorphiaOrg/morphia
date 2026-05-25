package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.bitOr;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestBitOr extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/expressions/bitOr/example1
     */
    @Test
    @DisplayName("Bitwise ``OR`` with Two Integers ")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion("6.3.0"),
                aggregation -> aggregation.pipeline(project().include("result", bitOr("$a", "$b"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/bitOr/example2
     *
     */
    @Disabled("this is getting odd unexpected results from the server")
    @Test
    @DisplayName("Bitwise ``OR`` with a Long and Integer ")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion("6.3.0").removeIds(true).orderMatters(false),
                aggregation -> aggregation.pipeline(project().include("result", bitOr("$a", 63L))));
    }

}
