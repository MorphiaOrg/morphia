package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.query.filters.Filters.eq;

public class TestSet extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/set/usingTwoSetStages
     * 
     */
    @Test
    @DisplayName("Using Two ``$set`` Stages")
    public void testUsingTwoSetStages() {
        testPipeline((aggregation) -> aggregation.pipeline(
                set().field("totalHomework", sum("$homework")).field("totalQuiz", sum("$quiz")),
                set().field("totalScore", add("$totalHomework", "$totalQuiz", "$extraCredit"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/set/addingFieldsToAnEmbeddedDocument
     * 
     */
    @Test
    @DisplayName("Adding Fields to an Embedded Document")
    public void testAddingFieldsToAnEmbeddedDocument() {
        testPipeline((aggregation) -> aggregation.pipeline(set().field("specs.fuel_type", "unleaded")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/set/overwritingAnExistingField
     * 
     */
    @Test
    @DisplayName("Overwriting an existing field")
    public void testOverwritingAnExistingField() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(set().field("cats", 20)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/set/addElementToAnArray
     * 
     */
    @Test
    @DisplayName("Add Element to an Array")
    public void testAddElementToAnArray() {
        testPipeline((aggregation) -> aggregation.pipeline(match(eq("_id", 1)),
                set().field("homework", concatArrays("$homework", array(7)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/set/creatingANewFieldWithExistingFields
     * 
     */
    @Test
    @DisplayName("Creating a New Field with Existing Fields")
    public void testCreatingANewFieldWithExistingFields() {
        testPipeline((aggregation) -> aggregation.pipeline(set().field("quizAverage", avg("$quiz"))));
    }

}
