package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static dev.morphia.aggregation.expressions.TimeUnit.SECOND;
import static dev.morphia.aggregation.expressions.WindowExpressions.derivative;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.filters.Filters.gt;

public class TestDerivative extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy(field("truckID"))
                        .sortBy(ascending("timeStamp"))
                        .output(output("truckAverageSpeed")
                                .operator(derivative(field("miles"))
                                        .unit(HOUR))
                                .window()
                                .range(-30, 0, SECOND)),
                match(gt("truckAverageSpeed", 50))));
    }
}
