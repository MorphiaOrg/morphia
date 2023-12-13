package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.DateExpressions.dateDiff;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.trunc;
import static dev.morphia.aggregation.expressions.TimeUnit.DAY;
import static dev.morphia.aggregation.expressions.TimeUnit.MONTH;
import static dev.morphia.aggregation.expressions.TimeUnit.WEEK;
import static dev.morphia.aggregation.expressions.TimeUnit.YEAR;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static java.time.DayOfWeek.*;
import static java.time.DayOfWeek.FRIDAY;

public class TestDateDiff extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                group(id(value(null)))
                        .field("averageTime",
                                avg(dateDiff(field("purchased"), field("delivered"), TimeUnit.DAY))),
                project()
                        .suppressId()
                        .include("numDays", trunc(field("averageTime"), value(1)))

        ));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("start", field("start"))
                        .include("end", field("end"))
                        .include("years", dateDiff(field("start"), field("end"), YEAR))
                        .include("months", dateDiff(field("start"), field("end"), MONTH))
                        .include("days", dateDiff(field("start"), field("end"), DAY))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("wks_default", dateDiff(field("start"), field("end"), WEEK))
                        .include("wks_monday", dateDiff(field("start"), field("end"), WEEK).startOfWeek(MONDAY))
                        .include("wks_friday", dateDiff(field("start"), field("end"), WEEK).startOfWeek(FRIDAY))));
    }

}
