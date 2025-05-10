package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.tsIncrement;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestTsIncrement extends AggregationTest {
    @Test
    public void testTimestampOrdinal() {
        testPipeline("5.1.0", "timestampOrdinal", (aggregation) -> aggregation.project(project()
                .suppressId()
                .include("saleTimestamp")
                .include("saleIncrement", tsIncrement(field("saleTimestamp")))));
    }
}
