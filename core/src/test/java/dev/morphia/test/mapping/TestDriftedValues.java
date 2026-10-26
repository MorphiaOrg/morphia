package dev.morphia.test.mapping;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

/**
 * Values stored with a type their property's codec can't read fall back to a generic decode and are then converted to
 * the property's type.
 */
public class TestDriftedValues extends TestBase {
    @Test
    public void constructorArgumentConvertsThroughPropertyType() {
        Initial initial = getDs().save(new Initial(new ObjectId(), 'a'));
        rawCollection(Initial.class).updateOne(new Document("_id", initial.id),
                new Document("$set", new Document("letter", 7)));

        Initial loaded = getDs().find(Initial.class).filter(eq("_id", initial.id)).first();

        Assertions.assertNotNull(loaded);
        Assertions.assertEquals("7", loaded.letter);
    }

    private com.mongodb.client.MongoCollection<Document> rawCollection(Class<?> type) {
        return getDatabase().getCollection(getDs().getCollection(type).getNamespace().getCollectionName());
    }

    @Entity
    private static class Initial {
        @Id
        private final ObjectId id;
        private final String letter;

        // no constructor matches the property types, so the creator falls back to this one, matched by name
        Initial(ObjectId id, Character letter) {
            this.id = id;
            this.letter = String.valueOf(letter);
        }
    }
}
