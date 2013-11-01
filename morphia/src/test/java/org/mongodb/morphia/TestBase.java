package org.mongodb.morphia;


import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.junit.After;
import org.junit.Before;
import org.mongodb.morphia.mapping.MappedClass;


public abstract class TestBase {
    private final Mongo mongo;
    private DB db;
    private Datastore ds;
    private AdvancedDatastore ads;
    private final Morphia morphia = new Morphia();

    protected TestBase() {
        try {
            mongo = new MongoClient(new MongoClientURI(System.getProperty("MONGO_URI", "mongodb://localhost:27017")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() {
        setDb(getMongo().getDB("morphia_test"));
        setDs(getMorphia().createDatastore(getMongo(), getDb().getName()));
        setAds((AdvancedDatastore) getDs());
    }

    protected void cleanup() {
        for (final MappedClass mc : getMorphia().getMapper().getMappedClasses()) {
            getDb().getCollection(mc.getCollectionName()).drop();
        }

    }

    @After
    public void tearDown() {
        cleanup();
        getMongo().close();
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

    public Mongo getMongo() {
        return mongo;
    }

    public Morphia getMorphia() {
        return morphia;
    }

    public void setDb(final DB db) {
        this.db = db;
    }

    public void setDs(final Datastore ds) {
        this.ds = ds;
    }

    public void setAds(final AdvancedDatastore ads) {
        this.ads = ads;
    }
}
