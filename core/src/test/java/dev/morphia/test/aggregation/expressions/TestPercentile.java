package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.percentile;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.test.ServerVersion.v70;
import static java.util.List.of;

public class TestPercentile extends AggregationTest {
    @Test
    public void testExample2() {
        testPipeline(v70, false, false, aggregation -> aggregation
                .pipeline(
                        group()
                                .field("test01_percentiles", percentile(field("test01"),
                                        of(value(0.95))))));
    }

    @Test
    public void testExample3() {
        testPipeline(v70, false, false, aggregation -> aggregation.pipeline(
                group()
                        .field("test01_percentiles",
                                percentile(field("test01"), of(value(0.5), value(0.75), value(0.9), value(0.95))))
                        .field("test02_percentiles",
                                percentile(field("test02"), of(value(0.5), value(0.75), value(0.9), value(0.95))))
                        .field("test03_percentiles",
                                percentile(field("test03"), of(value(0.5), value(0.75), value(0.9), value(0.95))))
                        .field("test03_percent_alt",
                                percentile(field("test03"), of(value(0.9), value(0.5), value(0.75), value(0.95))))));
    }

    @Test
    public void testExample4() {
        testPipeline(v70, false, false, aggregation -> aggregation
                .pipeline(project()
                        .suppressId()
                        .include("studentId")
                        .include("testPercentiles",
                                percentile(List.of(field("test01"), field("test02"), field("test03")),
                                        of(value(0.5), value(0.95))))));

    }

    @Test
    public void testExample5() {
        testPipeline(v70, false, false, aggregation -> aggregation.pipeline(
                setWindowFields()
                        .sortBy(ascending("test01"))
                        .output(output("test01_95percentile")
                                .operator(percentile(field("test01"), List.of(value(0.95))))
                                .window().range(-3, 3)),
                project()
                        .suppressId()
                        .include("studentId")
                        .include("test01_95percentile")

        ));

    }

}
