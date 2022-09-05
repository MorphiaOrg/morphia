package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Projection;
import dev.morphia.test.aggregation.AggregationTest;
import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.tsIncrement;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestTsIncrement extends AggregationTest {
    @Test
    public void testTimestampOrdinal() {
        getDatabase().getCollection("aggtest").insertOne(new Document("long", 42L));
        testPipeline(5.1, "timestampOrdinal", (aggregation) -> {
            return  aggregation.project(project()
                                            .suppressId()
                                            .include("saleTimestamp")
                                            .include("saleIncrement", tsIncrement(field("saleTimestamp"))));
        });
    }
}
