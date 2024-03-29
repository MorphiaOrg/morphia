package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Sort;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.min;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.test.ServerVersion.v50;

public class TestMin extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(v50, false, false, (aggregation) -> aggregation.pipeline(
                group(id("$item"))
                        .field("minQuantity", min("$quantity"))));
    }

    @Test
    public void testExample2() {
        testPipeline(v50, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("quizMin", min("$quizzes"))
                        .include("labMin", min("$labs"))
                        .include("examMin", min("$final", "$midterm"))));
    }

    @Test
    public void testExample3() {
        testPipeline(v50, false, true, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy("$state")
                        .sortBy(Sort.ascending("orderDate"))
                        .output(output("minimumQuantityForState")
                                .operator(min("$quantity"))
                                .window()
                                .documents("unbounded", "current"))));
    }

}
