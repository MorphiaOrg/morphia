package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.ifNull;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.divide;
import static dev.morphia.aggregation.expressions.MathExpressions.floor;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.SystemVariables.REMOVE;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gt;

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
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("cats", 20)));
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
        testPipeline(aggregation -> aggregation.pipeline(addFields().field("date", REMOVE)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/usingTwoAddFieldsStages
     */
    @Test
    @DisplayName("Using Two $addFields Stages")
    public void testUsingTwoAddFieldsStages() {
        testPipeline(aggregation -> aggregation.pipeline(match(gt("runtime", 1000)),
                addFields().field("runtimeHours", floor(divide("$runtime", 60)))
                        .field("ratingOutOf100", multiply("$imdb.rating", 10)),
                addFields().field("licenseFeeUSD", multiply("$runtimeHours", 0.5))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/addFifteenMinutesToTheRuntime
     */
    @Test
    @DisplayName("Add Fifteen Minutes to the Runtime")
    public void testAddFifteenMinutesToTheRuntime() {
        testPipeline(aggregation -> aggregation.pipeline(match(gt("runtime", 1000)),
                addFields().field("runtime", add("$runtime", 15))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/replaceTheIdAndTitle
     */
    @Test
    @DisplayName("Replace the Id and Title")
    public void testReplaceTheIdAndTitle() {
        testPipeline(aggregation -> aggregation.pipeline(match(gt("runtime", 1000)),
                addFields().field("_id", "$title").field("title", elementAt("$genres", 0))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/removeThePlot
     */
    @Test
    @DisplayName("Remove the Plot")
    public void testRemoveThePlot() {
        testPipeline(aggregation -> aggregation.pipeline(match(gt("runtime", 1000)), addFields().field("plot", REMOVE)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/removeAMissingRating
     */
    @Test
    @DisplayName("Remove a Missing Rating")
    public void testRemoveAMissingRating() {
        testPipeline(aggregation -> aggregation.pipeline(match(gt("runtime", 1000)),
                addFields().field("rated", ifNull().target("$rated").replacement(REMOVE))));
    }
}
