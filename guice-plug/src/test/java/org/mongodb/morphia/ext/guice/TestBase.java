package org.mongodb.morphia.ext.guice;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.junit.After;
import org.junit.Before;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;

@SuppressWarnings("deprecation")
public abstract class TestBase {
    private final MongoClient mongoClient;
    private final Morphia morphia = new Morphia();
    
    private DB db;
    private Datastore ds;
    private AdvancedDatastore ads;

    protected TestBase() {
        try {
            mongoClient = new MongoClient();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() {
        db = mongoClient.getDB("morphia_test");
        ds = morphia.createDatastore(this.mongoClient, this.db.getName());
        ads = (AdvancedDatastore) this.ds;
    }

    public AdvancedDatastore getAds() {
        return ads;
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

    protected void dropDB() {
        // this.mongoClient.dropDatabase("morphia_test");
        for (final MappedClass mc : morphia.getMapper().getMappedClasses()) {
            // if( mc.getEntityAnnotation() != null )
            db.getCollection(mc.getCollectionName()).drop();
        }

    }

    @After
    public void tearDown() {
        dropDB();
    }
}
