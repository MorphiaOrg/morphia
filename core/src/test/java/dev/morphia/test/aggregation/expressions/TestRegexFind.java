package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.first;
import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.ObjectExpressions.mergeObjects;
import static dev.morphia.aggregation.expressions.StringExpressions.regexFind;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.Unwind.unwind;

public class TestRegexFind extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("returnObject", regexFind(field("description"))
                                .pattern("line"))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("email", regexFind(field("comment"))
                                .pattern("[a-z0-9_.+-]+@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+")
                                .options("i")),
                set()
                        .field("email", field("$email.match"))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                unwind("details"),
                addFields()
                        .field("regexemail", regexFind(field("$details"))
                                .pattern("^[a-z0-9_.+-]+@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+$")
                                .options("i"))
                        .field("regexphone", regexFind(field("$details"))
                                .pattern("^[+]{0,1}[0-9]*\\-?[0-9_\\-]+$")),
                project()
                        .include("_id")
                        .include("name")
                        .include("details", document()
                                .field("email", field("regexemail.match"))
                                .field("phone", field("regexphone.match"))),
                group(id("_id"))
                        .field("name", first(field("name")))
                        .field("details", mergeObjects().add(field("details"))),
                sort().ascending("_id")));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("username", regexFind(field("email"))
                                .pattern("^([a-z0-9_.+-]+)@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+$")
                                .options("i")),
                set()
                        .field("username", elementAt(field("username.captures"), value(0)))));
    }

}
