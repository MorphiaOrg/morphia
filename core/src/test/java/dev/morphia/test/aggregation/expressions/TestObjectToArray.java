package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
     * test data: dev/morphia/test/aggregation/expressions/objectToArray/objectToArrayExample
     * 
     */
    @Test
    @DisplayName("``$objectToArray`` Example")
    public void testObjectToArrayExample() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("item").include("dimensions", objectToArray("$dimensions"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/objectToArray/objectToArrayToSumNestedFields
     * 
     */
    @Test
    @DisplayName("``$objectToArray`` to Sum Nested Fields")
    public void testObjectToArrayToSumNestedFields() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("warehouses", objectToArray("$instock")),
                unwind("warehouses"), group(id("$warehouses.k")).field("total", sum("$warehouses.v"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/objectToArray/objectToArrayArrayToObjectExample
     * 
     */
    @Test
    @DisplayName("``$objectToArray`` + ``$arrayToObject`` Example")
    public void testObjectToArrayArrayToObjectExample() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("instock", objectToArray("$instock")),
                addFields().field("instock",
                        concatArrays("$instock", array(document().field("k", "total").field("v", sum("$instock.v"))))),
                addFields().field("instock", arrayToObject("$instock"))));
    }

}
