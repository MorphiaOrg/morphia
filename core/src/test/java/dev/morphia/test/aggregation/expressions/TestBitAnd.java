package dev.morphia.test.aggregation.expressions;

import java.io.FileNotFoundException;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.bitAnd;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.v63;

public class TestBitAnd extends AggregationTest {

    /**
     * test data: dev/morphia/test/aggregation/expressions/bitAnd/example1
     */
    @Test(testName = "Bitwise ``AND`` with Two Integers ")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true,
                aggregation -> aggregation.pipeline(project().include("result", bitAnd("$a", "$b"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/bitAnd/example2
     *
     */
    @Test(testName = "Bitwise ``AND`` with a Long and Integer ")
    public void testExample2() throws FileNotFoundException {
        testPipeline(v63, false, true,
                aggregation -> aggregation.project(project().include("result", bitAnd("$a", 63L))));
    }

}
