package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.StringExpressions;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestToString extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("convertedZipCode", StringExpressions.toString(field("zipcode"))),
                sort()
                        .ascending("convertedZipCode")));
    }

}
