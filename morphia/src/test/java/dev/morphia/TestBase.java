package dev.morphia;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import org.bson.Document;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public abstract class TestBase {
    protected static final String TEST_DB_NAME = "morphia_test";
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final Datastore ds;

    protected TestBase() {
        this(MongoClients.create(getMongoURI()));
    }

    protected TestBase(final MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        this.database = getMongoClient().getDatabase(TEST_DB_NAME);
        this.ds = Morphia.createDatastore(getMongoClient(), database.getName());
    }

    protected static String getMongoURI() {
        return System.getProperty("MONGO_URI", "mongodb://localhost:27017");
    }

    public AdvancedDatastore getAds() {
        return (AdvancedDatastore) getDs();
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

    public Mapper getMapper() {
        return getDs().getMapper();
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
        MongoDatabase db = getDatabase();
        if (db != null) {
            db.drop();
        }
    }

    protected int count(final MongoIterable<?> iterable) {
        MongoCursor<?> iterator = iterable.iterator();
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
        return getServerVersion() >= version;
    }

    /**
     * @param version must be a major version, e.g. 1.8, 2,0, 2.2
     * @return true if server is at least specified version
     */
    protected boolean serverIsAtMostVersion(final double version) {
        return getServerVersion() <= version;
    }

    protected List<Document> getIndexInfo(final Class<?> clazz) {
        return getDs().getCollection(clazz).listIndexes().into(new ArrayList<>());
    }

    protected MappedClass getMappedClass(final Class<?> aClass) {
        Mapper mapper = getMapper();
        mapper.map(aClass);

        return mapper.getMappedClass(aClass);
    }

    protected String toString(final Document document) {
        return document.toJson(getMapper().getCodecRegistry().get(Document.class));
    }

    private double getServerVersion() {
        String version = (String) getMongoClient()
                                      .getDatabase("admin")
                                      .runCommand(new Document("serverStatus", 1))
                                      .get("version");
        return Double.parseDouble(version.substring(0, 3));
    }

    private Document runIsMaster() {
        throw new UnsupportedOperationException();
//        return mongoClient.getDatabase("admin").runCommand(new Document("ismaster", 1));
    }

    public Document obj(final String key, final Object value) {
        return new Document(key, value);
    }
}
