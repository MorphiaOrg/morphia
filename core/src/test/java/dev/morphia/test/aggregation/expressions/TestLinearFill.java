package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Sort;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.linearFill;
import static dev.morphia.aggregation.expressions.WindowExpressions.locf;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.test.ServerVersion.v53;

public class TestLinearFill extends AggregationTest {
    @Test
    public void testExample2() {
        testPipeline(v53, true, false, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .sortBy(Sort.ascending("time"))
                        .output(output("price")
                                .operator(linearFill("$price")))));

    }

    @Test
    public void testExample3() {
        testPipeline(v53, true, false, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .sortBy(Sort.ascending("time"))
                        .output(output("linearFillPrice")
                                .operator(linearFill("$price")),
                                output("locfPrice")
                                        .operator(locf("$price")))));

    }

}
