package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.shift;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.descending;

public class TestShift extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy("$state")
                        .sortBy(descending("quantity"))
                        .output(output("shiftQuantityForState")
                                .operator(shift("$quantity", 1, "Not available")))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy("$state")
                        .sortBy(descending("quantity"))
                        .output(output("shiftQuantityForState")
                                .operator(shift("$quantity", -1, "Not available")))));
    }

}
