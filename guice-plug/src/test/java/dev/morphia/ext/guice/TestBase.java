package dev.morphia.ext.guice;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import org.junit.After;
import org.junit.Before;

public abstract class TestBase {
    private final MongoClient mongoClient;

    private MongoDatabase db;
    private Datastore ds;
    private Mapper mapper;

    protected TestBase() {
        try {
            mongoClient = new MongoClient();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Datastore getDs() {
        return ds;
    }

    public Mapper getMapper() {
        return mapper;
    }

    @Before
    public void setUp() {
        db = mongoClient.getDatabase("morphia_test");
        ds = Morphia.createDatastore(this.mongoClient, this.db.getName());
        mapper = ds.getMapper();
    }

    @After
    public void tearDown() {
        dropDB();
    }

    protected void dropDB() {
        // this.mongoClient.dropDatabase("morphia_test");
        for (final MappedClass mc : mapper.getMappedClasses()) {
            // if( mc.getEntityAnnotation() != null )
            db.getCollection(mc.getCollectionName()).drop();
        }

    }
}
