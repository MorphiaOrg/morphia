package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.arrayToObject;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.ArrayExpressions.objectToArray;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Unwind.unwind;

public class TestObjectToArray extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/objectToArray/example1
     * 
     */
    @Test(testName = "``$objectToArray`` Example")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(project().include("item").include("dimensions", objectToArray("$dimensions"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/objectToArray/example2
     * 
     */
    @Test(testName = "``$objectToArray`` to Sum Nested Fields")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, false,
                (aggregation) -> aggregation.pipeline(project().include("warehouses", objectToArray("$instock")),
                        unwind("warehouses"), group(id("$warehouses.k")).field("total", sum("$warehouses.v"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/objectToArray/example3
     * 
     */
    @Test(testName = "``$objectToArray`` + ``$arrayToObject`` Example")
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(addFields().field("instock", objectToArray("$instock")),
                        addFields().field("instock",
                                concatArrays("$instock",
                                        array(document().field("k", "total").field("v", sum("$instock.v"))))),
                        addFields().field("instock", arrayToObject("$instock"))));
    }

}
