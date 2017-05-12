package org.mongodb.morphia.query;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

public class UnmapedFieldTest extends TestBase {

    @Test
    public void getSaveAndLoadNestedCollections() throws Exception {
        getMorphia().map(Class1.class);
        getDs().ensureIndexes(true);

        getDs().getDB().getCollection("user").save(
            new BasicDBObject()
                .append("@class", Class1.class.getName())
                .append("value1", "foo")
                .append("someMap", new BasicDBObject("someKey", "value"))
        );

        Query<Class1> query = getDs().createQuery(Class1.class);
        query.disableValidation().criteria("someMap.someKey").equal("value");
        Class1 retrievedValue = query.get();
        Assert.assertNotNull(retrievedValue);
        Assert.assertEquals("foo", retrievedValue.value1);
    }

    @Entity(value = "user", noClassnameStored = true)
    public static class Class1 {
        @Id
        private ObjectId id;

        private String value1;

    }

}
