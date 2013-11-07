package org.mongodb.morphia;


import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.junit.After;
import org.junit.Before;


public abstract class TestBase {
    private Mongo mongo;
    private DB db;
    private Datastore ds;
    private Morphia morphia = new Morphia();

    protected TestBase() {
        try {
            this.mongo = new MongoClient();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() {
        this.mongo.dropDatabase("morphia_test");
        this.db = this.mongo.getDB("morphia_test");
        this.ds = this.morphia.createDatastore(this.mongo, this.db.getName());
    }

    @After
    public void tearDown() {
        // new ScopedFirstLevelCacheProvider().release();
    }

    public DB getDb() {
        return db;
    }

    public Datastore getDs() {
        return ds;
    }

    public Mongo getMongo() {
        return mongo;
    }

    public Morphia getMorphia() {
        return morphia;
    }
}
