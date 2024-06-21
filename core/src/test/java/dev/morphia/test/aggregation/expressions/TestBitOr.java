package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.bitOr;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.v63;

public class TestBitOr extends AggregationTest {

    /**
     * test data: dev/morphia/test/aggregation/expressions/bitOr/example1
     */
    @Test(testName = "Bitwise ``OR`` with Two Integers ")
    public void testExample1() {
        testPipeline(dev.morphia.test.ServerVersion.ANY, false, true, aggregation -> aggregation.pipeline(
                project()
                        .include("result", bitOr("$a", "$b"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/bitOr/example2
     *
     */
    @Test(testName = "Bitwise ``OR`` with a Long and Integer ", enabled = false, description = "this is getting odd unexpected results from the server")
    public void testExample2() {
        testPipeline(v63, true, false,
                aggregation -> aggregation.project(project().include("result", bitOr("$a", 63L))));
    }

}
