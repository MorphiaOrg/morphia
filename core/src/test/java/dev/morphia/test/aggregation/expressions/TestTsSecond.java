package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.tsSecond;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestTsSecond extends AggregationTest {
    @Test
    public void testSeconds() {
        testPipeline(5.1, "seconds", (aggregation) -> {
            return aggregation.project(project()
                    .suppressId()
                    .include("saleTimestamp")
                    .include("saleSeconds", tsSecond(field("saleTimestamp"))));
        });
    }
}
