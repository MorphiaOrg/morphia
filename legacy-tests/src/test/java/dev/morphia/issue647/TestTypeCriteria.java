package dev.morphia.issue647;

import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.query.Query;
import dev.morphia.query.Type;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import static dev.morphia.query.experimental.filters.Filters.type;

public class TestTypeCriteria extends TestBase {

    @Test
    public void getStringTypeData() {
        Class1 entity = new Class1();
        entity.firstName = "first_name";
        getDs().save(entity);

        getMapper().map(Class1.class);

        Query<Class1> query = getDs().find(Class1.class);
        query.filter(type( "first_name", Type.STRING));
        Assert.assertTrue(query.count() > 0);
    }

    @Entity(value = "user", useDiscriminator = false)
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
