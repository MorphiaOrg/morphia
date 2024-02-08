package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.AccumulatorExpressions;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.query.Sort;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.test.ServerVersion.v50;

public class TestCount extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(v50, false, false, aggregation -> aggregation
                .pipeline(group(Group.id("$state"))
                        .field("countNumberOfDocumentsForState", AccumulatorExpressions.count())));

    }

    @Test
    public void testExample3() {
        testPipeline(v50, false, false,
                aggregation -> aggregation
                        .setWindowFields(setWindowFields()
                                .partitionBy(("$state"))
                                .sortBy(Sort.ascending("orderDate"))
                                .output(output("countNumberOfDocumentsForState")
                                        .operator(AccumulatorExpressions.count())
                                        .window().documents("unbounded", "current"))));
    }

}
