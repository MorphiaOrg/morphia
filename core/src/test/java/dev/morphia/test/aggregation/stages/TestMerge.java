package dev.morphia.test.aggregation.stages;

import java.time.LocalDate;
import java.time.Month;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static com.mongodb.client.model.MergeOptions.WhenMatched.FAIL;
import static com.mongodb.client.model.MergeOptions.WhenMatched.MERGE;
import static com.mongodb.client.model.MergeOptions.WhenMatched.REPLACE;
import static com.mongodb.client.model.MergeOptions.WhenNotMatched.INSERT;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Merge.merge;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lt;

public class TestMerge extends AggregationTest {
    public TestMerge() {
        skipDataCheck();
    }

    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                group(id()
                        .field("fiscal_year", field("fiscal_year"))
                        .field("dept", field("dept")))
                        .field("salaries", sum(field("salary"))),
                merge("reporting", "budgets")
                        .on("_id")
                        .whenMatched(REPLACE)
                        .whenNotMatched(INSERT)));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(gte("fiscal_year", 2019)),
                group(id()
                        .field("fiscal_year", field("fiscal_year"))
                        .field("dept", field("dept")))
                        .field("salaries", sum(field("salary"))),
                merge("reporting", "budgets")
                        .on("_id")
                        .whenMatched(REPLACE)
                        .whenNotMatched(INSERT)));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(eq("fiscal_year", 2019)),
                group(id()
                        .field("fiscal_year", field("fiscal_year"))
                        .field("dept", field("dept")))
                        .field("employees", push(field("employee"))),
                project()
                        .suppressId()
                        .include("dept", field("_id.dept"))
                        .include("fiscal_year", field("_id.fiscal_year"))
                        .include("employees"),
                merge("reporting", "orgArchive")
                        .on("dept", "fiscal_year")
                        .whenMatched(FAIL)));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                group()
                        .field("_id", field("quarter"))
                        .field("purchased", sum(field("qty"))),
                merge("quarterlyreport")
                        .on("_id")
                        .whenMatched(MERGE)
                        .whenNotMatched(INSERT)));
    }

    @Test
    public void testExample5() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(
                        gte("date", LocalDate.of(2019, Month.MAY, 7)),
                        lt("date", LocalDate.of(2019, Month.MAY, 8))),
                project()
                        .include("_id", dateToString()
                                .date(field("date"))
                                .format("%Y-%m"))
                        .include("thumbsup")
                        .include("thumbsdown"),
                merge("monthlytotals")
                        .on("_id")
                        .whenMatched(
                                addFields()
                                        .field("thumbsup", add(field("thumbsup"), value("$$new.thumbsup")))
                                        .field("thumbsdown", add(field("thumbsdown"), value("$$new.thumbsdown"))))
                        .whenNotMatched(INSERT)));
    }

    @Test
    public void testExample6() {
        // a bit of arcane example Morphia's not well-suited for
    }
}
