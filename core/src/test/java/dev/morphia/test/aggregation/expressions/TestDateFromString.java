package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateFromString;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateFromString extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/dateFromString/example1
     * 
     */
    @Test
    @DisplayName("Converting Dates")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("date", dateFromString().dateString("$date").timeZone("America/New_York"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateFromString/example2
     * 
     */
    @Test
    @DisplayName("``onError``")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("date",
                dateFromString().dateString("$date").timeZone("$timezone").onError("$date"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateFromString/example3
     * 
     */
    @Test
    @DisplayName("``onNull``")
    public void testExample3() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("date", dateFromString().dateString("$date").timeZone("$timezone").onNull("oops"))

        ));
    }

}
