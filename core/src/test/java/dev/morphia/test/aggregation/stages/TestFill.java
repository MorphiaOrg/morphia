package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.stages.Fill.Method;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Fill.fill;
import static dev.morphia.query.Sort.ascending;

public class TestFill extends AggregationTest {
    @Test
    public void testConstantValue() {
        testPipeline("5.3.0", "constantValue", aggregation -> aggregation
                .fill(fill()
                        .field("bootsSold", value(0))
                        .field("sandalsSold", value(0))
                        .field("sneakersSold", value(0))));
    }

    @Test
    public void testDistinctPartitions() {
        testPipeline("5.3.0", "distinctPartitions", aggregation -> aggregation
                .fill(fill()
                        .sortBy(ascending("date"))
                        .partitionBy(document("restaurant", field("restaurant")))
                        .field("score", Method.LOCF)));
    }

    @Test
    public void testLastObserved() {
        testPipeline("5.3.0", "lastObserved", aggregation -> aggregation
                .fill(fill()
                        .sortBy(ascending("date"))
                        .field("score", Method.LOCF)));
    }

    @Test
    public void testLinearInterpolation() {
        testPipeline("5.3.0", "linearInterpolation", aggregation -> aggregation
                .fill(fill()
                        .sortBy(ascending("time"))
                        .field("price", Method.LINEAR)));
    }
}
