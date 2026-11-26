package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.regexFindAll;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestRegexFindAll extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/regexFindAll/regexFindAllAndItsOptions
     * 
     */
    @Test
    @DisplayName("``$regexFindAll`` and Its Options")
    public void testRegexFindAllAndItsOptions() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("returnObject", regexFindAll("$description").pattern("line"))));

    }

    @Test
    @DisplayName("Find Every Line or Link")
    public void testFindEveryLineOrLink() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returnObject", regexFindAll("$description").pattern("lin(e|k)"))));
    }

    @Test
    @DisplayName("Find Every Tier")
    public void testFindEveryTier() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returnObject", regexFindAll("$description").pattern("tier"))));
    }

    @Test
    @DisplayName("i Option")
    public void testIOption() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returnObject", regexFindAll("$description").pattern("line").options("i"))));
    }

    @Test
    @DisplayName("m Option")
    public void testMOption() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returnObject", regexFindAll("$description").pattern("^s").options("im"))));
    }

    @Test
    @DisplayName("x Option")
    public void testXOption() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returnObject", regexFindAll("$description").pattern("lin(e|k) # matches line or link").options("x"))));
    }
}
