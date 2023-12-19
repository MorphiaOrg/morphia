package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.Miscellaneous.setField;
import static dev.morphia.aggregation.expressions.SystemVariables.*;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.ReplaceWith.replaceWith;
import static dev.morphia.aggregation.stages.Unset.unset;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.test.ServerVersion.*;

public class TestSetField extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(v50, false, true, (aggregation) -> aggregation.pipeline(
                replaceWith(setField("price.usd", ROOT, field("price"))),
                unset("price")));
    }

    @Test
    public void testExample2() {
        testPipeline(v50, false, true, (aggregation) -> aggregation.pipeline(
                replaceWith(setField(literal("$price"), ROOT, field("price"))),
                unset("price")));
    }

    @Test
    public void testExample3() {
        testPipeline(v50, false, true, (aggregation) -> aggregation.pipeline(
                match(eq("_id", 1)),
                replaceWith(setField("price.usd", ROOT, value(49.99)))));
    }

    @Test
    public void testExample4() {
        testPipeline(v50, false, true, (aggregation) -> aggregation.pipeline(
                match(eq("_id", 1)),
                replaceWith(setField(literal("$price"), ROOT, value(49.99)))));
    }

    @Test
    public void testExample5() {
        testPipeline(v50, false, true, (aggregation) -> aggregation.pipeline(
                replaceWith(setField("price.usd", ROOT, REMOVE))));
    }

    @Test
    public void testExample6() {
        testPipeline(v50, false, true, (aggregation) -> aggregation.pipeline(
                replaceWith(setField(literal("$price"), ROOT, REMOVE))));
    }

}
