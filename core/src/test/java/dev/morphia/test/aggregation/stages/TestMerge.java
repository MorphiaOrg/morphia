package dev.morphia.test.aggregation.stages;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.model.MergeOptions.WhenMatched;
import com.mongodb.client.model.MergeOptions.WhenNotMatched;

import dev.morphia.test.TemplatedTestBase;

import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.mongodb.client.model.MergeOptions.WhenMatched.KEEP_EXISTING;
import static com.mongodb.client.model.MergeOptions.WhenNotMatched.INSERT;
import static dev.morphia.aggregation.AggregationOptions.aggregationOptions;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Merge.merge;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lte;
import static dev.morphia.query.filters.Filters.ne;

public class TestMerge extends TemplatedTestBase {

    @Test
    @DisplayName("Merge Results from Multiple Collections")
    public void testMergeResultsFromMultipleCollections() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(eq("metacritic", 100), ne("rated", null), gte("year", 1970), lte("year", 1972)),
                group(id("$year")).field("movieCount", sum(1)),
                merge("yearlyStats").on("_id").whenMatched(WhenMatched.MERGE).whenNotMatched(WhenNotMatched.INSERT)));
    }

    @Test
    @DisplayName("On-Demand Materialized View: Update/Replace Data")
    public void testOnDemandMaterializedViewUpdateReplaceData() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(eq("metacritic", 100), ne("rated", null), gte("year", 1963)),
                group(id(document().field("year", "$year").field("rated", "$rated"))).field("count", sum(1)),
                merge("movieRatingSummary").on("_id").whenMatched(WhenMatched.REPLACE).whenNotMatched(WhenNotMatched.INSERT)));
    }

    @Test
    @DisplayName("Only Insert New Data")
    public void testOnlyInsertNewData() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(eq("metacritic", 100), ne("rated", null), gte("year", 1963)),
                group(id("$year")).field("titles", push("$title")),
                project().suppressId().include("year", "$_id").include("titles"),
                merge("movieArchive").on("year").whenMatched(WhenMatched.FAIL)));
    }

    @Test
    @DisplayName("Summarize Ratings into Another Collection")
    public void testSummarizeRatingsIntoAnotherCollection() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(eq("metacritic", 100), ne("rated", null), lte("year", 1972)),
                group(id(document().field("year", "$year").field("rated", "$rated"))).field("count", sum(1)),
                merge("movieRatingSummary").on("_id").whenMatched(WhenMatched.REPLACE).whenNotMatched(WhenNotMatched.INSERT)));
    }

    @Test
    @DisplayName("whenMatched: keepExisting keeps the target's document")
    public void keepExisting() {
        getDatabase().getCollection("mergeSource").insertMany(List.of(
                new Document("_id", 1).append("value", "new"),
                new Document("_id", 2).append("value", "new")));
        getDatabase().getCollection("mergeTarget").insertOne(new Document("_id", 1).append("value", "old"));

        getDs().aggregate(aggregationOptions().collection("mergeSource"))
                .pipeline(merge("mergeTarget").on("_id").whenMatched(KEEP_EXISTING).whenNotMatched(INSERT))
                .toList();

        Assertions.assertEquals(
                List.of(new Document("_id", 1).append("value", "old"), new Document("_id", 2).append("value", "new")),
                getDatabase().getCollection("mergeTarget").find().sort(new Document("_id", 1)).into(new ArrayList<>()));
    }
}
