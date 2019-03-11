package dev.morphia;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings({"deprecation", "WeakerAccess"})
public abstract class TestBase {
    protected static final String TEST_DB_NAME = "morphia_test";
    private final MongoClient mongoClient;
    private final Morphia morphia = new Morphia();
    private final DB db;
    private final MongoDatabase database;
    private final Datastore ds;

    protected TestBase() {
        this(new MongoClient(new MongoClientURI(getMongoURI())));
    }

    protected TestBase(final MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        this.db = getMongoClient().getDB(TEST_DB_NAME);
        this.database = getMongoClient().getDatabase(TEST_DB_NAME);
        this.ds = getMorphia().createDatastore(getMongoClient(), getDb().getName());
    }

    protected static String getMongoURI() {
        return System.getProperty("MONGO_URI", "mongodb://localhost:27017");
    }

    public AdvancedDatastore getAds() {
        return (AdvancedDatastore) getDs();
    }

    public DB getDb() {
        return db;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public Datastore getDs() {
        return ds;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public Morphia getMorphia() {
        return morphia;
    }

    public boolean isReplicaSet() {
        return runIsMaster().get("setName") != null;
    }

    @Before
    public void setUp() {
        cleanup();
    }

    @After
    public void tearDown() {
        cleanup();
        getMongoClient().close();
    }

    protected void checkMinServerVersion(final double version) {
        Assume.assumeTrue(serverIsAtLeastVersion(version));
    }

    protected void cleanup() {
        DB db = getDb();
        if (db != null) {
            db.dropDatabase();
        }
    }

    protected int count(final Iterator<?> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            count++;
            iterator.next();
        }
        return count;
    }

    /**
     * @param version must be a major version, e.g. 1.8, 2,0, 2.2
     * @return true if server is at least specified version
     */
    protected boolean serverIsAtLeastVersion(final double version) {
        String serverVersion = (String) getMongoClient().getDB("admin").command("serverStatus").get("version");
        return Double.parseDouble(serverVersion.substring(0, 3)) >= version;
    }

    /**
     * @param version must be a major version, e.g. 1.8, 2,0, 2.2
     * @return true if server is at least specified version
     */
    protected boolean serverIsAtMostVersion(final double version) {
        String serverVersion = (String) getMongoClient().getDB("admin").command("serverStatus").get("version");
        return Double.parseDouble(serverVersion.substring(0, 3)) <= version;
    }

    private CommandResult runIsMaster() {
        // Check to see if this is a replica set... if not, get out of here.
        return mongoClient.getDB("admin").command(new BasicDBObject("ismaster", 1));
    }

    public BasicDBObject obj(final String key, final Object value) {
        return new BasicDBObject(key, value);
    }

    protected static <E> List<E> toList(final MongoCursor<E> cursor) {
        final List<E> results = new ArrayList<E>();
        try {
            while (cursor.hasNext()) {
                results.add(cursor.next());
            }
        } finally {
            cursor.close();
        }
        return results;
    }

}
