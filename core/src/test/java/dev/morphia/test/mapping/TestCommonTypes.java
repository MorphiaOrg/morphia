package dev.morphia.test.mapping;

import com.mongodb.client.MongoCollection;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestCommonTypes extends TestBase {
    @Test
    public void testBigDecimal() {
        getDs().getMapper().map(Money.class);

        Money money = new Money();
        money.amount = new BigDecimal("123456.7890");

        getDs().save(money);
        assertNotNull(getDs().find(Money.class).first());

        MongoCollection<Document> collection = getDs()
            .getCollection(Money.class)
            .withDocumentClass(Document.class);
        Document document = new Document("_id", new ObjectId())
            .append("_t", "Money")
            .append("amount", "123456.7890");
        collection.insertOne(document);

        List<Money> monies = getDs().find(Money.class).iterator().toList();

        assertEquals(monies.get(0).amount, monies.get(1).amount);
    }

    @Entity
    private static class Money {
        @Id
        private ObjectId id;
        private BigDecimal amount;
    }
}
