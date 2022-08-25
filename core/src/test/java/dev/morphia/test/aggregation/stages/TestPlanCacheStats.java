package dev.morphia.test.aggregation.stages;

import com.mongodb.ReadConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import dev.morphia.aggregation.AggregationOptions;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Order;
import org.bson.Document;
import org.testng.annotations.Test;

import java.util.List;

import static org.bson.Document.parse;
import static org.testng.Assert.assertNotNull;

public class TestPlanCacheStats extends AggregationTest {
    @Test
    public void testPlanCacheStats() {
        checkMinServerVersion(4.2);
        List<Document> list = parseDocs(
            "{ '_id' : 1, 'item' : 'abc', 'price' : NumberDecimal('12'), 'quantity' : 2, 'type': 'apparel' }",
            "{ '_id' : 2, 'item' : 'jkl', 'price' : NumberDecimal('20'), 'quantity' : 1, 'type': 'electronics' }",
            "{ '_id' : 3, 'item' : 'abc', 'price' : NumberDecimal('10'), 'quantity' : 5, 'type': 'apparel' }",
            "{ '_id' : 4, 'item' : 'abc', 'price' : NumberDecimal('8'), 'quantity' : 10, 'type': 'apparel' }",
            "{ '_id' : 5, 'item' : 'jkl', 'price' : NumberDecimal('15'), 'quantity' : 15, 'type': 'electronics' }");

        MongoCollection<Document> orders = getDatabase().getCollection("orders");
        insert("orders", list);

        assertNotNull(orders.createIndex(new Document("item", 1)));
        assertNotNull(orders.createIndex(new Document("item", 1)
                                             .append("quantity", 1)));
        assertNotNull(orders.createIndex(new Document("item", 1)
                                             .append("price", 1),
            new IndexOptions()
                .partialFilterExpression(new Document("price", new Document("$gte", 10)))));
        assertNotNull(orders.createIndex(new Document("quantity", 1)));
        assertNotNull(orders.createIndex(new Document("quantity", 1)
                                             .append("type", 1)));

        orders.find(parse(" { item: 'abc', price: { $gte: NumberDecimal('10') } }"));
        orders.find(parse(" { item: 'abc', price: { $gte: NumberDecimal('5') } }"));
        orders.find(parse(" { quantity: { $gte: 20 } } "));
        orders.find(parse(" { quantity: { $gte: 5 }, type: 'apparel' } "));

        List<Document> stats = getDs().aggregate(Order.class)
                                      .planCacheStats()
                                      .execute(Document.class, new AggregationOptions()
                                                                   .readConcern(ReadConcern.LOCAL))
                                      .toList();

        assertNotNull(stats);
    }

}
