package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Sort;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.WindowExpressions.linearFill;
import static dev.morphia.aggregation.expressions.WindowExpressions.locf;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.test.ServerVersion.MDB53;

public class TestLinearFill extends AggregationTest {
    @Test
    public void missingValues() {
        testPipeline(MDB53, "missingValues", true, false, (aggregation) -> {
            return aggregation
                    .setWindowFields(setWindowFields()
                            .sortBy(Sort.ascending("time"))
                            .output(output("price")
                                    .operator(linearFill(field("price")))));
        });

    }

    @Test
    public void testMultipleFills() {
        testPipeline(MDB53, "multipleFills", true, false, (aggregation) -> {
            return aggregation
                    .setWindowFields(setWindowFields()
                            .sortBy(Sort.ascending("time"))
                            .output(output("linearFillPrice")
                                    .operator(linearFill(field("price"))),
                                    output("locfPrice")
                                            .operator(locf(field("price")))));
        });

    }

}
