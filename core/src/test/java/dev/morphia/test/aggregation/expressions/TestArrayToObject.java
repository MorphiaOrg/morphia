package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.arrayToObject;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.ArrayExpressions.objectToArray;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestArrayToObject extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/arrayToObject/example1
     * 
     */
    @Test(testName = "``$arrayToObject``  Example")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("item").include("dimensions", arrayToObject("$dimensions"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/arrayToObject/example2
     * 
     */
    @Test(testName = "``$objectToArray`` + ``$arrayToObject`` Example")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("instock", objectToArray("$instock")),
                addFields().field("instock",
                        concatArrays("$instock", array(document().field("k", "total").field("v", sum("$instock.v"))))),
                addFields().field("instock", arrayToObject("$instock"))));
    }

}
