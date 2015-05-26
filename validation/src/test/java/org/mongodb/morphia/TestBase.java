package org.mongodb.morphia;


import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.junit.After;
import org.junit.Before;


@SuppressWarnings("deprecation")
public abstract class TestBase {
    private MongoClient mongoClient;
    private DB db;
    private Datastore ds;
    private Morphia morphia = new Morphia();

    protected TestBase() {
        try {
            this.mongoClient = new MongoClient();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() {
        this.mongoClient.dropDatabase("morphia_test");
        this.db = this.mongoClient.getDB("morphia_test");
        this.ds = this.morphia.createDatastore(this.mongoClient, this.db.getName());
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

    public Morphia getMorphia() {
        return morphia;
    }
}
