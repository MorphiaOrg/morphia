package org.mongodb.morphia;


import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Date;
import java.util.List;

public class TestExpireAfterSeconds extends TestBase {

    @Entity
    public static class HasExpiryField {
        @Id
        private ObjectId id;
        @Indexed(expireAfterSeconds = 5)
        private final Date offerExpiresAt = new Date();
    }

    @Entity
    @Indexes({
                 @Index(value = "offerExpiresAt", expireAfterSeconds = 5)
             })
    public static class ClassAnnotation {
        @Id
        private ObjectId id;
        private final Date offerExpiresAt = new Date();
    }

    @Test
    public void testIndexedField() {
        getMorphia().map(HasExpiryField.class);
        getDs().ensureIndexes();

        getDs().save(new HasExpiryField());

        final DB db = getDs().getDB();
        final DBCollection dbCollection = db.getCollection("HasExpiryField");
        final List<DBObject> indexes = dbCollection.getIndexInfo();

        Assert.assertNotNull(indexes);
        Assert.assertEquals(2, indexes.size());
        DBObject index = null;
        for (final DBObject candidateIndex : indexes) {
            if (candidateIndex.containsField("expireAfterSeconds")) {
                index = candidateIndex;
            }
        }
        Assert.assertNotNull(index);
        Assert.assertEquals(5, index.get("expireAfterSeconds"));
    }

    @Test
    public void testClassAnnotation() {
        getMorphia().map(ClassAnnotation.class);
        getDs().ensureIndexes();

        getDs().save(new ClassAnnotation());

        final DB db = getDs().getDB();
        final DBCollection dbCollection = db.getCollection("ClassAnnotation");
        final List<DBObject> indexes = dbCollection.getIndexInfo();

        Assert.assertNotNull(indexes);
        Assert.assertEquals(2, indexes.size());
        DBObject index = null;
        for (final DBObject candidateIndex : indexes) {
            if (candidateIndex.containsField("expireAfterSeconds")) {
                index = candidateIndex;
            }
        }
        Assert.assertNotNull(index);
        Assert.assertTrue(index.containsField("expireAfterSeconds"));
        Assert.assertEquals(5, index.get("expireAfterSeconds"));
    }
}
