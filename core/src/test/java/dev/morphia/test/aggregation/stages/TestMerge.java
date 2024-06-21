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

    /**
     * test data: dev/morphia/test/aggregation/stages/merge/example1
     * 
     */
    @Test(testName = "On-Demand Materialized View: Initial Creation")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(
                        group(id().field("fiscal_year", "$fiscal_year").field("dept", "$dept")).field("salaries",
                                sum("$salary")),
                        merge("reporting", "budgets").on("_id").whenMatched(REPLACE).whenNotMatched(INSERT)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/merge/example2
     * 
     */
    @Test(testName = "On-Demand Materialized View: Update/Replace Data")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(match(gte("fiscal_year", 2019)),
                        group(id().field("fiscal_year", "$fiscal_year").field("dept", "$dept")).field("salaries",
                                sum("$salary")),
                        merge("reporting", "budgets").on("_id").whenMatched(REPLACE).whenNotMatched(INSERT)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/merge/example3
     * 
     */
    @Test(testName = "Only Insert New Data")
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(match(eq("fiscal_year", 2019)),
                        group(id().field("fiscal_year", "$fiscal_year").field("dept", "$dept")).field("employees",
                                push("$employee")),
                        project().suppressId().include("dept", "$_id.dept").include("fiscal_year", "$_id.fiscal_year")
                                .include("employees"),
                        merge("reporting", "orgArchive").on("dept", "fiscal_year").whenMatched(FAIL)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/merge/example4
     * 
     */
    @Test(testName = "Merge Results from Multiple Collections")
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(group().field("_id", "$quarter").field("purchased", sum("$qty")),
                        merge("quarterlyreport").on("_id").whenMatched(MERGE).whenNotMatched(INSERT)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/merge/example5
     * 
     */
    @Test(testName = "Use the Pipeline to Customize the Merge")
    public void testExample5() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(
                        match(gte("date", LocalDate.of(2019, Month.MAY, 7)),
                                lt("date", LocalDate.of(2019, Month.MAY, 8))),
                        project().include("_id", dateToString().date("$date").format("%Y-%m")).include("thumbsup")
                                .include("thumbsdown"),
                        merge("monthlytotals").on("_id")
                                .whenMatched(addFields().field("thumbsup", add("$thumbsup", "$$new.thumbsup"))
                                        .field("thumbsdown", add("$thumbsdown", "$$new.thumbsdown")))
                                .whenNotMatched(INSERT)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/merge/example6
     * 
     */
    @Test(testName = "Use Variables to Customize the Merge :: Merge Stage")
    public void testExample6() {
        // merging in to another db complicates the test infra and doesn't really
        // provide much value as another test
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/merge/example7
     * 
     */
    @Test(testName = "Use Variables to Customize the Merge :: Aggregate Command")
    public void testExample7() {
        // merging in to another db complicates the test infra and doesn't really
        // provide much value as another test
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/merge/example8
     * 
     */
    @Test(testName = "Use Variables to Customize the Merge :: Merge and Aggregate")
    public void testExample8() {
        // merging in to another db complicates the test infra and doesn't really
        // provide much value as another test
    }
}
