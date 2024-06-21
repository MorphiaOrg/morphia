package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
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
    /**
     * test data: dev/morphia/test/aggregation/stages/replaceWith/example1
     * 
     */
    @Test(testName = "``$replaceWith`` an Embedded Document Field")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(replaceWith(mergeObjects()
                .add(document().field("dogs", 0).field("cats", 0).field("birds", 0).field("fish", 0)).add("$pets"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/replaceWith/example2
     * 
     */
    @Test(testName = "``$replaceWith`` a Document Nested in an Array")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(unwind("grades"),
                match(gte("grades.grade", 90)), replaceWith("$grades")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/replaceWith/example3
     * 
     */
    @Test(testName = "``$replaceWith`` a Newly Created Document")
    public void testExample3() {
        skipDataCheck(); // the "asofDate" field will always differ
        testPipeline(ServerVersion.ANY, false, false,
                (aggregation) -> aggregation.pipeline(match(eq("status", "C")),
                        replaceWith().field("_id", "$_id").field("item", "$item")
                                .field("amount", multiply("$price", "$quantity")).field("status", "Complete")
                                .field("asofDate", NOW)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/replaceWith/example4
     * 
     */
    @Test(testName = "``$replaceWith`` a New Document Created from ``$$ROOT`` and a Default Document")
    public void testExample4() {
        testPipeline(
                ServerVersion.ANY, false, true, (
                        aggregation) -> aggregation
                                .pipeline(
                                        replaceWith(
                                                mergeObjects()
                                                        .add(document().field("_id", "").field("name", "")
                                                                .field("email", "").field("cell", "").field("home", ""))
                                                        .add(ROOT))));
    }
}
