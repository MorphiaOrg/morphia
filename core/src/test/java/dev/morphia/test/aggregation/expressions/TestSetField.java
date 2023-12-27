package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.DriverVersion;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.expressions.Miscellaneous.setField;
import static dev.morphia.aggregation.expressions.SystemVariables.*;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.ReplaceWith.replaceWith;
import static dev.morphia.aggregation.stages.Unset.unset;
import static dev.morphia.query.filters.Filters.eq;

public class TestSetField extends AggregationTest {
    public TestSetField() {
        minDriver = DriverVersion.v43;
    }

    @Test
    public void testExample1() {
        testPipeline(ServerVersion.v50, false, true, (aggregation) -> aggregation.pipeline(
                replaceWith(setField("price.usd", ROOT, "$price")),
                unset("price")));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                replaceWith(setField(literal("$price"), ROOT, "$price")),
                unset("price")));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(eq("_id", 1)),
                replaceWith(setField("price.usd", ROOT, 49.99))));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(eq("_id", 1)),
                replaceWith(setField(literal("$price"), ROOT, 49.99))));
    }

    @Test
    public void testExample5() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                replaceWith(setField("price.usd", ROOT, REMOVE))));
    }

    @Test
    public void testExample6() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                replaceWith(setField(literal("$price"), ROOT, REMOVE))));
    }

}
