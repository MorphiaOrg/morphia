package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.ObjectExpressions.mergeObjects;
import static dev.morphia.aggregation.expressions.SystemVariables.NOW;
import static dev.morphia.aggregation.expressions.SystemVariables.ROOT;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.ReplaceWith.replaceWith;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;

public class TestReplaceWith extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                replaceWith(mergeObjects()
                        .add(document()
                                .field("dogs", value(0))
                                .field("cats", value(0))
                                .field("birds", value(0))
                                .field("fish", value(0)))
                        .add(field("pets")))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                unwind("grades"),
                match(gte("grades.grade", 90)),
                replaceWith(field("grades"))));
    }

    @Test
    public void testExample3() {
        skipDataCheck(); // the "asofDate" field will always differ
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                match(eq("status", "C")),
                replaceWith()
                        .field("_id", field("_id"))
                        .field("item", field("item"))
                        .field("amount", multiply(field("price"), field("quantity")))
                        .field("status", value("Complete"))
                        .field("asofDate", NOW)));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                replaceWith(mergeObjects()
                        .add(document()
                                .field("_id", value(""))
                                .field("name", value(""))
                                .field("email", value(""))
                                .field("cell", value(""))
                                .field("home", value("")))
                        .add(ROOT))));
    }
}
