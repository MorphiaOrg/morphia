package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateFromString;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateFromString extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/dateFromString/convertingDates
     * 
     */
    @Test
    @DisplayName("Converting Dates")
    public void testConvertingDates() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("date", dateFromString().dateString("$date").timeZone("America/New_York"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateFromString/onError
     * 
     */
    @Test
    @DisplayName("``onError``")
    public void testOnError() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("date",
                dateFromString().dateString("$date").timeZone("$timezone").onError("$date"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateFromString/onNull
     * 
     */
    @Test
    @DisplayName("``onNull``")
    public void testOnNull() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("date", dateFromString().dateString("$date").timeZone("$timezone").onNull("oops"))

        ));
    }

}
