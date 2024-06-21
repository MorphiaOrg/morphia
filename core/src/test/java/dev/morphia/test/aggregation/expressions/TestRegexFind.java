package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

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

public class TestRegexFind extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/regexFind/example1
     * 
     */
    @Test(testName = "``$regexFind`` and Its Options")
    public void testExample1() {
        testPipeline(new ActionTestOptions().orderMatters(false), (aggregation) -> aggregation
                .pipeline(addFields().field("returnObject", regexFind("$description").pattern("line"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/regexFind/example2
     * 
     */
    @Test(testName = "Use ``$regexFind`` to Parse Email from String")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("email",
                        regexFind("$comment").pattern("[a-z0-9_.+-]+@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+").options("i")),
                set().field("email", "$email.match")));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/regexFind/example3
     * 
     */
    @Test(testName = "Apply ``$regexFind`` to String Elements of an Array")
    public void testExample3() {
        testPipeline((aggregation) -> aggregation.pipeline(unwind("details"),
                addFields()
                        .field("regexemail",
                                regexFind("$details").pattern("^[a-z0-9_.+-]+@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+$")
                                        .options("i"))
                        .field("regexphone", regexFind("$details").pattern("^[+]{0,1}[0-9]*\\-?[0-9_\\-]+$")),
                project().include("_id").include("name").include("details",
                        document().field("email", "$regexemail.match").field("phone", "$regexphone.match")),
                group(id("$_id")).field("name", first("$name")).field("details", mergeObjects().add("$details")),
                sort().ascending("_id")));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/regexFind/example4
     * 
     */
    @Test(testName = "Use Captured Groupings to Parse User Name")
    public void testExample4() {
        testPipeline(
                (aggregation) -> aggregation
                        .pipeline(
                                addFields().field("username",
                                        regexFind("$email").pattern("^([a-z0-9_.+-]+)@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+$")
                                                .options("i")),
                                set().field("username", elementAt("$username.captures", 0))));
    }

}
