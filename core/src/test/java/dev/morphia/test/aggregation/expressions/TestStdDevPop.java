package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.stdDevPop;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.*;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.*;

public class TestStdDevPop extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/stdDevPop/example1
     * 
     */
    @Test
    @DisplayName("Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(group(id("$quiz")).field("stdDev", stdDevPop("$score"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/stdDevPop/example2
     * 
     */
    @Test
    @DisplayName("Use in ``$project`` Stage")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("stdDev", stdDevPop("$scores.score"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/stdDevPop/example3
     * 
     */
    @Test
    @DisplayName("Use in ``$setWindowFields`` Stage")
    public void testExample3() {
        checkMinServerVersion("5.0.0");
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                        .sortBy(ascending("orderDate")).output(output("stdDevPopQuantityForState")
                                .operator(stdDevPop("$quantity")).window().documents("unbounded", "current"))

                ));
    }

}
