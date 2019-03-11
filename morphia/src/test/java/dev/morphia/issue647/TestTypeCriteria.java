package dev.morphia.issue647;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.query.Query;
import dev.morphia.query.Type;

public class TestTypeCriteria extends TestBase {

    @Test
    public void getStringTypeData() {
        Class1 entity = new Class1();
        entity.firstName = "first_name";
        getDs().save(entity);

        getMorphia().map(Class1.class);

        Query<Class1> query = getDs().find(Class1.class);
        query.criteria("first_name").type(Type.STRING);
        Assert.assertTrue(query.count() > 0);
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
