package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.filters.Filters;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.switchExpression;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.TypeExpressions.toBool;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Match.match;

public class TestToBool extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("convertedShippedFlag",
                                switchExpression()
                                        .branch(eq(field("shipped"), value("false")), value(false))
                                        .branch(eq(field("shipped"), value("")), value(false))
                                        .defaultCase(toBool(field("shipped")))),
                match(Filters.eq("convertedShippedFlag", false))));
    }

}
