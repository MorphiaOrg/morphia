package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.max;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;

public class TestMax extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/max/useInGroupStage
     * 
     */
    @Test
    @DisplayName("Use in ``$group`` Stage")
    public void testUseInGroupStage() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation
                        .pipeline(group(id("$item")).field("maxTotalAmount", max(multiply("$price", "$quantity")))
                                .field("maxQuantity", max("$quantity"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/max/useInProjectStage
     * 
     */
    @Test
    @DisplayName("Use in ``$project`` Stage")
    public void testUseInProjectStage() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("quizMax", max("$quizzes"))
                .include("labMax", max("$labs")).include("examMax", max("$final", "$midterm"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/max/useInSetWindowFieldsStage
     * 
     */
    @Test
    @DisplayName("Use in ``$setWindowFields`` Stage")
    public void testUseInSetWindowFieldsStage() {
        checkMinServerVersion("5.0.0");
        testPipeline(new ActionTestOptions(),
                (aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                        .sortBy(ascending("orderDate")).output(output("maximumQuantityForState")
                                .operator(max("$quantity")).window().documents("unbounded", "current"))));
    }

}
