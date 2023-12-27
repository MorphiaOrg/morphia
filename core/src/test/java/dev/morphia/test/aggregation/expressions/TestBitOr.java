package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.bitOr;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.v63;

public class TestBitOr extends AggregationTest {

    @Test
    public void testExample2() {
        testPipeline(v63, false, true, aggregation -> aggregation
                .pipeline(project()
                        .include("result",
                                bitOr("$a", "$b"))));
    }

    @Test
    public void testExample3() {
        testPipeline(v63, false, false, aggregation -> aggregation
                .project(project()
                        .include("result",
                                bitOr("$a", 63L))));
    }
}
