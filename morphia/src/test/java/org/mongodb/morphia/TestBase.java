package org.mongodb.morphia;


import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.junit.After;
import org.junit.Assume;
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

    /**
     * @param version  must be a major version, e.g. 1.8, 2,0, 2.2
     * @return true if server is at least specified version
     */
    protected boolean serverIsAtLeastVersion(final double version) {
        String serverVersion = (String) getMongo().getDB("admin").command("serverStatus").get("version");
        return Double.parseDouble(serverVersion.substring(0, 3)) >= version;
    }

    protected void checkMinServerVersion(final double version) {
        Assume.assumeTrue(serverIsAtLeastVersion(version));
    }
    
    /**
     * @param version  must be a major version, e.g. 1.8, 2,0, 2.2
     * @return true if server is at least specified version
     */
    protected boolean serverIsAtMostVersion(final double version) {
        String serverVersion = (String) getMongo().getDB("admin").command("serverStatus").get("version");
        return Double.parseDouble(serverVersion.substring(0, 3)) <= version;
    }

    protected void checkMaxServerVersion(final double version) {
        Assume.assumeTrue(serverIsAtMostVersion(version));
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
