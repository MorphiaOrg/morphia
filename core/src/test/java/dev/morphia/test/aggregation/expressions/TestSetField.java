package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.expressions.Miscellaneous.setField;
import static dev.morphia.aggregation.expressions.Miscellaneous.unsetField;
import static dev.morphia.aggregation.expressions.SystemVariables.*;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.ReplaceWith.replaceWith;
import static dev.morphia.aggregation.stages.Unset.unset;
import static dev.morphia.query.filters.Filters.eq;

public class TestSetField extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/setField/addFieldsThatContainPeriods
     * 
     */
    @Test
    @DisplayName("Add Fields that Contain Periods (``.``)")
    public void testAddFieldsThatContainPeriods() {
        testPipeline((aggregation) -> aggregation
                .pipeline(replaceWith(setField("price.usd", ROOT, "$price")), unset("price")));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/setField/addFieldsThatStartWithADollarSign
     * 
     */
    @Test
    @DisplayName("Add Fields that Start with a Dollar Sign (``$``)")
    public void testAddFieldsThatStartWithADollarSign() {
        testPipeline((aggregation) -> aggregation.pipeline(replaceWith(setField(literal("$price"), ROOT, "$price")),
                unset("price")));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/setField/updateFieldsThatContainPeriods
     * 
     */
    @Test
    @DisplayName("Update Fields that Contain Periods (``.``)")
    public void testUpdateFieldsThatContainPeriods() {
        testPipeline((aggregation) -> aggregation.pipeline(match(eq("_id", 1)),
                replaceWith(setField("price.usd", ROOT, 49.99))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/setField/updateFieldsThatStartWithADollarSign
     * 
     */
    @Test
    @DisplayName("Update Fields that Start with a Dollar Sign (``$``)")
    public void testUpdateFieldsThatStartWithADollarSign() {
        testPipeline((aggregation) -> aggregation.pipeline(match(eq("_id", 1)),
                replaceWith(setField(literal("$price"), ROOT, 49.99))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/setField/removeFieldsThatContainPeriods
     * 
     */
    @Test
    @DisplayName("Remove Fields that Contain Periods (``.``)")
    public void testRemoveFieldsThatContainPeriods() {
        testPipeline((aggregation) -> aggregation.pipeline(replaceWith(setField("price.usd", ROOT, REMOVE))));
    }

    @Test
    @DisplayName("Remove a Dollar-Prefixed Field with $setField")
    public void testRemoveADollarPrefixedFieldWithSetField() {
        testPipeline((aggregation) -> aggregation.pipeline(
                replaceWith(setField(literal("$price"), "$$ROOT", "$$REMOVE"))));
    }

    @Test
    @DisplayName("Remove a Dollar-Prefixed Field with $unsetField")
    public void testRemoveADollarPrefixedFieldWithUnsetField() {
        testPipeline((aggregation) -> aggregation.pipeline(
                replaceWith(unsetField(literal("$price"), "$$ROOT"))));
    }

    @Test
    @DisplayName("Remove a Field with a Period Using $unsetField")
    public void testRemoveAFieldWithAPeriodUsingUnsetField() {
        testPipeline((aggregation) -> aggregation.pipeline(
                replaceWith(unsetField("price.usd", "$$ROOT"))));
    }
}
