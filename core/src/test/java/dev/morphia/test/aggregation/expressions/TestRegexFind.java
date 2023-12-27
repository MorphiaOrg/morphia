package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.first;
import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.Expressions.document;
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
                        .field("returnObject", regexFind("$description")
                                .pattern("line"))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("email", regexFind("$comment")
                                .pattern("[a-z0-9_.+-]+@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+")
                                .options("i")),
                set()
                        .field("email", "$email.match")));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                unwind("details"),
                addFields()
                        .field("regexemail", regexFind("$details")
                                .pattern("^[a-z0-9_.+-]+@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+$")
                                .options("i"))
                        .field("regexphone", regexFind("$details")
                                .pattern("^[+]{0,1}[0-9]*\\-?[0-9_\\-]+$")),
                project()
                        .include("_id")
                        .include("name")
                        .include("details", document()
                                .field("email", "$regexemail.match")
                                .field("phone", "$regexphone.match")),
                group(id("$_id"))
                        .field("name", first("$name"))
                        .field("details", mergeObjects().add("$details")),
                sort().ascending("_id")));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("username", regexFind("$email")
                                .pattern("^([a-z0-9_.+-]+)@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+$")
                                .options("i")),
                set()
                        .field("username", elementAt("$username.captures", 0))));
    }

}
