package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Sort;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.min;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;

public class TestMin extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                group(id(field("item")))
                        .field("minQuantity", min(field("quantity")))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("quizMin", min(field("quizzes")))
                        .include("labMin", min(field("labs")))
                        .include("examMin", min(field("final"), field("midterm")))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy(field("state"))
                        .sortBy(Sort.ascending("orderDate"))
                        .output(output("minimumQuantityForState")
                                .operator(min(field("quantity")))
                                .window()
                                .documents("unbounded", "current"))));
    }

}
