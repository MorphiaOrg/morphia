package org.mongodb.morphia.issue647;

import java.net.UnknownHostException;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Type;

import com.mongodb.MongoClient;

public class TestTypeCriteria extends TestBase {

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


    @Test
    public void getStringTypeData() throws Exception {
        getMorphia().map(Class1.class);

        Morphia morphia =  getMorphia().map(Class1.class);
        MongoClient mongoClient = new MongoClient("localhost");
        Datastore ds = morphia.createDatastore(mongoClient, "test");

        Query<Class1> query = ds.createQuery(Class1.class);
        query.criteria("first_name").type(Type.STRING);
        Assert.assertTrue(query.asList().size() > 0);
    }

    
    @Test
    public void getArrayTypeData() throws Exception {
        getMorphia().map(Class1.class);

        Morphia morphia =  getMorphia().map(Class1.class);
        MongoClient mongoClient = new MongoClient("localhost");
        Datastore ds = morphia.createDatastore(mongoClient, "test");

        Query<Class1> query = ds.createQuery(Class1.class);
        query.criteria("first_name").type(Type.DOUBLE);
        Assert.assertTrue(query.asList().size() > 0);
    }
}
