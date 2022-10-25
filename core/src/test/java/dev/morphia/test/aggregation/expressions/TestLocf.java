package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Sort;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.WindowExpressions.locf;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;

public class TestLocf extends AggregationTest {
    @Test
    public void testMissingValues() {
        testPipeline(5.2, "missingValues", true, false, (aggregation) -> {
            return aggregation
                    .setWindowFields(setWindowFields()
                            .sortBy(Sort.ascending("time"))
                            .output(output("price")
                                    .operator(locf(field("price")))));
        });
    }
}
