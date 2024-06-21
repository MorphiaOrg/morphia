package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.AccumulatorExpressions;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.query.Sort;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.test.ServerVersion.v50;

public class TestCount extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/count/example1
     * 
     */
    @Test(testName = "Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v50).removeIds(false).orderMatters(false),
                aggregation -> aggregation.pipeline(group(Group.id("$state")).field("countNumberOfDocumentsForState",
                        AccumulatorExpressions.count())));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/count/example2
     * 
     */
    @Test(testName = "Use in ``$setWindowFields`` Stage")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(v50).removeIds(false).orderMatters(false),
                aggregation -> aggregation.setWindowFields(setWindowFields().partitionBy(("$state"))
                        .sortBy(Sort.ascending("orderDate")).output(output("countNumberOfDocumentsForState")
                                .operator(AccumulatorExpressions.count()).window().documents("unbounded", "current"))));
    }

}
