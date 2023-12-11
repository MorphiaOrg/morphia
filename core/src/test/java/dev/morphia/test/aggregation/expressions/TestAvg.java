package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Projection;
import dev.morphia.aggregation.stages.SetWindowFields;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAvg extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ANY, false, false, aggregation -> aggregation
                .pipeline(group(id(field("item")))
                        .field("avgAmount", avg(multiply(field("price"), field("quantity"))))
                        .field("avgQuantity", avg(field("quantity")))));

    }

    @Test
    public void testExample2() {
        testPipeline(ANY, false, false, aggregation -> aggregation
                .project(Projection.project()
                        .include("quizAvg", avg(field("quizzes")))
                        .include("labAvg", avg(field("labs")))
                        .include("examAvg", avg(field("final"), field("midterm")))));

    }

    @Test
    public void testExample3() {
        testPipeline(ANY, false, false, aggregation -> aggregation
                .setWindowFields(SetWindowFields.setWindowFields()
                        .partitionBy(field("state"))
                        .sortBy(ascending("orderDate"))
                        .output(output("averageQuantityForState")
                                .operator(avg(field("quantity")))
                                .window()
                                .documents("unbounded", "current"))

                )

        );

    }
}
