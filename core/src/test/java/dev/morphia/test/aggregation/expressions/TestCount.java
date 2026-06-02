package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.AccumulatorExpressions;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;

public class TestCount extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/count/example1
     * 
     */
    @Test
    @DisplayName("Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline(new ActionTestOptions().orderMatters(false), aggregation -> aggregation.pipeline(
                group(Group.id("$state")).field("countNumberOfDocumentsForState", AccumulatorExpressions.count())));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/count/example2
     * 
     */
    @Test
    @DisplayName("Use in ``$setWindowFields`` Stage")
    public void testExample2() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                aggregation -> aggregation.pipeline(setWindowFields().partitionBy(("$state"))
                        .sortBy(ascending("orderDate")).output(output("countNumberOfDocumentsForState")
                                .operator(AccumulatorExpressions.count()).window().documents("unbounded", "current"))));
    }

}
