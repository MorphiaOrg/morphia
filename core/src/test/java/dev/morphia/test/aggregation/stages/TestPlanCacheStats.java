package dev.morphia.test.aggregation.stages;

import com.mongodb.client.model.IndexOptions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.PlanCacheStats.planCacheStats;
import static dev.morphia.query.filters.Filters.eq;

public class TestPlanCacheStats extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/planCacheStats/example1
     * 
     */
    @Test(testName = "Return Information for All Entries in the Query Cache")
    public void testExample1() {
        Document keys = new Document("item", 1).append("price", 1);
        var options = new IndexOptions().partialFilterExpression(new Document("price", new Document("$gte", 10.0)));
        getDatabase().getCollection(EXAMPLE_TEST_COLLECTION).createIndex(keys, options);
        testPipeline(new ActionTestOptions().skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(planCacheStats()));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/planCacheStats/example2
     * 
     */
    @Test(testName = "Find Cache Entry Details for a Query Hash")
    public void testExample2() {
        Document keys = new Document("item", 1).append("price", 1);
        var options = new IndexOptions().partialFilterExpression(new Document("price", new Document("$gte", 10.0)));
        testPipeline(new ActionTestOptions().skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(planCacheStats(), match(eq("planCacheKey", "B1435201"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/planCacheStats/example3
     */
    @Test(testName = "Find Cache Entry Details for a Query Hash")
    public void testExample3() {
        testPipeline(new ActionTestOptions().skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(planCacheStats(), match(eq("planCacheKey", "B1435201"))));
    }
}
