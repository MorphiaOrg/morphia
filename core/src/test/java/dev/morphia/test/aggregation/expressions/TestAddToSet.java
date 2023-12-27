package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Group;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.addToSet;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAddToSet extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ANY, false, false, aggregation -> aggregation
                .pipeline(group(Group.id()
                        .field("day", dayOfYear("$date"))
                        .field("year", year("$date")))
                        .field("itemsSold", addToSet("$item")))

        );

    }

    @Test
    public void testExample2() {
        testPipeline(ANY, false, false, aggregation -> aggregation
                .setWindowFields(setWindowFields()
                        .partitionBy("$state")
                        .sortBy(ascending("orderDate"))
                        .output(output("cakeTypesForState")
                                .operator(addToSet("$type"))
                                .window()
                                .documents("unbounded", "current"))));
    }
}
