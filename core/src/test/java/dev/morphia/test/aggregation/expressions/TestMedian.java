package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.MathExpressions.median;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.test.ServerVersion.ANY;
import static dev.morphia.test.ServerVersion.v70;

public class TestMedian extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/median/example1
     */
    @Test(testName = "Use |operatorName| as an Accumulator")
    public void testExample1() {
        testPipeline(ANY, false, false,
                aggregation -> aggregation.pipeline(
                        group()
                                .field("test01_median", median("$test01"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/median/example2
     *
     */
    @Test(testName = "Use |operatorName| in a ``$project`` Stage")
    public void testExample2() {
        testPipeline(v70, false, true, aggregation -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("studentId")
                        .include("testMedians", median(array("$test01", "$test02", "$test03")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/median/example3
     *
     */
    @Test(testName = "Use |operatorName| in a ``$setWindowField`` Stage")
    public void testExample3() {
        testPipeline(v70, false, true,
                aggregation -> aggregation.pipeline(
                        setWindowFields().sortBy(ascending("test01"))
                                .output(output("test01_median").operator(median("$test01")).window().range(-3, 3)),
                        project().suppressId().include("studentId").include("test01_median")));
    }
}
