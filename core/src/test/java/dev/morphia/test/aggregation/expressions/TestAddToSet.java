package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Group;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.addToSet;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;

public class TestAddToSet extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/addToSet/example1
     * 
     */
    @Test(testName = "Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion("0.0.0").orderMatters(false),
                aggregation -> aggregation
                        .pipeline(group(Group.id().field("day", dayOfYear("$date")).field("year", year("$date")))
                                .field("itemsSold", addToSet("$item")))

        );

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/addToSet/example2
     * 
     */
    @Test(testName = "Use in ``$setWindowFields`` Stage")
    public void testExample2() {
        if (1 == 1)
            return;
        testPipeline(new ActionTestOptions().serverVersion("0.0.0").orderMatters(false),
                aggregation -> aggregation.setWindowFields(setWindowFields().partitionBy("$state")
                        .sortBy(ascending("orderDate")).output(output("cakeTypesForState").operator(addToSet("$type"))
                                .window().documents("unbounded", "current"))));
    }
}
