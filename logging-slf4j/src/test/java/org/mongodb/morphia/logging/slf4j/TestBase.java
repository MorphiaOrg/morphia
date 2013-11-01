package org.mongodb.morphia.logging.slf4j;


import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.junit.Before;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;


public abstract class TestBase {

    private Mongo mongo;
    private DB db;
    private Datastore ds;
    private Morphia morphia;

    @Before
    public void setUp() {
        try {
            this.mongo = new MongoClient();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        this.mongo.dropDatabase("morphia_test");
        morphia = new Morphia();
        this.db = this.mongo.getDB("morphia_test");
        this.ds = this.morphia.createDatastore(this.mongo, this.db.getName());
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
