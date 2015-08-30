package org.mongodb.morphia.logging.log4j;


import org.junit.Before;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;


public abstract class TestBase {

    private MongoDatabase db;
    private Datastore ds;
    private Morphia morphia;

    @Before
    public void setUp() {
        final MongoClient mongoClient;
        try {
            mongoClient = new MongoClient();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        mongoClient.dropDatabase("morphia_test");
        morphia = new Morphia();
        this.db = mongoClient.getDatabase("morphia_test");
        this.ds = this.morphia.createDatastore(mongoClient, this.db.getName());
    }

    public MongoDatabase getDb() {
        return db;
    }

    public Datastore getDs() {
        return ds;
    }

    public Morphia getMorphia() {
        return morphia;
    }
}
