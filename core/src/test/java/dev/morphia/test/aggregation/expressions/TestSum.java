package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Sort;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.test.ServerVersion.v50;

public class TestSum extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(v50, false, false, (aggregation) -> aggregation.pipeline(
                group(
                        id(document()
                                .field("day", dayOfYear(field("date")))
                                .field("year", year(field("date")))))
                        .field("totalAmount", sum(multiply(field("price"), field("quantity"))))
                        .field("count", sum(value(1)))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("quizTotal", sum(field("quizzes")))
                        .include("labTotal", sum(field("labs")))
                        .include("examTotal", sum(field("final"), field("midterm")))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy(field("state"))
                        .sortBy(Sort.ascending("orderDate"))
                        .output(output("sumQuantityForState")
                                .operator(sum(field("quantity")))
                                .window()
                                .documents("unbounded", "current"))));
    }

}
