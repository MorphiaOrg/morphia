package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.tsSecond;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.v51;

public class TestTsSecond extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(v51, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("saleTimestamp")
                        .include("saleSeconds", tsSecond("$saleTimestamp"))));
    }
}
