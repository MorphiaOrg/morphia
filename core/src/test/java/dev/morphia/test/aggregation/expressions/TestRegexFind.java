package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.regexFind;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestRegexFind extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/regexFind/regexFindAndItsOptions
     * 
     */
    @Test
    @DisplayName("``$regexFind`` and Its Options")
    public void testRegexFindAndItsOptions() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("returnObject", regexFind("$description").pattern("line"))));
    }

    @Test
    @DisplayName("Find the First Line or Link")
    public void testFindTheFirstLineOrLink() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returnObject", regexFind("$description").pattern("lin(e|k)"))));
    }

    @Test
    @DisplayName("Find the First Tier")
    public void testFindTheFirstTier() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returnObject", regexFind("$description").pattern("tier"))));
    }

    @Test
    @DisplayName("i Option")
    public void testIOption() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returnObject", regexFind("$description").pattern("line").options("i"))));
    }

    @Test
    @DisplayName("m Option")
    public void testMOption() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returnObject", regexFind("$description").pattern("^s").options("im"))));
    }

    @Test
    @DisplayName("s Option")
    public void testSOption() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returnObject", regexFind("$description").pattern("m.*line").options("si"))));
    }

    @Test
    @DisplayName("x Option")
    public void testXOption() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returnObject", regexFind("$description").pattern("lin(e|k) # matches line or link").options("x"))));
    }
}
