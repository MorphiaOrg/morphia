package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Group;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.MathExpressions.median;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.test.ServerVersion.v70;

public class TestMedian extends AggregationTest {
    @Test
    public void testExample2() {
        testPipeline(v70, false, false,
                aggregation -> aggregation
                        .group(Group.group()
                                .field("test01_median", median(field("test01")))));
    }

    @Test
    public void testExample3() {
        testPipeline(v70, false, true, aggregation -> aggregation.project(project()
                .suppressId()
                .include("studentId")
                .include("testMedians", median(
                        array(field("test01"), field("test02"), field("test03"))))));
    }

    @Test
    public void testExample4() {
        testPipeline(v70, false, true, aggregation -> aggregation
                .setWindowFields(setWindowFields()
                        .sortBy(ascending("test01"))
                        .output(output("test01_median")
                                .operator(median(field("test01")))
                                .window().range(-3, 3)))
                .project(project()
                        .suppressId()
                        .include("studentId")
                        .include("test01_median")));
    }
}
