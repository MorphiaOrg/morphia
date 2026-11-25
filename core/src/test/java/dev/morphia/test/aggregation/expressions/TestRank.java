package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.rank;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;

public class TestRank extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/rank/rankPartitionsByAnIntegerField
     * 
     */
    @Test
    @DisplayName("Rank Partitions by an Integer Field")
    public void testRankPartitionsByAnIntegerField() {
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(descending("quantity")).output(output("rankQuantityForState").operator(rank()))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/rank/rankPartitionsByADateField
     * 
     */
    @Test
    @DisplayName("Rank Partitions by a Date Field")
    public void testRankPartitionsByADateField() {
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(ascending("orderDate")).output(output("rankOrderDateForState").operator(rank()))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/rank/rankPartitionsContainingDuplicateValuesNullsOrMissingData
     * 
     */
    @Test
    @DisplayName("Rank Partitions Containing Duplicate Values, Nulls, or Missing Data")
    public void testRankPartitionsContainingDuplicateValuesNullsOrMissingData() {
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(descending("quantity")).output(output("rankQuantityForState").operator(rank()))));
    }

}
