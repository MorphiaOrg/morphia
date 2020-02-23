package dev.morphia;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.query.DefaultQueryFactory;
import org.bson.Document;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings("WeakerAccess")
public abstract class TestBase {
    protected static final String TEST_DB_NAME = "morphia_test";
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final Datastore ds;
    private final MapperOptions mapperOptions = MapperOptions.DEFAULT;

    protected TestBase() {
        Builder builder = MongoClientSettings.builder();

        try {
            builder.uuidRepresentation(mapperOptions.getUuidRepresentation());
        } catch(Exception ignored) {
            // not a 4.0 driver
        }

        MongoClientSettings clientSettings = builder
                               .applyConnectionString(new ConnectionString(getMongoURI()))
                                                                .build();

        this.mongoClient = MongoClients.create(clientSettings);
        this.database = getMongoClient().getDatabase(TEST_DB_NAME);
        this.ds = Morphia.createDatastore(getMongoClient(), database.getName());
        ds.setQueryFactory(new DefaultQueryFactory());
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

    protected void assertDocumentEquals(final Object expected, final Object actual) {
        assertDocumentEquals("", expected, actual);
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

    protected int count(final MongoCursor<?> cursor) {
        int count = 0;
        while (cursor.hasNext()) {
            cursor.next();
            count++;
        }
        return count;
    }

    protected int count(final Iterator<?> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            count++;
            iterator.next();
        }
        return count;
    }

    protected MongoCollection<Document> getDocumentCollection(final Class<?> type) {
        return getDatabase().getCollection(getMappedClass(type).getCollectionName());
    }

    /**
     * @param version must be a major version, e.g. 1.8, 2,0, 2.2
     * @return true if server is at least specified version
     */
    protected boolean serverIsAtLeastVersion(final double version) {
        return getServerVersion() >= version;
    }

    protected List<Document> getIndexInfo(final Class<?> clazz) {
        return getMapper().getCollection(clazz).listIndexes().into(new ArrayList<>());
    }

    protected MappedClass getMappedClass(final Class<?> aClass) {
        Mapper mapper = getMapper();
        mapper.map(aClass);

        return mapper.getMappedClass(aClass);
    }

    protected String toString(final Document document) {
        return document.toJson(getMapper().getCodecRegistry().get(Document.class));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertDocumentEquals(final String path, final Object expected, final Object actual) {
        assertSameNullity(path, expected, actual);
        if (expected == null) {
            return;
        }
        assertSameType(path, expected, actual);

        if (expected instanceof Document) {
            for (final Entry<String, Object> entry : ((Document) expected).entrySet()) {
                final String key = entry.getKey();
                Object expectedValue = entry.getValue();
                Object actualValue = ((Document) actual).get(key);
                assertDocumentEquals(path + "." + key, expectedValue, actualValue);
            }
        } else if (expected instanceof List) {
            List list = (List) expected;
            List copy = new ArrayList<>((List) actual);

            Object o;
            for (int i = 0; i < list.size(); i++) {
                o = list.get(i);
                boolean found = false;
                final Iterator other = copy.iterator();
                while (!found && other.hasNext()) {
                    try {
                        String newPath = format("%s[%d]", path, i);
                        assertDocumentEquals(newPath, o, other.next());
                        other.remove();
                        found = true;
                    } catch (AssertionError ignore) {
                    }
                }
                if (!found) {
                    fail(format("mismatch found at %s", path));
                }
            }
        } else {
            assertEquals(format("mismatch found at %s:%n%s", path, expected, actual), expected, actual);
        }
    }

    private void assertSameNullity(final String path, final Object expected, final Object actual) {
        if (expected == null && actual != null
            || actual == null && expected != null) {
            assertEquals(format("mismatch found at %s:%n%s", path, expected, actual), expected, actual);
        }
    }

    private void assertSameType(final String path, final Object expected, final Object actual) {
        if (expected instanceof List && actual instanceof List) {
            return;
        }
        if (!expected.getClass().equals(actual.getClass())) {
            assertEquals(format("mismatch found at %s:%n%s", path, expected, actual), expected, actual);
        }
    }

    protected double getServerVersion() {
        String version = (String) getMongoClient()
                                      .getDatabase("admin")
                                      .runCommand(new Document("serverStatus", 1))
                                      .get("version");
        return Double.parseDouble(version.substring(0, 3));
    }

    private Document runIsMaster() {
        return mongoClient.getDatabase("admin")
                          .runCommand(new Document("ismaster", 1));
    }

    public Document obj(final String key, final Object value) {
        return new Document(key, value);
    }
}
