package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lt;

public class TestGroup extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/group/pivotData
     * 
     */
    @Test
    @DisplayName("Pivot Data")
    public void testPivotData() {
        testPipeline((aggregation) -> aggregation.pipeline(group(id("$author")).field("books", push("$title"))));
    }

    @Test
    @DisplayName("Group by null")
    public void testGroupByNull() {
        testPipeline((aggregation) -> aggregation.pipeline(
                group(id(null)).field("totalRuntime", sum("$runtime")).field("averageRuntime", avg("$runtime")).field("count", sum(1))));
    }

    @Test
    @DisplayName("Group by Rating")
    public void testGroupByRating() {
        testPipeline((aggregation) -> aggregation.pipeline(
                group(id("$rated")).field("totalRuntime", sum("$runtime")),
                match(gte("totalRuntime", 100000))));
    }

    @Test
    @DisplayName("Group by Year")
    public void testGroupByYear() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(lt("year", 1910)),
                group(id("$year")).field("totalRuntime", sum("$runtime")).field("averageRuntime", avg("$runtime")).field("count", sum(1)),
                sort().descending("totalRuntime")));
    }

    @Test
    @DisplayName("Group Documents by Year")
    public void testGroupDocumentsByYear() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(lt("year", 1910)),
                group(id("$year")).field("movies", push("$$ROOT")),
                addFields().field("totalRuntime", sum("$movies.runtime")),
                sort().ascending("_id")));
    }

    @Test
    @DisplayName("Group Titles by Year")
    public void testGroupTitlesByYear() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(lt("year", 1910)),
                group(id("$year")).field("titles", push("$title")),
                sort().ascending("_id")));
    }

    @Test
    @DisplayName("Retrieve Distinct Values")
    public void testRetrieveDistinctValues() {
        testPipeline((aggregation) -> aggregation.pipeline(
                group(id("$rated"))));
    }
}
