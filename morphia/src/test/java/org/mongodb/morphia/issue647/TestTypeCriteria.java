package org.mongodb.morphia.issue647;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Type;

public class TestTypeCriteria extends TestBase {

    @Test
    public void getStringTypeData() throws Exception {
        Class1 entity = new Class1();
        entity.firstName = "first_name";
        getDs().save(entity);

        getMorphia().map(Class1.class);

        Query<Class1> query = getDs().createQuery(Class1.class);
        query.criteria("first_name").type(Type.STRING);
        Assert.assertTrue(query.asList().size() > 0);
    }

    @Entity(value = "user", noClassnameStored = true)
    public static class Class1 {
        @Id
        private ObjectId id;

        @Property("first_name")
        private String firstName;

        @Property("last_name")
        private String lastName;

        private boolean status;

        @Property("create_date")
        private long createDt;

    }

}
