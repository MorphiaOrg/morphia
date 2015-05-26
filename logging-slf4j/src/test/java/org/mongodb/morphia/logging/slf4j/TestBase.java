package org.mongodb.morphia.logging.slf4j;


import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.junit.Before;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;


@SuppressWarnings("deprecation")
public abstract class TestBase {

    private MongoClient mongoClient;
    private DB db;
    private Datastore ds;
    private Morphia morphia;

    @Before
    public void setUp() {
        try {
            this.mongoClient = new MongoClient();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        this.mongoClient.dropDatabase("morphia_test");
        morphia = new Morphia();
        this.db = this.mongoClient.getDB("morphia_test");
        this.ds = this.morphia.createDatastore(this.mongoClient, this.db.getName());
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
