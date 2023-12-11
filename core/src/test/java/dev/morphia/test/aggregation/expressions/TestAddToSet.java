package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Group;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.addToSet;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAddToSet extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ANY, false, false, aggregation -> aggregation
                .group(Group.group(Group.id()
                        .field("day", dayOfYear(field("date")))
                        .field("year", year(field("date"))))
                        .field("itemsSold", addToSet(field("item"))))

        );

    }

    @Test
    public void testExample2() {
        testPipeline(ANY, false, false, aggregation -> aggregation
                .setWindowFields(setWindowFields()
                        .partitionBy(field("state"))
                        .sortBy(ascending("orderDate"))
                        .output(output("cakeTypesForState")
                                .operator(addToSet(field("type")))
                                .window()
                                .documents("unbounded", "current"))));
    }
}
