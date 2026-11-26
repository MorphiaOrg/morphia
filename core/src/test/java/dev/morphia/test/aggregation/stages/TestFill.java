package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.expressions.StringExpressions;
import dev.morphia.aggregation.stages.Fill.Method;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ConditionalExpressions.ifNull;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.TypeExpressions.toBool;
import static dev.morphia.aggregation.stages.Fill.fill;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.query.Sort.ascending;

public class TestFill extends TemplatedTestBase {
    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.3.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/fill/fillMissingFieldValuesWithAConstantValue
     * 
     */
    @Test
    @DisplayName("Fill Missing Field Values with a Constant Value")
    public void testFillMissingFieldValuesWithAConstantValue() {
        testPipeline(aggregation -> aggregation
                .pipeline(fill().field("bootsSold", 0).field("sandalsSold", 0).field("sneakersSold", 0)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/fill/fillMissingFieldValuesWithLinearInterpolation
     * 
     */
    @Test
    @DisplayName("Fill Missing Field Values with Linear Interpolation")
    public void testFillMissingFieldValuesWithLinearInterpolation() {
        testPipeline(aggregation -> aggregation.pipeline(fill().sortBy(ascending("time")).field("price", Method.LINEAR)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/fill/fillMissingFieldValuesBasedOnTheLastObservedValue
     * 
     */
    @Test
    @DisplayName("Fill Missing Field Values Based on the Last Observed Value")
    public void testFillMissingFieldValuesBasedOnTheLastObservedValue() {
        testPipeline(aggregation -> aggregation.pipeline(fill().sortBy(ascending("date")).field("score", Method.LOCF)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/fill/fillDataForDistinctPartitions
     * 
     */
    @Test
    @DisplayName("Fill Data for Distinct Partitions")
    public void testFillDataForDistinctPartitions() {
        testPipeline(aggregation -> aggregation.pipeline(fill().sortBy(ascending("date"))
                .partitionBy(document("restaurant", "$restaurant")).field("score", Method.LOCF)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/fill/indicateIfAFieldWasPopulatedUsingFill
     * 
     */
    @Test
    @DisplayName("Indicate if a Field was Populated Using ``$fill``")
    public void testIndicateIfAFieldWasPopulatedUsingFill() {
        testPipeline((aggregation) -> aggregation
                .pipeline(
                        set().field("valueExisted",
                                ifNull().target(toBool(StringExpressions.toString("$score")))
                                        .replacement(false)),
                        fill().sortBy(ascending("date")).field("score", Method.LOCF)));
    }

}
