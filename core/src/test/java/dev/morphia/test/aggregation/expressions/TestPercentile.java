package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.percentile;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.test.ServerVersion.v70;
import static java.util.List.of;

public class TestPercentile extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/percentile/example1
     */
    @Test(testName = "Calculate a Single Value as an Accumulator")
    public void testExample1() {
        testPipeline(v70, false, false, aggregation -> aggregation
                .pipeline(group().field("test01_percentiles", percentile("$test01", of(0.95)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/percentile/example2
     *
     */
    @Test(testName = "Calculate Multiple Values as an Accumulator")
    public void testExample2() {
        testPipeline(v70, false, false,
                aggregation -> aggregation
                        .pipeline(group().field("test01_percentiles", percentile("$test01", of(0.5, 0.75, 0.9, 0.95)))
                                .field("test02_percentiles", percentile("$test02", of(0.5, 0.75, 0.9, 0.95)))
                                .field("test03_percentiles", percentile("$test03", of(0.5, 0.75, 0.9, 0.95)))
                                .field("test03_percent_alt", percentile("$test03", of(0.9, 0.5, 0.75, 0.95)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/percentile/example3
     *
     */
    @Test(testName = "Use |operatorName| in a ``$project`` Stage")
    public void testExample3() {
        testPipeline(v70, false, false, aggregation -> aggregation.pipeline(project().suppressId().include("studentId")
                .include("testPercentiles", percentile(List.of("$test01", "$test02", "$test03"), of(0.5, 0.95)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/percentile/example4
     *
     */
    @Test(testName = "Use |operatorName| in a ``$setWindowField`` Stage")
    public void testExample4() {

        testPipeline(v70, false, false,
                aggregation -> aggregation.pipeline(
                        setWindowFields().sortBy(ascending("test01"))
                                .output(output("test01_95percentile").operator(percentile("$test01", List.of(0.95)))
                                        .window().range(-3, 3)),
                        project().suppressId().include("studentId").include("test01_95percentile")

                ));
    }

    @Test(testName = "Use |operatorName| in a ``$setWindowField`` Stage")
    public void testExample5() {

    }

}
