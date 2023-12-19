package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.WindowExpressions.expMovingAvg;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.*;

public class TestExpMovingAvg extends AggregationTest {
    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy(field("stock"))
                        .sortBy(ascending("date"))
                        .output(output("expMovingAvgForStock")
                                .operator(expMovingAvg(field("price"), 2)))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy(field("stock"))
                        .sortBy(ascending("date"))
                        .output(output("expMovingAvgForStock")
                                .operator(expMovingAvg(field("price"), 0.75)))));
    }
}
