package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.ArrayExpressions.reduce;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.StringExpressions.regexFindAll;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Set.set;

public class TestRegexFindAll extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("returnObject", regexFindAll(field("description")).pattern("line"))));

    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("email", regexFindAll(field("comment"))
                                .pattern("[a-z0-9_.+-]+@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+")
                                .options("i")),
                set()
                        .field("email", field("$email.match"))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("names", regexFindAll(field("comment"))
                                .pattern("([a-z0-9_.+-]+)@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+")
                                .options("i")),
                set()
                        .field("names", reduce(field("names.captures"), array(), concatArrays(value("$$value"), value("$$this"))))));
    }

}
