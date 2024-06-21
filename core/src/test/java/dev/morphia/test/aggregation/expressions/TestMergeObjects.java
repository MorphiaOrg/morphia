package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.ObjectExpressions.mergeObjects;
import static dev.morphia.aggregation.expressions.SystemVariables.*;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Lookup.lookup;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.ReplaceRoot.replaceRoot;

public class TestMergeObjects extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/mergeObjects/example1
     * 
     */
    @Test(testName = "``$mergeObjects``")
    public void testExample1() {
        loadData("items", 2);
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(
                        lookup("items").foreignField("item").localField("item").as("fromItems"),
                        replaceRoot(mergeObjects().add(elementAt("$fromItems", 0)).add(ROOT)),
                        project().exclude("fromItems")));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/mergeObjects/example2
     * 
     */
    @Test(testName = "``$mergeObjects`` as an Accumulator")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation
                .pipeline(group(id("$item")).field("mergedSales", mergeObjects().add("$quantity"))));
    }

}
