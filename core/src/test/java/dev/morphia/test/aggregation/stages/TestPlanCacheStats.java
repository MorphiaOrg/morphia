package dev.morphia.test.aggregation.stages;

import com.mongodb.client.model.IndexOptions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.PlanCacheStats.planCacheStats;
import static dev.morphia.query.filters.Filters.eq;

public class TestPlanCacheStats extends AggregationTest {
    public TestPlanCacheStats() {
        skipDataCheck();
    }

    @Test
    public void testExample1() {
        Document keys = new Document("item", 1)
                .append("price", 1);
        var options = new IndexOptions()
                .partialFilterExpression(new Document("price", new Document("$gte", 10.0)));
        getDatabase().getCollection(EXAMPLE_TEST_COLLECTION).createIndex(keys, options);
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                planCacheStats()));
    }

    @Test
    public void testExample2() {
        Document keys = new Document("item", 1)
                .append("price", 1);
        var options = new IndexOptions()
                .partialFilterExpression(new Document("price", new Document("$gte", 10.0)));
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                planCacheStats(),
                match(eq("planCacheKey", "B1435201"))));
    }
}
