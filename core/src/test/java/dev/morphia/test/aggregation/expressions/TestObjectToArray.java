package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

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

public class TestObjectToArray extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/objectToArray/example1
     * 
     */
    @Test(testName = "``$objectToArray`` Example")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(project().include("item").include("dimensions", objectToArray("$dimensions"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/objectToArray/example2
     * 
     */
    @Test(testName = "``$objectToArray`` to Sum Nested Fields")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(project().include("warehouses", objectToArray("$instock")),
                        unwind("warehouses"), group(id("$warehouses.k")).field("total", sum("$warehouses.v"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/objectToArray/example3
     * 
     */
    @Test(testName = "``$objectToArray`` + ``$arrayToObject`` Example")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(addFields().field("instock", objectToArray("$instock")),
                        addFields().field("instock",
                                concatArrays("$instock",
                                        array(document().field("k", "total").field("v", sum("$instock.v"))))),
                        addFields().field("instock", arrayToObject("$instock"))));
    }

}
