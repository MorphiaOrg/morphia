package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.SystemVariables.REMOVE;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;

public class TestAddFields extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/addingFieldsToAnEmbeddedDocument
     * 
     */
    @Test
    @DisplayName("Adding Fields to an Embedded Document")
    public void testAddingFieldsToAnEmbeddedDocument() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("specs.fuel_type", "unleaded")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/overwritingAnExistingField
     * 
     */
    @Test
    @DisplayName("Overwriting an existing field")
    public void testOverwritingAnExistingField() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(addFields().field("cats", 20)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/addElementToAnArray
     * 
     */
    @Test
    @DisplayName("Add Element to an Array")
    public void testAddElementToAnArray() {
        testPipeline((aggregation) -> aggregation.pipeline(match(eq("_id", 1)),
                addFields().field("homework", concatArrays("$homework", array(7)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/removeFields
     * 
     * db.labReadings.aggregate( [ { $addFields: { date: "$$REMOVE" } } ] )
     */
    @Test
    @DisplayName("Remove Fields")
    public void testRemoveFields() {
        testPipeline(new ActionTestOptions().removeIds(true),
                aggregation -> aggregation.pipeline(addFields().field("date", REMOVE)));
    }

}
