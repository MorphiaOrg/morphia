package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateFromString;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateFromString extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/dateFromString/example1
     * 
     */
    @Test(testName = "Converting Dates")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project().include("date", dateFromString().dateString("$date").timeZone("America/New_York"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateFromString/example2
     * 
     */
    @Test(testName = "``onError``")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(project().include("date",
                dateFromString().dateString("$date").timeZone("$timezone").onError("$date"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateFromString/example3
     * 
     */
    @Test(testName = "``onNull``")
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project().include("date", dateFromString().dateString("$date").timeZone("$timezone").onNull("oops"))

        ));
    }

}
