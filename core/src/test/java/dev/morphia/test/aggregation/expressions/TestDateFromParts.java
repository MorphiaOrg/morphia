package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateFromParts;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateFromParts extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/dateFromParts/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(project().include("date", dateFromParts().year(2017).month(2).day(8).hour(12))
                        .include("date_iso", dateFromParts().isoWeekYear(2017).isoWeek(6).isoDayOfWeek(3).hour(12))
                        .include("date_timezone", dateFromParts().year(2016).month(12).day(31).hour(23).minute(46)
                                .second(12).timezone("America/New_York"))));
    }

}
