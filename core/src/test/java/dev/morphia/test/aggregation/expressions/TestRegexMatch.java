package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.regexMatch;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestRegexMatch extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/regexMatch/regexMatchAndItsOptions
     * 
     */
    @Test
    @DisplayName("``$regexMatch`` and Its Options")
    public void testRegexMatchAndItsOptions() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("result", regexMatch("$description").pattern("line"))));

    }

    @Test
    @DisplayName("i Option")
    public void testIOption() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("result", regexMatch("$description").pattern("line").options("i"))));
    }

    @Test
    @DisplayName("m Option")
    public void testMOption() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("result", regexMatch("$description").pattern("^s").options("im"))));
    }

    @Test
    @DisplayName("Match Line or Link")
    public void testMatchLineOrLink() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("result", regexMatch("$description").pattern("lin(e|k)"))));
    }

    @Test
    @DisplayName("s Option")
    public void testSOption() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returns", regexMatch("$description").pattern("m.*line").options("si"))));
    }

    @Test
    @DisplayName("x Option")
    public void testXOption() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("returns", regexMatch("$description").pattern("lin(e|k) # matches line or link").options("x"))));
    }
}
