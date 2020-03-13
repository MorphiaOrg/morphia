package dev.morphia.ext.guice;

import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import org.junit.After;
import org.junit.Before;

public abstract class TestBase {
    private MongoDatabase db;
    private Datastore ds;
    private Mapper mapper;

    public Datastore getDs() {
        return ds;
    }

    public Mapper getMapper() {
        return mapper;
    }

    @Before
    public void setUp() {
        ds = Morphia.createDatastore("morphia_test");
        db = ds.getDatabase();
        mapper = ds.getMapper();
    }

    @After
    public void tearDown() {
        dropDB();
    }

    protected void dropDB() {
        db.drop();
    }
}
